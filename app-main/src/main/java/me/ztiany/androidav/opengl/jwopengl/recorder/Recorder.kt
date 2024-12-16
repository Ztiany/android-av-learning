package me.ztiany.androidav.opengl.jwopengl.recorder

import android.opengl.EGLContext

interface Recorder {

    fun onStart(sharedEglContext: EGLContext)

    fun onFrame(frame: TextureWithTime)

    fun onStop()

}