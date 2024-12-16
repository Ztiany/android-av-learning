package me.ztiany.androidav.opengl.jwopengl.usecase.camera

import android.graphics.Point
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Size
import androidx.core.view.doOnLayout
import me.ztiany.androidav.databinding.OpenglActivityCameraPreviewBinding
import me.ztiany.androidav.opengl.jwopengl.common.EGLBridger
import me.ztiany.androidav.opengl.jwopengl.common.setGLRenderer
import me.ztiany.androidav.opengl.jwopengl.renderer.CameraRenderer
import me.ztiany.androidav.opengl.oglcamera.CameraBuilder
import me.ztiany.androidav.opengl.oglcamera.CameraListener
import me.ztiany.androidav.opengl.oglcamera.CameraOperator
import me.ztiany.lib.avbase.app.activity.BaseActivity

class OpenGLCameraPreviewActivity : BaseActivity<OpenglActivityCameraPreviewBinding>() {

    private var cameraOperator: CameraOperator? = null
    private var cameraRenderer: CameraRenderer? = null

    private fun onCameraAvailable(previewSize: Size, displayOrientation: Int, isMirror: Boolean) {
        if ((displayOrientation / 90).mod(2) == 1) {
            cameraRenderer?.setVideoAttribute(previewSize.height, previewSize.width, displayOrientation, isMirror)
        } else {
            cameraRenderer?.setVideoAttribute(previewSize.width, previewSize.height, displayOrientation, isMirror)
        }
        cameraRenderer?.getSurfaceTexture {
            cameraOperator?.startPreview(it)
        }
    }

    override fun setUpLayout(savedInstanceState: Bundle?) {
        window.decorView.doOnLayout {
            setUpGlSurfaceView()
            setUpCamera()
            binding.openglBtnSwitch.setOnClickListener {
                cameraOperator?.switchCamera()
            }
        }
    }

    private fun setUpGlSurfaceView() {
        binding.openglCameraView.setEGLContextClientVersion(2)
        cameraRenderer = CameraRenderer(this, object : EGLBridger {
            override fun requestRender() {
                binding.openglCameraView.requestRender()
            }
        })
        cameraRenderer?.let {
            binding.openglCameraView.setGLRenderer(it)
            binding.openglCameraView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    private fun setUpCamera() {
        val cameraListener = CameraListener { _, cameraID, previewSize, displayOrientation ->
            onCameraAvailable(previewSize, displayOrientation, CameraOperator.CAMERA_ID_FRONT == cameraID)
        }

        val openglCameraView = binding.openglCameraView

        cameraOperator = CameraBuilder()
            .cameraListener(cameraListener)
            .maxPreviewSize(Point(1920, 1080))
            .minPreviewSize(Point(100, 100))
            .targetPreviewSize(Point(800, 480))
            .previewViewSize(Point(openglCameraView.width, openglCameraView.height))
            .specificCameraId(CameraOperator.CAMERA_ID_FRONT)
            .isMirror(true)
            .context(applicationContext)
            .rotation(windowManager.defaultDisplay.rotation)
            .build("2")

        cameraOperator?.start()
    }

    override fun onResume() {
        super.onResume()
        cameraOperator?.start()
    }

    override fun onPause() {
        super.onPause()
        cameraOperator?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraOperator?.release()
    }

}