package me.ztiany.androidav.opengl.jwopengl.usecase.camera

import android.graphics.Point
import android.os.Bundle
import android.util.Size
import android.view.SurfaceView
import android.view.TextureView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import me.ztiany.androidav.R
import me.ztiany.androidav.opengl.jwopengl.common.*
import me.ztiany.androidav.opengl.jwopengl.egl14.EGLAttribute
import me.ztiany.androidav.opengl.jwopengl.egl14.EGLEnvironment
import me.ztiany.androidav.opengl.jwopengl.renderer.CameraRenderer
import me.ztiany.androidav.opengl.oglcamera.CameraBuilder
import me.ztiany.androidav.opengl.oglcamera.CameraListener
import me.ztiany.androidav.opengl.oglcamera.CameraOperator

class EGLCameraPreviewWithActivity : AppCompatActivity() {

    companion object {
        const val WITH_SURFACE_VIEW = 1
        const val WITH_TEXTURE_VIEW = 2
        const val RENDER_TYPE = "RENDER_TYPE"
    }

    private var cameraOperator: CameraOperator? = null
    private var eglEnvironment: EGLEnvironment? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getIntExtra(RENDER_TYPE, WITH_SURFACE_VIEW) == WITH_SURFACE_VIEW) {
            setContentView(R.layout.opengl_activity_egl_surv_preview)
            val surfaceView = findViewById<SurfaceView>(R.id.opengl_egl_surface_view)
            window.decorView.doOnLayout {
                setUpGlSurfaceView(SurfaceViewProvider(surfaceView))
                setUpCamera(Point(surfaceView.width, surfaceView.height))
            }
        } else {
            setContentView(R.layout.opengl_activity_egl_texv_preview)
            val textureView = findViewById<TextureView>(R.id.opengl_egl_texture_view)
            window.decorView.doOnLayout {
                setUpGlSurfaceView(TextureViewProvider(textureView))
                setUpCamera(Point(textureView.width, textureView.width))
            }
        }
        findViewById<Button>(R.id.opengl_btn_switch).setOnClickListener {
            cameraOperator?.switchCamera()
        }
    }

    private fun setUpGlSurfaceView(surfaceProvider: SurfaceProvider) {
        val eglEnvironment = EGLEnvironment(surfaceProvider, EGLAttribute()).apply {
            renderMode = RenderMode.WhenDirty
        }

        val cameraRenderer = CameraRenderer(this, object : EGLBridger {
            override fun requestRender() {
                eglEnvironment.requestRender()
            }
        })

        this.eglEnvironment = eglEnvironment
        this.cameraRenderer = cameraRenderer

        eglEnvironment.start(cameraRenderer)
    }

    private fun setUpCamera(point: Point) {
        val cameraListener = CameraListener { _, cameraID, previewSize, displayOrientation ->
            onCameraAvailable(previewSize, displayOrientation, CameraOperator.CAMERA_ID_FRONT == cameraID)
        }

        cameraOperator = CameraBuilder()
            .cameraListener(cameraListener)
            .maxPreviewSize(Point(1920, 1080))
            .minPreviewSize(Point(100, 100))
            .specificCameraId(CameraOperator.CAMERA_ID_BACK)
            .context(applicationContext)
            .previewViewSize(point)
            .targetPreviewSize(Point(800, 480))
            .rotation(windowManager.defaultDisplay.rotation)
            .build("2").also {
                it.start()
            }
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
        eglEnvironment?.release()
    }

}