package me.ztiany.androidav.player.mediacodec

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

interface MediaDataRenderer {

    fun updateMediaFormat(mediaFormat: MediaFormat)

    fun render(mediaData: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)

    fun release()

}