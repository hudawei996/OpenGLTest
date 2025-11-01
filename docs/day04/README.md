# Day 04: 纹理贴图基础

## 📚 今日目标

- 理解纹理和纹理坐标的概念
- 学习如何加载和绑定纹理
- 掌握纹理采样器的使用
- 将图片纹理映射到矩形
- 解决纹理翻转问题

## 🎯 学习内容

### 1. 什么是纹理？

**纹理（Texture）** 本质上是一张图片，用于给 3D 模型或 2D 图形"贴图"。

```
图片（PNG/JPEG）   →   纹理对象   →   映射到几何体
                    OpenGL 加载
```

**纹理的用途**：
- 给物体表面添加细节
- 实现图片滤镜效果
- 存储数据（如深度图、法线贴图）

### 2. 纹理坐标系统

#### 纹理坐标（UV 坐标）

纹理使用 **归一化坐标系**，范围 [0.0, 1.0]：

```
(0,1) ─────────── (1,1)
  │                 │
  │     纹理图片     │
  │                 │
(0,0) ─────────── (1,0)
```

- **U 轴（S）**：水平方向，从左 (0) 到右 (1)
- **V 轴（T）**：垂直方向，从下 (0) 到上 (1)

**注意**：
- OpenGL 纹理原点在**左下角** (0, 0)
- 图片文件原点通常在**左上角**
- **需要翻转处理**

#### 纹理坐标与顶点的对应

要将纹理映射到矩形，需要为每个顶点指定纹理坐标：

```kotlin
// 矩形顶点（2 个三角形）
val vertices = floatArrayOf(
    // 位置           纹理坐标
    -1f,  1f, 0f,    0f, 1f,  // 左上
    -1f, -1f, 0f,    0f, 0f,  // 左下
     1f,  1f, 0f,    1f, 1f,  // 右上

    -1f, -1f, 0f,    0f, 0f,  // 左下
     1f, -1f, 0f,    1f, 0f,  // 右下
     1f,  1f, 0f,    1f, 1f   // 右上
)
```

### 3. 纹理加载流程

```
┌──────────────────┐
│  读取图片文件    │ ← Bitmap
└────────┬─────────┘
         ↓
┌──────────────────┐
│  创建纹理对象    │ ← glGenTextures
└────────┬─────────┘
         ↓
┌──────────────────┐
│  绑定纹理        │ ← glBindTexture
└────────┬─────────┘
         ↓
┌──────────────────┐
│  设置纹理参数    │ ← glTexParameteri
└────────┬─────────┘
         ↓
┌──────────────────┐
│  上传纹理数据    │ ← glTexImage2D / texImage2D
└────────┬─────────┘
         ↓
┌──────────────────┐
│  解绑纹理        │ ← glBindTexture(0)
└──────────────────┘
```

### 4. 纹理参数

#### 纹理过滤（Filtering）

当纹理被放大或缩小时，如何计算像素颜色？

| 参数 | 值 | 说明 |
|------|-----|------|
| `GL_TEXTURE_MIN_FILTER` | `GL_NEAREST` | 最近邻过滤（快，但有锯齿） |
| | `GL_LINEAR` | 线性过滤（平滑） |
| | `GL_NEAREST_MIPMAP_NEAREST` | 使用 mipmap |
| `GL_TEXTURE_MAG_FILTER` | `GL_NEAREST` | 放大时最近邻 |
| | `GL_LINEAR` | 放大时线性过滤 |

**推荐设置**：
```kotlin
// 缩小时使用线性过滤
GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
// 放大时使用线性过滤
GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
```

#### 纹理环绕（Wrapping）

当纹理坐标超出 [0, 1] 范围时如何处理？

| 参数 | 值 | 说明 |
|------|-----|------|
| `GL_TEXTURE_WRAP_S` | `GL_REPEAT` | 重复纹理 |
| | `GL_CLAMP_TO_EDGE` | 夹紧到边缘（推荐） |
| | `GL_MIRRORED_REPEAT` | 镜像重复 |
| `GL_TEXTURE_WRAP_T` | 同上 | T 方向的环绕方式 |

