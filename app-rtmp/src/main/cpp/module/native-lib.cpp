#include <jni.h>
#include <string>
#include "utils/Log.h"
#include "pusher/RtmpPusher.h"

JavaVM *javaVM;
static RtmpPusher *rtmpPusher;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    LOGE("Native 加载完成");
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_ztiany_rtmp_common_RtmpPusher_init(JNIEnv *env, jobject thiz) {
    //check if already started.
    if (rtmpPusher) {
        return;
    }
    rtmpPusher = new RtmpPusher();
    rtmpPusher->initJavaCaller(new JavaCaller(javaVM, env, &thiz));
}

extern "C"
JNIEXPORT void JNICALL
Java_me_ztiany_rtmp_common_RtmpPusher_initVideoCodec(
        JNIEnv *env,
        jobject thiz,
        jint width,
        jint height,
        jint fps,
        jint bitrate,
        jint format
) {
    if (!rtmpPusher) {
        return;
    }
    rtmpPusher->initVideoCodec(width, height, fps, bitrate, format);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_ztiany_rtmp_common_RtmpPusher_initAudioCodec(
        JNIEnv *env,
        jobject thiz,
        jint sample_rate,
        jint channels
) {
    if (!rtmpPusher) {
        return;
    }
    rtmpPusher->initAudioCodec(sample_rate, channels);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_ztiany_rtmp_common_RtmpPusher_start(JNIEnv *env, jobject thiz, jstring url) {
    if (!rtmpPusher) {
        return;
    }
    if (rtmpPusher->isPushing()) {
        return;
    }

    //convert the url
    const char *path = env->GetStringUTFChars(url, nullptr);
    char *address = new char[strlen(path) + 1];
    strcpy(address, path);
    env->ReleaseStringUTFChars(url, path);

    //start the pusher
    rtmpPusher->start(address);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_ztiany_rtmp_common_RtmpPusher_sendVideoPacket(
        JNIEnv *env,
        jobject thiz,
        jbyteArray data,
        jint video_type,
        jlong tms
) {
    if (!rtmpPusher || !rtmpPusher->isPushing()) {
        LOGE("RTMP 已经停止运行");
        return;
    }
    jbyte *buf = env->GetByteArrayElements(data, nullptr);
    int length = env->GetArrayLength(data);
    rtmpPusher->processData((int8_t *) buf, length, tms, video_type);
    env->ReleaseByteArrayElements(data, buf, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_ztiany_rtmp_common_RtmpPusher_sendAudioPacket(
        JNIEnv *env,
        jobject thiz,
        jbyteArray data,
        jint audio_type,
        jlong tms
) {
    if (!rtmpPusher || !rtmpPusher->isPushing()) {
        LOGE("RTMP 已经停止运行");
        return;
    }
    jbyte *buf = env->GetByteArrayElements(data, nullptr);
    int length = env->GetArrayLength(data);
    rtmpPusher->processData((int8_t *) buf, length, tms, audio_type);
    env->ReleaseByteArrayElements(data, buf, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_ztiany_rtmp_common_RtmpPusher_stop(JNIEnv *env, jobject thiz) {
    if (!rtmpPusher) {
        return;
    }
    rtmpPusher->stop();
}

extern "C"
JNIEXPORT void JNICALL
Java_me_ztiany_rtmp_common_RtmpPusher_release(JNIEnv *env, jobject thiz) {
    rtmpPusher = nullptr;
}