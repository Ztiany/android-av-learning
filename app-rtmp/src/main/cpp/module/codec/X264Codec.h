#ifndef ANDROID_AV_X264CODEC_H
#define ANDROID_AV_X264CODEC_H

#include <x264/x264.h>
#include <rtmp.h>
#include "../utils/Config.h"

typedef void (*VideoCodecCallback)(void *attachment, RTMPPacket *rtmpPacket);

class X264Codec {

private:
    int _width = 0;
    int _height = 0;

    x264_picture_t *_pic_in = nullptr;
    int _ySize = 0;
    int _uvSize = 0;

    x264_t *_videoCodec = nullptr;

    VideoCodecCallback _videoCodecCallback = nullptr;

private:
    void clean() {
        if (_pic_in) {
            x264_picture_clean(_pic_in);
            delete _pic_in;
        }
        if (_videoCodec) {
            x264_encoder_close(_videoCodec);
            _videoCodec = nullptr;
        }
    }

public:
    X264Codec() = default;

    ~X264Codec() {
        clean();
    }

private:
    int calcUVSize(int format) {
        if (I420 == format) {
            return _ySize / 4;
        }
        //TODO：兼容其他格式
        return _ySize / 4;
    }

    void copyYUVtoImg(int8_t *data) {
        //TODO：兼容其他格式
        //复制一帧的数据到 img
        //y数据
        memcpy(_pic_in->img.plane[0], data, _ySize);
        //u数据
        memcpy(_pic_in->img.plane[1], data + _ySize, _uvSize);
        //v数据
        memcpy(_pic_in->img.plane[2], data + _ySize + _uvSize, _uvSize);
    }

