package me.ztiany.androidav.opengl.jwopengl.recorder

import android.opengl.EGLContext
import android.opengl.GLES20
import android.view.Surface
import me.ztiany.androidav.opengl.jwopengl.common.GLRenderer
import me.ztiany.androidav.opengl.jwopengl.common.RenderMode
import me.ztiany.androidav.opengl.jwopengl.common.SurfaceProvider
import me.ztiany.androidav.opengl.jwopengl.common.SurfaceProviderCallback
import me.ztiany.androidav.opengl.jwopengl.egl14.*
import me.ztiany.androidav.opengl.jwopengl.gles2.*
import me.ztiany.androidav.opengl.jwopengl.recorder.encoder.Encoder
import me.ztiany.androidav.opengl.jwopengl.recorder.encoder.EncoderMode
import timber.log.Timber

/** 渲染的是 FBO 中的纹理，使用标准的坐标系 */
class RecorderEncodeRenderer : GLRenderer {

    /**CPU 着色器程序*/
    private lateinit var glProgram: GLProgram

    /**矩形的坐标*/
    private val vertexVbo = generateVBOBuffer(newVertexCoordinateFull3())

    /**纹理坐标*/
    private val textureCoordinateBuffer = generateVBOBuffer(newTextureCoordinateStandard())

    /**自定义的 EGL 环境*/
    private var eglEnvironment: EGLEnvironment? = null

    private var mediaCodecSurfaceProvider: MediaCodecSurfaceProvider? = null

    /**编码器*/
    private var encoder: Encoder? = null

    override fun onSurfaceCreated() {
        Timber.d("onSurfaceCreated() called")
        glProgram = GLProgram.fromAssets(
            "shader/vertex_base.glsl",
            "shader/fragment_texture.glsl"
        )

        //vertex
        glProgram.activeAttribute("aPosition")
        glProgram.activeAttribute("aTextureCoordinate")

        //fragment
        glProgram.activeUniform("uTexture")
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        Timber.d("onSurfaceChanged() called with: width = $width, height = $height")
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(attachment: Any?) {
        val textureWithTime = attachment as? TextureWithTime ?: return
        glProgram.startDraw {
            //vertex
            vertexAttribPointerFloat("aPosition", 3, vertexVbo)
            vertexAttribPointerFloat("aTextureCoordinate", 2, textureCoordinateBuffer)
            //fragment
            textureWithTime.glTexture.activeTexture(uniformHandle("uTexture"))
            //draw
            drawArraysStrip(4/*4 个顶点*/)
            //setTime【按照文档来说，这里其实没有必要设置】
            eglEnvironment?.setPresentationTime(textureWithTime.timestamp)
        }
    }

    override fun onSurfaceDestroy() {
        Timber.d("onSurfaceDestroy")
    }

    private var textureWidth = 0
    private var textureHeight = 0

    fun setVideoAttribute(attribute: TextureAttribute) {
        Timber.d("setVideoAttribute() called with: attribute = $attribute")
        if ((attribute.orientation / 90).mod(2) == 1) {// 竖屏
            textureWidth = attribute.height
            textureHeight = attribute.width
        } else {// 横屏
            textureWidth = attribute.width
            textureHeight = attribute.height
        }
    }

    fun start(sharedEGLContext: EGLContext, encoder: Encoder) {
        if (this.encoder != null) {
            throw UnsupportedOperationException("Already started..")
        }

        this.encoder = encoder
        encoder.init(textureWidth, textureHeight)

        if (encoder.mode == EncoderMode.Hard) {
            startWithHardEncoder(sharedEGLContext, encoder)
        } else {
            throw UnsupportedOperationException("Not supported.")
        }
    }

    private fun startWithHardEncoder(sharedEGLContext: EGLContext, encoder: Encoder) {
        encoder.start()
        val surfaceProvider = MediaCodecSurfaceProvider(encoder.getInputSurfaceView())
        eglEnvironment = EGLEnvironment(
            surfaceProvider,
            EGLAttribute(sharedEGLContext)
        ).apply {
            renderMode = RenderMode.WhenDirty
            start(this@RecorderEncodeRenderer)
        }
        this.mediaCodecSurfaceProvider = surfaceProvider
    }

    fun stop() {
        mediaCodecSurfaceProvider?.stop()
        eglEnvironment?.release()
        encoder?.stop()
        eglEnvironment = null
        mediaCodecSurfaceProvider = null
        encoder = null
    }

    fun onFrame(frame: TextureWithTime) {
        eglEnvironment?.requestRender(frame)
    }

    private inner class MediaCodecSurfaceProvider(private val surface: Surface) : SurfaceProvider {

        private lateinit var surfaceProviderCallback: SurfaceProviderCallback

        override fun start(surfaceProviderCallback: SurfaceProviderCallback) {
            if (this::surfaceProviderCallback.isInitialized) {
                throw UnsupportedOperationException("call this only once.")
            }
            this.surfaceProviderCallback = surfaceProviderCallback
            surfaceProviderCallback.onSurfaceAvailable(surface)
            surfaceProviderCallback.onSurfaceChanged(surface, textureWidth, textureHeight)
        }

        override fun stop() {
            surfaceProviderCallback.onSurfaceDestroyed()
        }
    }

}