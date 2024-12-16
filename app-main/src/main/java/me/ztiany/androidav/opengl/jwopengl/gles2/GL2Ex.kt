@file:JvmName("GL2Ex")

package me.ztiany.androidav.opengl.jwopengl.gles2

import android.opengl.GLES20
import me.ztiany.lib.avbase.utils.FileUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * 返回一个矩形的顶点数组，每个点 4 个元素。顺序为：
 * 1. left-bottom
 * 2. right-bottom
 * 3. left-top
 * 4. right-top
 */
fun newVertexCoordinateFull4() = floatArrayOf(
    -1.0f, -1.0f, 0.0F, 1.0F,  //left-bottom
    1.0f, -1.0f, 0.0F, 1.0F, //right-bottom
    -1.0f, 1.0f, 0.0F, 1.0F, //left-top
    1.0f, 1.0f, 0.0F, 1.0F,//right-top
)

/**
 * 返回一个矩形的顶点数组，每个点 3 个元素。顺序为：
 * 1. left-bottom
 * 2. right-bottom
 * 3. left-top
 * 4. right-top
 */
fun newVertexCoordinateFull3() = floatArrayOf(
    -1.0F, -1.0F, 0.0F,   //left-bottom
    1.0F, -1.0F, 0.0F,  //right-bottom
    -1.0F, 1.0F, 0.0F,  //left-top
    1.0F, 1.0F, 0.0F,//right-top
)

fun newRectangleIndices() = intArrayOf(
    0, 1, 2,
    2, 3, 1
)

/**
 * 返回一个矩形的纹理坐标数组，每个点 2 个元素。顺序为：
 * 1. left-bottom
 * 2. right-bottom
 * 3. left-top
 * 4. right-top
 */
fun newTextureCoordinateStandard() = floatArrayOf(
    0.0F, 0.0F,//left-bottom
    1.0F, 0.0F,//right-bottom
    0.0F, 1.0F,//left-top
    1.0F, 1.0F,//right-top
)

/**
 * 返回一个矩形的纹理坐标数组，每个点 2 个元素。顺序为：
 * 1. left-bottom
 * 2. right-bottom
 * 3. left-top
 * 4. right-top
 */
fun newTextureCoordinateAndroid() = floatArrayOf(
    0.0F, 1.0F,//left-bottom
    1.0F, 1.0F,//right-bottom
    0.0F, 0.0F,//left-top
    1.0F, 0.0F//right-top
)

fun loadShader(type: Int, shaderCode: String): Int {
    val shader = GLES20.glCreateShader(type)
    //设置源码
    GLES20.glShaderSource(shader, shaderCode)
    //编译源码
    GLES20.glCompileShader(shader)

    //状态检测
    val status = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
    check(status[0] == GLES20.GL_TRUE) {
        "load vertex shader:" + GLES20.glGetShaderInfoLog(shader)
    }
    return shader
}

fun loadVertexShader(shaderCode: String): Int {
    return loadShader(GLES20.GL_VERTEX_SHADER, shaderCode)
}

fun loadFragmentShader(shaderCode: String): Int {
    return loadShader(GLES20.GL_FRAGMENT_SHADER, shaderCode)
}

fun loadShaderFromAssets(type: Int, path: String): Int {
    return loadShader(type, FileUtils.loadAssets(path))
}

fun loadVertexShaderFromAssets(type: Int, path: String): Int {
    return loadShaderFromAssets(GLES20.GL_VERTEX_SHADER, path)
}

fun loadFragmentShaderFromAssets(type: Int, path: String): Int {
    return loadShaderFromAssets(GLES20.GL_FRAGMENT_SHADER, path)
}

fun generateGLProgram(vertexSource: String, fragmentSource: String): Int {
    //创建着色器程序
    val program = GLES20.glCreateProgram()

    //加载着色器
    val vShader = loadVertexShader(vertexSource)
    val fShader = loadFragmentShader(fragmentSource)

    //绑定着色器
    GLES20.glAttachShader(program, vShader)
    GLES20.glAttachShader(program, fShader)
    //连接到着色器程序
    GLES20.glLinkProgram(program)

    //检测状态
    val status = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
    if (status[0] != GLES20.GL_TRUE) {
        throw   IllegalStateException("link program:" + GLES20.glGetProgramInfoLog(program));
    }

    //释放资源
    GLES20.glDeleteShader(vShader)
    GLES20.glDeleteShader(fShader)
    return program
}

fun generateGLProgramFromAssets(vertexPath: String, fragmentPath: String): Int {
    return generateGLProgram(FileUtils.loadAssets(vertexPath), FileUtils.loadAssets(fragmentPath))
}

/**
 * 顶点缓冲对象（Vertex Buffer Objects，VBO），VBO 是在显卡存储空间中开辟出的一块内存缓存区， * 用于存储顶点的各类属性信息，如顶点坐标，顶点法向量，顶点颜色数据等。
 */
fun generateVBOBuffer(vboData: FloatArray): FloatBuffer = ByteBuffer.allocateDirect(vboData.size * 4 /*one float has four bytes.*/)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .put(vboData).also { it.position(0) }//将坐标数据转换为 FloatBuffer