    RTMPPacket *createSpsPpsPacket(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {
        RTMPPacket *packet = new RTMPPacket;
        int bodySize = 13 + sps_len + 3 + pps_len;
        RTMPPacket_Alloc(packet, bodySize);

        int index = 0;
        //固定头
        packet->m_body[index++] = 0x17;
        //类型
        packet->m_body[index++] = 0x00;
        //composition time 0x000000
        packet->m_body[index++] = 0x00;
        packet->m_body[index++] = 0x00;
        packet->m_body[index++] = 0x00;

        //版本
        packet->m_body[index++] = 0x01;
        //编码规格
        packet->m_body[index++] = sps[1];
        packet->m_body[index++] = sps[2];
        packet->m_body[index++] = sps[3];
        packet->m_body[index++] = 0xFF;

        //整个sps
        packet->m_body[index++] = 0xE1;
        //sps长度
        packet->m_body[index++] = (sps_len >> 8) & 0xff;
        packet->m_body[index++] = sps_len & 0xff;
        memcpy(&packet->m_body[index], sps, sps_len);
        index += sps_len;

        //pps
        packet->m_body[index++] = 0x01;
        packet->m_body[index++] = (pps_len >> 8) & 0xff;
        packet->m_body[index++] = (pps_len) & 0xff;
        memcpy(&packet->m_body[index], pps, pps_len);

        //视频
        packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
        packet->m_nBodySize = bodySize;
        //随意分配一个管道（尽量避开 rtmp.c 中使用的）
        packet->m_nChannel = RTMP_VIDEO_CHANNEL;
        //sps pps没有时间戳
        packet->m_nTimeStamp = 0;
        //不使用绝对时间
        packet->m_hasAbsTimestamp = 0;
        packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;

        return packet;
    }

    RTMPPacket *createFramePacket(int type, int length, uint8_t *data) {
        //去掉 00 00 00 01 / 00 00 01
        if (data[2] == 0x00) {
            length -= 4;
            data += 4;
        } else if (data[2] == 0x01) {
            length -= 3;
            data += 3;
        }

        RTMPPacket *packet = new RTMPPacket;
        int bodySize = 9 + length;
        RTMPPacket_Alloc(packet, bodySize);
        RTMPPacket_Reset(packet);
        packet->m_body[0] = 0x27;

        //关键帧
        if (type == NAL_SLICE_IDR) {
            LOGE("关键帧");
            packet->m_body[0] = 0x17;
        }
        //类型
        packet->m_body[1] = 0x01;
        //时间戳
        packet->m_body[2] = 0x00;
        packet->m_body[3] = 0x00;
        packet->m_body[4] = 0x00;
        //数据长度 int 4个字节 相当于把 int 转成 4 个字节的 byte 数组
        packet->m_body[5] = (length >> 24) & 0xff;
        packet->m_body[6] = (length >> 16) & 0xff;
        packet->m_body[7] = (length >> 8) & 0xff;
        packet->m_body[8] = (length) & 0xff;

        //图片数据
        memcpy(&packet->m_body[9], data, length);

        packet->m_hasAbsTimestamp = 0;
        packet->m_nBodySize = bodySize;
        packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
        packet->m_nChannel = RTMP_VIDEO_CHANNEL;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;

        return packet;
    }

public:
    void initCodec(int width, int height, int fps, int bitrate, jint format) {
        clean();
        LOGI("setVideoInfo, w = %d, h = %d, fps = %d, bitrate = %d", width, height, fps, bitrate);

        //save video info
        _width = width;
        _height = height;
        //calc size
        _ySize = _width * _height;
        _uvSize = calcUVSize(format);

        //初始化编码器参数
        x264_param_t param;
        //预设一个规格：ultrafast 表示最快的编码速度，zerolatency 表示 0 延迟。
        x264_param_default_preset(&param, "ultrafast", "zerolatency");

        //设置编码等级，32 就表示 x264 中的 3.2。
        param.i_level_idc = 32;

        //待编码的视频格式
        param.i_csp = X264_CSP_I420;
        param.i_width = width;
        param.i_height = height;

        //码率控制
        //控制模式：一般使用 X264_RC_ABR
        param.rc.i_rc_method = X264_RC_ABR;
        //设置码率
        param.rc.i_bitrate = bitrate / 1024;

        //帧率
        param.i_fps_num = fps;
        param.i_fps_den = 1;
        param.i_timebase_den = param.i_fps_num;
        param.i_timebase_num = param.i_fps_den;

        //B 帧【这里设置为 0，直播中不建议使用 B 帧】
        param.i_bframe = 0;

        //最大 I 帧间隔，直播中进行小一些，因为网络不稳定。
        param.i_keyint_max = fps * 2;

        //使用用 FPS 而不是时间戳来计算帧间距离
        param.b_vfr_input = 0;

        // 是否复制 sps 和 pps 放在每个关键帧的前面,该参数设置是让每个关键帧(I 帧)都附带 sps/pps。
        param.b_repeat_headers = 1;

        //编码的多线程数
        param.i_threads = 1;

        //最后用 baseline 限制
        x264_param_apply_profile(&param, "baseline");

        //=============== 参数设置完毕，打开编码器 ===============

        //打开编码器
        _videoCodec = x264_encoder_open(&param);

        //编码前的数据
        _pic_in = new x264_picture_t;
        //为容器初始化大小，容器大小是确定的。
        x264_picture_alloc(_pic_in, X264_CSP_I420, width, height);

        LOGI("x264 编码器初始化成功");
    }

    void encodeData(int8_t *data, int len, void *attachment) {
        //拷贝 YUV 到 x264 的容器
        copyYUVtoImg(data);

        //编码成 H264 码流
        //	1：定义出参
        int pi_nal; //the number of NAL units outputted in pp_nal
        x264_nal_t *pp_nal; //编码出的数据 H264
        x264_picture_t pic_out;
        //	2：执行编码
        x264_encoder_encode(_videoCodec, &pp_nal, &pi_nal, _pic_in, &pic_out);

        uint8_t sps[100];
        uint8_t pps[100];
        int sps_len, pps_len;

        if (pi_nal > 0) {
            for (int i = 0; i < pi_nal; ++i) {
                if (pp_nal[i].i_type == NAL_SPS) {
                    sps_len = pp_nal[i].i_payload - 4;//丢掉 nal 分隔符
                    //把 sps 存下来
                    memcpy(sps, pp_nal[i].p_payload + 4, sps_len);
                } else if (pp_nal[i].i_type == NAL_PPS) {
                    pps_len = pp_nal[i].i_payload - 4;//丢掉 nal 分隔符
                    //把 pps 拷贝出来
                    memcpy(pps, pp_nal[i].p_payload + 4, pps_len);
                    //发送 sps 和 pps
                    RTMPPacket *packet = createSpsPpsPacket(sps, pps, sps_len, pps_len);
                    if (this->_videoCodecCallback) {
                        this->_videoCodecCallback(attachment, packet);
                    }
                } else {
                    //关键帧、非关键帧
                    // pp_nal[i].p_payload 中存储的是编码后的数据
                    // pp_nal[i].i_payload 表示的是对应 p_payload 的长度。
                    RTMPPacket *packet = createFramePacket(pp_nal[i].i_type, pp_nal[i].i_payload, pp_nal[i].p_payload);
                    if (this->_videoCodecCallback) {
                        this->_videoCodecCallback(attachment, packet);
                    }
                }
            }
        }
    }

    void setCodecCallback(VideoCodecCallback videoCodecCallback) {
        this->_videoCodecCallback = videoCodecCallback;
    }

};

#endif //ANDROID_AV_X264CODEC_H
