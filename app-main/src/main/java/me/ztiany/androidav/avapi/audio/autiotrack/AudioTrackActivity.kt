package me.ztiany.androidav.avapi.audio.autiotrack

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.android.base.delegate.simpl.DelegateActivity
import com.android.sdk.mediaselector.common.ResultListener
import com.android.sdk.mediaselector.system.newSystemMediaSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ztiany.androidav.databinding.AudioActivityAtBinding
import me.ztiany.lib.avbase.app.activity.BaseActivity
import timber.log.Timber

class AudioTrackActivity : BaseActivity<AudioActivityAtBinding>() {

    private var selectedFile: Uri? = null

    private val systemMediaSelector by lazy {
        newSystemMediaSelector(this, object : ResultListener {
            override fun onTakeSuccess(result: List<Uri>) {
                Timber.d("onTakeSuccess: $result")
                selectedFile = result.getOrNull(0)
                showSelectedFile()
            }
        })
    }

    private fun showSelectedFile() {
        binding.audioSelectedFile.text = "已经选择：${selectedFile.toString()}"
    }

    private val audioTracker by lazy { AudioTracker(this) }

    override fun setUpLayout(savedInstanceState: Bundle?) {
        binding.audioSelect.setOnClickListener {
            systemMediaSelector.takeFileFromSystem().mimeType("audio/*").start()
        }

        binding.audioPlayStatic.setOnClickListener {
            playWav(true)
        }

        binding.audioPlayStream.setOnClickListener {
            playWav(false)
        }

        binding.audioStop.setOnClickListener {
            audioTracker.stop()
        }
    }

    private fun playWav(static: Boolean) {
        selectedFile?.let {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    audioTracker.play(it, static)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioTracker.stop()
    }

}