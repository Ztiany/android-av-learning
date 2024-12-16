#include <jni.h>
#include <string>
#include "common/log.h"
#include "common/times.h"
#include "common/resources.h"

extern "C" {
#include "libavutil/avutil.h"
}

JavaVM *javaVM;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    LOGE("Native -------------------------------开始加载");
    LOGE("FFmpeg configuration: %s", avutil_configuration());
    LOGE("Native -------------------------------加载完成");
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_ztiany_androidav_JNIBridge_initNative(JNIEnv *env, jobject thiz, jobject context, jobject asset_manager) {
    initAssetManager(env, asset_manager);
}