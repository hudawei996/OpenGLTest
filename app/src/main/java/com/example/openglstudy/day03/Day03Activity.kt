package com.example.openglstudy.day03

import android.opengl.GLSurfaceView
import com.example.openglstudy.base.BaseGLActivity

/**
 * Day 03 Activity
 * 学习目标：着色器基础 - GLSL 语言
 * 实现功能：渐变色三角形，使用 varying 变量传递颜色
 */
class Day03Activity : BaseGLActivity() {

    private lateinit var renderer: Day03Renderer

    override fun createRenderer(): GLSurfaceView.Renderer {
        renderer = Day03Renderer()
        return renderer
    }

    override fun getActivityTitle(): String {
        return "Day 03: 着色器基础"
    }
}
