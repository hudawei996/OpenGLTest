# Day 12: LUT 滤镜（Look-Up Table）

## 📚 今日目标

- 理解 LUT（查找表）的工作原理
- 掌握 3D LUT 的使用方法
- 实现专业级调色滤镜
- 学习 LUT 资源加载和管理
- 支持多种 LUT 滤镜快速切换

## 🎯 学习内容

### 1. LUT 简介

**LUT（Look-Up Table，查找表）** 是一种高效的颜色映射技术，广泛应用于专业调色和滤镜效果。

#### 什么是 LUT？

LUT 本质是一个**颜色映射表**：
```
输入颜色 (R, G, B) → LUT 查找 → 输出颜色 (R', G', B')
```

**类比**：
- 字典：输入单词 → 查找 → 输出释义
- LUT：输入颜色 → 查找 → 输出新颜色

#### LUT 的优势

| 特性 | LUT 滤镜 | 传统滤镜 |
|------|---------|---------|
| **复杂度** | 任意复杂 | 受算法限制 |
| **性能** | 极快（一次采样） | 可能很慢 |
| **调色精度** | 专业级 | 有限 |
| **制作方式** | Photoshop/Lightroom | 手写代码 |
| **灵活性** | 可随意替换 | 需修改代码 |

### 2. 3D LUT 原理

#### 2.1 颜色立方体

RGB 颜色空间是一个**立方体**：
```
        B (0,0,1)
       /|
      / |
     /  |
(0,0,0)─────> R (1,0,0)
    |  /
    | /
    |/
    G (0,1,0)
```

**3D LUT** 是这个颜色立方体的**离散化采样**：
- 8×8×8 = 512 种颜色
- 16×16×16 = 4,096 种颜色
- **64×64×64 = 262,144 种颜色** ← 常用

#### 2.2 LUT 图片格式

标准的 64×64×64 LUT 存储为 **512×512** 的 2D 图片：

```
┌────────────────────────────────┐
│ 64x64 │ 64x64 │ 64x64 │ ... x8 │  ← 蓝色 = 0
├────────────────────────────────┤
│ 64x64 │ 64x64 │ 64x64 │ ... x8 │  ← 蓝色 = 1
├────────────────────────────────┤
│  ...  │  ...  │  ...  │  ...   │
├────────────────────────────────┤
│ 64x64 │ 64x64 │ 64x64 │ ... x8 │  ← 蓝色 = 7
└────────────────────────────────┘
  ↑ 红色和绿色从 0-63 变化
```

**布局规则**：
- 横向 8 块（蓝色通道：0-7）
- 纵向 8 块（蓝色通道：0-7）
- 每块内部：红色（X轴），绿色（Y轴）

#### 2.3 查找算法

```
输入：RGB = (0.75, 0.50, 0.25)

步骤 1：计算 3D 索引
  r_index = 0.75 × 63 = 47.25
  g_index = 0.50 × 63 = 31.5
  b_index = 0.25 × 63 = 15.75

步骤 2：映射到 2D 纹理坐标
  blue_x = floor(b_index) % 8 = 7
  blue_y = floor(b_index) / 8 = 1
  
  offset_x = blue_x * 64 + r_index = 7*64 + 47.25 = 495.25
  offset_y = blue_y * 64 + g_index = 1*64 + 31.5 = 95.5
  
  uv = (offset_x / 512, offset_y / 512)

步骤 3：采样 LUT 纹理
  new_color = texture2D(uLUT, uv)
```

### 3. GLSL 实现

#### 3.1 基础 LUT 着色器

```glsl
precision mediump float;

uniform sampler2D uTexture;    // 相机纹理
uniform sampler2D uLUTTexture; // LUT 纹理
uniform float uIntensity;      // LUT 强度

varying vec2 vTexCoord;

vec4 applyLUT(vec4 color, sampler2D lut) {
    // 1. 获取 RGB 值（0.0 - 1.0）
    float r = color.r;
    float g = color.g;
    float b = color.b;
    
    // 2. 计算蓝色索引
    float blueIndex = b * 63.0;
    
    // 3. 计算 LUT 坐标
    float quad = floor(floor(blueIndex) / 8.0);
    float xOffset = (floor(blueIndex) - quad * 8.0) / 8.0;
    float yOffset = quad / 8.0;
    
    // 4. 在 64x64 小格子内的位置
    float lutX = xOffset + (r / 8.0);
    float lutY = yOffset + (g / 8.0);
    
    // 5. 采样 LUT
    vec4 newColor = texture2D(lut, vec2(lutX, lutY));
    
    return newColor;
}

void main() {
    vec4 color = texture2D(uTexture, vTexCoord);
    vec4 lutColor = applyLUT(color, uLUTTexture);
    
    // 使用强度混合原色和 LUT 颜色
    gl_FragColor = mix(color, lutColor, uIntensity);
}
```

#### 3.2 优化版 LUT 着色器（线性插值）

