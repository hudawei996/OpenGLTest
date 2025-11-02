# Day 11: 实时滤镜效果

## 📚 今日目标

- 掌握常见滤镜算法的原理和实现
- 学习色彩空间转换（RGB、HSV）
- 实现多种实时滤镜效果
- 理解滤镜参数调节机制
- 为美颜功能打下基础

## 🎯 学习内容

### 1. 滤镜基础原理

**滤镜本质**：在片段着色器中修改每个像素的颜色值。

```
原始像素颜色 → 滤镜算法处理 → 输出新颜色
   (R, G, B)  →   f(R, G, B)   → (R', G', B')
```

#### 滤镜的分类

| 类型 | 说明 | 示例 |
|------|------|------|
| **色彩调整** | 修改 RGB 值 | 灰度、反色、亮度 |
| **色调风格** | 特定色彩倾向 | 复古、暖色、冷色 |
| **卷积滤镜** | 使用周围像素 | 模糊、锐化、边缘检测 |
| **空间变换** | 改变像素位置 | 扭曲、瘦脸 |

### 2. 常见滤镜实现

#### 2.1 灰度滤镜

**原理**：将 RGB 转换为单一亮度值。

**加权平均公式**（符合人眼对颜色的敏感度）：
```
Gray = 0.299 × R + 0.587 × G + 0.114 × B
```

**GLSL 实现**：
```glsl
vec4 grayscale(vec4 color) {
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    return vec4(vec3(gray), color.a);
}
```

**简化版**（性能更高）：
```glsl
vec4 grayscale(vec4 color) {
    float gray = (color.r + color.g + color.b) / 3.0;
    return vec4(vec3(gray), color.a);
}
```

#### 2.2 反色滤镜

**原理**：每个颜色分量用 1.0 减去原值。

**GLSL 实现**：
```glsl
vec4 invert(vec4 color) {
    return vec4(1.0 - color.rgb, color.a);
}
```

#### 2.3 亮度调整

**原理**：所有颜色分量加上/减去同一个值。

**GLSL 实现**：
```glsl
uniform float uBrightness;  // -1.0 到 1.0

vec4 brightness(vec4 color, float value) {
    return vec4(color.rgb + value, color.a);
}
```

#### 2.4 对比度调整

**原理**：拉伸或压缩颜色值的范围。

**公式**：
```
newColor = (color - 0.5) × contrast + 0.5
```

**GLSL 实现**：
```glsl
uniform float uContrast;  // 0.0 到 2.0

vec4 contrast(vec4 color, float value) {
    return vec4((color.rgb - 0.5) * value + 0.5, color.a);
}
```

#### 2.5 饱和度调整

**原理**：在灰度和原色之间插值。

**GLSL 实现**：
```glsl
uniform float uSaturation;  // 0.0 到 2.0

vec4 saturation(vec4 color, float value) {
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    vec3 result = mix(vec3(gray), color.rgb, value);
    return vec4(result, color.a);
}
```

#### 2.6 暖色调滤镜

**原理**：增加红色和黄色，营造温暖感觉。

**GLSL 实现**：
```glsl
vec4 warm(vec4 color) {
    color.r = color.r + 0.1;  // 增加红色
    color.g = color.g + 0.05; // 微增绿色（产生黄色）
    return clamp(color, 0.0, 1.0);
}
```

#### 2.7 冷色调滤镜

**原理**：增加蓝色，营造清冷感觉。

**GLSL 实现**：
```glsl
vec4 cool(vec4 color) {
    color.b = color.b + 0.1;  // 增加蓝色
    return clamp(color, 0.0, 1.0);
}
```

#### 2.8 复古滤镜

**原理**：降低饱和度 + 增加棕褐色调。

**GLSL 实现**：
```glsl
vec4 sepia(vec4 color) {
    float r = color.r * 0.393 + color.g * 0.769 + color.b * 0.189;
    float g = color.r * 0.349 + color.g * 0.686 + color.b * 0.168;
    float b = color.r * 0.272 + color.g * 0.534 + color.b * 0.131;
    return vec4(r, g, b, color.a);
}
```

### 3. 色彩空间转换

#### 3.1 RGB vs HSV

| 色彩空间 | 组成 | 适用场景 |
|---------|------|---------|
| **RGB** | Red, Green, Blue | 显示、存储 |
| **HSV** | Hue, Saturation, Value | 色彩调整 |

**HSV 的优势**：
- **Hue（色相）**：调整整体色调（0-360°）
- **Saturation（饱和度）**：调整颜色鲜艳程度（0-1）
- **Value（明度）**：调整亮度（0-1）

