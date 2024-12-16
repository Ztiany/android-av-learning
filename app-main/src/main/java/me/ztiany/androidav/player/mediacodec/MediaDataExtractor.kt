package me.ztiany.androidav.player.mediacodec

import android.media.MediaFormat
import android.net.Uri
import me.ztiany.lib.avbase.utils.av.MediaMetadata
import java.io.IOException
import java.nio.ByteBuffer

interface MediaDataExtractor {

    fun setSource(source: Uri)

    fun setAudioTrackSelector(selector: TrackSelector)

    fun setVideoTrackSelector(selector: TrackSelector)

    /**
     * @throws IllegalStateException 没有提前调用 [setSource], [setAudioTrackSelector], [setVideoTrackSelector] 方法。
     * @throws IOException 数据源错误
     */
    fun prepare(): MediaInfo

    fun release()

    fun readAudioPacket(buffer: ByteBuffer, packet: PacketInfo? = null): Int

    fun readVideoPacket(buffer: ByteBuffer, packet: PacketInfo? = null): Int

    /**
     * seek 到指定位置，多线程环境下，seek 与读操作（[readAudioPacket] 和 [readVideoPacket] ）不保证线程安全，
     * 也就是说多线程环境下，请保证在 seek 的同时没有其他线程在 read packet。
     */
    fun seek(position: Long)

}

class MediaInfo(
    val metadata: MediaMetadata,
    val audioFormat: MediaFormat?,
    val videoFormat: MediaFormat?
)

typealias TrackSelector = (MediaFormat) -> Boolean

val AUDIO_SELECTOR: TrackSelector = {
    it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") ?: false
}

val VIDEO_SELECTOR: TrackSelector = {
    it.getString(MediaFormat.KEY_MIME)?.startsWith("video/") ?: false
}

data class PacketInfo(
    var sampleTime: Long,
    var sampleFlags: Int
)

