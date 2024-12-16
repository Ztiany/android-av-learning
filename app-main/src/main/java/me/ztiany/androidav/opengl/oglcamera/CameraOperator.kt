package me.ztiany.androidav.opengl.oglcamera

import android.graphics.SurfaceTexture

interface CameraOperator {

    companion object {
        const val CAMERA_ID_FRONT = "1"
        const val CAMERA_ID_BACK = "0"
    }

    fun start()

    fun stop()

    fun release()

    fun startPreview(surfaceTexture: SurfaceTexture)

    fun switchCamera()

}