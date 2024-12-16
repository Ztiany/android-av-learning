#ifndef ANDROID_AV_BACKGROUNDRENDERER_HPP
#define ANDROID_AV_BACKGROUNDRENDERER_HPP

#include "../common/GLRenderer.h"

class BackgroundRenderer : public GLRenderer {

public:
    static const int TYPE = 0;

    void onSurfaceCreated() override {
        /*
         * 理解 glClearColor：这里 gl 是 OpenGL API 统一的前缀，ClearColor 是一个名词，表示清屏颜色。
         */
        glClearColor(0.6F, 0.4F, 0.1F, 1.0F);
    }

    void onSurfaceChanged(int width, int height) override {
        glViewport(0, 0, width, height);
    }

    void onDrawFrame(void *data) override {
        /*
         * glClear 函数是 OpenGL 中的一个函数，它用于将指定的缓冲区清除为预定义的值。以下是 glClear 函数的原型：
         *
         *      void glClear(GLbitfield mask);
         *
         * glClear 函数接受一个参数 mask，这是一个位掩码，用于指定要清除的缓冲区。mask 可以是以下一个或多个值的按位或：
         *
         *  - GL_COLOR_BUFFER_BIT：指定要清除颜色缓冲区。
         *  - GL_DEPTH_BUFFER_BIT：指定要清除深度缓冲区。
         *  - GL_STENCIL_BUFFER_BIT：指定要清除模板缓冲区。
         *
         * glClear 函数将所有指定的缓冲区的值设置为预定义的值。对于颜色缓冲区，预定义的值为当前的清除颜色（可以使用 glClearColor 函数设置）。
         * 对于深度缓冲区，预定义的值为最大深度值；对于模板缓冲区，预定义的值为零。可以使用 glDepthFunc 和 glStencilFunc 函数设置深度函数
         * 和模板函数，以控制深度测试和模板测试的结果。
         *
         * 需要注意的是，glClear 函数只是将缓冲区的值设置为预定义的值，它并不会更新窗口的内容。要更新窗口的内容，需要使用其他 OpenGL 函数，
         * 例如 glFlush 和 glSwapBuffers。
         */
        glClear(GL_COLOR_BUFFER_BIT);
    }

    void onSurfaceDestroy() override {

    }

};

#endif //ANDROID_AV_BACKGROUNDRENDERER_HPP
