package me.ztiany.androidav.player.mediacodec

import android.net.Uri
import android.os.Bundle
import android.view.SurfaceHolder
import androidx.lifecycle.lifecycleScope
import com.android.sdk.mediaselector.common.ResultListener
import com.android.sdk.mediaselector.system.newSystemMediaSelector
import kotlinx.coroutines.launch
import me.ztiany.androidav.databinding.PlayerActivityMediaCodecBinding
import me.ztiany.lib.avbase.app.activity.BaseActivity
import me.ztiany.lib.avbase.utils.av.loadMediaMetadataSuspend
import timber.log.Timber

class MediaCodecPlayerActivity : BaseActivity<PlayerActivityMediaCodecBinding>() {

    private var selectedFile: Uri? = null

    private val videoPlayer by lazy { CodecPlayerController(this) }

    /** 是否处于拉拽进度条的状态 */
    private var isTrackingTouching = false

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
        binding.playerTvSelectedFile.text = "已经选择：${selectedFile.toString()}"
        selectedFile?.let {
            lifecycleScope.launch {
                Timber.d(loadMediaMetadataSuspend(this@MediaCodecPlayerActivity, it).toString())
            }
        }
    }

    override fun setUpLayout(savedInstanceState: Bundle?) {
        setContentView(binding.root)
        setUpButtons()
        setUpSurfaceView()
        setUpVideoPlayer()
    }

    private fun setUpButtons() {
        binding.playerBtnVideoSelect.setOnClickListener {
            systemMediaSelector.takeFileFromSystem().mimeType("video/*").start()
        }

        binding.playerBtnPlay.setOnClickListener {
            if (videoPlayer.isPlaying) {
                videoPlayer.stop()
                binding.playerBtnPlay.text = "Play"
                return@setOnClickListener
            }

            selectedFile?.let {
                binding.playerBtnPlay.text = "Stop"
                videoPlayer.setDataSource(it)
                lifecycleScope.launch {
                    startPlayer()
                }
            }
        }
    }

    private suspend fun startPlayer() {
        if (videoPlayer.prepare()) {
            videoPlayer.mediaInfo?.metadata?.let {
                binding.videoSurfaceView.setAspectRatio(it.width.toDouble() / it.height)
            }
            videoPlayer.start()
        }
    }

    private fun setUpSurfaceView() {
        binding.videoSurfaceView.holder.addCallback(object : SurfaceHolder.Callback2 {
            override fun surfaceCreated(holder: SurfaceHolder) {
                videoPlayer.setVideoRenderer(holder.surface)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }

            override fun surfaceRedrawNeeded(holder: SurfaceHolder) {

            }
        })
    }

    private fun setUpVideoPlayer() {
        videoPlayer.listener = object : CodecPlayerController.ControllerListener {
            override fun onError(code: Int) {

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoPlayer.release()
    }

}