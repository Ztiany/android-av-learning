package me.ztiany.androidav.avapi.audio.audiorecord

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ztiany.androidav.databinding.AudioActivityArBinding
import me.ztiany.lib.avbase.utils.Directory

class AudioRecordActivity : AppCompatActivity() {

    private lateinit var binding: AudioActivityArBinding

    private val audioRecorder by lazy { AudioRecorder() }

    private var pcmPath = ""
    private var wavPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AudioActivityArBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpView()
    }

    private fun setUpView() {
        binding.audioBtnStart.setOnClickListener {
            startRecord()
            binding.audioBtnStart.isEnabled = false
        }

        binding.audioBtnEnd.setOnClickListener {
            audioRecorder.end()
            binding.audioBtnStart.isEnabled = true
            ToastUtils.showLong("保存路径 pcm: $pcmPath wav: $wavPath")
        }
    }

    private fun startRecord() {
        pcmPath = Directory.createSDCardRootAppTimeNamingPath(Directory.AUDIO_FORMAT_PCM).toString()
        wavPath = Directory.createSDCardRootAppTimeNamingPath(Directory.AUDIO_FORMAT_WAV).toString()
        audioRecorder.init(pcmPath, wavPath)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                audioRecorder.start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.end()
    }

}