#### 3.2 RGB 转 HSV

```glsl
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}
```

#### 3.3 HSV 转 RGB

```glsl
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}
```

#### 3.4 色相偏移滤镜

```glsl
uniform float uHueShift;  // 0.0 到 1.0

vec4 hueShift(vec4 color, float shift) {
    vec3 hsv = rgb2hsv(color.rgb);
    hsv.x = fract(hsv.x + shift);  // 循环色相
    vec3 rgb = hsv2rgb(hsv);
    return vec4(rgb, color.a);
}
```

### 4. 卷积滤镜

#### 4.1 什么是卷积？

卷积使用周围像素来计算当前像素的值。

**3×3 卷积核示例**：
```
┌─────┬─────┬─────┐
│ TL  │ TM  │ TR  │  TL: Top Left, TM: Top Middle, TR: Top Right
├─────┼─────┼─────┤
│ ML  │ MC  │ MR  │  ML: Middle Left, MC: Middle Center, MR: Middle Right
├─────┼─────┼─────┤
│ BL  │ BM  │ BR  │  BL: Bottom Left, BM: Bottom Middle, BR: Bottom Right
└─────┴─────┴─────┘
```

#### 4.2 模糊滤镜（均值滤波）

**卷积核**（所有权重相等）：
```
┌─────┬─────┬─────┐
│ 1/9 │ 1/9 │ 1/9 │
├─────┼─────┼─────┤
│ 1/9 │ 1/9 │ 1/9 │
├─────┼─────┼─────┤
│ 1/9 │ 1/9 │ 1/9 │
└─────┴─────┴─────┘
```

**GLSL 实现**：
```glsl
uniform vec2 uTexelSize;  // vec2(1.0/width, 1.0/height)

vec4 blur(sampler2D texture, vec2 texCoord) {
    vec4 sum = vec4(0.0);
    for (float x = -1.0; x <= 1.0; x += 1.0) {
        for (float y = -1.0; y <= 1.0; y += 1.0) {
            vec2 offset = vec2(x, y) * uTexelSize;
            sum += texture2D(texture, texCoord + offset);
        }
    }
    return sum / 9.0;
}
```

#### 4.3 锐化滤镜

**卷积核**：
```
┌─────┬─────┬─────┐
│  0  │ -1  │  0  │
├─────┼─────┼─────┤
│ -1  │  5  │ -1  │
├─────┼─────┼─────┤
│  0  │ -1  │  0  │
└─────┴─────┴─────┘
```

**GLSL 实现**：
```glsl
vec4 sharpen(sampler2D texture, vec2 texCoord, vec2 texelSize) {
    vec4 center = texture2D(texture, texCoord);
    vec4 top    = texture2D(texture, texCoord + vec2(0.0, texelSize.y));
    vec4 bottom = texture2D(texture, texCoord - vec2(0.0, texelSize.y));
    vec4 left   = texture2D(texture, texCoord - vec2(texelSize.x, 0.0));
    vec4 right  = texture2D(texture, texCoord + vec2(texelSize.x, 0.0));
    
    return center * 5.0 - top - bottom - left - right;
}
```

### 5. 综合滤镜着色器

```glsl
#extension GL_OES_EGL_image_external : require
precision mediump float;

varying vec2 vTexCoord;
uniform samplerExternalOES uTexture;
uniform int uFilterType;
uniform vec2 uTexelSize;

// 滤镜参数
uniform float uIntensity;  // 滤镜强度 0.0 - 1.0

// RGB 转 HSV
vec3 rgb2hsv(vec3 c) {
    // ... 实现代码 ...
}

// HSV 转 RGB
vec3 hsv2rgb(vec3 c) {
    // ... 实现代码 ...
}

// 灰度滤镜
vec4 grayscale(vec4 color) {
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    return vec4(vec3(gray), color.a);
}

// 复古滤镜
vec4 sepia(vec4 color) {
    float r = color.r * 0.393 + color.g * 0.769 + color.b * 0.189;
    float g = color.r * 0.349 + color.g * 0.686 + color.b * 0.168;
    float b = color.r * 0.272 + color.g * 0.534 + color.b * 0.131;
    return vec4(r, g, b, color.a);
}

// 暖色调滤镜
vec4 warm(vec4 color) {
    color.r = min(color.r + 0.1, 1.0);
    color.g = min(color.g + 0.05, 1.0);
    return color;
}

// 冷色调滤镜
vec4 cool(vec4 color) {
    color.b = min(color.b + 0.1, 1.0);
    return color;
}

// 反色滤镜
vec4 invert(vec4 color) {
    return vec4(1.0 - color.rgb, color.a);
}

// 黑白滤镜（高对比度）
vec4 blackWhite(vec4 color) {
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    float bw = step(0.5, gray);  // 阈值 0.5
    return vec4(vec3(bw), color.a);
}

void main() {
    vec4 color = texture2D(uTexture, vTexCoord);
    vec4 filtered;
    
    if (uFilterType == 0) {
        // 原图
        filtered = color;
    } else if (uFilterType == 1) {
        // 灰度
        filtered = grayscale(color);
    } else if (uFilterType == 2) {
        // 复古
        filtered = sepia(color);
    } else if (uFilterType == 3) {
        // 暖色调
        filtered = warm(color);
    } else if (uFilterType == 4) {
        // 冷色调
        filtered = cool(color);
    } else if (uFilterType == 5) {
        // 反色
        filtered = invert(color);
    } else if (uFilterType == 6) {
        // 黑白
        filtered = blackWhite(color);
    } else {
        filtered = color;
    }
    
    // 应用滤镜强度（原图和滤镜效果的混合）
    gl_FragColor = mix(color, filtered, uIntensity);
}
```

