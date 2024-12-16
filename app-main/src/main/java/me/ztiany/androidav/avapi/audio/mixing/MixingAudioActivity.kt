package me.ztiany.androidav.avapi.audio.mixing

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ztiany.androidav.databinding.AudioActivityMixingBinding
import me.ztiany.lib.avbase.utils.Directory
import timber.log.Timber
import java.io.File

/**给视频混入背景音乐。*/
class MixingAudioActivity : AppCompatActivity() {

    private var bgMusicVolume = 0
    private var videoVolume = 0
    private var duration = 0F

    private val originVideoPath = Directory.createSDCardRootAppPath("input.mp4").absolutePath
    private val originMusicPath = Directory.createSDCardRootAppPath("input.mp3").absolutePath

    private val handler = Handler(Looper.getMainLooper())
    private var times = 0

    private lateinit var binding: AudioActivityMixingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AudioActivityMixingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpView()
        configVideoView()
    }

    private fun setUpView() {
        //Volume
        binding.audioBgMusicSeekBar.max = 100
        binding.audioVideoSeekBar.max = 100

        val value = object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (binding.audioBgMusicSeekBar == seekBar) {
                    bgMusicVolume = progress;
                } else {
                    videoVolume = progress
                }
            }
        }

        binding.audioBgMusicSeekBar.setOnSeekBarChangeListener(value)
        binding.audioVideoSeekBar.setOnSeekBarChangeListener(value)

        //mixing
        binding.audioBtnExecute.setOnClickListener {
            doMixing()
        }
    }

    private fun configVideoView() {
        binding.audioVideoView.setOnErrorListener { _, _, _ ->
            Timber.d("setOnErrorListener")
            true
        }
    }

    private fun startPlay(path: String) {
        val layoutParams: ViewGroup.LayoutParams = binding.audioVideoView.layoutParams
        layoutParams.height = 675
        layoutParams.width = 1285
        binding.audioVideoView.layoutParams = layoutParams

        binding.audioVideoView.setVideoPath(path)
        binding.audioVideoView.start()
        binding.audioVideoView.setOnPreparedListener { mediaPlayer ->
            duration = mediaPlayer.duration / 1000F
            mediaPlayer.isLooping = true
            configSeekBar()
            lifecycleScope.launch {
                while (true) {
                    if (binding.audioVideoView.currentPosition >= binding.audioRangeSeekBar.currentRange[1] * 1000) {
                        binding.audioVideoView.seekTo(binding.audioRangeSeekBar.currentRange[0].toInt() * 1000)
                    }
                    delay(1000)
                }
            }
        }
    }

    private fun configSeekBar() {
        with(binding.audioRangeSeekBar) {
            setRange(0F, duration)
            setValue(0F, duration)
            isEnabled = true
            requestLayout()
            setOnRangeChangedListener { _, min, _, _ ->
                binding.audioVideoView.seekTo(min.toInt() * 1000)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startPlayChecked()
    }

    private fun startPlayChecked() {
        if (File(originVideoPath).exists() && !binding.audioVideoView.isPlaying) {
            startPlay(originVideoPath)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.audioVideoView.stopPlayback()
        handler.removeCallbacksAndMessages(null)
    }

    private fun doMixing() {
        if (!File(originMusicPath).exists() || !File(originVideoPath).exists()) {
            return
        }

        binding.audioBtnExecute.isEnabled = false

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                VideoAudioMixing.mixAudioTrack(
                    originVideoPath,
                    originMusicPath,
                    Directory.createSDCardRootAppPath("output-${times++}.mp4").absolutePath,
                    (binding.audioRangeSeekBar.currentRange[0] * 1000 * 1000).toInt(),
                    (binding.audioRangeSeekBar.currentRange[1] * 1000 * 1000).toInt(),
                    videoVolume,
                    bgMusicVolume
                )
            }

            Toast.makeText(this@MixingAudioActivity, "剪辑完毕", Toast.LENGTH_LONG).show()
            binding.audioBtnExecute.isEnabled = true
        }

    }

}