```kotlin
// 夹紧到边缘
GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
```

### 5. 纹理采样器

在着色器中使用 **sampler2D** 类型访问纹理：

#### 顶点着色器

```glsl
attribute vec4 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;

void main() {
    vTexCoord = aTexCoord;  // 传递纹理坐标
    gl_Position = aPosition;
}
```

#### 片段着色器

```glsl
precision mediump float;
varying vec2 vTexCoord;
uniform sampler2D uTexture;  // 纹理采样器

void main() {
    // 从纹理中采样颜色
    gl_FragColor = texture2D(uTexture, vTexCoord);
}
```

**texture2D 函数**：
- 参数 1：纹理采样器
- 参数 2：纹理坐标（vec2）
- 返回值：采样得到的颜色（vec4）

## 💻 代码实践

### 1. 从资源加载图片

```kotlin
fun loadTexture(context: Context, resourceId: Int): Int {
    val textureIds = IntArray(1)

    // 1. 生成纹理 ID
    GLES20.glGenTextures(1, textureIds, 0)
    if (textureIds[0] == 0) {
        throw RuntimeException("Error generating texture")
    }

    // 2. 加载 Bitmap
    val options = BitmapFactory.Options().apply {
        inScaled = false  // 不缩放
    }
    val bitmap = BitmapFactory.decodeResource(
        context.resources,
        resourceId,
        options
    ) ?: throw RuntimeException("Error decoding bitmap")

    // 3. 绑定纹理
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

    // 4. 设置纹理参数
    GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D,
        GLES20.GL_TEXTURE_MIN_FILTER,
        GLES20.GL_LINEAR
    )
    GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D,
        GLES20.GL_TEXTURE_MAG_FILTER,
        GLES20.GL_LINEAR
    )
    GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D,
        GLES20.GL_TEXTURE_WRAP_S,
        GLES20.GL_CLAMP_TO_EDGE
    )
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

    return textureIds[0]
}
```

### 2. Day04Renderer 完整实现

```kotlin
class Day04Renderer(private val context: Context) : GLSurfaceView.Renderer {

    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            vTexCoord = aTexCoord;
            gl_Position = aPosition;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """.trimIndent()

    // 矩形顶点（位置 + 纹理坐标）
    private val vertices = floatArrayOf(
        // 位置           纹理坐标
        -1f,  1f, 0f,    0f, 1f,  // 左上
        -1f, -1f, 0f,    0f, 0f,  // 左下
         1f,  1f, 0f,    1f, 1f,  // 右上

        -1f, -1f, 0f,    0f, 0f,  // 左下
         1f, -1f, 0f,    1f, 0f,  // 右下
         1f,  1f, 0f,    1f, 1f   // 右上
    )

    private lateinit var vertexBuffer: FloatBuffer
    private var program: Int = 0
    private var textureId: Int = 0

    private var aPositionLocation: Int = 0
    private var aTexCoordLocation: Int = 0
    private var uTextureLocation: Int = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 创建顶点缓冲
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)

        // 编译着色器和创建程序
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        // 获取位置
        aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLocation = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTextureLocation = GLES20.glGetUniformLocation(program, "uTexture")

        // 加载纹理
        textureId = loadTexture(context, R.drawable.sample_image)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        // 激活并绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTextureLocation, 0)

        // 位置属性
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            aPositionLocation, 3, GLES20.GL_FLOAT, false, 5 * 4, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        // 纹理坐标属性
        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(
            aTexCoordLocation, 2, GLES20.GL_FLOAT, false, 5 * 4, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(aTexCoordLocation)

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)

        // 清理
        GLES20.glDisableVertexAttribArray(aPositionLocation)
        GLES20.glDisableVertexAttribArray(aTexCoordLocation)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun loadTexture(context: Context, resourceId: Int): Int {
        // ... （上面的 loadTexture 函数）
    }
}
```

### 3. 解决纹理翻转问题

**问题**：图片可能上下颠倒。

**方案 1**：翻转纹理坐标（推荐）

