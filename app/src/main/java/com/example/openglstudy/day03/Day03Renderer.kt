package com.example.openglstudy.day03

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Day 03 Renderer
 * 学习目标：着色器基础 - GLSL 语言
 * 实现功能：渐变色三角形，使用 varying 变量传递颜色
 */
class Day03Renderer : GLSurfaceView.Renderer {

    // 顶点着色器代码
    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec4 aColor;
        varying vec4 vColor;
        void main() {
            vColor = aColor;
            gl_Position = aPosition;
        }
    """.trimIndent()

    // 片段着色器代码
    private val fragmentShaderCode = """
        precision mediump float;
        varying vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """.trimIndent()

    // 顶点数据（位置 + 颜色）
    // 格式：x, y, z, r, g, b, a
    private val verticesWithColor = floatArrayOf(
        // 位置             颜色（RGBA）
         0.0f,  0.5f, 0.0f,  1.0f, 0.0f, 0.0f, 1.0f,  // 顶点 0：上方（红色）
        -0.5f, -0.5f, 0.0f,  0.0f, 1.0f, 0.0f, 1.0f,  // 顶点 1：左下（绿色）
         0.5f, -0.5f, 0.0f,  0.0f, 0.0f, 1.0f, 1.0f   // 顶点 2：右下（蓝色）
    )

    private lateinit var vertexBuffer: FloatBuffer
    private var program: Int = 0
    private var aPositionLocation: Int = 0
    private var aColorLocation: Int = 0

    // 每个顶点的字节数：7 个 float (位置3 + 颜色4) × 4字节
    private val VERTEX_STRIDE = 7 * 4

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置背景色（黑色）
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 创建顶点缓冲
        vertexBuffer = ByteBuffer.allocateDirect(verticesWithColor.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(verticesWithColor)
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
        aColorLocation = GLES20.glGetAttribLocation(program, "aColor")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 使用程序
        GLES20.glUseProgram(program)

        // 设置位置属性（前 3 个 float）
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            aPositionLocation,  // 属性位置
            3,                  // 3 个分量 (x, y, z)
            GLES20.GL_FLOAT,    // 数据类型
            false,              // 是否归一化
            VERTEX_STRIDE,      // 步长：7 个 float = 28 字节
            vertexBuffer        // 数据缓冲
        )
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        // 设置颜色属性（后 4 个 float）
        vertexBuffer.position(3)  // 跳过前 3 个位置数据
        GLES20.glVertexAttribPointer(
            aColorLocation,     // 属性位置
            4,                  // 4 个分量 (r, g, b, a)
            GLES20.GL_FLOAT,    // 数据类型
            false,              // 是否归一化
            VERTEX_STRIDE,      // 步长相同
            vertexBuffer        // 数据缓冲
        )
        GLES20.glEnableVertexAttribArray(aColorLocation)

        // 绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)

        // 禁用顶点属性
        GLES20.glDisableVertexAttribArray(aPositionLocation)
        GLES20.glDisableVertexAttribArray(aColorLocation)
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
