package me.ztiany.androidav.opengl.jwopengl.renderer

import android.opengl.GLES20
import me.ztiany.androidav.opengl.jwopengl.common.GLRenderer
import timber.log.Timber

class BackgroundRenderer : GLRenderer {

    override fun onSurfaceCreated() {
        Timber.d("onSurfaceCreated() called")
        //设置当前颜色状态【OpenGL 是一个状态机】
        GLES20.glClearColor(0.6F, 0.4F, 0.1F, 1.0F)
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        Timber.d("onSurfaceChanged() called with:  width = $width, height = $height")
        //设置视口，Viewport 可以理解为 OpenGL 的画布。
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(attachment: Any? ) {
        //用 glClearColor 设置的颜色来擦除颜色缓冲区。【除了颜色缓冲区，还有 GL_DEPTH_BUFFER_BIT（深度） 和 GL_STENCIL_BUFFER_BIT（蒙版）】
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    override fun onSurfaceDestroy() {
    }

}