package me.ztiany.androidav.opengl.jwopengl.renderer

import android.opengl.GLES20
import me.ztiany.lib.avbase.utils.FileUtils
import me.ztiany.androidav.opengl.jwopengl.common.GLRenderer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class TriangleRenderer : GLRenderer {

    private var program = 0
    private var aPositionHandle = 0
    private var uColorHandle = 0

    private lateinit var vertexBuffer: FloatBuffer

    private val triangleCoordinate = floatArrayOf(
        0.0F, 0.5F, 0.0F,  // top
        -0.5F, -0.5F, 0.0F,  // bottom left
        0.5F, -0.5F, 0.0F // bottom right
    )

    //设置颜色，依次为红绿蓝和透明通道
    private val color = floatArrayOf(1.0F, 0F, 0F, 1.0F)

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    override fun onSurfaceCreated() {
        //申请 native 空间
        vertexBuffer = ByteBuffer.allocateDirect(triangleCoordinate.size * 4 /*one float has four bytes.*/)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(triangleCoordinate).also {
                it.position(0)
            }//将坐标数据转换为 FloatBuffer

        //生成 Shader
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, FileUtils.loadAssets("shader/vertex_base.glsl"))
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FileUtils.loadAssets("shader/fragment_color.glsl"))

        //创建一个空的OpenGLES程序
        program = GLES20.glCreateProgram()
        //将顶点着色器加入到程序
        GLES20.glAttachShader(program, vertexShader)
        //将片元着色器加入到程序中
        GLES20.glAttachShader(program, fragmentShader)
        //连接到着色器程序
        GLES20.glLinkProgram(program)

        //获取顶点着色器的vPosition成员句柄
        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        //获取片元着色器的vColor成员的句柄
        uColorHandle = GLES20.glGetUniformLocation(program, "aColor")
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(attachment: Any? ) {
        //清理
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        //将程序加入到OpenGLES2.0环境
        GLES20.glUseProgram(program)

        //启用三角形顶点的句柄
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        //准备三角形的坐标数据：CPU -> GPU
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false/*自动修正，不需要*/, 12/*3 * 4*/, vertexBuffer)

        //设置绘制三角形的颜色
        GLES20.glUniform4fv(uColorHandle, 1/*4fv，所以是 1*/, color, 0)
        //绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3/*3 个点*/)

        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glUseProgram(0)
    }

    override fun onSurfaceDestroy() {
    }

}