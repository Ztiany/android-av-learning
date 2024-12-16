package me.ztiany.androidav.opengl.jwopengl.common

import android.view.Surface

interface SurfaceProvider {

    fun start(surfaceProviderCallback: SurfaceProviderCallback)

    fun stop()

}

interface SurfaceProviderCallback {

    fun onSurfaceAvailable(surface: Surface)

    fun onSurfaceChanged(surface: Surface, width: Int, height: Int)

    fun onSurfaceDestroyed()

}