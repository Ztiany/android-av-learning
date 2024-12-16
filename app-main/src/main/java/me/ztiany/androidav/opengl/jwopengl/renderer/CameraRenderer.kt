package me.ztiany.androidav.opengl.jwopengl.renderer

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import androidx.core.content.ContextCompat
import me.ztiany.androidav.opengl.jwopengl.common.EGLBridger
import me.ztiany.androidav.opengl.jwopengl.common.GLRenderer
import me.ztiany.androidav.opengl.jwopengl.gles2.*
import timber.log.Timber

class CameraRenderer(
    private val context: Context,
    private val eglBridger: EGLBridger
) : GLRenderer {

    private val glMVPMatrix = GLMVPMatrix()
    private lateinit var glProgram: GLProgram
    private lateinit var glTexture: GLTexture

    /**用于修正相机的方向*/
    private var displayOrientation = 0
    private var isMirror = false

    /**承载视频的纹理*/
    private lateinit var surfaceTexture: SurfaceTexture

    /**矩形的坐标*/
    private val vertexVbo = generateVBOBuffer(newVertexCoordinateFull3())

    /**纹理坐标*/
    private val textureCoordinateBuffer = generateVBOBuffer(newTextureCoordinateAndroid())

    private var onSurfaceText: ((SurfaceTexture) -> Unit)? = null

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

    fun setVideoAttribute(width: Int, height: Int, displayOrientation: Int, isMirror: Boolean) {
        Timber.d("setVideoAttribute")
        this.displayOrientation = displayOrientation
        this.isMirror = isMirror
        glMVPMatrix.setModelSize(width, height)
        adjustMatrix()
    }

    private fun adjustMatrix() {
        glMVPMatrix.lookAtNormally()
        glMVPMatrix.adjustToOrthogonal()
        glMVPMatrix.combineMVP()
        //绕着 Z 轴旋转
        if (!isMirror) {
            //后摄，一般情况下相机的画面被逆时针转了 90 度，这是这里也将顶点坐标转同样的角度，再去纹理采样
            //注意【顶点是先插值，然后我们利用矩阵再将顶点修正到正确的采样进行位置】。
            Matrix.rotateM(glMVPMatrix.mvpMatrix, 0, -this.displayOrientation.toFloat(), 0F, 0F, 1F)
        } else {
            Matrix.scaleM(glMVPMatrix.mvpMatrix, 0, -1F, 1F, 1F)
            Matrix.rotateM(glMVPMatrix.mvpMatrix, 0, this.displayOrientation.toFloat(), 0F, 0F, 1F)
        }
    }

}