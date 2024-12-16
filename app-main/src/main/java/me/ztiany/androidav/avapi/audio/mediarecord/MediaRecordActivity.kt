package me.ztiany.androidav.avapi.audio.mediarecord

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ztiany.androidav.databinding.AudioActivityMrBinding
import me.ztiany.lib.avbase.utils.Directory
import timber.log.Timber

class MediaRecordActivity : AppCompatActivity() {

    private lateinit var binding: AudioActivityMrBinding

    private val mediaRecorderKit by lazy {
        MediaRecorderKit(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AudioActivityMrBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpView()
    }

    private fun setUpView() {
        binding.audioBtnStart.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    Timber.d("before startRecording")
                    mediaRecorderKit.startRecording(Directory.createSDCardRootAppTimeNamingPath(Directory.AUDIO_FORMAT_AAC).absolutePath)
                    Timber.d("after startRecording")
                }
            }
        }

        binding.audioBtnEnd.setOnClickListener {
            val length = mediaRecorderKit.stopRecording()
            ToastUtils.showLong("length = $length")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorderKit.stopRecording()
    }

}