package com.example.openglstudy.day05

import android.opengl.GLSurfaceView
import com.example.openglstudy.base.BaseGLActivity

/**
 * Day 05 Activity
 * 学习目标：纹理变换与矩阵操作
 * 实现功能：使用矩阵实现图片的旋转、缩放、平移
 */
class Day05Activity : BaseGLActivity() {

    private lateinit var renderer: Day05Renderer

    override fun createRenderer(): GLSurfaceView.Renderer {
        renderer = Day05Renderer(this)
        return renderer
    }

    override fun getActivityTitle(): String {
        return "Day 05: 纹理变换与矩阵操作"
    }
}
