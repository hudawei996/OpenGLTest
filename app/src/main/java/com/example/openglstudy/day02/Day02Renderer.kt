package com.example.openglstudy.day02

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Day 02 Renderer
 * 学习目标：渲染第一个三角形
 * 实现功能：顶点数据、着色器、绘制三角形
 */
class Day02Renderer : GLSurfaceView.Renderer {

    // 顶点着色器代码
    private val vertexShaderCode = """
        attribute vec4 aPosition;
        void main() {
            gl_Position = aPosition;
        }
    """.trimIndent()

    // 片段着色器代码（橙色）
    private val fragmentShaderCode = """
        precision mediump float;
        void main() {
            gl_FragColor = vec4(1.0, 0.5, 0.2, 1.0);
        }
    """.trimIndent()

    // 三角形顶点坐标
    private val vertices = floatArrayOf(
         0.0f,  0.5f, 0.0f,  // 顶点 0：上方
        -0.5f, -0.5f, 0.0f,  // 顶点 1：左下
         0.5f, -0.5f, 0.0f   // 顶点 2：右下
    )

    private lateinit var vertexBuffer: FloatBuffer
    private var program: Int = 0
    private var aPositionLocation: Int = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置背景色（黑色）
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 创建顶点缓冲
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)

        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // 创建程序
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        // 检查链接状态
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Program link failed: $log")
        }

        // 获取 attribute 位置
        aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 使用程序
        GLES20.glUseProgram(program)

        // 启用顶点属性
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        // 传递顶点数据
        GLES20.glVertexAttribPointer(
            aPositionLocation,  // 属性位置
            3,                  // 每个顶点的分量数（x, y, z）
            GLES20.GL_FLOAT,    // 数据类型
            false,              // 是否归一化
            0,                  // 步长（0 表示紧密排列）
            vertexBuffer        // 数据缓冲
        )

        // 绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)

        // 禁用顶点属性
        GLES20.glDisableVertexAttribArray(aPositionLocation)
    }

    /**
     * 加载着色器
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        // 检查编译状态
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compilation failed: $log")
        }

        return shader
    }
}
