package me.ztiany.androidav.opengl.jwopengl.recorder.filter

import me.ztiany.androidav.opengl.jwopengl.gles2.GLProgram

class EffectSplit3Filter : BaseEffectFilter() {

    override fun createGLProgram(): GLProgram {
        return GLProgram.fromAssets(
            "shader/vertex_base.glsl",
            "shader/fragment_split3.glsl"
        )
    }

}