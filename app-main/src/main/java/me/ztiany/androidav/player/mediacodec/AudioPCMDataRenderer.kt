package me.ztiany.androidav.player.mediacodec

import android.media.*
import me.ztiany.lib.avbase.utils.av.getChannelOutConfig
import timber.log.Timber
import java.nio.ByteBuffer

class AudioPCMDataRenderer : MediaDataRenderer {

    private var audioTrack: AudioTrack? = null

    private lateinit var audioOutTempBuffer: ShortArray

    override fun updateMediaFormat(mediaFormat: MediaFormat) {
        //释放之前的 Track
        releaseAudioTrack()

        //声音的规格
        val channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val pcmEncoding = if (mediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }

        val channelConfig = getChannelOutConfig(channelCount)
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, pcmEncoding)
        val maxBufferSize: Int = try {
            mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } catch (e: Exception) {
            Timber.e("get KEY_MAX_INPUT_SIZE", e)
            0
        }
        val finalBufferSize = if (maxBufferSize == 0 || minBufferSize * 2 > maxBufferSize) {
            minBufferSize
        } else {
            minBufferSize * 2
        }

        Timber.d("updateMediaFormat channelCount = $channelCount, sampleRate = $sampleRate, pcmEncoding = $pcmEncoding")
        Timber.d("updateMediaFormat minBufferSize = $minBufferSize, maxBufferSize = $maxBufferSize, finalBufferSize = $finalBufferSize")

        //初始化 AudioTrack
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

            finalBufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        //开启 AudioTrack
        audioTrack?.play()
    }

    override fun render(mediaData: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
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

    override fun release() {
        releaseAudioTrack()
    }

    private fun releaseAudioTrack() {
        val track = audioTrack
        audioTrack = null
        track?.run {
            stop()
            release()
        }
    }

}