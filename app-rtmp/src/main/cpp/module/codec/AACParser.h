#ifndef ANDROID_AV_AACPARSER_H
#define ANDROID_AV_AACPARSER_H

#include "../utils/Config.h"

class AACParser {

public:

    void parsePacket(
            int8_t *buf,
            int len,
            int type,
            long tms,
            SafeQueue<RTMPPacket *> *queue
    ) {

        //准备 Packet
        int body_size = len + 2;
        RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
        RTMPPacket_Alloc(packet, body_size);

        //音频头【用于指导解码器如果解码，下面是写死的，其实应该根据 MediaCodec 产生的 Audio Specific Config 来配置】
        //第 1 个字节
        //前四位表示音频数据格式  10（十进制）表示 AAC，16 进制就是 A。
        //第 5-6 位的数值表示采样率，0 = 5.5 kHz，1 = 11 kHz，2 = 22 kHz，3(11) = 44 kHz。
        //第 7 位表示采样精度，0 = 8bits，1 = 16bits。
        //第 8 位表示音频类型，0 = mono，1 = stereo
        //这里是 44100 立体声=11；16bit 采样=1；立体声=1；所以综合起来二进制就是 1111，16 进制就是 F
        packet->m_body[0] = 0xAF;

        //第二个字节，0x00 表示 aac 头信息【Audio Specific Config 】,  0x01 表示  aac 原始数据。
        //其实 aac 头信息不发送也没关系，因为每个 RTMPPacket 已经包含了音频的解码信息。
        if (type == AAC_INFO) {
            packet->m_body[1] = 0x00;
        } else if (type == AAC_DATA) {
            packet->m_body[1] = 0x01;
        }

        //拷贝实体数据
        memcpy(&packet->m_body[2], buf, len);

        //其他描述信息
        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
        packet->m_nChannel = RTMP_AUDIO_CHANNEL;
        packet->m_nBodySize = body_size;
        packet->m_nTimeStamp = tms;
        packet->m_hasAbsTimestamp = 0;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;

        //添加到队列中
        queue->push(packet);
    }

};

#endif //ANDROID_AV_AACPARSER_H
