package me.ztiany.androidav.avapi.audio.autiotrack

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import androidx.annotation.WorkerThread
import me.ztiany.lib.avbase.utils.av.PcmWavUtils
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

internal class AudioTracker(private val context: Context) {

    private val isPlaying = AtomicBoolean(false)

    private var audioTrack: AudioTrack? = null

    @WorkerThread
    fun play(uri: Uri, staticMode: Boolean) {
        if (!isPlaying.compareAndSet(false, true)) {
            Timber.d("AudioTracker is playing the audio.")
            return
        }

        val wavInputStream = parseUri(uri)
        val wavHeader = PcmWavUtils.parseWavFile(wavInputStream, staticMode/*read whole data in static mode*/, true)
        if (wavHeader == null) {
            isPlaying.set(false)
            return
        }
        Timber.d("WavHeader: $wavHeader")

        if (staticMode) {
            playAudioInStaticMode(wavHeader)
        } else {
            playAudioInStreamMode(uri, wavHeader)
        }
    }

    private fun playAudioInStaticMode(wavFile: PcmWavUtils.WavFile) {
        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(wavFile.sampleRate)
                .setEncoding(wavFile.audioEncoding)
                .setChannelMask(wavFile.channelConfig)
                .build(),
            wavFile.audioLength,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        this.audioTrack = audioTrack

        wavFile.audioData?.let {
            val result = audioTrack.write(it, 0, it.size)
            if (result < 0) {
                Timber.e("playAudioInStaticMode: ${getErrorMessage(result)}")
                stop()
                return
            }

            audioTrack.play()
            //don't call stop after play() is called.
        }
    }

    private fun playAudioInStreamMode(uri: Uri, wavFile: PcmWavUtils.WavFile) {
        val minBufferSize = AudioTrack.getMinBufferSize(wavFile.sampleRate, wavFile.channelConfig, wavFile.audioEncoding)

        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(wavFile.sampleRate)
                .setEncoding(wavFile.audioEncoding)
                .setChannelMask(wavFile.channelConfig)
                .build(),
            minBufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        this.audioTrack = audioTrack

        val inputStream = parseUri(uri)
        if (inputStream == null) {
            isPlaying.set(false)
            return
        }

        audioTrack.play()
        val buffer = ByteArray(minBufferSize)
        var sizeRead: Int
        while (isPlaying.get()) {
            sizeRead = inputStream.read(buffer)
            if (sizeRead <= 0) {
                break
            }
            val result = audioTrack.write(buffer, 0, sizeRead)
            if (result < 0) {
                Timber.e("playAudioInStreamMode: ${getErrorMessage(result)}")
                break
            }
        }

        stop()
    }

    private fun parseUri(uri: Uri): InputStream? {
        return if (uri.toString().startsWith("content://")) {
            context.contentResolver.openInputStream(uri)
        } else {
            val path = uri.path
            if (path.isNullOrEmpty()) {
                null
            } else {
                File(path).inputStream()
            }
        }
    }

    fun stop() {
        if (isPlaying.compareAndSet(true, false)) {
            isPlaying.set(false)
            audioTrack?.run {
                stop()
                release()
            }
            audioTrack = null
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

}