# Day 03: 着色器基础 - GLSL 语言

## 📚 今日目标

- 深入学习 GLSL 语法和数据类型
- 掌握 uniform、attribute、varying 变量的使用
- 理解着色器内置函数
- 实现动态改变图形颜色和大小
- 制作渐变色三角形

## 🎯 学习内容

### 1. GLSL 语言基础

#### 版本声明

```glsl
#version 100  // OpenGL ES 2.0 使用 GLSL ES 1.00
```

注意：Android 中通常不需要显式声明版本。

#### 精度限定符

GLSL ES 要求指定浮点数精度（节省 GPU 资源）：

```glsl
precision highp float;    // 高精度
precision mediump float;  // 中精度（推荐）
precision lowp float;     // 低精度
```

**精度选择建议**：

| 精度 | 范围 | 精度 | 使用场景 |
|------|------|------|----------|
| highp | -2^62 ~ 2^62 | 最高 | 顶点坐标、大型计算 |
| mediump | -2^14 ~ 2^14 | 中等 | 纹理坐标、颜色（推荐） |
| lowp | -2 ~ 2 | 最低 | 颜色输出、简单计算 |

**片段着色器必须声明精度**，顶点着色器有默认精度。

### 2. 数据类型详解

#### 标量类型

```glsl
bool b = true;
int i = 1;
float f = 1.0;
```

#### 向量类型

```glsl
vec2 v2 = vec2(1.0, 2.0);           // 2D 浮点向量
vec3 v3 = vec3(1.0, 2.0, 3.0);      // 3D 浮点向量
vec4 v4 = vec4(1.0, 2.0, 3.0, 4.0); // 4D 浮点向量

ivec2 iv2 = ivec2(1, 2);  // 整数向量
bvec3 bv3 = bvec3(true);  // 布尔向量
```

#### 向量分量访问

GLSL 提供多种访问方式：

```glsl
vec4 v = vec4(1.0, 2.0, 3.0, 4.0);

// 方式 1：xyzw（位置）
float x = v.x;  // 1.0
float y = v.y;  // 2.0

// 方式 2：rgba（颜色）
float r = v.r;  // 1.0
float g = v.g;  // 2.0

// 方式 3：stpq（纹理）
float s = v.s;  // 1.0
float t = v.t;  // 2.0

// Swizzle（重组）
vec2 xy = v.xy;         // (1.0, 2.0)
vec3 bgr = v.bgr;       // (3.0, 2.0, 1.0)
vec4 xxxx = v.xxxx;     // (1.0, 1.0, 1.0, 1.0)
```

#### 矩阵类型

```glsl
mat2 m2 = mat2(1.0);  // 2x2 矩阵
mat3 m3 = mat3(1.0);  // 3x3 矩阵
mat4 m4 = mat4(1.0);  // 4x4 矩阵（最常用）
```

### 3. 变量限定符

#### attribute（顶点属性）

- **仅用于顶点着色器**
- 每个顶点的输入数据
- 从应用程序传入

```glsl
attribute vec4 aPosition;  // 顶点位置
attribute vec2 aTexCoord;  // 纹理坐标
attribute vec4 aColor;     // 顶点颜色
```

#### uniform（统一变量）

- **两种着色器都可用**
- 所有顶点/片段使用相同的值
- 适合传递常量（如时间、矩阵、颜色）

```glsl
uniform float uTime;       // 时间
uniform vec4 uColor;       // 颜色
uniform mat4 uMatrix;      // 变换矩阵
```

#### varying（易变变量）

- 从**顶点着色器传递到片段着色器**
- 会自动进行插值

```glsl
// 顶点着色器
varying vec4 vColor;
void main() {
    vColor = aColor;  // 传递颜色
    gl_Position = aPosition;
}

// 片段着色器
varying vec4 vColor;
void main() {
    gl_FragColor = vColor;  // 接收插值后的颜色
}
```

### 4. 内置变量

#### 顶点着色器

| 变量 | 类型 | 说明 |
|------|------|------|
| `gl_Position` | vec4 | **输出**：顶点位置（裁剪坐标） |
| `gl_PointSize` | float | **输出**：点的大小（像素） |

#### 片段着色器

| 变量 | 类型 | 说明 |
|------|------|------|
| `gl_FragCoord` | vec4 | **输入**：片段的窗口坐标 |
| `gl_FragColor` | vec4 | **输出**：片段颜色（RGBA） |
| `gl_FrontFacing` | bool | **输入**：是否为正面 |

### 5. 内置函数

#### 常用数学函数

