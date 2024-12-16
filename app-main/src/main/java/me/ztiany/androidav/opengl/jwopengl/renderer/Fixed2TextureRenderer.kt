package me.ztiany.androidav.opengl.jwopengl.renderer

import android.opengl.GLES20
import me.ztiany.androidav.R
import me.ztiany.lib.avbase.utils.loadBitmap
import me.ztiany.androidav.opengl.jwopengl.gles2.*
import me.ztiany.androidav.opengl.jwopengl.common.GLRenderer

class Fixed2TextureRenderer : GLRenderer {

    private lateinit var program: GLProgram
    private lateinit var glTexture: GLTexture

    private val mvpMatrix by lazy { GLMVPMatrix() }

    /**矩形的坐标*/
    private val vertexVbo = generateVBOBuffer(newVertexCoordinateFull3())

    /**纹理坐标*/
    private val textureCoordinateBuffer = generateVBOBuffer(newTextureCoordinateAndroid())

    override fun onSurfaceCreated() {
        program = GLProgram.fromAssets(
            "shader/vertex_mvp_separated.glsl",
            "shader/fragment_texture.glsl"
        )

        program.activeAttribute("aPosition")
        program.activeAttribute("aTextureCoordinate")
        program.activeUniform("uTexture")
        program.activeUniform("uModelMatrix")
        program.activeUniform("uViewMatrix")
        program.activeUniform("uProjectionMatrix")

        glTexture = generateTextureFromBitmap(
            program.uniformHandle("uTexture"),
            0,
            loadBitmap(R.drawable.beautiful_gril1)
        )
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        mvpMatrix.setWorldSize(width, height)
        mvpMatrix.setModelSize(glTexture.width, glTexture.height)
        mvpMatrix.lookAtNormally()
        mvpMatrix.adjustToOrthogonal()
    }

    override fun onDrawFrame(attachment: Any? ) {
        program.startDraw {
            clearColorBuffer()
            glTexture.activeTexture()
            uniformMatrix4fv("uModelMatrix", mvpMatrix.modelMatrix)
            uniformMatrix4fv("uViewMatrix", mvpMatrix.viewMatrix)
            uniformMatrix4fv("uProjectionMatrix", mvpMatrix.projectionMatrix)
            vertexAttribPointerFloat("aPosition", 3, vertexVbo)
            vertexAttribPointerFloat("aTextureCoordinate", 2, textureCoordinateBuffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4/*4 个点*/)
        }
    }

    override fun onSurfaceDestroy() {
    }

}