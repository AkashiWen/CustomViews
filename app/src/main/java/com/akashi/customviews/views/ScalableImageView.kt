package com.akashi.customviews.views

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import com.akashi.customviews.R
import com.akashi.customviews.dp
import com.akashi.customviews.getBitmap

/**
 * 可作为原图的宽度，数值越小，缩放后越模糊
 */
private val IMAGE_SIZE = 300.dp

/**
 * 增加方法比例，让放大后可滑动查看
 */
private const val EXTRA_SCALE_FACTOR = 1.5f

/**
 * View触控原理Demo：
 * 双向滑动的ImageView
 * 1. onSizeChanged 适配屏幕大小，进行缩放
 * 2. GestureDetectorCompat控制手势替换onTouchEvent
 */
class ScalableImageView(context: Context?, attrs: AttributeSet) : View(context, attrs),
    GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private val bitmap = getBitmap(resources, R.mipmap.genshin_1, IMAGE_SIZE.toInt())
    private var startX = 0F
    private var startY = 0F
    private val paint = Paint()

    /**
     * 缩小时、放大时的缩放比例
     */
    private var smallScale = 0F
    private var bigScale = 0F

    /**
     * 动画中要缩放的倍数
     */
    private var scaleFraction = 0f
        set(value) {
            field = value
            invalidate()
        }
    private val scaleAnimator: ObjectAnimator by lazy {
        ObjectAnimator.ofFloat(this, "scaleFraction", 0f, 1f)
    }

    private val mGestureDetectorCompat = GestureDetectorCompat(context, this)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 居中
        startX = (width - bitmap.width) / 2f
        startY = (height - bitmap.height) / 2f

        // ->if 是横图，按宽度填充手机，上下留白
        // ->else 是竖图，按高度填充手机，左右留白
        if (bitmap.width / bitmap.height > width / height) {
            smallScale = width / bitmap.width.toFloat()
            bigScale = height / bitmap.height.toFloat() * EXTRA_SCALE_FACTOR
        } else {
            smallScale = height / bitmap.height.toFloat()
            bigScale = width / bitmap.width.toFloat() * EXTRA_SCALE_FACTOR
        }

    }

    override fun onDraw(canvas: Canvas) {

        // 缩放比例
        val scale = smallScale + (bigScale - smallScale) * scaleFraction

        canvas.scale(scale, scale, width / 2f, height / 2f)
        canvas.drawBitmap(bitmap, startX, startY, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mGestureDetectorCompat.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent?): Boolean = true


    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean = false

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean = false

    override fun onLongPress(e: MotionEvent?) {}

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean = false

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean = false

    private var isSmallScaled = true

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        // 动画放大/缩小
        isSmallScaled = !isSmallScaled
        if (isSmallScaled) {
            scaleAnimator.reverse()
        } else {
            scaleAnimator.start()
        }
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean = false
}