| 函数 | 说明 | 示例 |
|------|------|------|
| `abs(x)` | 绝对值 | `abs(-1.0)` → 1.0 |
| `sin(x)`, `cos(x)`, `tan(x)` | 三角函数 | `sin(3.14159 / 2.0)` → 1.0 |
| `pow(x, y)` | x 的 y 次方 | `pow(2.0, 3.0)` → 8.0 |
| `sqrt(x)` | 平方根 | `sqrt(4.0)` → 2.0 |
| `floor(x)`, `ceil(x)` | 向下/向上取整 | `floor(1.5)` → 1.0 |
| `min(x, y)`, `max(x, y)` | 最小/最大值 | `max(1.0, 2.0)` → 2.0 |
| `clamp(x, min, max)` | 限制范围 | `clamp(1.5, 0.0, 1.0)` → 1.0 |

#### 向量函数

| 函数 | 说明 |
|------|------|
| `length(v)` | 向量长度 |
| `distance(v1, v2)` | 两向量距离 |
| `dot(v1, v2)` | 点积 |
| `cross(v1, v2)` | 叉积（仅 vec3） |
| `normalize(v)` | 归一化向量 |

#### 插值和混合函数

| 函数 | 说明 | 示例 |
|------|------|------|
| `mix(x, y, a)` | 线性插值 | `mix(0.0, 1.0, 0.5)` → 0.5 |
| `step(edge, x)` | 阶跃函数 | `step(0.5, 0.6)` → 1.0 |
| `smoothstep(e0, e1, x)` | 平滑插值 | 平滑过渡 |

### 6. 控制流

```glsl
// if-else
if (x > 0.5) {
    color = vec4(1.0, 0.0, 0.0, 1.0);
} else {
    color = vec4(0.0, 0.0, 1.0, 1.0);
}

// for 循环
for (int i = 0; i < 10; i++) {
    // 循环体
}

// while 循环
while (i < 10) {
    i++;
}
```

**注意**：避免在片段着色器中使用复杂循环，会影响性能。

## 💻 代码实践

### 1. 使用 uniform 改变颜色

#### 片段着色器

```glsl
precision mediump float;
uniform vec4 uColor;  // 从应用传入的颜色
void main() {
    gl_FragColor = uColor;
}
```

#### Renderer 代码

```kotlin
class Day03Renderer : GLSurfaceView.Renderer {

    private var uColorLocation: Int = 0
    private var currentColor = floatArrayOf(1.0f, 0.5f, 0.2f, 1.0f)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // ... 编译着色器、创建程序 ...

        // 获取 uniform 位置
        uColorLocation = GLES20.glGetUniformLocation(program, "uColor")
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        // 传递 uniform 值
        GLES20.glUniform4fv(uColorLocation, 1, currentColor, 0)

        // ... 设置顶点属性并绘制 ...
    }

    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        currentColor = floatArrayOf(r, g, b, a)
    }
}
```

### 2. 渐变色三角形

#### 顶点数据（带颜色）

```kotlin
// 顶点格式：x, y, z, r, g, b, a
private val verticesWithColor = floatArrayOf(
    // 位置             颜色
     0.0f,  0.5f, 0.0f,  1.0f, 0.0f, 0.0f, 1.0f,  // 红色
    -0.5f, -0.5f, 0.0f,  0.0f, 1.0f, 0.0f, 1.0f,  // 绿色
     0.5f, -0.5f, 0.0f,  0.0f, 0.0f, 1.0f, 1.0f   // 蓝色
)
```

#### 顶点着色器

```glsl
attribute vec4 aPosition;
attribute vec4 aColor;
varying vec4 vColor;  // 传递到片段着色器
void main() {
    vColor = aColor;
    gl_Position = aPosition;
}
```

#### 片段着色器

```glsl
precision mediump float;
varying vec4 vColor;  // 接收插值后的颜色
void main() {
    gl_FragColor = vColor;
}
```

#### Renderer 代码

```kotlin
override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    // ...
    aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition")
    aColorLocation = GLES20.glGetAttribLocation(program, "aColor")
}

override fun onDrawFrame(gl: GL10?) {
    // ...

    // 设置位置属性（前 3 个 float）
    GLES20.glVertexAttribPointer(
        aPositionLocation,
        3,                    // 3 个分量 (x, y, z)
        GLES20.GL_FLOAT,
        false,
        7 * 4,                // 步长：7 个 float = 28 字节
        vertexBuffer
    )
    GLES20.glEnableVertexAttribArray(aPositionLocation)

    // 设置颜色属性（后 4 个 float）
    vertexBuffer.position(3)  // 跳过前 3 个位置数据
    GLES20.glVertexAttribPointer(
        aColorLocation,
        4,                    // 4 个分量 (r, g, b, a)
        GLES20.GL_FLOAT,
        false,
        7 * 4,                // 步长相同
        vertexBuffer
    )
    GLES20.glEnableVertexAttribArray(aColorLocation)

    // 绘制
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
}
```

