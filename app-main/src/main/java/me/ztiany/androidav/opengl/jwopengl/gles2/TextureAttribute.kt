package me.ztiany.androidav.opengl.jwopengl.gles2

data class TextureAttribute(
    val width: Int,
    val height: Int,
    val orientation: Int = 0,
    val isFront: Boolean = false,
    val isCamera: Boolean = true
)