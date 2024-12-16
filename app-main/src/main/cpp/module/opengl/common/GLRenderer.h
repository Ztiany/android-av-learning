#ifndef ANDROID_AV_GLRENDERER_H
#define ANDROID_AV_GLRENDERER_H

#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <iostream>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <GLES2/gl2platform.h>
#include <glm/glm.hpp>
#include <glm/ext.hpp>

class GLRenderer {
public:

    virtual ~GLRenderer() = default;

    virtual void onSurfaceCreated() = 0;

    virtual void onSurfaceChanged(int width, int height) = 0;

    virtual void onDrawFrame(void *data) = 0;

    virtual void onSurfaceDestroy() = 0;
};

#endif //ANDROID_AV_GLRENDERER_H
