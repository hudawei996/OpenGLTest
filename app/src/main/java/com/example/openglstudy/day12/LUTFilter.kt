package com.example.openglstudy.day12

/**
 * LUT 滤镜数据类
 */
data class LUTFilter(
    val name: String,
    val fileName: String,
    val category: Category,
    var textureId: Int = 0
) {
    enum class Category(val displayName: String) {
        ALL("全部"),
        BABY("宝宝"),
        FOOD("美食"),
        LANDSCAPE("风景"),
        PLANT("植物"),
        RECOMMEND("推荐"),
        STREET("街景"),
        ORIGINAL("原图")
    }
    
    companion object {
        /**
         * 获取所有 LUT 滤镜列表
         */
        fun getAllFilters() = listOf(
            // 原图
            LUTFilter("原图", "filters/res_original.png", Category.ORIGINAL),
            
            // 宝宝系列
            LUTFilter("宝宝-甜蜜", "filters/res_baby_1.png", Category.BABY),
            LUTFilter("宝宝-可爱", "filters/res_baby_2.png", Category.BABY),
            LUTFilter("宝宝-温馨", "filters/res_baby_3.png", Category.BABY),
            LUTFilter("宝宝-活力", "filters/res_baby_4.png", Category.BABY),
            LUTFilter("宝宝-柔和", "filters/res_baby_5.png", Category.BABY),
            
            // 美食系列
            LUTFilter("美食-诱人", "filters/res_food_1.png", Category.FOOD),
            LUTFilter("美食-暖色", "filters/res_food_2.png", Category.FOOD),
            LUTFilter("美食-鲜艳", "filters/res_food_3.png", Category.FOOD),
            
            // 风景系列
            LUTFilter("风景-清新", "filters/res_landscape_1.png", Category.LANDSCAPE),
            LUTFilter("风景-壮丽", "filters/res_landscape_2.png", Category.LANDSCAPE),
            LUTFilter("风景-梦幻", "filters/res_landscape_3.png", Category.LANDSCAPE),
            
            // 植物系列
            LUTFilter("植物-生机", "filters/res_plant_1.png", Category.PLANT),
            LUTFilter("植物-清爽", "filters/res_plant_2.png", Category.PLANT),
            
            // 推荐系列
            LUTFilter("推荐-日常", "filters/res_recomend_1.png", Category.RECOMMEND),
            LUTFilter("推荐-自拍", "filters/res_recomend_2.png", Category.RECOMMEND),
            
            // 街景系列
            LUTFilter("街景-复古", "filters/res_street_1.png", Category.STREET),
            LUTFilter("街景-摩登", "filters/res_street_2.png", Category.STREET),
            LUTFilter("街景-夜色", "filters/res_street_3.png", Category.STREET)
        )
        
        /**
         * 按分类获取滤镜
         */
        fun getFiltersByCategory(category: Category): List<LUTFilter> {
            val all = getAllFilters()
            return if (category == Category.ALL) {
                all
            } else {
                all.filter { it.category == category }
            }
        }
    }
}

