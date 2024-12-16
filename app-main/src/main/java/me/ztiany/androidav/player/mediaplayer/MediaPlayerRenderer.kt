package me.ztiany.androidav.player.mediaplayer

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import androidx.core.content.ContextCompat
import me.ztiany.androidav.opengl.jwopengl.common.EGLBridger
import me.ztiany.androidav.opengl.jwopengl.common.GLRenderer
import me.ztiany.androidav.opengl.jwopengl.gles2.*
import me.ztiany.lib.avbase.utils.av.MediaMetadata
import timber.log.Timber

class MediaPlayerRenderer(
    private val context: Context,
    private val eglBridger: EGLBridger
) : GLRenderer {

    private val glMVPMatrix = GLMVPMatrix()
    private lateinit var glProgram: GLProgram
    private lateinit var glTexture: GLTexture

    /**用于修正视频的方向*/
    private var displayOrientation = 0

    /**承载视频的纹理*/
    private lateinit var surfaceTexture: SurfaceTexture

    /**矩形的坐标*/
    private val vertexVbo = generateVBOBuffer(newVertexCoordinateFull3())

    /**纹理坐标*/
    private val textureCoordinateBuffer = generateVBOBuffer(newTextureCoordinateAndroid())

    private var onSurfaceText: ((SurfaceTexture) -> Unit)? = null

    private var adjustVideoOrientation = false

    fun getSurfaceTexture(onSurfaceText: (SurfaceTexture) -> Unit) {
        if (::surfaceTexture.isInitialized) {
            onSurfaceText(surfaceTexture)
        } else {
            this.onSurfaceText = onSurfaceText
        }
    }

    override fun onSurfaceCreated() {
        Timber.d("onSurfaceCreated")

        glProgram = GLProgram.fromAssets(
            "shader/vertex_mvp.glsl",
            "shader/fragment_camera.glsl"
        )

        glProgram.activeAttribute("aPosition")
        glProgram.activeAttribute("aTextureCoordinate")
        glProgram.activeUniform("uTexture")
        glProgram.activeUniform("uMVPModelMatrix")

        glTexture = generateTexture(
            glProgram.uniformHandle("uTexture"),
            0,
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        )

        surfaceTexture = SurfaceTexture(glTexture.id)
        ContextCompat.getMainExecutor(context).execute {
            onSurfaceText?.invoke(surfaceTexture)
            onSurfaceText = null
        }
        surfaceTexture.setOnFrameAvailableListener {
            onFrameAvailable(it)
        }
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        Timber.d("onSurfaceChanged")
        GLES20.glViewport(0, 0, width, height)
        glMVPMatrix.setWorldSize(width, height)
        adjustMatrix()
    }

    override fun onDrawFrame(attachment: Any?) {
        glProgram.startDraw {
            clearColorBuffer()
            glTexture.activeTexture()
            surfaceTexture.updateTexImage()
            uniformMatrix4fv("uMVPModelMatrix", glMVPMatrix.mvpMatrix)
            vertexAttribPointerFloat("aPosition", 3, vertexVbo)
            vertexAttribPointerFloat("aTextureCoordinate", 2, textureCoordinateBuffer)
            drawArraysStrip(4/*4 个顶点*/)
        }
    }

    override fun onSurfaceDestroy() {
        Timber.d("onSurfaceDestroy")
    }

    private fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        eglBridger.requestRender()
    }

    private fun adjustMatrix() {
        glMVPMatrix.lookAtNormally()
        glMVPMatrix.adjustToOrthogonal()
        glMVPMatrix.resetToIdentity(glMVPMatrix.modelMatrix)
        if (adjustVideoOrientation) {
            Matrix.rotateM(glMVPMatrix.modelMatrix, 0, -this.displayOrientation.toFloat(), 0F, 0F, 1F)
        }
        glMVPMatrix.combineMVP()
    }

    fun onGetMediaMetaData(metadata: MediaMetadata, adjust: Boolean) {
        adjustVideoOrientation = adjust
        Timber.d("onGetMediaMetaData")
        this.displayOrientation = metadata.rotation
        if ((displayOrientation / 90).mod(2) != 0 && adjustVideoOrientation) {
            Timber.d("mode size = ${metadata.height}x${metadata.width}")
            glMVPMatrix.setModelSize(metadata.height, metadata.width)
        } else {
            Timber.d("mode size = ${metadata.width}x${metadata.height}")
            glMVPMatrix.setModelSize(metadata.width, metadata.height)
        }
        adjustMatrix()
    }

}
