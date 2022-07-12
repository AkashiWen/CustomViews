package com.akashi.customviews.pie.data

import android.graphics.Paint
import android.graphics.RectF

/**
 * 个人数据中心
 * Created by Akashi on 2020/9/4.
 */
data class PieViewData(
    var duration: String,
    var courseName: String,
    var percent: Float,
    var color: Int
)

/**
 * 饼图数据源
 * Created by Akashi on 2020/9/14.
 */
data class ArcData(
    var index: Int,
    var startAngle: Float,
    var endAngle: Float,
    var swipeAngle: Float,
    var textAbove: String,
    var textBelow: String,
    var color: Int
)

/**
 * 线条信息
 * Created by Akashi on 2020/9/11.
 */
data class LineData(
    // 落笔起点
    var startX: Float,
    var startY: Float,
    // 转折点
    var turningX: Float,
    var turningY: Float,
    // 终点
    var endX: Float,
    var endY: Float,
    var alpha: Int = 0,
    var color: Int,
    // 距离上一个lineData距离（以turningY为准）
    var dpToPre: Float,
    // 是否可以绘制
    var isFitToRender: Boolean
)

/**
 * 饼图信息
 * Created by Akashi on 2020/9/10.
 */
data class ArcCache(
    var rectF: RectF,
    var startAngle: Float,
    var swipeAngle: Float,
    var color: Int
)

/**
 * 文字缓存
 * Created by Akashi on 2020/9/14.
 */
data class TextCache(
    var text1: String,
    var text2: String,
    var x1: Float,
    var y1: Float,
    var x2: Float,
    var y2: Float,
    var textAlign: Paint.Align,
    var color1: Int,
    var color2: Int,
    var alpha: Int
)

/**
 * Created by Akashi on 2020/9/14.
 */
data class WhiteLineData(
    var endX: Float,
    var endY: Float
)
