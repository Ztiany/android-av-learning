package me.ztiany.androidav.opengl.jwopengl.gles2

import android.opengl.GLES20
import me.ztiany.lib.avbase.utils.FileUtils
import java.nio.FloatBuffer

class GLProgram(
    vertexSource: String,
    fragmentSource: String
) {

    private val programHandle = generateGLProgram(vertexSource, fragmentSource)

    private val attributeMap = mutableMapOf<String, Int>()
    private val uniformMap = mutableMapOf<String, Int>()

    companion object {
        fun fromAssets(vertexPath: String, fragmentPath: String) = GLProgram(
            FileUtils.loadAssets(vertexPath),
            FileUtils.loadAssets(fragmentPath)
        )
    }

    fun activeAttribute(attributeName: String) {
        val attribLocation = GLES20.glGetAttribLocation(programHandle, attributeName)
        attributeMap[attributeName] = attribLocation
    }

    fun activeUniform(uniformName: String) {
        val attribLocation = GLES20.glGetUniformLocation(programHandle, uniformName)
        uniformMap[uniformName] = attribLocation
    }

    fun setBgColor(red: Float, green: Float, blue: Float, alpha: Float) {
        GLES20.glClearColor(red, green, blue, alpha)
    }

    fun clearColorBuffer() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    fun clearBuffer(bufferMask: Int) {
        GLES20.glClear(bufferMask)
    }

    fun startDraw(onDraw: GLProgram.() -> Unit) {
        GLES20.glUseProgram(programHandle)

        attributeMap.values.forEach {
            GLES20.glEnableVertexAttribArray(it)
        }

        onDraw(this)

        attributeMap.values.forEach {
            GLES20.glDisableVertexAttribArray(it)
        }
        GLES20.glUseProgram(0)
    }

    /**
     * - elementsPerVertex: 每个点几个 float。
     */
    fun vertexAttribPointerFloat(attribute: String, elementsPerVertex: Int, vbo: FloatBuffer) {
        GLES20.glVertexAttribPointer(
            attributeHandle(attribute),
            elementsPerVertex,
            GLES20.GL_FLOAT,
            false,
            elementsPerVertex * 4/*每个点 4 个 float，每个 float 4 个  byte*/,
            vbo
        )
    }

    fun drawArraysStrip(count: Int) {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, count)
    }

    /**
     * - elementsPerVertex: 每个点几个 float。
     */
    fun vertexAttribPointerInt(attribute: String, elementsPerVertex: Int, vbo: FloatBuffer) {
        GLES20.glVertexAttribPointer(
            attributeHandle(attribute),
            elementsPerVertex,
            GLES20.GL_INT,
            false,
            elementsPerVertex * 4/*每个点 4 个 float，每个 float 4 个  byte*/,
            vbo
        )
    }

    fun uniformMatrix4fv(uniformName: String, matrix: FloatArray) {
        GLES20.glUniformMatrix4fv(
            uniformHandle(uniformName),
            1,
            false,
            matrix,
            0
        )
    }

    fun uniform1f(uniformName: String, value: Float) {
        GLES20.glUniform1f(uniformHandle(uniformName), value)
    }

    fun uniform1i(uniformName: String, value: Int) {
        GLES20.glUniform1i(uniformHandle(uniformName), value)
    }

    fun attributeHandle(attribute: String) = attributeMap[attribute] ?: throw NoSuchElementException()

    fun uniformHandle(attribute: String) = uniformMap[attribute] ?: throw NoSuchElementException()

}