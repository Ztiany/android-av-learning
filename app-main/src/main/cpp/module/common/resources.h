#ifndef ANDROID_AV_RESOURCES_H
#define ANDROID_AV_RESOURCES_H

#include <jni.h>

void initAssetManager(JNIEnv *env, jobject assetManager);

unsigned char *loadFileContent(const char *path, int &fileSize);

#endif //ANDROID_AV_RESOURCES_H
