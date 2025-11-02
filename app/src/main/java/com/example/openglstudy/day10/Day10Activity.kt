package com.example.openglstudy.day10

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.openglstudy.R
import java.util.concurrent.Executors

/**
 * Day 10 Activity
 * 学习目标：相机预览与 OpenGL 结合
 * 实现功能：使用 OpenGL 渲染相机实时预览
 */
class Day10Activity : AppCompatActivity() {

    companion object {
        private const val TAG = "Day10Activity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            // Android 10 及以下需要存储权限
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: Day10Renderer
    private lateinit var btnSwitchCamera: Button

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: 开始创建 Activity")
        setContentView(R.layout.activity_day10)

        // 设置标题
        supportActionBar?.title = "Day 10: 相机预览与 OpenGL 结合"

        // 初始化 GLSurfaceView
        glSurfaceView = findViewById(R.id.gl_surface_view)
        glSurfaceView.setEGLContextClientVersion(2)
        Log.d(TAG, "onCreate: OpenGL ES 版本设置为 2.0")

        // 创建 Renderer
        renderer = Day10Renderer(glSurfaceView)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY  // 按需渲染
        Log.d(TAG, "onCreate: Renderer 设置完成，使用按需渲染模式")

        // 初始化切换相机按钮
        btnSwitchCamera = findViewById(R.id.btn_switch_camera)
        btnSwitchCamera.setOnClickListener {
            switchCamera()
        }

        // 检查权限
        if (allPermissionsGranted()) {
            Log.d(TAG, "onCreate: 权限已授予，等待 SurfaceTexture 创建")
            // 在后台线程等待 SurfaceTexture 创建完成，然后启动相机
            cameraExecutor.execute {
                try {
                    val surfaceTexture = renderer.getSurfaceTexture()
                    runOnUiThread {
                        startCamera(surfaceTexture)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onCreate: 获取 SurfaceTexture 失败", e)
                    runOnUiThread {
                        Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Log.d(TAG, "onCreate: 权限未授予，请求权限")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        Log.d(TAG, "onCreate: Activity 创建完成")
    }

    /**
     * 启动相机
     */
    private fun startCamera(surfaceTexture: SurfaceTexture) {
        Log.d(TAG, "startCamera: 开始启动相机")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "startCamera: CameraProvider 获取成功")

                // 创建 Preview Use Case
                val preview = Preview.Builder().build()

                // 创建 Surface 并设置 SurfaceProvider
                preview.setSurfaceProvider { request: SurfaceRequest ->
                    Log.d(TAG, "startCamera: SurfaceProvider 被调用")
                    
                    // 获取相机分辨率
                    val resolution = request.resolution
                    Log.d(TAG, "startCamera: 相机分辨率 ${resolution.width}x${resolution.height}")
                    
                    // 设置 SurfaceTexture 的默认缓冲大小
                    surfaceTexture.setDefaultBufferSize(resolution.width, resolution.height)
                    
                    // 创建 Surface
                    val surface = Surface(surfaceTexture)
                    
                    // 提供 Surface 给相机
                    request.provideSurface(
                        surface,
                        ContextCompat.getMainExecutor(this)
                    ) { result ->
                        Log.d(TAG, "startCamera: Surface 使用完成")
                        surface.release()
                    }
                }

                // 选择相机
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                
                val cameraType = if (lensFacing == CameraSelector.LENS_FACING_BACK) "后置" else "前置"
                Log.d(TAG, "startCamera: 选择${cameraType}相机")

                // 解绑之前的 Use Cases
                cameraProvider.unbindAll()
                Log.d(TAG, "startCamera: 已解绑旧的 Use Cases")

                // 绑定 Use Cases 到生命周期
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview
                )
                Log.d(TAG, "startCamera: Use Cases 绑定成功")

                Toast.makeText(this, "相机启动成功", Toast.LENGTH_SHORT).show()

            } catch (exc: Exception) {
                Log.e(TAG, "startCamera: 相机启动失败", exc)
                Toast.makeText(this, "相机启动失败: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 切换前后摄像头
     */
    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            Log.d(TAG, "switchCamera: 切换到前置相机")
            CameraSelector.LENS_FACING_FRONT
        } else {
            Log.d(TAG, "switchCamera: 切换到后置相机")
            CameraSelector.LENS_FACING_BACK
        }

        // 重新启动相机
        cameraExecutor.execute {
            try {
                val surfaceTexture = renderer.getSurfaceTexture()
                runOnUiThread {
                    startCamera(surfaceTexture)
                }
            } catch (e: Exception) {
                Log.e(TAG, "switchCamera: 切换相机失败", e)
                runOnUiThread {
                    Toast.makeText(this, "切换相机失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 检查所有权限是否已授予
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 权限请求结果回调
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "onRequestPermissionsResult: 权限授予成功")
                // 在后台线程等待 SurfaceTexture 创建完成
                cameraExecutor.execute {
                    try {
                        val surfaceTexture = renderer.getSurfaceTexture()
                        runOnUiThread {
                            startCamera(surfaceTexture)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "onRequestPermissionsResult: 获取 SurfaceTexture 失败", e)
                        runOnUiThread {
                            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Log.e(TAG, "onRequestPermissionsResult: 权限被拒绝")
                Toast.makeText(
                    this,
                    "相机权限被拒绝，应用无法使用相机功能",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: GLSurfaceView 恢复渲染")
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: GLSurfaceView 暂停渲染")
        glSurfaceView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: 释放资源")
        cameraExecutor.shutdown()
        renderer.release()
    }
}

