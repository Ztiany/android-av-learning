package me.ztiany.androidav.opengl.jwopengl.gles2

import android.graphics.Bitmap
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import timber.log.Timber

class GLTexture(
    /**An allocated id to this Texture.*/
    val id: Int,
    /**The type of this Texture.*/
    val type: Int,
    /**The handle to Vertex Shader。Not necessary, May be 0.*/
    val handleInShader: Int,
    /**Not necessary, May be 0.*/
    val index: Int,
    /**Not necessary, May be 0.*/
    val width: Int,
    /**Not necessary, May be 0.*/
    val height: Int,
) {

    companion object {
        const val NONE = -1
    }

    override fun toString(): String {
        return "GLTexture(id=$id, type=$type, handleInShader=$handleInShader, width=$width, height=$height, index=$index)"
    }
}

fun GLTexture.activeTexture() {
    if (handleInShader == GLTexture.NONE) {
        throw IllegalStateException("The GLTexture was not bound with a handle. Call an alternative one instead.")
    }
    //激活指定纹理单元【有 32 个纹理单位，默认只有 0 号纹理单元是激活的】为了代码的统一性，不管哪个纹理，都调用一次 glActiveTexture，多次调用没有问题。
    GLES20.glActiveTexture(getTextureIdentificationByIndex(index))
    //绑定纹理 ID 到当前激活的纹理单元。
    GLES20.glBindTexture(type, id)
    //将激活的纹理单元传递到着色器里面。
    //我们使用 glUniform1i 设置 uniform 采样器的位置值，或者说纹理单元。通过 glUniform1i 的设置，我们保证每个 uniform 采样器对应着正确的纹理单元。
    //为一个纹理变量指定纹理数值 0，表示从 GL_TEXTURE0 采样。
    //为一个纹理变量指定纹理数值 1，表示从 GL_TEXTURE1 采样。
    GLES20.glUniform1i(handleInShader, index)
}

fun GLTexture.activeTexture(handle: Int) {
    GLES20.glActiveTexture(getTextureIdentificationByIndex(index))
    GLES20.glBindTexture(type, id)
    GLES20.glUniform1i(handle, index)
}

fun GLTexture.activeTexture(handle: Int, index: Int) {
    GLES20.glActiveTexture(getTextureIdentificationByIndex(index))
    GLES20.glBindTexture(type, id)
    GLES20.glUniform1i(handle, index)
}

fun GLTexture.deleteTexture() {
    GLES20.glBindTexture(type, 0)
    GLES20.glDeleteTextures(1, intArrayOf(id), 0)
}

/**
 * - [textureHandle]：纹理句柄【非必须时传 [GLTexture.NONE]】
 * - [textureType]: 比如 [GLES20.GL_TEXTURE_2D]，或者 [GLES11Ext.GL_TEXTURE_EXTERNAL_OES] 等。
 * - [textureIndex]：0-31 闭区间
 */
fun generateTexture(
    textureHandle: Int,
    textureIndex: Int,
    textureType: Int = GLES20.GL_TEXTURE_2D
): GLTexture {
    //申请纹理
    val textureObjectIds = allocateTextureObject()
    //设置通用属性
    setCommonAttribute(textureType, textureObjectIds[0])
    //返回纹理封装对象
    return GLTexture(textureObjectIds[0], textureType, textureHandle, textureIndex, 0, 0).apply {
        Timber.d("new GLTexture: $this with Identification ${getTextureIdentificationByIndex(index)}")
    }
}

/**
 * - [textureHandle]：纹理句柄【非必须时传 [GLTexture.NONE]】
 * - [bitmap]：纹理图片
 * - [textureIndex]：0-31 闭区间
 */
fun generateTextureFromBitmap(
    textureHandle: Int,
    textureIndex: Int,
    bitmap: Bitmap
): GLTexture {
    //申请纹理
    val textureObjectIds = allocateTextureObject()

    //设置通用属性
    val textureType = GLES20.GL_TEXTURE_2D
    setCommonAttribute(textureType, textureObjectIds[0])

    //通过 OpenGL 对象读取 Bitmap 数据，并且绑定到纹理对象上，绑定之后就可以回收 Bitmap 对象。
    GLES20.glBindTexture(textureType, textureObjectIds[0])
    GLUtils.texImage2D(textureType, 0, bitmap, 0)
    GLES20.glBindTexture(textureType, 0)

    //返回纹理封装对象
    return GLTexture(textureObjectIds[0], textureType, textureHandle, textureIndex, bitmap.width, bitmap.height).apply {
        Timber.d("new GLTexture: $this with Identification ${getTextureIdentificationByIndex(index)}")
    }
}

/**
 * - [textureHandle]：纹理句柄【非必须时传 [GLTexture.NONE]】
 * - [textureIndex]：0-31 闭区间
 * - [width]：纹理宽度
 * - [height]：纹理高度
 */
