#ifndef ANDROID_AV_X264PARSER_H
#define ANDROID_AV_X264PARSER_H

#include <jni.h>
#include <rtmp.h>
#include "../utils/Log.h"

typedef struct {
    int16_t sps_len;
    int16_t pps_len;
    int8_t *sps;
    int8_t *pps;
} X264Header;

class X264Parser {

private:
    X264Header *x264Header = nullptr;

    bool isSPS(int8_t *data, int offset) {
        return (data[offset] & 0x1F) == 7;
    }

    bool isPPS(int8_t *data, int offset) {
        return (data[offset] & 0x1F) == 8;
    }

    bool isIFrame(int8_t *data, int offset) {
        return (data[offset] & 0x1F) == 5;
    }

    void preserveHeader(int8_t *data, int len) {
        for (int i = 0; i < len; i++) {
            //x264 编码，NALU 分隔符：0x00 0x00 0x00 0x01
            if (i + 4 < len) {

                if (data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x00 && data[i + 3] == 0x01) {//找到分隔符
                    //将 sps pps 分开，作为类型，sps 和 pps 占用一个字节中的后 5 位
                    //01100111 --> 0x00 0x01 0x00 | 0x00 0x01 0x01 0x01 7 是 sps
                    //01101000 --> 0x00 0x01 0x00 | 0x01 0x00 0x00 0x00  8 是 pps

                    if (isPPS(data, i + 4)) {
                        LOGI("定位到 PPS");
                        //找到 pps，运行到这里，已经跨过了 sps

                        //保存 sps 数据
                        LOGI("保存 sps 数据");
                        x264Header->sps_len = i - 4;
                        x264Header->sps = static_cast<int8_t *>(malloc(x264Header->sps_len));
                        memcpy(x264Header->sps, data + 4, x264Header->sps_len);

                        //保存 pps 数据
                        LOGI("保存 pps 数据");
                        x264Header->pps_len = len - (4 + x264Header->sps_len) - 4;
                        x264Header->pps = static_cast<int8_t *>(malloc(x264Header->pps_len));
                        memcpy(x264Header->pps, data + 4 + x264Header->sps_len + 4, x264Header->pps_len);
                        LOGE("sps: %d pps: %d", x264Header->sps_len, x264Header->pps_len);
                        break;
                    }
                }
            }
        }
    }

    RTMPPacket *createSSP_PPSFrame() {
        int body_size = 13 + x264Header->sps_len + 3 + x264Header->pps_len;
        RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
        RTMPPacket_Alloc(packet, body_size);

        int i = 0;
        //AVC sequence header 与IDR一样
        packet->m_body[i++] = 0x17;
        //AVC sequence header 设置为0x00
        packet->m_body[i++] = 0x00;
        //CompositionTime
        packet->m_body[i++] = 0x00;
        packet->m_body[i++] = 0x00;
        packet->m_body[i++] = 0x00;
        //AVC sequence header
        packet->m_body[i++] = 0x01;   //configurationVersion 版本号 1
        packet->m_body[i++] = x264Header->sps[1]; //profile 如 baseline、main、 high

        packet->m_body[i++] = x264Header->sps[2]; //profile_compatibility 兼容性
        packet->m_body[i++] = x264Header->sps[3]; //profile level
        packet->m_body[i++] = 0xFF; // reserved（111111） + lengthSizeMinusOne（2位 nal 长度） 总是0xff
        //sps
        packet->m_body[i++] = 0xE1; //reserved（111） + lengthSizeMinusOne（5位 sps 个数） 总是0xe1
        //sps length 2字节
        packet->m_body[i++] = (x264Header->sps_len >> 8) & 0xff; //第0个字节
        packet->m_body[i++] = x264Header->sps_len & 0xff;          //第1个字节
        memcpy(&packet->m_body[i], x264Header->sps, x264Header->sps_len);
        i += x264Header->sps_len;

        /*pps*/
        packet->m_body[i++] = 0x01; //pps number
        //pps length
        packet->m_body[i++] = (x264Header->pps_len >> 8) & 0xff;
        packet->m_body[i++] = x264Header->pps_len & 0xff;
        memcpy(&packet->m_body[i], x264Header->pps, x264Header->pps_len);

        packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
        packet->m_nBodySize = body_size;
        packet->m_nChannel = RTMP_VIDEO_CHANNEL;
        packet->m_nTimeStamp = 0;
        packet->m_hasAbsTimestamp = 0;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        return packet;
    }

    RTMPPacket *createDataFrame(int8_t *buf, int len, long tms) {
        //去掉分隔符
        buf += 4;
        len -= 4;

        //准备 Packet
        int body_size = len + 9;
        RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
        RTMPPacket_Alloc(packet, len + 9);

        //填充数据
        packet->m_body[0] = 0x27;
        if (buf[0] == 0x65) { //关键帧
            packet->m_body[0] = 0x17;
        }
        packet->m_body[1] = 0x01;
        packet->m_body[2] = 0x00;
        packet->m_body[3] = 0x00;
        packet->m_body[4] = 0x00;

        //长度
        packet->m_body[5] = (len >> 24) & 0xff;
        packet->m_body[6] = (len >> 16) & 0xff;
        packet->m_body[7] = (len >> 8) & 0xff;
        packet->m_body[8] = (len) & 0xff;

        //数据
        memcpy(&packet->m_body[9], buf, len);

        //其他描述数据
        packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
        packet->m_nBodySize = body_size;
        packet->m_nChannel = RTMP_VIDEO_CHANNEL;
        packet->m_nTimeStamp = tms;
        packet->m_hasAbsTimestamp = 0;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        return packet;
    }

public:
    X264Parser() {
        x264Header = (X264Header *) malloc(sizeof(X264Header));
    }

    ~X264Parser() {
        free(x264Header);
    }

    void parsePacket(int8_t *buf, int len, long tms, SafeQueue<RTMPPacket *> *queue) {
        if (isSPS(buf, 4)) {
            LOGI("遇到 SPS");
            preserveHeader(buf, len);
            return;
        }
        if (isIFrame(buf, 4)) {
            LOGI("遇到 I Frame, %ld", tms);
            queue->push(createSSP_PPSFrame());
        }
        queue->push(createDataFrame(buf, len, tms));
    }

};

#endif //ANDROID_AV_X264PARSER_H
