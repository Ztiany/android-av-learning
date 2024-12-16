package me.ztiany.androidav.player.mediacodec

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import me.ztiany.lib.avbase.utils.av.loadMediaMetadata
import timber.log.Timber
import java.nio.ByteBuffer

class MediaDataExtractorImplFixed(
    private val context: Context
) : MediaDataExtractor {

    private var audioExtractor: MediaExtractor? = null
    private var videoExtractor: MediaExtractor? = null

    private lateinit var source: Uri

    private var audioFormat: MediaFormat? = null
    private var videoFormat: MediaFormat? = null

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
        val extractor = audioExtractor ?: throw IllegalStateException("No Audio Track")
        return readSamples(extractor, buffer, packet)
    }

    override fun readVideoPacket(buffer: ByteBuffer, packet: PacketInfo?): Int {
        val extractor = videoExtractor ?: throw IllegalStateException("No Video Track")
        return readSamples(extractor, buffer, packet)
    }

    private fun readSamples(extractor: MediaExtractor, buffer: ByteBuffer, packet: PacketInfo?): Int {
        val readSize = extractor.readSampleData(buffer, 0)
        packet?.sampleTime = extractor.sampleTime
        packet?.sampleFlags = extractor.sampleFlags
        extractor.advance()
        return readSize
    }

    private fun initExtractor() {
        if (!::source.isInitialized) {
            throw IllegalStateException("call setSource first.")
        }

        findTrack(audioSelector) { extractor, track, mediaFormat ->
            audioFormat = mediaFormat
            audioExtractor = extractor
            Timber.d("MediaDataExtractorImpl select audio: track($track) and format is $mediaFormat.")
        }

        findTrack(videoSelector) { extractor, track, mediaFormat ->
            videoFormat = mediaFormat
            videoExtractor = extractor
            Timber.d("MediaDataExtractorImpl select video: track($track) and format is $mediaFormat.")
        }
    }

    private fun findTrack(selector: TrackSelector, onFound: (extractor: MediaExtractor, Int, MediaFormat) -> Unit) {
        val extractor = MediaExtractor().apply { setDataSource(context, source, null) }
        val trackCount = extractor.trackCount
        var mediaFormat: MediaFormat
        for (track in 0 until trackCount) {
            mediaFormat = extractor.getTrackFormat(track)
            if (selector(mediaFormat)) {
                onFound(extractor, track, mediaFormat)
                extractor.selectTrack(track)
                break
            }
        }
    }

    override fun release() {
        Timber.d("release")
        audioExtractor?.release()
        videoExtractor?.release()
        audioExtractor = null
        videoExtractor = null
        audioFormat = null
        videoFormat = null
    }

    override fun seek(position: Long) {
        audioExtractor?.run {
            seekTo(position, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            advance()
        }
        videoExtractor?.run {
            seekTo(position, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            advance()
        }
    }

}