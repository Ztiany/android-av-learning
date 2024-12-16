package me.ztiany.rtmp.practice.screen

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection

class ScreenVideoSource {

    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null

    fun start(mediaProjection: MediaProjection, videoConfig: VideoConfig) {
        this.mediaProjection = mediaProjection
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "screen-codec",
            videoConfig.targetWidth,
            videoConfig.targetHeight,
            1,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
            videoConfig.surface,
            null,
            null
        );
    }

    fun stop() {
        mediaProjection?.stop()
        virtualDisplay?.release()
    }

}