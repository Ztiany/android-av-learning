#ifndef ANDROID_AV_JAVACALLER_H
#define ANDROID_AV_JAVACALLER_H

#include <jni.h>
#include "../utils/Log.h"

enum Thread {
    Child,
    Main
};

class JavaCaller {

private:
    _JavaVM *jvm = nullptr;
    JNIEnv *mainEnv = nullptr;
    jobject handle;

    jmethodID methodOnInitResult = nullptr;
    jmethodID methodSendError = nullptr;

    void findJavaMethods() {
        jclass rtmpPusherClass = mainEnv->GetObjectClass(handle);
        if (!rtmpPusherClass) {
            LOGE("get jclass wrong");
        }
        methodOnInitResult = mainEnv->GetMethodID(rtmpPusherClass, "onInitResult", "(Z)V");
        methodSendError = mainEnv->GetMethodID(rtmpPusherClass, "onSendError", "()V");
    }

public:
    JavaCaller(_JavaVM *javaVM, JNIEnv *jniEnv, jobject *obj) : jvm(javaVM), mainEnv(jniEnv) {
        handle = jniEnv->NewGlobalRef(*obj);
        findJavaMethods();
    }

    ~JavaCaller() {
        jvm = nullptr;
        mainEnv->DeleteGlobalRef(handle);
        mainEnv = nullptr;
    }

    void notifyInitResult(bool succeeded, int type) {
        if (!methodOnInitResult) {
            return;
        }

        jboolean result = succeeded ? JNI_TRUE : JNI_FALSE;
        if (type == Main) {
            mainEnv->CallVoidMethod(handle, methodOnInitResult, result);
        } else {
            JNIEnv *jniEnv;
            if (jvm->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
                LOGE("call onCallTimeInfo wrong");
                return;
            }
            jniEnv->CallVoidMethod(handle, methodOnInitResult, result);
            jvm->DetachCurrentThread();
        }
    }

    void notifySendError(int type) {
        if (!methodSendError) {
            return;
        }

        if (type == Main) {
            mainEnv->CallVoidMethod(handle, methodSendError);
        } else {
            JNIEnv *jniEnv;
            if (jvm->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
                LOGE("call onSendError wrong");
                return;
            }
            jniEnv->CallVoidMethod(handle, methodSendError);
            jvm->DetachCurrentThread();
        }
    }
};

#endif //ANDROID_AV_JAVACALLER_H
