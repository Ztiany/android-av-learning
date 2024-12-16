package me.ztiany.androidav.opengl.jwopengl.recorder.filter

import me.ztiany.androidav.opengl.jwopengl.gles2.*
import timber.log.Timber

/**灵魂出鞘效果，注意：其接收来自相机的纹理。*/
abstract class BaseEffectFBOFilter : BaseGLFilter() {

    private var glFBO: GLFBOWithTexture? = null

    /**矩形的坐标*/
    protected val vertexVbo = generateVBOBuffer(newVertexCoordinateFull3())

    /**纹理坐标*/
    protected val textureCoordinateBuffer = generateVBOBuffer(newTextureCoordinateStandard())

    protected var textureWidth = 0
        private set
    protected var textureHeight = 0
        private set

    override fun setWorldSize(width: Int, height: Int) {

    }

    override fun setTextureAttribute(attribute: TextureAttribute) {
        this.textureWidth = attribute.width
        this.textureHeight = attribute.height
    }

    final override fun doDraw(sharedTexture: GLTexture): GLTexture {
        val fbo = getFBO()
        fbo.use {
            drawOnFBO(sharedTexture)
        }
        return fbo.texture
    }

    abstract fun drawOnFBO(sharedTexture: GLTexture)

    private fun getFBO(): GLFBOWithTexture {
        var fbo = glFBO

        if (fbo != null && (fbo.texture.width != textureWidth || fbo.texture.height != textureHeight)) {
            fbo.delete()
            fbo = null
        }

        if (fbo == null) {
            Timber.d("create new fbo $textureWidth x $textureHeight.")
            val glTexture = generateFBOTexture(
                glProgram.uniformHandle("uTexture"),
                0,
                //use the real texture size.
                textureWidth,
                textureHeight
            )
            fbo = generateFBOWithTexture(glTexture)
            glFBO = fbo
        }

        return fbo
    }

}