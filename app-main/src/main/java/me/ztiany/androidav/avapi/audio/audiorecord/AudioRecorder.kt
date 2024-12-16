package me.ztiany.androidav.avapi.audio.audiorecord

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.WorkerThread
import me.ztiany.lib.avbase.utils.av.PcmWavUtils
import timber.log.Timber
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** 采样率：能够保证在所有设备上使用的采样率是 44100Hz，其也是最常见的采样率。*/
private const val SAMPLE_RATE_IN_HZ = 44100

/** 声道数：CHANNEL_IN_MONO 为单声道，CHANNEL_IN_STEREO 双声道，其中 CHANNEL_IN_MONO 可以保证在所有设备能够使用。【有的设备不支持双声道】 */
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO

/** 位深度：每个采样用多少数据来存储。*/
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

/**
 * Factor by that the minimum buffer size is multiplied. The bigger the factor is the less
 * likely it is that samples will be dropped, but more memory will be used. The minimum buffer
 * size is determined by [AudioRecord.getMinBufferSize] and depends on the
 * recording settings.
 *
 * refers to [AudioRecorder.getMinBufferSize() returns -2](https://stackoverflow.com/questions/7829398/audiorecorder-getminbuffersize-returns-2) for details.
 */
private const val BUFFER_SIZE_FACTOR = 2

class AudioRecorder {

    private var audioRecord: AudioRecord? = null

    private var recordBufSizeInByte = 0

    private val isRecording = AtomicBoolean(false)

    private var pcmPath: String = ""
    private var wavPath: String = ""

    fun init(pcmPath: String, wavPath: String) {
        if (audioRecord != null) {
            Timber.d("AudioRecorder is initialized.")
            return
        }

        this.pcmPath = pcmPath
        this.wavPath = wavPath
        initAudioRecord()
    }

    @WorkerThread
    fun start() {
        val record = audioRecord
        if (record == null) {
            Timber.d("AudioRecorder has not been initialized.")
            return
        }
        if (!isRecording.compareAndSet(false, true)) {
            Timber.d("AudioRecorder is running.")
            return
        }

        doRecord(record)
    }

    private fun initAudioRecord(): AudioRecord {
        recordBufSizeInByte = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR
        Timber.d("recordBufSizeInByte = $recordBufSizeInByte")

        return AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE_IN_HZ,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            recordBufSizeInByte
        ).also {
            audioRecord = it
        }
    }

    private fun doRecord(record: AudioRecord): Boolean {
        val fileOutputStream = try {
            FileOutputStream(pcmPath)
        } catch (e: Exception) {
            return false
        }

        val byteArray = ByteArray(recordBufSizeInByte)
        var readSize: Int

        record.startRecording()
        while (isRecording.get()) {
            readSize = record.read(byteArray, 0, recordBufSizeInByte)
            if (readSize < 0) {
                Timber.d("AudioRecord read: ${getBufferReadFailureReason(readSize)}")
                break
            }
            try {
                fileOutputStream.write(byteArray, 0, readSize)
            } catch (e: IOException) {
                Timber.e("FileOutputStream write", e)
            }
        }

        try {
            fileOutputStream.close()
        } catch (e: IOException) {
            Timber.e("FileOutputStream close", e)
        }

        PcmWavUtils.pcmToWav(
            pcmPath,
            wavPath,
            SAMPLE_RATE_IN_HZ,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        return true
    }


    fun end() {
        isRecording.set(false)
        releaseAudioRecord()
    }

    private fun releaseAudioRecord() {
        audioRecord?.run {
            stop()
            release()
        }
        audioRecord = null
    }

    private fun getBufferReadFailureReason(errorCode: Int): String {
        return when (errorCode) {
            AudioRecord.ERROR_INVALID_OPERATION -> "ERROR_INVALID_OPERATION"
            AudioRecord.ERROR_BAD_VALUE -> "ERROR_BAD_VALUE"
            AudioRecord.ERROR_DEAD_OBJECT -> "ERROR_DEAD_OBJECT"
            AudioRecord.ERROR -> "ERROR"
            else -> "Unknown ($errorCode)"
        }
    }

}