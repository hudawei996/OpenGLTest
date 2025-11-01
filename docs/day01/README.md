# Day 01: OpenGL ES 入门与环境搭建

## 📚 今日目标

- 理解 OpenGL ES 的基本概念和发展历史
- 了解 Android 中的 OpenGL 架构
- 掌握 GLSurfaceView 的使用方法
- 创建第一个 OpenGL ES 应用，实现清屏并显示纯色背景

## 🎯 学习内容

### 1. OpenGL ES 简介

#### 什么是 OpenGL ES？

OpenGL ES (OpenGL for Embedded Systems) 是 OpenGL 的子集，专为嵌入式设备（如智能手机、平板电脑）设计的图形渲染 API。

**核心特点**：
- 跨平台：Android、iOS 等主流移动平台都支持
- 高性能：直接利用 GPU 进行硬件加速
- 灵活强大：可以实现复杂的 2D 和 3D 图形效果

#### OpenGL ES 版本演进

| 版本 | 发布年份 | 主要特性 |
|------|---------|----------|
| OpenGL ES 1.0/1.1 | 2003-2004 | 固定管线，简单易用但不够灵活 |
| OpenGL ES 2.0 | 2007 | 引入可编程管线（着色器），灵活性大增 |
| OpenGL ES 3.0 | 2012 | 新增多渲染目标、变换反馈等高级特性 |
| OpenGL ES 3.1/3.2 | 2014-2015 | 计算着色器、几何着色器等 |

**本教程使用 OpenGL ES 2.0**，原因：
- Android 所有设备都支持（从 API 8 开始）
- 概念清晰，适合学习
- 功能已足够实现滤镜和美颜

### 2. Android 中的 OpenGL 架构

#### 核心组件

```
┌─────────────────────────────────────┐
│         GLSurfaceView               │  ← 显示 OpenGL 内容的 View
├─────────────────────────────────────┤
│    GLSurfaceView.Renderer           │  ← 渲染器接口（自己实现）
├─────────────────────────────────────┤
│             EGL                      │  ← OpenGL 与窗口系统的桥梁
├─────────────────────────────────────┤
│          OpenGL ES API               │  ← 图形渲染 API
├─────────────────────────────────────┤
│             GPU                      │  ← 图形处理器硬件
└─────────────────────────────────────┘
```

**GLSurfaceView**：
- Android 提供的专用 View，用于展示 OpenGL 渲染内容
- 自动管理 OpenGL 环境（EGL 上下文、渲染线程等）
- 简化开发，推荐使用

**Renderer**：
- 定义三个核心回调方法，我们在这里编写 OpenGL 代码

### 3. GLSurfaceView.Renderer 接口

```kotlin
interface Renderer {
    // 1. 当 Surface 创建时调用（初始化）
    fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)

    // 2. 当 Surface 尺寸改变时调用（如旋转屏幕）
    fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)

    // 3. 每一帧渲染时调用（绘制）
    fun onDrawFrame(gl: GL10?)
}
```

**方法说明**：

| 方法 | 调用时机 | 主要用途 |
|------|---------|----------|
| `onSurfaceCreated` | Surface 创建、GL 上下文重建 | 初始化 OpenGL 设置、加载资源 |
| `onSurfaceChanged` | Surface 尺寸变化 | 设置视口（Viewport）和投影矩阵 |
| `onDrawFrame` | 每帧渲染 | 执行实际的绘制操作 |

### 4. OpenGL 坐标系统

OpenGL 使用**标准化设备坐标系（NDC）**：

```
        Y (1.0)
         ↑
         |
(-1,0) ←─┼─→ (1,0) X
         |
         ↓
      (-1.0)
```

- X 轴：从左（-1.0）到右（1.0）
- Y 轴：从下（-1.0）到上（1.0）
- Z 轴：从远（-1.0）到近（1.0）

超出 [-1.0, 1.0] 范围的坐标会被裁剪掉，不会显示。

### 5. OpenGL 状态机

OpenGL 是一个**状态机**：
- 设置状态后，状态会一直保持，直到再次改变
- 例如：设置清屏颜色后，每次清屏都使用这个颜色

```kotlin
// 设置清屏颜色为红色
GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)

// 清屏（使用之前设置的红色）
GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
```

## 💻 代码实践

### Day01Renderer 实现

今天我们将实现一个简单的 Renderer，清屏并显示不同的背景颜色。

**核心代码**：

```kotlin
class Day01Renderer : GLSurfaceView.Renderer {

    // 背景颜色（RGBA）
    private var red = 0.2f
    private var green = 0.3f
    private var blue = 0.5f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置清屏颜色（蓝灰色）
        GLES20.glClearColor(red, green, blue, 1.0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 设置视口大小（整个 Surface）
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 清空颜色缓冲区（使用设置的清屏颜色）
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    // 可以动态改变背景颜色
    fun setBackgroundColor(r: Float, g: Float, b: Float) {
        red = r
        green = g
        blue = b
        GLES20.glClearColor(red, green, blue, 1.0f)
    }
}
```

**代码详解**：

1. **onSurfaceCreated**：
   - 使用 `glClearColor()` 设置清屏颜色
   - RGBA 值范围：0.0 ~ 1.0
   - 这里设置为蓝灰色（0.2, 0.3, 0.5, 1.0）