### 6. 滤镜参数调节

#### 6.1 滤镜强度

```kotlin
// 使用 SeekBar 调节滤镜强度
seekBarIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        val intensity = progress / 100.0f
        glSurfaceView.queueEvent {
            renderer.setFilterIntensity(intensity)
        }
    }
})
```

#### 6.2 Renderer 中的参数传递

```kotlin
class Day11Renderer : GLSurfaceView.Renderer {
    
    @Volatile
    private var filterType: FilterType = FilterType.NONE
    
    @Volatile
    private var filterIntensity: Float = 1.0f
    
    override fun onDrawFrame(gl: GL10?) {
        // ... 其他代码 ...
        
        // 传递滤镜参数
        GLES20.glUniform1i(uFilterTypeLocation, filterType.ordinal)
        GLES20.glUniform1f(uIntensityLocation, filterIntensity)
        
        // 绘制
        drawQuad()
    }
    
    fun setFilterType(type: FilterType) {
        filterType = type
    }
    
    fun setFilterIntensity(intensity: Float) {
        filterIntensity = intensity.coerceIn(0f, 1f)
    }
}
```

### 7. 性能优化

#### 7.1 避免复杂计算

```glsl
// ❌ 不好：每次都计算
vec4 color = texture2D(uTexture, vTexCoord);
float gray = color.r * 0.299 + color.g * 0.587 + color.b * 0.114;

// ✅ 好：使用内置函数
float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
```

#### 7.2 减少分支

```glsl
// ❌ 不好：大量 if-else
if (type == 0) { ... }
else if (type == 1) { ... }
else if (type == 2) { ... }
// ... 很多分支

// ✅ 好：使用不同的着色器程序
// 每种滤镜使用独立的着色器，避免分支
```

#### 7.3 降低采样次数

```glsl
// 对于模糊等需要多次采样的滤镜
// 可以先降低分辨率，再放大
```

### 8. 实用技巧

#### 8.1 滤镜预设

```kotlin
data class FilterPreset(
    val name: String,
    val type: FilterType,
    val intensity: Float = 1.0f,
    val params: Map<String, Float> = emptyMap()
)

val presets = listOf(
    FilterPreset("原图", FilterType.NONE),
    FilterPreset("黑白", FilterType.GRAYSCALE, 1.0f),
    FilterPreset("淡雅", FilterType.GRAYSCALE, 0.5f),
    FilterPreset("复古", FilterType.SEPIA, 0.8f),
    FilterPreset("暖阳", FilterType.WARM, 1.0f),
    FilterPreset("冰雪", FilterType.COOL, 1.0f)
)
```

#### 8.2 滤镜组合

```glsl
// 先应用灰度，再应用对比度
vec4 color = texture2D(uTexture, vTexCoord);
color = grayscale(color);
color = contrast(color, 1.5);
gl_FragColor = color;
```

#### 8.3 实时预览优化

```kotlin
// 使用低分辨率预览
val preview = Preview.Builder()
    .setTargetResolution(Size(640, 480))  // 降低分辨率
    .build()
```

## 💻 代码实践

### 今日任务

实现实时滤镜相机应用：

1. **集成 Day10 的相机预览功能**
2. **实现 7 种滤镜效果**：
   - 原图
   - 灰度
   - 复古
   - 暖色调
   - 冷色调
   - 反色
   - 黑白
3. **添加滤镜强度调节**
4. **支持滤镜实时切换**

### 实现效果

