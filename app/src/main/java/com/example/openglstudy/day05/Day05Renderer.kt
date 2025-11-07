package com.example.openglstudy.day05

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.openglstudy.R
import com.example.openglstudy.utils.TextureHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Day 05 Renderer
 * 学习目标：纹理变换与矩阵操作
 * 实现功能：使用矩阵实现图片的旋转、缩放、平移
 */
class Day05Renderer(private val context: Context) : GLSurfaceView.Renderer {

    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        uniform mat4 uMatrix;

        void main() {
            vTexCoord = aTexCoord;
            gl_Position = uMatrix * aPosition;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;

        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """.trimIndent()

    // 正方形顶点
    private val vertices = floatArrayOf(
        // 位置          纹理坐标
        -0.5f,  0.5f, 0f,   0f, 0f,  // 左上
        -0.5f, -0.5f, 0f,   0f, 1f,  // 左下
         0.5f,  0.5f, 0f,   1f, 0f,  // 右上

        -0.5f, -0.5f, 0f,   0f, 1f,  // 左下
         0.5f, -0.5f, 0f,   1f, 1f,  // 右下
         0.5f,  0.5f, 0f,   1f, 0f   // 右上
    )

    private lateinit var vertexBuffer: FloatBuffer
    private var program: Int = 0
    private var textureId: Int = 0
    private var uMatrixLocation: Int = 0

    // 变换参数
    var rotation: Float = 0f      // 旋转角度
    var scale: Float = 1.0f       // 缩放比例
    var translateX: Float = 0f    // X 平移
    var translateY: Float = 0f    // Y 平移

    private val modelMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val VERTEX_STRIDE = 5 * 4

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)

        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        // 获取位置
        val aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        val aTexCoordLocation = GLES20.glGetAttribLocation(program, "aTexCoord")
        val uTextureLocation = GLES20.glGetUniformLocation(program, "uTexture")
        uMatrixLocation = GLES20.glGetUniformLocation(program, "uMatrix")

        // 加载纹理
        textureId = TextureHelper.loadTexture(context, R.mipmap.icon_test)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        // 创建正交投影矩阵
        val ratio = width.toFloat() / height.toFloat()
        if (width > height) {
            Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, -1f, 1f)
        } else {
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -1 / ratio, 1 / ratio, -1f, 1f)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 旋转动画：持续累加旋转角度
        rotation += 1.0f
        if (rotation >= 360f) {
            rotation = 0f
        }

        // 构建模型矩阵（SRT 顺序：缩放-旋转-平移）
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, translateX, translateY, 0f)
        Matrix.rotateM(modelMatrix, 0, rotation, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, scale, scale, 1f)

        // 计算 MVP 矩阵
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)

        // 使用程序
        GLES20.glUseProgram(program)

        // 传递矩阵
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, mvpMatrix, 0)

        // 激活纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTexture"), 0)

        // 设置顶点属性
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            GLES20.glGetAttribLocation(program, "aPosition"),
            3, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(program, "aPosition"))

        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(
            GLES20.glGetAttribLocation(program, "aTexCoord"),
            2, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(program, "aTexCoord"))

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)

        // 清理
        GLES20.glDisableVertexAttribArray(GLES20.glGetAttribLocation(program, "aPosition"))
        GLES20.glDisableVertexAttribArray(GLES20.glGetAttribLocation(program, "aTexCoord"))
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
