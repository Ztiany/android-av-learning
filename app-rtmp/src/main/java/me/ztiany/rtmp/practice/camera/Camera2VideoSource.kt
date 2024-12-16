package me.ztiany.rtmp.practice.camera

import android.graphics.Point
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity

class Camera2VideoSource(
    private val context: AppCompatActivity,
    private val textureView: TextureView
) {

    private var camera2Helper: Camera2Helper? = null

    fun start(targetWidth: Int, targetHeight: Int, cameraId: String, camera2Listener: Camera2Listener) {
        Camera2Helper.Builder()
            .cameraListener(camera2Listener)
            .maxPreviewSize(Point(1000, 1000))
            .minPreviewSize(Point(240, 160))
            .previewViewSize(Point(textureView.width, textureView.height))
            .previewSize(Point(targetWidth, targetHeight))
            .specificCameraId(cameraId)
            .context(context.application)
            .previewOn(textureView)
            .rotation(context.windowManager.defaultDisplay.rotation)
            .build().apply {
                camera2Helper = this
                start()
            }
    }

    fun stop() {
        camera2Helper?.stop()
        camera2Helper?.release()
    }

    fun pause() {
        camera2Helper?.stop()
    }

    fun resume() {
        camera2Helper?.start()
    }

}