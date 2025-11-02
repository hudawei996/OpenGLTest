package com.example.openglstudy.day10

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Day 10 Renderer
 * 学习目标：相机预览与 OpenGL 结合
 * 实现功能：使用 OES 纹理渲染相机实时预览
 */
class Day10Renderer(private val glSurfaceView: GLSurfaceView) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "Day10Renderer"
    }

    // OES 纹理的顶点着色器
    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec4 aTexCoord;
        uniform mat4 uTransformMatrix;
        varying vec2 vTexCoord;
        
        void main() {
            // 应用变换矩阵到纹理坐标
            vec4 transformedCoord = uTransformMatrix * aTexCoord;
            vTexCoord = transformedCoord.xy;
            gl_Position = aPosition;
        }
    """.trimIndent()

    // OES 纹理的片段着色器
    private val fragmentShaderCode = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        
        varying vec2 vTexCoord;
        uniform samplerExternalOES uTexture;
        
        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """.trimIndent()

    // 全屏四边形顶点（位置坐标）
    private val quadVertices = floatArrayOf(
        -1f, -1f,  // 左下
         1f, -1f,  // 右下
        -1f,  1f,  // 左上
         1f,  1f   // 右上
    )

    // 纹理坐标（4D，用于变换矩阵）
    private val texCoords = floatArrayOf(
        0f, 0f, 0f, 1f,  // 左下
        1f, 0f, 0f, 1f,  // 右下
        0f, 1f, 0f, 1f,  // 左上
        1f, 1f, 0f, 1f   // 右上
    )

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer

    private var program: Int = 0
    private var oesTextureId: Int = 0
    
    // Uniform 位置
    private var uTextureLocation: Int = 0
    private var uTransformMatrixLocation: Int = 0
    
    // Attribute 位置
    private var aPositionLocation: Int = 0
    private var aTexCoordLocation: Int = 0

    // SurfaceTexture 和变换矩阵
    private lateinit var surfaceTexture: SurfaceTexture
    private val transformMatrix = FloatArray(16)

    @Volatile
    private var surfaceTextureReady = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated: 开始初始化")
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        // 创建顶点缓冲
        vertexBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadVertices)
        vertexBuffer.position(0)

        // 创建纹理坐标缓冲
        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords)
        texCoordBuffer.position(0)

        Log.d(TAG, "onSurfaceCreated: 顶点缓冲创建完成")

        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // 创建程序
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        checkProgramLink(program, "Camera Preview Program")
        Log.d(TAG, "onSurfaceCreated: 着色器程序创建完成")

        // 获取变量位置
        aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLocation = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTextureLocation = GLES20.glGetUniformLocation(program, "uTexture")
        uTransformMatrixLocation = GLES20.glGetUniformLocation(program, "uTransformMatrix")
        
        Log.d(TAG, "onSurfaceCreated: 变量位置 - aPosition=$aPositionLocation, aTexCoord=$aTexCoordLocation")
        Log.d(TAG, "onSurfaceCreated: 变量位置 - uTexture=$uTextureLocation, uTransformMatrix=$uTransformMatrixLocation")

        // 创建 OES 纹理
        oesTextureId = createOESTexture()
        Log.d(TAG, "onSurfaceCreated: OES 纹理创建完成，textureId=$oesTextureId")

        // 创建 SurfaceTexture
        surfaceTexture = SurfaceTexture(oesTextureId)
        surfaceTexture.setOnFrameAvailableListener {
            // 有新帧时请求重新渲染
            glSurfaceView.requestRender()
        }
        
        surfaceTextureReady = true
        Log.d(TAG, "onSurfaceCreated: SurfaceTexture 创建完成，可以启动相机")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: 视口改变 width=$width, height=$height")
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (!surfaceTextureReady) {
            return
        }

        try {
            // 更新 SurfaceTexture（获取最新帧）
            surfaceTexture.updateTexImage()
            
            // 获取纹理变换矩阵
            surfaceTexture.getTransformMatrix(transformMatrix)
        } catch (e: Exception) {
            Log.e(TAG, "onDrawFrame: 更新纹理失败", e)
            return
        }

        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 使用着色器程序
        GLES20.glUseProgram(program)

        // 绑定 OES 纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(uTextureLocation, 0)

        // 传递变换矩阵
        GLES20.glUniformMatrix4fv(uTransformMatrixLocation, 1, false, transformMatrix, 0)

        // 设置顶点属性
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            aPositionLocation,
            2, GLES20.GL_FLOAT, false, 0, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        // 设置纹理坐标属性
        texCoordBuffer.position(0)
        GLES20.glVertexAttribPointer(
            aTexCoordLocation,
            4, GLES20.GL_FLOAT, false, 0, texCoordBuffer
        )
        GLES20.glEnableVertexAttribArray(aTexCoordLocation)

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 清理
        GLES20.glDisableVertexAttribArray(aPositionLocation)
        GLES20.glDisableVertexAttribArray(aTexCoordLocation)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    /**
     * 创建 OES 纹理
     */
    private fun createOESTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)

        val textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        // 设置纹理参数
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

        return textureId
    }

    /**
     * 获取 SurfaceTexture（用于相机绑定）
     */
    fun getSurfaceTexture(): SurfaceTexture {
        // 等待 SurfaceTexture 创建完成
        while (!surfaceTextureReady) {
            Thread.sleep(10)
        }
        return surfaceTexture
    }

    /**
     * 释放资源
     */
    fun release() {
        Log.d(TAG, "release: 释放资源")
        if (::surfaceTexture.isInitialized) {
            surfaceTexture.release()
        }
        if (oesTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(oesTextureId), 0)
        }
    }

    /**
     * 编译着色器
     */
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

    /**
     * 检查程序链接状态
     */
    private fun checkProgramLink(program: Int, name: String) {
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            Log.e(TAG, "checkProgramLink: $name 链接失败 - $log")
            throw RuntimeException("Program link failed: $log")
        }
        Log.d(TAG, "checkProgramLink: $name 链接成功")
    }
}