- 📹 实时相机预览
- 🎨 7 种滤镜效果
- 🎚️ 滤镜强度调节（SeekBar）
- 🔄 实时切换无卡顿
- 📸 支持拍照保存

### 核心代码结构

```kotlin
class Day11Renderer(glSurfaceView: GLSurfaceView) : GLSurfaceView.Renderer {

    enum class FilterType {
        NONE,       // 原图
        GRAYSCALE,  // 灰度
        SEPIA,      // 复古
        WARM,       // 暖色调
        COOL,       // 冷色调
        INVERT,     // 反色
        BLACK_WHITE // 黑白
    }
    
    private var filterType: FilterType = FilterType.NONE
    private var filterIntensity: Float = 1.0f
    
    override fun onDrawFrame(gl: GL10?) {
        // 更新纹理
        surfaceTexture.updateTexImage()
        
        // 传递滤镜参数
        GLES20.glUniform1i(uFilterTypeLocation, filterType.ordinal)
        GLES20.glUniform1f(uIntensityLocation, filterIntensity)
        
        // 绘制
        drawQuad()
    }
}
```

## 🧪 练习任务

### 基础任务

1. ✅ 实现至少 5 种滤镜效果
2. ✅ 添加滤镜强度调节
3. ✅ 支持实时切换

### 进阶任务

1. 🎨 实现自定义滤镜（调节色相、饱和度、亮度）
2. 💾 保存当前滤镜效果的照片
3. 🖼️ 添加滤镜预览缩略图
4. 📊 显示当前滤镜名称和参数

### 挑战任务

1. 🎬 实现 LUT（Look-Up Table）滤镜
2. 🌈 实现多重滤镜叠加
3. 🎭 实现局部滤镜（只对部分区域应用）
4. 📈 性能监控（FPS、帧耗时）

## 📖 知识点总结

### 滤镜分类

| 类型 | 原理 | 性能 | 示例 |
|------|------|------|------|
| **颜色调整** | 修改像素 RGB 值 | 高 | 灰度、反色 |
| **色调风格** | 特定色彩变换 | 高 | 复古、暖色 |
| **卷积滤镜** | 使用周围像素 | 低 | 模糊、锐化 |
| **空间变换** | 改变像素位置 | 中 | 扭曲、旋转 |

### GLSL 常用函数

| 函数 | 说明 | 示例 |
|------|------|------|
| `dot(v1, v2)` | 点积 | `dot(color.rgb, vec3(0.299, 0.587, 0.114))` |
| `mix(x, y, a)` | 线性插值 | `mix(original, filtered, intensity)` |
| `clamp(x, min, max)` | 限制范围 | `clamp(color, 0.0, 1.0)` |
| `step(edge, x)` | 阶跃函数 | `step(0.5, gray)` |
| `smoothstep(e0, e1, x)` | 平滑阶跃 | `smoothstep(0.4, 0.6, gray)` |

### 最佳实践

1. ✅ **使用内置函数**：性能更好
2. ✅ **避免过多分支**：影响 GPU 性能
3. ✅ **参数可调节**：提供更好的用户体验
4. ✅ **提供强度控制**：允许部分应用滤镜
5. ✅ **预设滤镜**：方便用户快速选择

## 🔗 参考资料

### 滤镜算法
- [Instagram Filters](https://github.com/danielamitay/Instagram-Filters) - Instagram 滤镜算法
- [GPUImage](https://github.com/cats-oss/android-gpuimage) - 开源滤镜库
- [Color Theory](https://www.colormatters.com/color-and-design/basic-color-theory) - 色彩理论

### GLSL 资源
- [The Book of Shaders](https://thebookofshaders.com/) - GLSL 教程
- [Shadertoy](https://www.shadertoy.com/) - GLSL 示例库

## 📝 今日总结

今天我们学习了实时滤镜效果：

1. ✅ 理解了滤镜的基本原理：修改像素颜色值
2. ✅ 掌握了 7 种常见滤镜的实现
3. ✅ 学习了色彩空间转换（RGB ↔ HSV）
4. ✅ 实现了滤镜强度调节机制
5. ✅ 了解了性能优化技巧

**关键要点**：
- 滤镜本质是片段着色器中的颜色变换
- 使用内置函数提高性能
- 滤镜强度通过 mix() 控制
- 卷积滤镜需要多次采样，性能较低

**阶段三完成！** 🎉

明天我们将进入 **阶段四：高级特效**，学习 **美颜算法 - 磨皮**！

---

**完成打卡**：学完本节后，请在 `LEARNING_PROGRESS.md` 中勾选 Day 11 ✅

