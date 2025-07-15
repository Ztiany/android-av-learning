package me.ztiany.androidav.opengl.jwopengl.recorder

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLContext
import android.opengl.GLES11Ext
import androidx.core.content.ContextCompat
import me.ztiany.androidav.opengl.jwopengl.common.EGLBridger
import me.ztiany.androidav.opengl.jwopengl.common.GLRenderer
import me.ztiany.androidav.opengl.jwopengl.gles2.GLTexture
import me.ztiany.androidav.opengl.jwopengl.gles2.TextureAttribute
import me.ztiany.androidav.opengl.jwopengl.gles2.generateTexture
import me.ztiany.androidav.opengl.jwopengl.recorder.filter.GLFilter
import me.ztiany.androidav.opengl.jwopengl.recorder.filter.NoneEffectFBOFilter
import me.ztiany.androidav.opengl.jwopengl.recorder.filter.ScreenFilter
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/** 录像特效 + 展示 */
class RecorderShowRenderer(
    private val context: Context,
    private val eglBridger: EGLBridger
) : GLRenderer {

    /**承载视频的纹理*/
    private lateinit var cameraSurfaceTexture: SurfaceTexture
    private lateinit var cameraTexture: GLTexture
    private var onSurfaceText: ((SurfaceTexture) -> Unit)? = null

    private val foundationFBOFilter = NoneEffectFBOFilter()
    private val foundationScreenFilter = ScreenFilter()

    private val effectFilters = CopyOnWriteArrayList<GLFilter>()

    @Volatile private var recorder: Recorder? = null
    @Volatile private var textureSizeReceived = false

    private lateinit var eglContext: EGLContext

    private var attribute: TextureAttribute? = null

    fun startRecording(recorder: Recorder) {
        this.recorder?.onStop()
        this.recorder = recorder
        this.recorder?.onStart(eglContext)
    }

    fun stopRecording() {
        val stoppingRecorder = this.recorder
        this.recorder = null
        stoppingRecorder?.onStop()
    }

    fun getSurfaceTexture(onSurfaceText: (SurfaceTexture) -> Unit) {
        if (::cameraSurfaceTexture.isInitialized) {
            onSurfaceText(cameraSurfaceTexture)
        } else {
            this.onSurfaceText = onSurfaceText
        }
    }

    override fun onSurfaceCreated() {
        Timber.d("onSurfaceCreated() called")

        eglContext = EGL14.eglGetCurrentContext()

        cameraTexture = generateTexture(
            GLTexture.NONE,
            0,
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        )

        cameraSurfaceTexture = SurfaceTexture(cameraTexture.id)

        ContextCompat.getMainExecutor(context).execute {
            onSurfaceText?.invoke(cameraSurfaceTexture)
            onSurfaceText = null
        }

        cameraSurfaceTexture.setOnFrameAvailableListener {
            onFrameAvailable(it)
        }

        foundationFBOFilter.initProgram()
        foundationScreenFilter.initProgram()
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        Timber.d("onSurfaceChanged() called with: width = $width, height = $height")
        foundationFBOFilter.setWorldSize(width, height)
        foundationScreenFilter.setWorldSize(width, height)
        effectFilters.forEach {
            it.setWorldSize(width, height)
        }
    }

    override fun onDrawFrame(attachment: Any?) {
        if (!textureSizeReceived) {
            return
        }

        cameraSurfaceTexture.updateTexImage()

        //draw raw video on fbo
        var glTexture = foundationFBOFilter.onDrawFrame(cameraTexture)

        //do effect on fbo
        effectFilters.forEach {
            glTexture = it.onDrawFrame(glTexture)
        }

        //draw fbo on screen.
        glTexture = foundationScreenFilter.onDrawFrame(glTexture)

        //send effect to recorder if need.
        recorder?.run {
            onFrame(TextureWithTime(glTexture, cameraSurfaceTexture.timestamp))
        }
    }

    override fun onSurfaceDestroy() {
        Timber.d("onSurfaceDestroy")
    }

    private fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        eglBridger.requestRender()
    }

    fun setVideoAttribute(attribute: TextureAttribute) {
        Timber.d("setVideoAttribute() called with: attribute = $attribute")
        this.attribute = attribute
        textureSizeReceived = true
        foundationFBOFilter.setTextureAttribute(attribute)
        foundationScreenFilter.setTextureAttribute(attribute)
        effectFilters.forEach {
            it.setTextureAttribute(attribute)
        }
    }

    fun addEffect(glFilter: GLFilter) {
        attribute?.let {
            glFilter.setTextureAttribute(it)
        }
        effectFilters.add(glFilter)
    }

    fun removeEffect(glFilter: GLFilter) {
        effectFilters.remove(glFilter)
    }

    fun removeAllEffect() {
        effectFilters.clear()
    }

}

class TextureWithTime(
    /**存储了当前帧的纹理*/
    val glTexture: GLTexture,
    /**单位：nanoseconds*/
    val timestamp: Long
)
