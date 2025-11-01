package com.example.openglstudy.day04

import android.opengl.GLSurfaceView
import com.example.openglstudy.base.BaseGLActivity

/**
 * Day 04 Activity
 * 学习目标：纹理贴图基础
 * 实现功能：加载并显示图片纹理
 */
class Day04Activity : BaseGLActivity() {

    private lateinit var renderer: Day04Renderer

    override fun createRenderer(): GLSurfaceView.Renderer {
        renderer = Day04Renderer(this)
        return renderer
    }

    override fun getActivityTitle(): String {
        return "Day 04: 纹理贴图基础"
    }
}