为了更平滑的过渡，可以在相邻蓝色层之间插值：

```glsl
vec4 applyLUTWithInterpolation(vec4 color, sampler2D lut) {
    float r = color.r * 63.0;
    float g = color.g * 63.0;
    float b = color.b * 63.0;
    
    // 蓝色索引的整数和小数部分
    float b_floor = floor(b);
    float b_fract = fract(b);
    
    // 计算两个相邻蓝色层的坐标
    vec2 uv1 = getLUTCoord(r, g, b_floor);
    vec2 uv2 = getLUTCoord(r, g, b_floor + 1.0);
    
    // 采样两个层
    vec4 color1 = texture2D(lut, uv1);
    vec4 color2 = texture2D(lut, uv2);
    
    // 在两层之间插值
    return mix(color1, color2, b_fract);
}

vec2 getLUTCoord(float r, float g, float b) {
    float quad = floor(b / 8.0);
    float xOffset = (b - quad * 8.0) / 8.0;
    float yOffset = quad / 8.0;
    
    float lutX = xOffset + (r / 512.0) + 0.5 / 512.0;
    float lutY = yOffset + (g / 512.0) + 0.5 / 512.0;
    
    return vec2(lutX, lutY);
}
```

### 4. LUT 资源加载

#### 4.1 从 Assets 加载

```kotlin
fun loadLUTFromAssets(assetManager: AssetManager, fileName: String): Int {
    val bitmap = assetManager.open(fileName).use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    }
    
    return loadLUTTexture(bitmap)
}

fun loadLUTTexture(bitmap: Bitmap?): Int {
    if (bitmap == null) return 0
    
    val textures = IntArray(1)
    GLES20.glGenTextures(1, textures, 0)
    
    val textureId = textures[0]
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    
    // 重要：LUT 纹理必须使用线性过滤
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    
    bitmap.recycle()
    
    return textureId
}
```

#### 4.2 LUT 管理器

```kotlin
class LUTManager(private val assetManager: AssetManager) {
    
    data class LUTFilter(
        val name: String,
        val fileName: String,
        var textureId: Int = 0
    )
    
    val filters = listOf(
        LUTFilter("原图", "filters/res_original.png"),
        LUTFilter("宝宝1", "filters/res_baby_1.png"),
        LUTFilter("宝宝2", "filters/res_baby_2.png"),
        LUTFilter("美食1", "filters/res_food_1.png"),
        LUTFilter("风景1", "filters/res_landscape_1.png"),
        // ... 更多
    )
    
    fun loadAllLUTs() {
        filters.forEach { filter ->
            filter.textureId = loadLUTFromAssets(assetManager, filter.fileName)
            Log.d(TAG, "加载 LUT: ${filter.name}, textureId=${filter.textureId}")
        }
    }
    
    fun getFilterByName(name: String): LUTFilter? {
        return filters.find { it.name == name }
    }
}
```

### 5. LUT 的优势

#### 5.1 与传统滤镜对比

| 特性 | 传统滤镜（着色器算法） | LUT 滤镜 |
|------|---------------------|---------|
| **复杂度** | 简单（灰度、复古） | 任意复杂（专业调色） |
| **性能** | 中等 | 极快（一次采样） |
| **调色精度** | 有限 | 专业级 |
| **制作方式** | 写代码 | Photoshop/Lightroom |
| **灵活性** | 需修改代码 | 直接替换图片 |

#### 5.2 应用场景

- 📸 **Instagram 风格**：复刻各种 Instagram 滤镜
- 🎬 **电影调色**：模拟电影色调
- 🎨 **专业调色**：设计师定制的色彩方案
- 🌈 **品牌风格**：统一的品牌视觉风格

### 6. LUT 制作方法

#### 方法 1：Adobe Photoshop

1. 打开图片，调整色彩（曲线、色阶、色相/饱和度）
2. 应用调整图层到标准 LUT 模板
3. 导出为 512×512 PNG 图片

#### 方法 2：Adobe Lightroom

1. 调整照片到满意的效果
2. 导出为 LUT 文件（.cube 格式）
3. 转换为 PNG 图片（使用工具）

#### 方法 3：在线工具

