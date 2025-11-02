package com.example.openglstudy.day11

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
 * Day 11 Renderer
 * 学习目标：实时滤镜效果
 * 实现功能：在相机预览上应用多种实时滤镜
 */
class Day11Renderer(private val glSurfaceView: GLSurfaceView) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "Day11Renderer"
    }

    /**
     * 滤镜类型枚举
     */
    enum class FilterType(val displayName: String) {
        NONE("原图"),
        GRAYSCALE("灰度"),
        SEPIA("复古"),
        WARM("暖色调"),
        COOL("冷色调"),
        INVERT("反色"),
        BLACK_WHITE("黑白")
    }

    // 顶点着色器（与 Day10 相同）
    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec4 aTexCoord;
        uniform mat4 uTransformMatrix;
        varying vec2 vTexCoord;
        
        void main() {
            vec4 transformedCoord = uTransformMatrix * aTexCoord;
            vTexCoord = transformedCoord.xy;
            gl_Position = aPosition;
        }
    """.trimIndent()

    // 片段着色器（包含多种滤镜）
    private val fragmentShaderCode = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        
        varying vec2 vTexCoord;
        uniform samplerExternalOES uTexture;
        uniform int uFilterType;
        uniform float uIntensity;
        
        // 灰度滤镜
        vec4 grayscale(vec4 color) {
            float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
            return vec4(vec3(gray), color.a);
        }
        
        // 复古滤镜（Sepia）
        vec4 sepia(vec4 color) {
            float r = color.r * 0.393 + color.g * 0.769 + color.b * 0.189;
            float g = color.r * 0.349 + color.g * 0.686 + color.b * 0.168;
            float b = color.r * 0.272 + color.g * 0.534 + color.b * 0.131;
            return vec4(r, g, b, color.a);
        }
        
        // 暖色调滤镜
        vec4 warm(vec4 color) {
            color.r = min(color.r + 0.1, 1.0);
            color.g = min(color.g + 0.05, 1.0);
            return color;
        }
        
        // 冷色调滤镜
        vec4 cool(vec4 color) {
            color.b = min(color.b + 0.1, 1.0);
            return color;
        }
        
        // 反色滤镜
        vec4 invert(vec4 color) {
            return vec4(1.0 - color.rgb, color.a);
        }
        
        // 黑白滤镜（高对比度）
        vec4 blackWhite(vec4 color) {
            float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
            float bw = step(0.5, gray);
            return vec4(vec3(bw), color.a);
        }
        
        void main() {
            vec4 color = texture2D(uTexture, vTexCoord);
            vec4 filtered;
            
            if (uFilterType == 0) {
                // 原图
                filtered = color;
            } else if (uFilterType == 1) {
                // 灰度
                filtered = grayscale(color);
            } else if (uFilterType == 2) {
                // 复古
                filtered = sepia(color);
            } else if (uFilterType == 3) {
                // 暖色调
                filtered = warm(color);
            } else if (uFilterType == 4) {
                // 冷色调
                filtered = cool(color);
            } else if (uFilterType == 5) {
                // 反色
                filtered = invert(color);
            } else if (uFilterType == 6) {
                // 黑白
                filtered = blackWhite(color);
            } else {
                filtered = color;
            }
            
            // 应用滤镜强度（原图和滤镜效果的混合）
            gl_FragColor = mix(color, filtered, uIntensity);
        }
    """.trimIndent()

    // 顶点数据
    private val quadVertices = floatArrayOf(
        -1f, -1f,  // 左下
         1f, -1f,  // 右下
        -1f,  1f,  // 左上
         1f,  1f   // 右上
    )

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
    private var uFilterTypeLocation: Int = 0
    private var uIntensityLocation: Int = 0

    // Attribute 位置
    private var aPositionLocation: Int = 0
    private var aTexCoordLocation: Int = 0

    // SurfaceTexture
    private lateinit var surfaceTexture: SurfaceTexture
    private val transformMatrix = FloatArray(16)

    @Volatile
    private var surfaceTextureReady = false

    // 滤镜参数
    @Volatile
    var currentFilter: FilterType = FilterType.NONE
    @Volatile
    var filterIntensity: Float = 1.0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated: 开始初始化")
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        // 创建顶点缓冲
        vertexBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadVertices)
        vertexBuffer.position(0)

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords)
        texCoordBuffer.position(0)

        Log.d(TAG, "onSurfaceCreated: 顶点缓冲创建完成")

        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        checkProgramLink(program, "Filter Program")
        Log.d(TAG, "onSurfaceCreated: 着色器程序创建完成")

        // 获取变量位置
        aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLocation = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTextureLocation = GLES20.glGetUniformLocation(program, "uTexture")
        uTransformMatrixLocation = GLES20.glGetUniformLocation(program, "uTransformMatrix")
        uFilterTypeLocation = GLES20.glGetUniformLocation(program, "uFilterType")
        uIntensityLocation = GLES20.glGetUniformLocation(program, "uIntensity")

        Log.d(TAG, "onSurfaceCreated: 变量位置获取完成")

        // 创建 OES 纹理
        oesTextureId = createOESTexture()
        Log.d(TAG, "onSurfaceCreated: OES 纹理创建完成，textureId=$oesTextureId")

        // 创建 SurfaceTexture
        surfaceTexture = SurfaceTexture(oesTextureId)
        surfaceTexture.setOnFrameAvailableListener {
            glSurfaceView.requestRender()
        }

        surfaceTextureReady = true
        Log.d(TAG, "onSurfaceCreated: SurfaceTexture 创建完成")
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
            surfaceTexture.updateTexImage()
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

        // 传递滤镜参数
        GLES20.glUniform1i(uFilterTypeLocation, currentFilter.ordinal)
        GLES20.glUniform1f(uIntensityLocation, filterIntensity)

        // 设置顶点属性
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            aPositionLocation,
            2, GLES20.GL_FLOAT, false, 0, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(aPositionLocation)

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

    private fun createOESTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)

        val textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

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

    fun getSurfaceTexture(): SurfaceTexture {
        while (!surfaceTextureReady) {
            Thread.sleep(10)
        }
        return surfaceTexture
    }

    fun setFilter(filter: FilterType) {
        currentFilter = filter
        Log.d(TAG, "setFilter: 切换滤镜为 ${filter.displayName}")
    }

    fun setIntensity(intensity: Float) {
        filterIntensity = intensity.coerceIn(0f, 1f)
        Log.d(TAG, "setIntensity: 滤镜强度更新为 $filterIntensity")
    }

    fun release() {
        Log.d(TAG, "release: 释放资源")
        if (::surfaceTexture.isInitialized) {
            surfaceTexture.release()
        }
        if (oesTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(oesTextureId), 0)
        }
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

        Log.d(TAG, "loadShader: $shaderType 编译成功")
        return shader
    }

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

