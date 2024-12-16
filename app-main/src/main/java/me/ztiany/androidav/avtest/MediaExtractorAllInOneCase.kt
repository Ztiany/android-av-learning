package me.ztiany.androidav.avtest

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import me.ztiany.androidav.player.mediacodec.AUDIO_SELECTOR
import me.ztiany.androidav.player.mediacodec.VIDEO_SELECTOR
import timber.log.Timber
import java.nio.ByteBuffer
import kotlin.concurrent.thread

/** 按照网上所说到，不调用 selectTrack，直接调用 readSampleData，在一个 MediaExtractor 中读取所有的 track。【事实上，根本就是错误的用法】*/
class MediaExtractorAllInOneCase(
    private val context: Context,
    private val source: Uri,
) : TestCase {

    private lateinit var mediaExtractor: MediaExtractor

    @Volatile private var isTesting = false

    override fun start() {
        isTesting = true
        initMediaExtractor()
        startReader(20)
    }

    private fun startReader(readTime: Int) {
        val buffer = ByteBuffer.allocate(1024 * 1024 * 5)
        thread {

            var count = 0

            while (isTesting) {
                Timber.d("before read, sampleTime = ${mediaExtractor.sampleTime}, sampleTrackIndex = ${mediaExtractor.sampleTrackIndex}")
                val readSize = mediaExtractor.readSampleData(buffer, 0)
                Timber.d("after read, readSize = $readSize,  sampleTime = ${mediaExtractor.sampleTime}, sampleTrackIndex = ${mediaExtractor.sampleTrackIndex}")
                mediaExtractor.advance()

                if (++count > readTime || count < 0) {
                    break
                }
            }

            mediaExtractor.release()
        }
    }

    private fun initMediaExtractor() {
        var audioTrack = 0
        var videoTrack = 0

        mediaExtractor = MediaExtractor().apply {
            setDataSource(context, source, null)
            val trackCount = trackCount
            var tempFormat: MediaFormat?
            for (track in 0 until trackCount) {
                tempFormat = getTrackFormat(track)
                if (AUDIO_SELECTOR(tempFormat)) {
                    audioTrack = track
                    Timber.d("AudioTrack is $track and format is  $tempFormat")
                } else if (VIDEO_SELECTOR(tempFormat)) {
                    videoTrack = track
                    Timber.d("VideoTrack is $track and format is  $tempFormat")
                }
                if (videoTrack != -1 && audioTrack != -1) {
                    break
                }
            }
        }
    }

    override fun stop() {
        isTesting = false
    }

}