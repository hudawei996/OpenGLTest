package com.example.openglstudy.day02

import android.opengl.GLSurfaceView
import com.example.openglstudy.base.BaseGLActivity

/**
 * Day 02 Activity
 * 学习目标：渲染第一个三角形
 * 实现功能：顶点数据、着色器、绘制三角形
 */
class Day02Activity : BaseGLActivity() {

    private lateinit var renderer: Day02Renderer

    override fun createRenderer(): GLSurfaceView.Renderer {
        renderer = Day02Renderer()
        return renderer
    }

    override fun getActivityTitle(): String {
        return "Day 02: 渲染第一个三角形"
    }
}
