#ifndef ANDROID_AV_AACCODEC_H
#define ANDROID_AV_AACCODEC_H

#include "../utils/Config.h"
#include "fdk-aac/aacenc_lib.h"

typedef void (*AudioCodecCallback)(void *attachment, RTMPPacket *rtmpPacket);


class AACCodec {

private:
    AudioCodecCallback _audioCodecCallback = nullptr;

public:

    void setCodecCallback(AudioCodecCallback audioCodecCallback) {
        this->_audioCodecCallback = audioCodecCallback;
    }

    void initCodec(int sampleRate, int channels) {

    }

    void encodeData(int8_t *data, int len, void *attachment) {

    }

};

#endif //ANDROID_AV_AACCODEC_H
