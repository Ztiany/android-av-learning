package me.ztiany.androidav.avtest

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import me.ztiany.androidav.player.mediacodec.AUDIO_SELECTOR
import me.ztiany.androidav.player.mediacodec.VIDEO_SELECTOR
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * （1）在一个 MediaExtractor 中读取所有的 track，其实是可以的，只不过要注意多线程环境下需要处理好同步。
 * （2）但是如果是要做播放器，音频和和视频解码器共用一个 MediaExtractor 会出现异常。
 *
 * 因此推荐对于纯粹的解封操作，可以只使用一个 MediaExtractor，而如果是播放器，应该让每一个 MediaCodec 对应一个 MediaExtractor。
 */
class MediaExtractorMultiThreadingCase(
    private val context: Context,
    private val source: Uri,
) : TestCase {

    private lateinit var mediaExtractor: MediaExtractor

    private var audioTrack = -1
    private var videoTrack = -1

    @Volatile private var isTesting = false

    private var audioReleased = AtomicBoolean(false)
    private var videoReleased = AtomicBoolean(false)

    private val readTimes = 10

    override fun start() {
        isTesting = true
        initMediaExtractor()
        if (videoTrack == -1 || audioTrack == -1) {
            Timber.e("select a media file containing both audio and video track.")
            return
        }

        audioReleased.set(false)
        videoReleased.set(false)

        startReader(audioTrack, "Audio", audioReleased)
        startReader(videoTrack, "Video", videoReleased)
    }

    private fun startReader(
        track: Int,
        flag: String,
        releaseFlag: AtomicBoolean,
    ) {
        Timber.d("startReader $flag reader")

        val buffer = ByteBuffer.allocate(1024 * 1024 * 5)
        thread {

            var count = 0
            var readSize: Int

            while (isTesting) {

                synchronized(this@MediaExtractorMultiThreadingCase) {
                    mediaExtractor.selectTrack(track)
                    buffer.clear()
                    Timber.d("before read $flag, sampleSize = ${mediaExtractor.sampleSize}")
                    readSize = mediaExtractor.readSampleData(buffer, 0)
                    Timber.d("read $flag, readSize = $readSize,  sampleTime = ${mediaExtractor.sampleTime}, sampleTrackIndex = ${mediaExtractor.sampleTrackIndex}")
                    mediaExtractor.advance()
                    //不要调用这个方法
                    //mediaExtractor.unselectTrack(track)
                }

                if (++count >= readTimes || readSize < 0) {
                    break
                }
            }

            releaseFlag.set(true)
            if (audioReleased.get() && videoReleased.get()) {
                mediaExtractor.release()
                Timber.e("MediaExtractor released.")
            }
        }
    }

    private fun initMediaExtractor() {
        mediaExtractor = MediaExtractor().apply {
            setDataSource(context, source, null)
            val trackCount = trackCount
            var tempFormat: MediaFormat?
            for (track in 0 until trackCount) {
                tempFormat = getTrackFormat(track)
                if (AUDIO_SELECTOR(tempFormat)) {
                    audioTrack = track
                    Timber.d("select: track($track) $tempFormat as AudioTrack.")
                } else if (VIDEO_SELECTOR(tempFormat)) {
                    videoTrack = track
                    Timber.d(" select:track($track) $tempFormat as VideoTrack.")
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