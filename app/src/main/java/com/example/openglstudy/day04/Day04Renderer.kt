package com.example.openglstudy.day04

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.example.openglstudy.R
import com.example.openglstudy.utils.TextureHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Day 04 Renderer
 * 学习目标：纹理贴图基础
 * 实现功能：加载并显示图片纹理
 *
 * 注意：需要在 res/drawable/ 目录下添加一张示例图片，命名为 sample_image.png
 */
class Day04Renderer(private val context: Context) : GLSurfaceView.Renderer {

    // 顶点着色器代码
    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            vTexCoord = aTexCoord;
            gl_Position = aPosition;
        }
    """.trimIndent()

    // 片段着色器代码
    private val fragmentShaderCode = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """.trimIndent()

    // 矩形顶点（位置 + 纹理坐标）
    // 格式：x, y, z, u, v
    private val vertices = floatArrayOf(
        // 位置          纹理坐标（V 翻转）
        -1f,  1f, 0f,   0f, 0f,  // 左上
        -1f, -1f, 0f,   0f, 1f,  // 左下
         1f,  1f, 0f,   1f, 0f,  // 右上

        -1f, -1f, 0f,   0f, 1f,  // 左下
         1f, -1f, 0f,   1f, 1f,  // 右下
         1f,  1f, 0f,   1f, 0f   // 右上
    )

    private lateinit var vertexBuffer: FloatBuffer
    private var program: Int = 0
    private var textureId: Int = 0

    private var aPositionLocation: Int = 0
    private var aTexCoordLocation: Int = 0
    private var uTextureLocation: Int = 0

    // 每个顶点的字节数：5 个 float (位置3 + 纹理坐标2) × 4字节
    private val VERTEX_STRIDE = 5 * 4

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 创建顶点缓冲
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)

        // 编译着色器和创建程序
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
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

        // 获取位置
        aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLocation = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTextureLocation = GLES20.glGetUniformLocation(program, "uTexture")

        // 加载纹理
        // 注意：需要在 res/drawable/ 添加 sample_image.png
        textureId = TextureHelper.loadTexture(context, R.drawable.sample_image)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        // 激活并绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTextureLocation, 0)

        // 位置属性
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            aPositionLocation, 3, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        // 纹理坐标属性
        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(
            aTexCoordLocation, 2, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(aTexCoordLocation)

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)

        // 清理
        GLES20.glDisableVertexAttribArray(aPositionLocation)
        GLES20.glDisableVertexAttribArray(aTexCoordLocation)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

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
