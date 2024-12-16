package me.ztiany.androidav.player.mediacodec

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.nio.ByteBuffer

class VideoSurfaceRenderer(
    private val surface: Surface
) : MediaDataRenderer, SurfaceRenderer {

    override fun updateMediaFormat(mediaFormat: MediaFormat) {

    }

    override fun render(mediaData: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {

    }

    override fun release() {

    }

    override fun provideSurface(): Surface {
        return surface
    }

}