### 3. 动态缩放三角形

使用 uniform 传递缩放因子：

#### 顶点着色器

```glsl
attribute vec4 aPosition;
uniform float uScale;  // 缩放因子
void main() {
    gl_Position = vec4(aPosition.xyz * uScale, 1.0);
}
```

#### 应用代码

```kotlin
val uScaleLocation = GLES20.glGetUniformLocation(program, "uScale")
GLES20.glUniform1f(uScaleLocation, 1.5f)  // 放大 1.5 倍
```

## 🎨 练习任务

### 基础任务

1. **实现渐变色三角形**
   - 使用上面的代码，看到红绿蓝渐变的三角形
   - 尝试修改顶点颜色

2. **使用 uniform 改变颜色**
   - 添加按钮，动态改变三角形颜色
   - 使用 `glUniform4f()` 传递颜色

3. **动态缩放**
   - 使用 SeekBar 控制三角形大小
   - 范围：0.5 ~ 2.0

### 进阶任务

1. **颜色动画**
   - 让三角形颜色周期性变化
   - 提示：使用 `System.currentTimeMillis()` 和 `sin()` 函数

2. **呼吸效果**
   - 三角形大小周期性变化（缩放动画）
   - 提示：在 `onDrawFrame` 中计算时间

3. **混合颜色**
   - 使用 `mix()` 函数在两种颜色间过渡
   - 用 uniform 控制混合比例

## 📖 重要概念总结

### Uniform 相关 API

| API | 说明 |
|-----|------|
| `glGetUniformLocation(program, name)` | 获取 uniform 位置 |
| `glUniform1f(location, v0)` | 传递 1 个 float |
| `glUniform2f(location, v0, v1)` | 传递 2 个 float |
| `glUniform3f(location, v0, v1, v2)` | 传递 3 个 float |
| `glUniform4f(location, v0, v1, v2, v3)` | 传递 4 个 float |
| `glUniform4fv(location, count, value, offset)` | 传递 float 数组 |
| `glUniformMatrix4fv(...)` | 传递矩阵 |

### 关键概念

- **精度限定符**：控制浮点数精度，影响性能和质量
- **Swizzle**：重组向量分量的强大特性
- **Varying 插值**：顶点间的值会自动平滑过渡
- **内置函数**：GLSL 提供丰富的数学和向量函数

## ❓ 常见问题

### Q1: varying 变量如何插值？

GPU 会根据片段在三角形中的位置，对顶点的 varying 值进行**线性插值**。

示例：三角形三个顶点颜色分别为红、绿、蓝，中心的片段颜色会是三者的混合。

### Q2: 为什么顶点着色器不需要声明精度？

顶点着色器有默认精度（highp），而片段着色器没有，必须显式声明。

### Q3: glVertexAttribPointer 的步长参数怎么计算？

步长 = 每个顶点的总字节数

示例：位置(3 float) + 颜色(4 float) = 7 * 4 = 28 字节

### Q4: uniform 和 attribute 有什么区别？

| 特性 | attribute | uniform |
|------|-----------|---------|
| 作用域 | 仅顶点着色器 | 两种着色器 |
| 值 | 每个顶点不同 | 所有顶点相同 |
| 用途 | 顶点位置、颜色等 | 全局参数（时间、矩阵） |

## 🔗 扩展阅读

- [GLSL ES 1.00 规范](https://www.khronos.org/files/opengles_shading_language.pdf)
- [GLSL 内置函数参考](https://www.khronos.org/registry/OpenGL-Refpages/es2.0/)

## ✅ 今日小结

今天我们：
1. ✅ 深入学习了 GLSL 的数据类型和语法
2. ✅ 掌握了 uniform、attribute、varying 的使用
3. ✅ 学习了 GLSL 内置函数
4. ✅ 实现了渐变色三角形
5. ✅ 学会了动态控制着色器参数

**明天预告**：学习纹理贴图，将图片渲染到 OpenGL！🖼️

---

**学习检查清单**：
- [ ] 理解 GLSL 的精度限定符
- [ ] 掌握向量的 Swizzle 用法
- [ ] 理解 varying 变量的插值原理
- [ ] 能够使用 uniform 传递参数
- [ ] 成功实现渐变色三角形
