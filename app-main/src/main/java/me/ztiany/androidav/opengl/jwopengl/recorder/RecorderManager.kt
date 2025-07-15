package me.ztiany.androidav.opengl.jwopengl.recorder

import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLSurfaceView
import android.util.Size
import me.ztiany.androidav.opengl.jwopengl.common.EGLBridger
import me.ztiany.androidav.opengl.jwopengl.common.setGLRenderer
import me.ztiany.androidav.opengl.jwopengl.gles2.TextureAttribute
import me.ztiany.androidav.opengl.jwopengl.recorder.encoder.Encoder
import me.ztiany.androidav.opengl.jwopengl.recorder.encoder.HardEncoder
import me.ztiany.androidav.opengl.jwopengl.recorder.filter.GLFilter
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class RecorderManager {

    ///////////////////////////////////////////////////////////////////////////
    // Base
    ///////////////////////////////////////////////////////////////////////////

    private lateinit var showRenderer: RecorderShowRenderer

    private lateinit var encodeRenderer: RecorderEncodeRenderer

    @Volatile var isRecording = false
        private set

    @Volatile private var isOperating = false

    private val hasInitialized = AtomicBoolean(false)

    fun init(glSurfaceView: GLSurfaceView) {
        if (!hasInitialized.compareAndSet(false, true)) {
            throw IllegalStateException("Already init.")
        }
        // encode
        encodeRenderer = RecorderEncodeRenderer()
        // init the renderer
        showRenderer = RecorderShowRenderer(glSurfaceView.context, object : EGLBridger {
            override fun requestRender() {
                glSurfaceView.requestRender()
            }
        })

        glSurfaceView.run {
            setEGLContextClientVersion(2)
            setGLRenderer(showRenderer)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    private fun checkIfInitialized() {
        if (!hasInitialized.get()) {
            throw IllegalStateException("call init first.")
        }
    }

    fun onCameraAvailable(previewSize: Size, displayOrientation: Int, isFront: Boolean, startPreview: (SurfaceTexture) -> Unit) {
        Timber.d("onCameraAvailable() called with: previewSize = $previewSize, displayOrientation = $displayOrientation, isFront = $isFront")
        checkIfInitialized()
        // provide the renderers texture the video size.
        val textureAttribute = TextureAttribute(previewSize.width, previewSize.height, displayOrientation, isFront, false)
        showRenderer.setVideoAttribute(textureAttribute)
        encodeRenderer.setVideoAttribute(textureAttribute)
        //start preview.
        showRenderer.getSurfaceTexture {
            startPreview(it)
        }
    }

    fun startRecording(path: String) {
        Timber.d("startRecording() called isRecording = $isRecording, isOperating = $isOperating")
        checkIfInitialized()

        if (isRecording || isOperating) {
            return
        }
        isRecording = true
        isOperating = true

        showRenderer.startRecording(object : Recorder {
            override fun onStart(sharedEglContext: EGLContext) {
                encodeRenderer.start(sharedEglContext, newEncoder(path))
                isOperating = false
            }

            override fun onFrame(frame: TextureWithTime) {
                encodeRenderer.onFrame(frame)
            }

            override fun onStop() {
                encodeRenderer.stop()
                isOperating = false
            }
        })
    }

    fun stopRecording() {
        Timber.d("stopRecording() called isRecording = $isRecording, isOperating = $isOperating")
        if (!isRecording || isOperating) {
            return
        }
        isRecording = false
        isOperating = true
        showRenderer.stopRecording()
    }

    private fun newEncoder(path: String): Encoder {
        return HardEncoder(path, speed.getSpeedValue())
    }

    ///////////////////////////////////////////////////////////////////////////
    //config
    ///////////////////////////////////////////////////////////////////////////

    var speed = Speed.MODE_NORMAL

    fun addEffect(glFilter: GLFilter) {
        checkIfInitialized()
        showRenderer.addEffect(glFilter)
    }

    fun removeEffect(glFilter: GLFilter) {
        checkIfInitialized()
        showRenderer.removeEffect(glFilter)
    }

    fun removeAllEffect() {
        checkIfInitialized()
        showRenderer.removeAllEffect()
    }

}