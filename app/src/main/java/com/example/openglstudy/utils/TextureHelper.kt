package com.example.openglstudy.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log

/**
 * 纹理加载辅助类
 */
object TextureHelper {

    private const val TAG = "TextureHelper"

    /**
     * 从资源文件加载纹理
     * @param context Context
     * @param resourceId 资源 ID（如 R.drawable.sample_image）
     * @return 纹理 ID，失败返回 0
     */
    fun loadTexture(context: Context, resourceId: Int): Int {
        val textureIds = IntArray(1)

        // 1. 生成纹理 ID
        GLES20.glGenTextures(1, textureIds, 0)
        if (textureIds[0] == 0) {
            Log.e(TAG, "Error generating texture")
            return 0
        }

        // 2. 加载 Bitmap
        val options = BitmapFactory.Options().apply {
            inScaled = false  // 不缩放
        }
        val bitmap = BitmapFactory.decodeResource(
            context.resources,
            resourceId,
            options
        )

        if (bitmap == null) {
            Log.e(TAG, "Error decoding bitmap from resource $resourceId")
            GLES20.glDeleteTextures(1, textureIds, 0)
            return 0
        }

        // 3. 绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

        // 4. 设置纹理参数
        // 缩小时使用线性过滤
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        // 放大时使用线性过滤
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        // S 方向（水平）夹紧到边缘
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        // T 方向（垂直）夹紧到边缘
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        // 5. 上传纹理数据
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        // 6. 回收 Bitmap
        bitmap.recycle()

        // 7. 解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        Log.d(TAG, "Texture loaded successfully, ID: ${textureIds[0]}")
        return textureIds[0]
    }
}
