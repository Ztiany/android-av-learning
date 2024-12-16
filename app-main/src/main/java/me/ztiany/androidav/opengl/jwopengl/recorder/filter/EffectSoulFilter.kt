package me.ztiany.androidav.opengl.jwopengl.recorder.filter

import android.opengl.GLES20
import me.ztiany.androidav.opengl.jwopengl.gles2.GLProgram
import me.ztiany.androidav.opengl.jwopengl.gles2.GLTexture
import me.ztiany.androidav.opengl.jwopengl.gles2.activeTexture

/**灵魂出鞘效果，注意：其接收来自相机的纹理。*/
class EffectSoulFilter : BaseEffectFBOFilter() {

    private var mixPercent = 0F
    private var scalePercent = 0F

    override fun createAndInitProgram(): GLProgram {
        val glProgram = GLProgram.fromAssets(
            "shader/vertex_base.glsl",
            "shader/fragment_soul.glsl"
        )

        //vertex
        glProgram.activeAttribute("aPosition")
        glProgram.activeAttribute("aTextureCoordinate")

        //fragment
        glProgram.activeUniform("scalePercent")
        glProgram.activeUniform("mixPercent")
        glProgram.activeUniform("uTexture")

        return glProgram
    }

    override fun setWorldSize(width: Int, height: Int) {

    }

    override fun drawOnFBO(sharedTexture: GLTexture) {
        glProgram.startDraw {
            clearColorBuffer()
            GLES20.glViewport(0, 0, textureWidth, textureHeight)
            //vertex
            vertexAttribPointerFloat("aPosition", 3, vertexVbo)
            vertexAttribPointerFloat("aTextureCoordinate", 2, textureCoordinateBuffer)
            //fragment
            uniform1f("scalePercent", 1.0F + scalePercent)//[1, 2]
            uniform1f("mixPercent", 1.0F - mixPercent)//[1,0]
            updatePercent()
            //texture【将分享过来的纹理绘制到 FBO 的纹理上】
            sharedTexture.activeTexture(uniformHandle("uTexture"))
            //draw
            drawArraysStrip(4/*4 个顶点*/)
        }
    }


    private fun updatePercent() {
        scalePercent += 0.08F
        mixPercent += 0.08F
        if (scalePercent >= 1.0) {
            scalePercent = 0.0F
        }
        if (mixPercent >= 1.0) {
            mixPercent = 0.0F
        }
    }

}