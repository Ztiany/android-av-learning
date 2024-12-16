package me.ztiany.androidav.opengl.jwopengl.gles2

import android.opengl.GLES20

class GLFBO(val name: Int) {
    override fun toString(): String {
        return "GLFBO(name=$name)"
    }
}

class GLFBOWithTexture(
    val id: Int,
    val texture: GLTexture
) {
    override fun toString(): String {
        return "GLFBOWithTexture(id=$id, texture=$texture)"
    }
}

fun generateFBO(): GLFBO {
    val frameBufferIds = intArrayOf(1)
    GLES20.glGenFramebuffers(1, frameBufferIds, 0)
    return GLFBO(frameBufferIds[0])
}

fun generateFBOWithTexture(glTexture: GLTexture): GLFBOWithTexture {
    val frameBufferIds = intArrayOf(1)
    GLES20.glGenFramebuffers(1, frameBufferIds, 0)
    return GLFBOWithTexture(frameBufferIds[0], glTexture)
}

fun GLFBOWithTexture.bindFBO() {
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, id)
    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture.id, 0)
}

fun unbindFBO() {
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
}

fun GLFBOWithTexture.use(onDraw: GLFBOWithTexture.() -> Unit) {
    bindFBO()
    onDraw(this)
    unbindFBO()
}

fun GLFBOWithTexture.delete() {
    //删除 FBO
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
    GLES20.glDeleteFramebuffers(1, intArrayOf(id), 0)
    //删除 Texture
    texture.deleteTexture()
}