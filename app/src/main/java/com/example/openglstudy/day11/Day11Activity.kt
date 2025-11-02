package com.example.openglstudy.day11

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
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
 * Day 11 Activity
 * 学习目标：实时滤镜效果
 * 实现功能：在相机预览上应用多种实时滤镜
 */
class Day11Activity : AppCompatActivity() {

    companion object {
        private const val TAG = "Day11Activity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: Day11Renderer
    
    private lateinit var tvCurrentFilter: TextView
    private lateinit var tvIntensity: TextView
    private lateinit var seekBarIntensity: SeekBar
    
    private lateinit var btnNone: Button
    private lateinit var btnGrayscale: Button
    private lateinit var btnSepia: Button
    private lateinit var btnWarm: Button
    private lateinit var btnCool: Button
    private lateinit var btnInvert: Button
    private lateinit var btnBlackWhite: Button
    private lateinit var btnSwitchCamera: Button

    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: 开始创建 Activity")
        setContentView(R.layout.activity_day11)

        // 设置标题
        supportActionBar?.title = "Day 11: 实时滤镜效果"

        // 初始化 GLSurfaceView
        glSurfaceView = findViewById(R.id.gl_surface_view)
        glSurfaceView.setEGLContextClientVersion(2)
        Log.d(TAG, "onCreate: OpenGL ES 版本设置为 2.0")

        // 创建 Renderer
        renderer = Day11Renderer(glSurfaceView)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        Log.d(TAG, "onCreate: Renderer 设置完成")

        // 初始化视图
        initViews()
        setupListeners()

        // 检查权限
        if (allPermissionsGranted()) {
            Log.d(TAG, "onCreate: 权限已授予")
            cameraExecutor.execute {
                try {
                    val surfaceTexture = renderer.getSurfaceTexture()
                    runOnUiThread {
                        startCamera(surfaceTexture)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onCreate: 获取 SurfaceTexture 失败", e)
                }
            }
        } else {
            Log.d(TAG, "onCreate: 请求权限")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        Log.d(TAG, "onCreate: Activity 创建完成")
    }

    private fun initViews() {
        tvCurrentFilter = findViewById(R.id.tv_current_filter)
        tvIntensity = findViewById(R.id.tv_intensity)
        seekBarIntensity = findViewById(R.id.seekbar_intensity)

        btnNone = findViewById(R.id.btn_none)
        btnGrayscale = findViewById(R.id.btn_grayscale)
        btnSepia = findViewById(R.id.btn_sepia)
        btnWarm = findViewById(R.id.btn_warm)
        btnCool = findViewById(R.id.btn_cool)
        btnInvert = findViewById(R.id.btn_invert)
        btnBlackWhite = findViewById(R.id.btn_black_white)
        btnSwitchCamera = findViewById(R.id.btn_switch_camera)

        // 设置初始值
        seekBarIntensity.progress = 100  // 100% 强度
        tvIntensity.text = "强度: 100%"
        updateButtonStates(Day11Renderer.FilterType.NONE)
    }

    private fun setupListeners() {
        // SeekBar 监听器
        seekBarIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val intensity = progress / 100.0f
                tvIntensity.text = "强度: $progress%"
                
                glSurfaceView.queueEvent {
                    renderer.setIntensity(intensity)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 滤镜按钮
        btnNone.setOnClickListener {
            setFilter(Day11Renderer.FilterType.NONE)
        }

        btnGrayscale.setOnClickListener {
            setFilter(Day11Renderer.FilterType.GRAYSCALE)
        }

        btnSepia.setOnClickListener {
            setFilter(Day11Renderer.FilterType.SEPIA)
        }

        btnWarm.setOnClickListener {
            setFilter(Day11Renderer.FilterType.WARM)
        }

        btnCool.setOnClickListener {
            setFilter(Day11Renderer.FilterType.COOL)
        }

        btnInvert.setOnClickListener {
            setFilter(Day11Renderer.FilterType.INVERT)
        }

        btnBlackWhite.setOnClickListener {
            setFilter(Day11Renderer.FilterType.BLACK_WHITE)
        }

        // 切换相机
        btnSwitchCamera.setOnClickListener {
            switchCamera()
        }
    }

    private fun setFilter(filter: Day11Renderer.FilterType) {
        glSurfaceView.queueEvent {
            renderer.setFilter(filter)
        }
        tvCurrentFilter.text = "当前滤镜: ${filter.displayName}"
        updateButtonStates(filter)
        Log.d(TAG, "setFilter: 切换到 ${filter.displayName}")
    }

    private fun updateButtonStates(currentFilter: Day11Renderer.FilterType) {
        val allButtons = listOf(
            btnNone, btnGrayscale, btnSepia, btnWarm,
            btnCool, btnInvert, btnBlackWhite
        )

        // 重置所有按钮
        allButtons.forEach {
            it.setBackgroundResource(android.R.drawable.btn_default)
            it.setTextColor(getColor(android.R.color.darker_gray))
        }

        // 高亮当前按钮
        val selectedButton = when (currentFilter) {
            Day11Renderer.FilterType.NONE -> btnNone
            Day11Renderer.FilterType.GRAYSCALE -> btnGrayscale
            Day11Renderer.FilterType.SEPIA -> btnSepia
            Day11Renderer.FilterType.WARM -> btnWarm
            Day11Renderer.FilterType.COOL -> btnCool
            Day11Renderer.FilterType.INVERT -> btnInvert
            Day11Renderer.FilterType.BLACK_WHITE -> btnBlackWhite
        }

        selectedButton.setBackgroundResource(android.R.drawable.button_onoff_indicator_on)
        selectedButton.setTextColor(getColor(android.R.color.holo_blue_dark))
    }

    private fun startCamera(surfaceTexture: SurfaceTexture) {
        Log.d(TAG, "startCamera: 开始启动相机")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "startCamera: CameraProvider 获取成功")

                val preview = Preview.Builder().build()

                preview.setSurfaceProvider { request: SurfaceRequest ->
                    val resolution = request.resolution
                    Log.d(TAG, "startCamera: 相机分辨率 ${resolution.width}x${resolution.height}")

                    surfaceTexture.setDefaultBufferSize(resolution.width, resolution.height)

                    val surface = Surface(surfaceTexture)

                    request.provideSurface(
                        surface,
                        ContextCompat.getMainExecutor(this)
                    ) {
                        surface.release()
                    }
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview
                )

                Log.d(TAG, "startCamera: 相机启动成功")
                Toast.makeText(this, "相机启动成功", Toast.LENGTH_SHORT).show()

            } catch (exc: Exception) {
                Log.e(TAG, "startCamera: 相机启动失败", exc)
                Toast.makeText(this, "相机启动失败: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            Log.d(TAG, "switchCamera: 切换到前置相机")
            CameraSelector.LENS_FACING_FRONT
        } else {
            Log.d(TAG, "switchCamera: 切换到后置相机")
            CameraSelector.LENS_FACING_BACK
        }

        cameraExecutor.execute {
            try {
                val surfaceTexture = renderer.getSurfaceTexture()
                runOnUiThread {
                    startCamera(surfaceTexture)
                }
            } catch (e: Exception) {
                Log.e(TAG, "switchCamera: 切换相机失败", e)
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "onRequestPermissionsResult: 权限授予成功")
                cameraExecutor.execute {
                    try {
                        val surfaceTexture = renderer.getSurfaceTexture()
                        runOnUiThread {
                            startCamera(surfaceTexture)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "onRequestPermissionsResult: 获取 SurfaceTexture 失败", e)
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

