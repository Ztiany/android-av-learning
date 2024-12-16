package me.ztiany.androidav.opengl.jwopengl.common

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

fun GLSurfaceView.setGLRenderer(glRenderer: GLRenderer) {
    setRenderer(RendererAdapter(glRenderer))
}

class RendererAdapter(private val glRenderer: GLRenderer) : GLSurfaceView.Renderer {

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glRenderer.onSurfaceCreated()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glRenderer.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        glRenderer.onDrawFrame()
    }

}