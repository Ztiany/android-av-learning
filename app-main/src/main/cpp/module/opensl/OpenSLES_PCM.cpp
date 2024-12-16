#include <jni.h>
#include "OpenSLESKit.h"

static PcmPlayerConfig pcmPlayerConfig;

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_ztiany_androidav_avapi_audio_opensles_OpenSlES_createPCMPlayer(
        JNIEnv *env,
        jobject thiz,
        jint sample_rate,
        jint channel_count,
        jint bits_per_sample
) {

    pcmPlayerConfig.sampleRate = sample_rate;
    pcmPlayerConfig.channelCount = channel_count;
    pcmPlayerConfig.bitsPerSample = bits_per_sample;

    return createPcmPlayer(pcmPlayerConfig);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_ztiany_androidav_avapi_audio_opensles_OpenSlES_startPcmPlayer(JNIEnv *env, jobject thiz, jstring file_path) {
    const char *filePath = env->GetStringUTFChars(file_path, nullptr);
    if (filePath == nullptr) {
        LOGE("startPcmPlayer error for GetStringUTFChars");
        return false;
    }
    PcmPlayConfig pcmPlayConfig = {
            .bufferSize = commonPcmBufferSize(pcmPlayerConfig, 200)
    };
    bool result = startPcmPlayerForFilePath(filePath, pcmPlayConfig);
    env->ReleaseStringUTFChars(file_path, filePath);
    return result;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_ztiany_androidav_avapi_audio_opensles_OpenSlES_pausePcmPlayer(JNIEnv *env, jobject thiz) {
    return pausePcmPlayer();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_ztiany_androidav_avapi_audio_opensles_OpenSlES_resumePcmPlayer(JNIEnv *env, jobject thiz) {
    return resumePcmPlayer();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_ztiany_androidav_avapi_audio_opensles_OpenSlES_stopPcmPlayer(JNIEnv *env, jobject thiz) {
    return stopPcmPlayerForFilePath();
}
 