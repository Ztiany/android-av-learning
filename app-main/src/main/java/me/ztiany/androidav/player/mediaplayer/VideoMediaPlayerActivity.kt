package me.ztiany.androidav.player.mediaplayer

import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Surface
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.android.sdk.mediaselector.common.ResultListener
import com.android.sdk.mediaselector.system.newSystemMediaSelector
import kotlinx.coroutines.launch
import me.ztiany.androidav.databinding.PlyaerActivityMediaPlayerBinding
import me.ztiany.androidav.opengl.jwopengl.common.EGLBridger
import me.ztiany.androidav.opengl.jwopengl.common.setGLRenderer
import me.ztiany.lib.avbase.app.activity.BaseActivity
import me.ztiany.lib.avbase.utils.av.loadMediaMetadataSuspend
import timber.log.Timber
import java.util.concurrent.TimeUnit

class VideoMediaPlayerActivity : BaseActivity<PlyaerActivityMediaPlayerBinding>() {

    private var selectedFile: Uri? = null

    private val videoPlayer = VideoPlayer()

    /** 是否处于拉拽进度条的状态 */
    private var isTrackingTouching = false

    private lateinit var glRenderer: MediaPlayerRenderer

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
        binding.videoSelectedFile.text = "已经选择：${selectedFile.toString()}"
        selectedFile?.let {
            lifecycleScope.launch {
                Timber.d(loadMediaMetadataSuspend(this@VideoMediaPlayerActivity, it).toString())
            }
        }
    }

    override fun setUpLayout(savedInstanceState: Bundle?) {
        setContentView(binding.root)
        setUpGLSurfaceView()
        setUpButtons()
        setUpVideoPlayer()
    }

    private fun setUpVideoPlayer() {
        videoPlayer.listener = object : VideoPlayer.PlayerListener {

            override fun onPlayerStart() = Unit
            override fun onPlayerStop() = Unit
            override fun onPlayerPause() = Unit

            override fun onPlayerUpdate(percent: Float) {
                if (!isTrackingTouching) {
                    binding.videoSeekbar.progress = (percent * 100).toInt()
                }
                binding.videoTvProgress.text = formatVideoTime(videoPlayer.currentPosition.toLong())
            }
        }

        binding.videoSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = Unit

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isTrackingTouching = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                videoPlayer.seekTo((seekBar.progress / 100.0F * videoPlayer.duration).toInt())
                isTrackingTouching = false
            }
        })
    }

    private fun setUpGLSurfaceView() {
        val mediaPlayerRenderer = MediaPlayerRenderer(this, object : EGLBridger {
            override fun requestRender() {
                binding.videoGlSurfaceView.requestRender()
            }
        })
        glRenderer = mediaPlayerRenderer

        with(binding.videoGlSurfaceView) {
            setEGLContextClientVersion(2)
            setGLRenderer(mediaPlayerRenderer)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }

        mediaPlayerRenderer.getSurfaceTexture {
            videoPlayer.setSurface(Surface(it))
        }
    }

    private fun setUpButtons() {
        binding.videoSelect.setOnClickListener {
            systemMediaSelector.takeFileFromSystem().mimeType("video/*").start()
        }

        binding.videoPlay.setOnClickListener {
            if (videoPlayer.isPlaying) {
                videoPlayer.stop()
                videoPlayer.reset()
                binding.videoPlay.text = "Play"
                return@setOnClickListener
            }

            selectedFile?.let {
                binding.videoPlay.text = "Stop"
                startVideoPlayer(it)
            }
        }
    }

    private fun startVideoPlayer(uri: Uri) {
        lifecycleScope.launch {
            val metadata = loadMediaMetadataSuspend(this@VideoMediaPlayerActivity, uri)
            glRenderer.onGetMediaMetaData(metadata, binding.videoCbAdjust.isChecked)
            videoPlayer.setDataSource(this@VideoMediaPlayerActivity, uri)
            videoPlayer.isLooping = true
            videoPlayer.setOnPreparedListener {
                videoPlayer.start()
                binding.videoTextDuration.text = formatVideoTime(videoPlayer.duration.toLong())
            }
            videoPlayer.prepareAsync()
        }
    }

    private fun formatVideoTime(time: Long): String {
        if (time > 0) {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(time)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
            return String.format("%d:%02d", minutes, seconds)
        }
        return "0:00"
    }

    override fun onPause() {
        super.onPause()
        videoPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoPlayer.stop()
        videoPlayer.release()
    }

}