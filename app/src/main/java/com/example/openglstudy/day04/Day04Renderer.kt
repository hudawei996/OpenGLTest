package com.example.openglstudy.day04

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.example.openglstudy.R
import com.example.openglstudy.utils.TextureHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Day 04 Renderer - 作业版本
 * 学习目标：纹理贴图基础 + 双纹理混合
 * 实现功能：
 * 1. 加载两张图片纹理
 * 2. 在片段着色器中使用 mix() 函数混合两个纹理
 *
 * 作业要求：
 * - 混合两张纹理
 * - 使用 mix() 函数控制混合比例
 */
class Day04Renderer(private val context: Context) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "Day04Renderer"
    }

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

    // 片段着色器代码 - 支持双纹理混合
    private val fragmentShaderCode = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture0;  // 第一个纹理
        uniform sampler2D uTexture1;  // 第二个纹理
        uniform float uMixRatio;      // 混合比例 (0.0 ~ 1.0)
        void main() {
            // 采样两个纹理
            vec4 color0 = texture2D(uTexture0, vTexCoord);
            vec4 color1 = texture2D(uTexture1, vTexCoord);

            // 使用 mix() 函数混合两个纹理
            // mix(x, y, a) = x * (1 - a) + y * a
            gl_FragColor = mix(color0, color1, uMixRatio);
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
    private var textureId0: Int = 0  // 第一个纹理 ID
    private var textureId1: Int = 0  // 第二个纹理 ID

    private var aPositionLocation: Int = 0
    private var aTexCoordLocation: Int = 0
    private var uTexture0Location: Int = 0  // 第一个纹理的 uniform 位置
    private var uTexture1Location: Int = 0  // 第二个纹理的 uniform 位置
    private var uMixRatioLocation: Int = 0  // 混合比例的 uniform 位置

    // 混合比例 (0.0 = 完全显示纹理0, 1.0 = 完全显示纹理1)
    private var mixRatio: Float = 0.5f

    // 每个顶点的字节数：5 个 float (位置3 + 纹理坐标2) × 4字节
    private val VERTEX_STRIDE = 5 * 4

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated: 开始初始化")
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 创建顶点缓冲
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)
        Log.d(TAG, "onSurfaceCreated: 顶点缓冲创建完成，顶点数=${vertices.size / 5}")

        // 编译着色器和创建程序
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        Log.d(TAG, "onSurfaceCreated: 着色器编译完成")

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
            Log.e(TAG, "onSurfaceCreated: 程序链接失败 - $log")
            throw RuntimeException("Program link failed: $log")
        }
        Log.d(TAG, "onSurfaceCreated: 程序链接成功，program=$program")

        // 获取位置
        aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLocation = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTexture0Location = GLES20.glGetUniformLocation(program, "uTexture0")
        uTexture1Location = GLES20.glGetUniformLocation(program, "uTexture1")
        uMixRatioLocation = GLES20.glGetUniformLocation(program, "uMixRatio")
        Log.d(TAG, "onSurfaceCreated: aPosition=$aPositionLocation, aTexCoord=$aTexCoordLocation")
        Log.d(TAG, "onSurfaceCreated: uTexture0=$uTexture0Location, uTexture1=$uTexture1Location, uMixRatio=$uMixRatioLocation")

        // 加载两个纹理
        textureId0 = TextureHelper.loadTexture(context, R.mipmap.icon_test)
        Log.d(TAG, "onSurfaceCreated: 纹理0加载完成，textureId=$textureId0")

        textureId1 = TextureHelper.loadTexture(context, R.drawable.sample_image)
        Log.d(TAG, "onSurfaceCreated: 纹理1加载完成，textureId=$textureId1")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: 视口改变 width=$width, height=$height")
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        // 激活纹理单元 0 并绑定第一个纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId0)
        GLES20.glUniform1i(uTexture0Location, 0)  // 告诉着色器使用纹理单元 0

        // 激活纹理单元 1 并绑定第二个纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId1)
        GLES20.glUniform1i(uTexture1Location, 1)  // 告诉着色器使用纹理单元 1

        // 设置混合比例
        GLES20.glUniform1f(uMixRatioLocation, mixRatio)

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
        val shaderType = if (type == GLES20.GL_VERTEX_SHADER) "顶点着色器" else "片段着色器"
        Log.d(TAG, "loadShader: 开始编译$shaderType")

        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            Log.e(TAG, "loadShader: $shaderType 编译失败 - $log")
            throw RuntimeException("Shader compilation failed: $log")
        }

        Log.d(TAG, "loadShader: $shaderType 编译成功，shader=$shader")
        return shader
    }
}
