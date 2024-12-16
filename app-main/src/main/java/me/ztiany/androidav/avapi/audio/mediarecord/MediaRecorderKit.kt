package me.ztiany.androidav.avapi.audio.mediarecord

import android.content.Context
import android.media.MediaRecorder
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class MediaRecorderKit(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null

    private val isRecording = AtomicBoolean(false)

    private var startTimestamp = 0L

    fun startRecording(savePath: String) {
        if (!isRecording.compareAndSet(false, true)) {
            Timber.d("MediaRecorderKit is recording.")
            return
        }

        mediaRecorder = MediaRecorder().apply {
            //设置音频源
            setAudioSource(MediaRecorder.AudioSource.MIC)
            //输出格式
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            //音频编码
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            //存储路径
            setOutputFile(savePath)

            try {
                prepare()
                Timber.i("prepare() succeeded")
            } catch (e: Exception) {
                stopRecording()
                Timber.e(e, "prepare() failed")
            }

            try {
                start()
                startTimestamp = System.currentTimeMillis()
                Timber.i("start() succeeded")
            } catch (e: Exception) {
                stopRecording()
                Timber.e(e, "start() failed")
            }
        }
    }

    fun getMaxAmplitude(): Int {
        return mediaRecorder?.maxAmplitude ?: 0
    }

    fun stopRecording(): Long {
        if (isRecording.compareAndSet(true, false)) {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            return System.currentTimeMillis() - startTimestamp
        }

        Timber.d("MediaRecorderKit is not recording.")
        return 0
    }

}