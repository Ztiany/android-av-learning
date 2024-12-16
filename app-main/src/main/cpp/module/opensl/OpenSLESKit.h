#ifndef ANDROID_AV_OPENSLESKIT_H
#define ANDROID_AV_OPENSLESKIT_H

#include <stdint.h>
#include <string>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <log.h>

//========================================== Common Utils ==========================================

SLuint32 mapSlSampleRate(int32_t sampleRate);

SLuint32 mapSlChannel(int channelCount);

SLuint16 mapSlFormat(int bitsPerSample);

//========================================== PCM Playback ==========================================

struct PcmPlayerConfig {
    int32_t sampleRate;
    int channelCount;
    int bitsPerSample;
};

struct PcmPlayConfig {
    int32_t bufferSize;
};

int32_t commonPcmBufferSize(PcmPlayerConfig &pcmPlayerConfig, int durationPerBufferInMS);

bool createPcmPlayer(PcmPlayerConfig &pcmConfig);

/** 直接播放 PCM 文件，不会开启线程预读 PCM 数据，而是在 OpenSL ES 的回调中读取 PCM 数据并填充到队列中【相对比较低效】 */
bool startPcmPlayerForFilePath(const char *filePath, PcmPlayConfig &pcmPlayConfig);

bool pausePcmPlayer();

bool resumePcmPlayer();

bool stopPcmPlayerForFilePath();

//========================================== PCM Playback ==========================================

#endif //ANDROID_AV_OPENSLESKIT_H
