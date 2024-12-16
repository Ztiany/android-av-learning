package me.ztiany.androidav.player.mediacodec

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import me.ztiany.lib.avbase.utils.av.loadMediaMetadata
import timber.log.Timber
import java.nio.ByteBuffer

/** 实验发现，下面实现会崩溃。 */
@Deprecated("This implementation can not work properly.")
class MediaDataExtractorImpl(
    private val context: Context
) : MediaDataExtractor {

    private var mediaExtractor: MediaExtractor? = null

    private lateinit var source: Uri

    private var audioFormat: MediaFormat? = null
    private var audioTrack = -1

    private var videoFormat: MediaFormat? = null
    private var videoTrack = -1

    private var audioSelector: TrackSelector = AUDIO_SELECTOR
    private var videoSelector: TrackSelector = VIDEO_SELECTOR

    override fun setSource(source: Uri) {
        this.source = source
    }

    override fun setAudioTrackSelector(selector: TrackSelector) {
        this.audioSelector = selector
    }

    override fun setVideoTrackSelector(selector: TrackSelector) {
        this.videoSelector = selector
    }

    override fun prepare(): MediaInfo {
        release()
        initExtractor()
        return MediaInfo(
            loadMediaMetadata(context, source),
            audioFormat,
            videoFormat
        )
    }

    override fun readAudioPacket(buffer: ByteBuffer, packet: PacketInfo?): Int {
        if (audioTrack == -1) {
            throw IllegalStateException("No Audio Track")
        }
        synchronized(this) {
            return readSamples(audioTrack, buffer, packet)
        }
    }

    override fun readVideoPacket(buffer: ByteBuffer, packet: PacketInfo?): Int {
        if (videoTrack == -1) {
            throw IllegalStateException("No Video Track")
        }
        synchronized(this) {
            return readSamples(videoTrack, buffer, packet)
        }
    }

    private fun readSamples(track: Int, buffer: ByteBuffer, packet: PacketInfo?): Int {
        val extractor = mediaExtractor ?: return -1

        Timber.d("start readSamples track($track)")
        extractor.selectTrack(track)
        val readSize = extractor.readSampleData(buffer, 0)
        packet?.sampleFlags = extractor.sampleFlags
        packet?.sampleTime = extractor.sampleTime
        extractor.advance()
        Timber.d("end readSamples track($track)")

        return readSize
    }

    private fun initExtractor() {
        if (!::source.isInitialized) {
            throw IllegalStateException("call setSource first.")
        }

        val extractor = MediaExtractor().apply {
            setDataSource(context, source, null)
            mediaExtractor = this
        }

        val trackCount = extractor.trackCount
        var mediaFormat: MediaFormat
        for (track in 0 until trackCount) {
            mediaFormat = extractor.getTrackFormat(track)
            if (audioSelector(mediaFormat)) {
                audioTrack = track
                audioFormat = mediaFormat
                Timber.d("MediaDataExtractorImpl select audio: track($track) and format is $mediaFormat.")
            } else if (videoSelector(mediaFormat)) {
                videoTrack = track
                videoFormat = mediaFormat
                Timber.d("MediaDataExtractorImpl select audio: track($track) and format is $mediaFormat.")
            }
        }
    }

    override fun release() {
        Timber.d("release")
        mediaExtractor?.release()
        mediaExtractor = null
        audioTrack = -1
        videoTrack = -1
        audioFormat = null
        videoFormat = null
    }

    override fun seek(position: Long) {
        if (position < 0) {
            return
        }

        mediaExtractor?.run {
            synchronized(this@MediaDataExtractorImpl) {
                if (audioTrack != -1) {
                    selectTrack(audioTrack)
                    seekTo(position, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    advance()
                }
                if (videoTrack != -1) {
                    selectTrack(videoTrack)
                    seekTo(position, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    advance()
                }
            }
        }
    }

}