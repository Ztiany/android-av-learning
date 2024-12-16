package me.ztiany.androidav.avapi.audio.autiotrack

import android.content.Context
import android.media.*
import android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM
import android.net.Uri
import me.ztiany.androidav.player.mediacodec.AUDIO_SELECTOR
import me.ztiany.lib.avbase.utils.av.getChannelOutConfig
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

internal class MediaExtractorAudioTrackKit(private val context: Context) {

    private var mediaExtractor: MediaExtractor? = null
    private var audioTrack: AudioTrack? = null
    private var decoder: MediaCodec? = null

    private var sampleTime = 0L
    private var sampleFlags = 0
    private var reachEndOfStream = false
    private lateinit var audioOutTempBuffer: ShortArray

    private val outputBufferInfo by lazy {
        MediaCodec.BufferInfo()
    }

    private val isPlaying = AtomicBoolean(false)

    fun play(source: Uri) {
        if (isPlaying.compareAndSet(false, true)) {
            thread {
                reset()
                val mediaFormat = initExtractor(source)
                Timber.d("initExtractor")
                initRenderer(mediaFormat)
                Timber.d("initRenderer")
                initDecoder(mediaFormat)
                Timber.d("initDecoder")
                startDecoding()
                Timber.d("startDecoding")
                release()
                Timber.d("release")
            }
        }
    }

    private fun reset() {
        reachEndOfStream = false
        sampleTime = 0
        sampleFlags = 0
        outputBufferInfo.flags = 0
    }

    private fun initExtractor(source: Uri): MediaFormat {
        var mediaFormat: MediaFormat? = null

        MediaExtractor().apply {
            setDataSource(context, source, null)
            val trackCount = trackCount
            var tempFormat: MediaFormat?
            for (track in 0 until trackCount) {
                tempFormat = getTrackFormat(track)
                if (AUDIO_SELECTOR(tempFormat)) {
                    mediaFormat = tempFormat
                    selectTrack(track)
                    Timber.d("MediaDataExtractorImpl select: $mediaFormat .")
                    break
                }
            }
            mediaExtractor = this
        }

        return mediaFormat ?: throw IllegalStateException("no audio track found.")
    }

    private fun readFromMediaExtractor(buffer: ByteBuffer): Int {
        val extractor = mediaExtractor ?: return -1
        buffer.clear()
        val readSize = extractor.readSampleData(buffer, 0)
        if (readSize < 0) {
            return -1
        }
        sampleTime = extractor.sampleTime
        sampleFlags = extractor.sampleFlags
        extractor.advance()
        return readSize
    }

    private fun initRenderer(mediaFormat: MediaFormat) {
        val channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val pcmEncoding = if (mediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }

        val channelConfig = getChannelOutConfig(channelCount)
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, pcmEncoding)

        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(pcmEncoding)
                .setChannelMask(channelConfig)
                .build(),
            minBufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        ).apply {
            play()
        }
    }

    private fun render(mediaData: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        audioTrack?.let {
            if (!::audioOutTempBuffer.isInitialized || audioOutTempBuffer.size < bufferInfo.size / 2) {
                audioOutTempBuffer = ShortArray(bufferInfo.size / 2)
            }
            mediaData.position(0)
            mediaData.asShortBuffer().get(audioOutTempBuffer, 0, bufferInfo.size / 2)
            val result = it.write(audioOutTempBuffer, 0, bufferInfo.size / 2)
            if (result < 0) {
                Timber.e("playAudioInStreamMode: ${getErrorMessage(result)}")
            }
        }
    }

    private fun getErrorMessage(result: Int): String {
        return when (result) {
            AudioTrack.ERROR_INVALID_OPERATION -> "play fail: ERROR_INVALID_OPERATION"
            AudioTrack.ERROR_BAD_VALUE -> "play fail: ERROR_BAD_VALUE"
            AudioManager.ERROR_DEAD_OBJECT -> "play fail: ERROR_DEAD_OBJECT"
            else -> "play fail: null mAudioTrack"
        }
    }

    private fun initDecoder(mediaFormat: MediaFormat) {
        val name = MediaCodecList(MediaCodecList.ALL_CODECS).findDecoderForFormat(mediaFormat)
        Timber.d("findDecoderForFormat: $name")
        decoder = MediaCodec.createByCodecName(name).apply {
            configure(mediaFormat, null, null, 0)
            start()
        }
    }

    private fun startDecoding() {
        val mediaCodec: MediaCodec = decoder ?: throw IllegalStateException("decoder not initialized.")
        while (isPlaying.get()) {
            pushData(mediaCodec)
            pullData(mediaCodec)
        }
    }

    private fun pushData(mediaCodec: MediaCodec) {
        if (reachEndOfStream) {
            Timber.d("pushData reachEndOfStream")
            return
        }

        val inputBufferIndex = mediaCodec.dequeueInputBuffer(-1)
        if (inputBufferIndex < 0) {
            Timber.d("pushData inputBufferIndex < 0")
            return
        }
        val byteBuffer = mediaCodec.getInputBuffer(inputBufferIndex) ?: return
        val sampleSize = readFromMediaExtractor(byteBuffer)
        Timber.d("pushData sampleSize = $sampleSize sampleTime = $sampleTime")
        if (sampleSize < 0) {
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            reachEndOfStream = true
        } else {
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, sampleTime, 0)
        }
    }

    private fun pullData(mediaCodec: MediaCodec) {
        val index = mediaCodec.dequeueOutputBuffer(outputBufferInfo, 1000)
        if (outputBufferInfo.flags and BUFFER_FLAG_END_OF_STREAM != 0) {
            Timber.d("pullData received BUFFER_FLAG_END_OF_STREAM")
            stop()
            return
        }
        when {
            index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                Timber.d("pullData received INFO_OUTPUT_FORMAT_CHANGED ${mediaCodec.outputFormat}")
            }
            index == MediaCodec.INFO_TRY_AGAIN_LATER -> {

            }
            index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {

            }
            index >= 0 -> {
                mediaCodec.getOutputBuffer(index)?.let {
                    render(it, outputBufferInfo)
                    mediaCodec.releaseOutputBuffer(index, false)
                }
            }
        }
    }

    fun stop() {
        isPlaying.set(false)
    }

    private fun release() {
        decoder?.apply {
            stop()
            release()
        }
        decoder = null

        mediaExtractor?.release()
        mediaExtractor = null

        audioTrack?.apply {
            stop()
            release()
        }
        audioTrack = null
    }

}