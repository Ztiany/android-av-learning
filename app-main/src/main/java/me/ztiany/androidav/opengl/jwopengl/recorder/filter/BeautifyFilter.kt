package me.ztiany.androidav.opengl.jwopengl.recorder.filter

import me.ztiany.androidav.opengl.jwopengl.gles2.GLProgram

class BeautifyFilter(private val version: Int) : BaseEffectFilter() {

    override fun createGLProgram(): GLProgram {
        return GLProgram.fromAssets(
            "shader/vertex_base.glsl",
            if (version == 1) "shader/fragment_beautify1.glsl" else "shader/fragment_beautify2.glsl"
        ).apply {
            activeUniform("width")
            activeUniform("height")
        }
    }

    override fun beforeDraw() {
        with(glProgram) {
            uniform1i("width", textureWidth)
            uniform1i("height", textureHeight)
        }
    }

}