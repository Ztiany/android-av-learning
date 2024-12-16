package me.ztiany.rtmp

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.blankj.utilcode.util.ToastUtils
import com.permissionx.guolindev.PermissionX
import me.ztiany.lib.avbase.app.activity.BaseActivity
import me.ztiany.rtmp.common.Pusher
import me.ztiany.rtmp.databinding.ActivityMainBinding
import me.ztiany.rtmp.practice.camera.Camera2Helper
import me.ztiany.rtmp.practice.camera.CameraSoftPusher
import me.ztiany.rtmp.practice.screen.ScreenPusher

private const val URL_SELF = "rtmp://139.198.183.182/live/livestream"

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private var pusher: Pusher? = null

    override fun setUpLayout(savedInstanceState: Bundle?) {
        askPermissions()

        binding.rtmpBtnScreenHard.setOnClickListener {
            liveScreenHard()
        }

        binding.rtmpBtnCameraBack.setOnClickListener {
            liveCameraSoft(Camera2Helper.CAMERA_ID_BACK)
        }

        binding.rtmpBtnCameraFront.setOnClickListener {
            ToastUtils.showLong("TODO")
            //liveCameraSoft(Camera2Helper.CAMERA_ID_FRONT)
        }

        binding.rtmpBtnStop.setOnClickListener {
            pusher?.stop()
            showFunctions()
        }
    }

    private fun showFunctions() {
        binding.rtmpBtnStop.visibility = View.GONE
        binding.rtmpBtnScreenHard.visibility = View.VISIBLE
        binding.rtmpBtnCameraFront.visibility = View.VISIBLE
        binding.rtmpBtnCameraBack.visibility = View.VISIBLE
    }

    private fun showController() {
        binding.rtmpBtnStop.visibility = View.VISIBLE
        binding.rtmpBtnScreenHard.visibility = View.GONE
        binding.rtmpBtnCameraFront.visibility = View.GONE
        binding.rtmpBtnCameraBack.visibility = View.GONE
    }

    private fun askPermissions() {
        PermissionX.init(this)
            .permissions(
                arrayListOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                )
            ).request { _, _, deniedList ->
                if (deniedList.isNotEmpty()) {
                    supportFinishAfterTransition()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        pusher?.onActivityResult(requestCode, resultCode, data)
    }

    private fun liveScreenHard() {
        //开始直播
        ScreenPusher(this).also {
            this.pusher = it
            it.start(URL_SELF)
        }
        //切换 UI
        showController()
    }

    private fun liveCameraSoft(cameraId: String) {
        //开始直播
        CameraSoftPusher(
            cameraId,
            this,
            binding.textureView
        ).also {
            this.pusher = it
            it.start(URL_SELF)
        }
        //切换 UI
        showController()
    }

    override fun onPause() {
        super.onPause()
        pusher?.pause()
    }

    override fun onResume() {
        super.onResume()
        pusher?.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        pusher?.stop()
    }

}