fun generateFBOTexture(
    textureHandle: Int = 0,
    textureIndex: Int,
    width: Int,
    height: Int
): GLTexture {
    //申请纹理
    val textureObjectIds = allocateTextureObject()

    //设置通用属性
    val textureType = GLES20.GL_TEXTURE_2D
    setCommonAttribute(textureType, textureObjectIds[0])

    //这里应该是申请了内存
    GLES20.glBindTexture(textureType, textureObjectIds[0])
    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
    GLES20.glBindTexture(textureType, 0)

    //返回纹理封装对象
    return GLTexture(textureObjectIds[0], textureType, textureHandle, textureIndex, width, height).apply {
        Timber.d("new GLTexture: $this with Identification ${getTextureIdentificationByIndex(index)}")
    }
}


private fun setCommonAttribute(textureType: Int, textureObjectId: Int) {
    //绑定到这个纹理对象，之后的纹理操作就是对这个纹理对象进行操作。
    GLES20.glBindTexture(textureType, textureObjectId)

    //设置纹理缩放过滤
    // GL_NEAREST: 使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
    // GL_LINEAR: 使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色，速度较慢，但视觉效果好
    GLES20.glTexParameteri(textureType, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(textureType, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

    //纹理坐标的范围是 0-1。超出这一范围的坐标将被 OpenGL 根据 GL_TEXTURE_WRAP 参数的值进行处理
    //  GL_TEXTURE_WRAP_S, GL_TEXTURE_WRAP_T 分别为 x，y 方向。
    //  GL_REPEAT：平铺。
    //  GL_MIRRORED_REPEAT：纹理坐标是奇数时使用镜像平铺。
    //  GL_CLAMP_TO_EDGE：坐标超出部分被截取成 0、1，边缘拉伸。
    GLES20.glTexParameteri(textureType, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
    GLES20.glTexParameteri(textureType, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

    //操作完了，就解除绑定
    GLES20.glBindTexture(textureType, 0)
}

private fun allocateTextureObject(): IntArray {
    /*
    申请一个纹理对象，返回这个对象的 id，注意：
        1. The generated textures have no dimensionality; they assume the dimensionality of the texture target to which they are first bound (see glBindTexture).
        2. glGenTextures only allocates texture "names" (eg ids) with no "dimensionality". So you are not actually allocating texture memory as such, and the overhead here is negligible compared to actual texture memory allocation.
        3. glTexImage will actually control the amount of texture memory used per texture.
     */
    val textureObjectIds = IntArray(1)
    GLES20.glGenTextures(1, textureObjectIds, 0)
    if (textureObjectIds[0] == 0) {
        Timber.w("Could not generate a new OpenGL texture object.")
        throw IllegalStateException()
    }

    return textureObjectIds
}

/**
 * 1. OpenGL 一共有 32 个纹理位置（单元），从 0-31 号纹理都有对应的标识，定义在 [GLES20] 中。
 * 2. 一个纹理的默认纹理位置是 0，它是默认的激活纹理单元。
 * 3. 纹理单元的主要目的是让我们在着色器中可以使用多于一个的纹理。通过把纹理单元赋值给采样器，我们可以一次绑定多个纹理。
 */
private fun getTextureIdentificationByIndex(textureIndex: Int): Int {
    return when (textureIndex) {
        0 -> GLES20.GL_TEXTURE0
        1 -> GLES20.GL_TEXTURE1
        2 -> GLES20.GL_TEXTURE2
        3 -> GLES20.GL_TEXTURE3
        4 -> GLES20.GL_TEXTURE4
        5 -> GLES20.GL_TEXTURE5
        6 -> GLES20.GL_TEXTURE6
        7 -> GLES20.GL_TEXTURE7
        8 -> GLES20.GL_TEXTURE8
        9 -> GLES20.GL_TEXTURE9
        10 -> GLES20.GL_TEXTURE10
        11 -> GLES20.GL_TEXTURE11
        12 -> GLES20.GL_TEXTURE12
        13 -> GLES20.GL_TEXTURE13
        14 -> GLES20.GL_TEXTURE14
        15 -> GLES20.GL_TEXTURE15
        16 -> GLES20.GL_TEXTURE16
        17 -> GLES20.GL_TEXTURE17
        18 -> GLES20.GL_TEXTURE18
        19 -> GLES20.GL_TEXTURE19
        20 -> GLES20.GL_TEXTURE20
        21 -> GLES20.GL_TEXTURE21
        22 -> GLES20.GL_TEXTURE22
        23 -> GLES20.GL_TEXTURE23
        24 -> GLES20.GL_TEXTURE24
        25 -> GLES20.GL_TEXTURE25
        26 -> GLES20.GL_TEXTURE26
        27 -> GLES20.GL_TEXTURE27
        28 -> GLES20.GL_TEXTURE28
        29 -> GLES20.GL_TEXTURE29
        30 -> GLES20.GL_TEXTURE30
        31 -> GLES20.GL_TEXTURE31
        else -> throw IllegalArgumentException()
    }
}