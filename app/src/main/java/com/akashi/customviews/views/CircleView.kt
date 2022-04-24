package com.akashi.customviews.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.akashi.customviews.dp

/**
 * 限定宽度
 */
private val RADIUS = 100.dp
private val PADDING = 100.dp

/**
 * 圆
 * 示例：resolveSize和用法
 */
class CircleView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 比较 最大size 和widthMeasureSpec，取更小
        val size = ((PADDING + RADIUS) * 2).toInt()
        val width = resolveSize(size, widthMeasureSpec)

        setMeasuredDimension(width, width)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawCircle(RADIUS + PADDING, RADIUS + PADDING, RADIUS, mPaint)
    }
}