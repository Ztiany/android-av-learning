#ifndef ANDROID_AV_LOG_H
#define ANDROID_AV_LOG_H

#include <string>
#include <android/log.h>

#define DEFAULT_TAG "Native"

void xLog(int priority, const char *tag, const char *format, ...);

#define LOGD(...) xLog(ANDROID_LOG_DEBUG, DEFAULT_TAG,__VA_ARGS__)
#define LOGI(...) xLog(ANDROID_LOG_INFO, DEFAULT_TAG,__VA_ARGS__)
#define LOGW(...) xLog(ANDROID_LOG_WARN, DEFAULT_TAG,__VA_ARGS__)
#define LOGE(...) xLog(ANDROID_LOG_ERROR, DEFAULT_TAG,__VA_ARGS__)

#define LOGD_TAG(TAG, ...) xLog(ANDROID_LOG_DEBUG, TAG,__VA_ARGS__)
#define LOGI_TAG(TAG, ...) xLog(ANDROID_LOG_INFO, TAG,__VA_ARGS__)
#define LOGW_TAG(TAG, ...) xLog(ANDROID_LOG_WARN, TAG,__VA_ARGS__)
#define LOGE_TAG(TAG, ...) xLog(ANDROID_LOG_ERROR, TAG,__VA_ARGS__)

#endif //ANDROID_AV_LOG_H
