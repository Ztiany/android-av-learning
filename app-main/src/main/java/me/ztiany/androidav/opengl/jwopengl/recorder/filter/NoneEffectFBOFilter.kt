package me.ztiany.androidav.opengl.jwopengl.recorder.filter

import android.opengl.GLES20
import android.opengl.Matrix
import me.ztiany.androidav.opengl.jwopengl.gles2.*
import timber.log.Timber

class NoneEffectFBOFilter : BaseGLFilter() {

    private var glFBO: GLFBOWithTexture? = null

    /**用于修正坐标位置*/
    private val glMVPMatrix = GLMVPMatrix()

    /**矩形的坐标*/
    private val vertexVbo = generateVBOBuffer(newVertexCoordinateFull3())

    /**纹理坐标*/
    private val textureCoordinateBuffer = generateVBOBuffer(newTextureCoordinateAndroid())

    override fun createAndInitProgram(): GLProgram {
        Timber.d("createAndInitProgram() called")

        val glProgram = GLProgram.fromAssets(
            "shader/vertex_mvp.glsl",
            "shader/fragment_camera.glsl"
        )

        //vertex
        glProgram.activeAttribute("aPosition")
        glProgram.activeAttribute("aTextureCoordinate")
        glProgram.activeUniform("uMVPModelMatrix")

        //fragment
        glProgram.activeUniform("uTexture")

        return glProgram
    }

    override fun setWorldSize(width: Int, height: Int) {

    }

    override fun setTextureAttribute(attribute: TextureAttribute) {
        Timber.d("setVideoAttribute() called with: attribute = $attribute")
        if ((attribute.orientation / 90).mod(2) == 1) {//竖屏
            glMVPMatrix.setWorldSize(attribute.height, attribute.width)
            glMVPMatrix.setModelSize(attribute.height, attribute.width)
        } else {//横屏
            glMVPMatrix.setWorldSize(attribute.width, attribute.height)
            glMVPMatrix.setModelSize(attribute.width, attribute.height)
        }

        glMVPMatrix.resetToIdentity(glMVPMatrix.mvpMatrix)
        glMVPMatrix.lookAtNormally()
        glMVPMatrix.adjustToOrthogonal()
        glMVPMatrix.combineMVP()
        //绕着 Z 轴旋转
        if (!attribute.isFront) {
            //后摄，一般情况下相机的画面被逆时针转了 90 度，这是这里也将顶点坐标转同样的角度。
            //注意【顶点是先插值，然后我们利用矩阵再将顶点修正到正确的采样进行位置】。
            Matrix.rotateM(glMVPMatrix.mvpMatrix, 0, -attribute.orientation.toFloat(), 0F, 0F, 1F)
        } else {
            Matrix.scaleM(glMVPMatrix.mvpMatrix, 0, -1F, 1F, 1F)
            Matrix.rotateM(glMVPMatrix.mvpMatrix, 0, attribute.orientation.toFloat(), 0F, 0F, 1F)
        }
    }

    override fun doDraw(sharedTexture: GLTexture): GLTexture {
        val fbo = getFBO()
        fbo.use {
            glProgram.startDraw {
                clearColorBuffer()
                GLES20.glViewport(0, 0, glMVPMatrix.getModelWidth(), glMVPMatrix.getModelHeight())
                //vertex
                vertexAttribPointerFloat("aPosition", 3, vertexVbo)
                vertexAttribPointerFloat("aTextureCoordinate", 2, textureCoordinateBuffer)
                uniformMatrix4fv("uMVPModelMatrix", glMVPMatrix.mvpMatrix)
                //texture【将分享过来的视频纹理绘制到自己的 FBO 对应的纹理上】
                sharedTexture.activeTexture(uniformHandle("uTexture"))
                //draw
                drawArraysStrip(4/*4 个顶点*/)
            }
        }
        return fbo.texture
    }

    private fun getFBO(): GLFBOWithTexture {
        var fbo = glFBO

        if (fbo != null && (fbo.texture.width != glMVPMatrix.getModelWidth() || fbo.texture.height != glMVPMatrix.getModelHeight())) {
            fbo.delete()
            fbo = null
        }

        if (fbo == null) {
            Timber.d("create new fbo ${glMVPMatrix.getModelWidth()} x ${glMVPMatrix.getModelHeight()}.")
            val glTexture = generateFBOTexture(
                glProgram.uniformHandle("uTexture"),
                0,
                //use the real texture size.
                glMVPMatrix.getModelWidth(),
                glMVPMatrix.getModelHeight()
            )
            fbo = generateFBOWithTexture(glTexture)
            glFBO = fbo
        }

        return fbo
    }

}