- [LUT Generator](https://www.lutify.me/)
- [3D LUT Creator](https://3dlutcreator.com/)

### 7. 性能优化

#### 7.1 LUT 纹理缓存

```kotlin
// ✅ 好的做法：预加载所有 LUT
override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    lutManager.loadAllLUTs()  // 只加载一次
}

// ❌ 不好的做法：切换时加载
fun switchLUT(name: String) {
    val texture = loadLUTFromAssets(name)  // 每次都加载，慢！
}
```

#### 7.2 纹理压缩

```kotlin
// LUT 纹理通常较小（512×512），不需要压缩
// 但可以考虑使用 RGB565（如果不需要 Alpha）
```

#### 7.3 异步加载

```kotlin
// 在后台线程加载 LUT
GlobalScope.launch(Dispatchers.IO) {
    val textures = lutManager.loadAllLUTs()
    
    withContext(Dispatchers.Main) {
        glSurfaceView.queueEvent {
            lutManager.updateTextures(textures)
        }
    }
}
```

## 💻 代码实践

### 今日任务

实现 LUT 滤镜相机：

1. **加载 19 个 LUT 滤镜**
2. **实现 LUT 查找算法**（GLSL）
3. **支持滤镜快速切换**
4. **添加滤镜强度调节**
5. **提供滤镜分类（宝宝、美食、风景等）**

### 核心功能

```kotlin
class Day12Renderer(glSurfaceView: GLSurfaceView) : GLSurfaceView.Renderer {
    
    // LUT 滤镜列表
    private val lutFilters = mutableListOf<LUTFilter>()
    private var currentLUTTextureId = 0
    private var lutIntensity = 1.0f
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 加载所有 LUT
        loadAllLUTs()
    }
    
    override fun onDrawFrame(gl: GL10?) {
        // 应用当前 LUT
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, currentLUTTextureId)
        GLES20.glUniform1i(uLUTTextureLocation, 1)
        GLES20.glUniform1f(uIntensityLocation, lutIntensity)
        
        // 渲染
        drawQuad()
    }
}
```

## 🎨 滤镜分类

根据您的 LUT 资源，我们有以下分类：

### 🍼 宝宝系列（5个）
- baby_1, baby_2, baby_3, baby_4, baby_5
- 适合：儿童摄影、可爱风格

### 🍔 美食系列（3个）
- food_1, food_2, food_3
- 适合：美食拍摄、暖色调

### 🏞️ 风景系列（3个）
- landscape_1, landscape_2, landscape_3
- 适合：风景摄影、自然风光

### 🌿 植物系列（2个）
- plant_1, plant_2
- 适合：绿植、清新风格

### ⭐ 推荐系列（2个）
- recomend_1, recomend_2
- 适合：日常自拍

### 🏙️ 街景系列（3个）
- street_1, street_2, street_3
- 适合：城市摄影、街拍

## 🧪 练习任务

### 基础任务

1. ✅ 实现 LUT 查找算法
2. ✅ 加载所有 LUT 纹理
3. ✅ 支持滤镜切换

### 进阶任务

1. 🎨 实现滤镜分类浏览
2. 🎚️ 添加滤镜强度调节（0-100%）
3. 🖼️ 显示滤镜缩略图预览
4. ⭐ 收藏喜爱的滤镜

### 挑战任务

1. 🎬 实现滤镜过渡动画（从一个 LUT 平滑过渡到另一个）
2. 🎭 实现双 LUT 混合
3. 📊 分析 LUT 色彩特征
4. 🔧 自定义 LUT 生成工具

## 📖 知识点总结

### LUT 的本质

```
LUT = 预计算的颜色映射表
    = 将复杂的调色算法"烘焙"到一张图片中
    = 查表比计算快得多
```

### 关键概念

| 概念 | 说明 |
|------|------|
| **3D LUT** | 三维颜色查找表（R、G、B 三个维度） |
| **512×512** | 标准 LUT 图片大小 |
| **64×64×64** | 颜色精度（262,144 种颜色） |
| **线性插值** | 在相邻颜色间平滑过渡 |

### 最佳实践

1. ✅ **预加载所有 LUT**：启动时一次性加载
2. ✅ **使用线性过滤**：`GL_LINEAR` 而非 `GL_NEAREST`
3. ✅ **添加强度控制**：mix(original, lut, intensity)
4. ✅ **纹理缓存**：避免重复加载
5. ✅ **异步加载**：不阻塞 UI 线程

## 🔗 参考资料

### LUT 技术
- [3D LUT 原理](https://en.wikipedia.org/wiki/3D_lookup_table)
- [Color Grading with LUTs](https://blog.frame.io/2017/03/06/luts-grutas/)

### LUT 资源
- [Free LUTs](https://www.freepresets.com/product/free-luts/)
- [RocketStock LUTs](https://www.rocketstock.com/free-after-effects-templates/35-free-luts-for-color-grading-videos/)

## 📝 今日总结

今天我们学习了 LUT 滤镜技术：

1. ✅ 理解了 LUT 的工作原理：颜色查找表
2. ✅ 掌握了 3D LUT 的存储格式：512×512 图片
3. ✅ 实现了 LUT 查找算法：RGB → 2D 纹理坐标
4. ✅ 支持 19 种专业滤镜
5. ✅ 理解了 LUT 的性能优势

**关键要点**：
- LUT 将复杂调色"烘焙"成查找表
- 一次纹理采样即可实现专业级调色
- 性能极高，适合实时应用
- 可以直接使用 Photoshop/Lightroom 制作

通过 LUT，您现在可以轻松实现 Instagram、VSCO 等应用的专业滤镜效果！🎨

---

**完成打卡**：学完本节后，请在 `LEARNING_PROGRESS.md` 中记录 ✅