```kotlin
// 将 V 坐标翻转
val vertices = floatArrayOf(
    // 位置           纹理坐标（V 翻转）
    -1f,  1f, 0f,    0f, 0f,  // 左上
    -1f, -1f, 0f,    0f, 1f,  // 左下
     1f,  1f, 0f,    1f, 0f,  // 右上
    // ...
)
```

**方案 2**：在着色器中翻转

```glsl
varying vec2 vTexCoord;
void main() {
    vec2 flippedCoord = vec2(vTexCoord.x, 1.0 - vTexCoord.y);
    gl_FragColor = texture2D(uTexture, flippedCoord);
}
```

## 🎨 练习任务

### 基础任务

1. **加载并显示图片**
   - 在 `res/drawable` 添加图片
   - 运行代码，显示纹理图片

2. **修改纹理过滤方式**
   - 尝试 `GL_NEAREST` 和 `GL_LINEAR` 的区别
   - 观察放大图片时的效果

3. **调整纹理坐标**
   - 只显示图片的一部分（裁剪）
   - 提示：修改纹理坐标范围，如 [0.25, 0.75]

### 进阶任务

1. **纹理重复**
   - 设置环绕模式为 `GL_REPEAT`
   - 纹理坐标使用 [0, 2] 范围，观察效果

2. **镜像效果**
   - 水平或垂直镜像图片
   - 提示：翻转纹理坐标

3. **混合两张纹理**
   - 加载两张图片
   - 在片段着色器中混合两个纹理
   - 使用 `mix()` 函数

## 📖 重要概念总结

### 纹理相关 API

| API | 说明 |
|-----|------|
| `glGenTextures(n, ids, offset)` | 生成纹理 ID |
| `glBindTexture(target, texture)` | 绑定纹理 |
| `glTexParameteri(target, pname, param)` | 设置纹理参数 |
| `glTexImage2D(...)` | 上传纹理数据 |
| `glActiveTexture(texture)` | 激活纹理单元 |
| `glUniform1i(location, textureUnit)` | 传递纹理单元索引 |

### 关键概念

- **纹理单元**：OpenGL 支持多个纹理单元（GL_TEXTURE0, GL_TEXTURE1...）
- **纹理绑定**：必须先绑定才能操作纹理
- **纹理坐标**：使用归一化坐标 [0, 1]
- **采样器**：在着色器中使用 sampler2D 访问纹理

## ❓ 常见问题

### Q1: 为什么图片是黑色的？

**检查清单**：
- [ ] 纹理是否成功加载？（textureId != 0）
- [ ] 是否调用了 glActiveTexture 和 glBindTexture？
- [ ] 是否设置了 uniform sampler？
- [ ] 纹理坐标是否正确？

### Q2: 为什么图片上下颠倒？

OpenGL 纹理坐标原点在左下角，而图片文件原点在左上角，需要翻转 V 坐标。

### Q3: 什么是纹理单元？

OpenGL 支持同时使用多个纹理（GL_TEXTURE0 ~ GL_TEXTURE31）。每个纹理单元可以绑定一个纹理。

### Q4: GLUtils.texImage2D 和 glTexImage2D 有什么区别？

- `GLUtils.texImage2D`：Android 提供的便捷方法，直接传入 Bitmap
- `glTexImage2D`：原生 OpenGL API，需要手动准备 ByteBuffer

## 🔗 扩展阅读

- [OpenGL 纹理教程](https://learnopengl-cn.github.io/01%20Getting%20started/06%20Textures/)
- [Android Bitmap 与 OpenGL](https://developer.android.com/reference/android/opengl/GLUtils)

## ✅ 今日小结

今天我们：
1. ✅ 理解了纹理和纹理坐标的概念
2. ✅ 学习了纹理的加载和参数设置
3. ✅ 掌握了纹理采样器的使用
4. ✅ 成功将图片渲染到屏幕
5. ✅ 解决了纹理翻转问题

**明天预告**：学习纹理变换和矩阵操作，实现图片的旋转、缩放！🔄

---

**学习检查清单**：
- [ ] 理解纹理坐标系统
- [ ] 掌握纹理过滤和环绕模式
- [ ] 能够加载并显示图片纹理
- [ ] 理解 texture2D 函数的用法
- [ ] 成功解决纹理翻转问题