2. **onSurfaceChanged**：
   - 使用 `glViewport()` 设置视口
   - 参数：(x, y, width, height)
   - 视口定义了 OpenGL 渲染结果在窗口中的位置和大小

3. **onDrawFrame**：
   - 使用 `glClear()` 清空缓冲区
   - `GL_COLOR_BUFFER_BIT`：清空颜色缓冲区
   - 后续还会用到 `GL_DEPTH_BUFFER_BIT`（深度缓冲区）

### Day01Activity 实现

```kotlin
class Day01Activity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: Day01Renderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建 GLSurfaceView
        glSurfaceView = GLSurfaceView(this)

        // 设置 OpenGL ES 版本为 2.0
        glSurfaceView.setEGLContextClientVersion(2)

        // 创建并设置 Renderer
        renderer = Day01Renderer()
        glSurfaceView.setRenderer(renderer)

        // 设置渲染模式（默认为 RENDERMODE_CONTINUOUSLY）
        // RENDERMODE_CONTINUOUSLY: 持续渲染（60fps）
        // RENDERMODE_WHEN_DIRTY: 仅在需要时渲染（手动调用 requestRender）
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        setContentView(glSurfaceView)
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }
}
```

**代码要点**：

1. **setEGLContextClientVersion(2)**：
   - 指定使用 OpenGL ES 2.0
   - 必须在 setRenderer 之前调用

2. **生命周期管理**：
   - 必须在 Activity 的 onResume/onPause 中调用 GLSurfaceView 的对应方法
   - 否则可能导致渲染线程未正确暂停/恢复

3. **渲染模式**：
   - `RENDERMODE_CONTINUOUSLY`：持续渲染，适合动画
   - `RENDERMODE_WHEN_DIRTY`：按需渲染，省电

## 🎨 练习任务

### 基础任务

1. **运行示例代码**
   - 在 MainActivity 中点击"Day 01"进入 Day01Activity
   - 看到蓝灰色背景即为成功

2. **修改背景颜色**
   - 尝试修改 RGB 值，观察颜色变化
   - 提示：R=1.0, G=0.0, B=0.0 是红色

3. **动态改变颜色**
   - 添加按钮，点击时随机改变背景颜色
   - 使用 `Random.nextFloat()` 生成随机颜色值

### 进阶任务

1. **颜色渐变动画**
   - 让背景颜色从一种颜色平滑过渡到另一种颜色
   - 提示：在 onDrawFrame 中逐帧改变颜色值

2. **颜色选择器**
   - 添加三个 SeekBar 控制 RGB 值
   - 实时更新背景颜色

## 📖 重要概念总结

### OpenGL ES 核心 API（本节用到的）

| API | 说明 | 参数 |
|-----|------|------|
| `glClearColor(r, g, b, a)` | 设置清屏颜色 | RGBA，范围 0.0~1.0 |
| `glClear(mask)` | 清空缓冲区 | GL_COLOR_BUFFER_BIT 等 |
| `glViewport(x, y, w, h)` | 设置视口 | 左下角坐标和宽高 |

### 关键术语

- **GLSurfaceView**：用于展示 OpenGL 内容的 View
- **Renderer**：渲染器，实现 OpenGL 绘制逻辑
- **EGL**：OpenGL 与本地窗口系统的接口层
- **Viewport**：视口，定义渲染结果在窗口中的位置和大小
- **Color Buffer**：颜色缓冲区，存储每个像素的颜色值

## ❓ 常见问题

### Q1: 为什么屏幕是黑色的？

**可能原因**：
1. 未设置 EGL 版本：`setEGLContextClientVersion(2)`
2. 未调用 glClearColor 或 glClear
3. OpenGL 代码在错误的线程执行

### Q2: 为什么颜色和预期不一致？

**检查**：
- RGB 值范围应该是 0.0 ~ 1.0（不是 0~255）
- 确认 Alpha 值设置为 1.0（完全不透明）

### Q3: 什么是 GL10 参数？

GL10 是 OpenGL ES 1.0 的接口，现在基本不用。在 OpenGL ES 2.0 中，使用 GLES20 类的静态方法。

### Q4: 渲染线程是什么？

GLSurfaceView 会创建一个独立的渲染线程来执行 OpenGL 操作。所有 OpenGL 代码必须在这个线程中执行，不能在主线程。

## 🔗 扩展阅读

- [Android OpenGL ES 官方指南](https://developer.android.com/develop/ui/views/graphics/opengl)
- [Khronos OpenGL ES 2.0 参考卡片](https://www.khronos.org/files/opengles20-reference-card.pdf)

## ✅ 今日小结

今天我们：
1. ✅ 了解了 OpenGL ES 的基本概念和发展历史
2. ✅ 理解了 Android 中的 OpenGL 架构
3. ✅ 掌握了 GLSurfaceView 和 Renderer 的使用
4. ✅ 创建了第一个 OpenGL ES 应用
5. ✅ 学会了设置清屏颜色和视口

**明天预告**：我们将开始真正的图形绘制，渲染第一个三角形！🎉

---

**学习检查清单**：
- [ ] 理解 OpenGL ES 2.0 和 3.0 的区别
- [ ] 能够说出 Renderer 三个方法的作用
- [ ] 理解 OpenGL 坐标系统
- [ ] 能够成功运行代码并看到彩色背景
- [ ] 完成至少 1 个练习任务
