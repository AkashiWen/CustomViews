package com.akashi.customviews.views

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import androidx.core.animation.doOnEnd
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import com.akashi.customviews.R
import com.akashi.customviews.dp
import com.akashi.customviews.getBitmap
import kotlin.math.max
import kotlin.math.min

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
 * 3. 双击onDoubleTap、滑动onScroll
 * 4. 滑动偏移修正onScroll
 * 5. 惯性滑动onFling()、OverScroller
 * 6. 缩放动画优化onDoubleTap
 */
class ScalableImageView(context: Context?, attrs: AttributeSet) : View(context, attrs),
    GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, Runnable {

    private val bitmap = getBitmap(resources, R.mipmap.genshin_1, IMAGE_SIZE.toInt())
    private var startX = 0F
    private var startY = 0F
    private var offsetX = 0F
    private var offsetY = 0F
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

    private val scroller by lazy {
        OverScroller(context)
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
        // 移动坐标系
        canvas.translate(offsetX * scaleFraction, offsetY * scaleFraction)
        // 缩放比例
        val scale = smallScale + (bigScale - smallScale) * scaleFraction
        canvas.scale(scale, scale, width / 2f, height / 2f)
        // 绘制
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
    ): Boolean {
        if (isSmallScaled) return false
        offsetX -= distanceX
        offsetY -= distanceY
        fixOffsets()

        invalidate()
        return false
    }

    private fun fixOffsets() {
        // 限制左右边界
        val limitOffsetX = (bitmap.width * bigScale - width) / 2
        offsetX = min(offsetX, limitOffsetX) // 手指向左，坐标系向右移动，X正向
        offsetX = max(offsetX, -limitOffsetX) // 手指向右，坐标系向左移动，X反向
        // 限制上下边界
        val limitOffsetY = (bitmap.height * bigScale - height) / 2
        offsetY = min(offsetY, limitOffsetY) // 手指向上，坐标系向下移动，Y正向
        offsetY = max(offsetY, -limitOffsetX) // 手指向下，坐标系向上移动，Y反向
    }

    override fun onLongPress(e: MotionEvent?) {}

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (isSmallScaled) return false

        val absX = (bitmap.width * bigScale - width) / 2
        val absY = (bitmap.height * bigScale - height) / 2
        scroller.fling(
            offsetX.toInt(),
            offsetY.toInt(),
            velocityX.toInt(),
            velocityY.toInt(),
            -absX.toInt(),
            absX.toInt(),
            -absY.toInt(),
            absY.toInt(),
            40.dp.toInt(),
            40.dp.toInt()
        )
        ViewCompat.postOnAnimation(this, this)

        return false
    }

    override fun run() {
        if (scroller.computeScrollOffset()) {
            offsetX = scroller.currX.toFloat()
            offsetY = scroller.currY.toFloat()
            invalidate()
            ViewCompat.postOnAnimation(this, this)
        }
    }


    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean = false

    private var isSmallScaled = true

    override fun onDoubleTap(e: MotionEvent): Boolean {
        // 动画放大/缩小
        isSmallScaled = !isSmallScaled
        if (isSmallScaled) {
            scaleAnimator.reverse()
        } else {
            // 以双击坐标为中心放大图片
            val bigTimesSmall = bigScale / smallScale // 方法比例是缩小比例的几倍
            // 放大bigTimesSmall倍就是放大后的x偏移量，再减去缩小时偏移量，得到放大后相对放大前x的偏移量差，是多移动的距离，取负数，修正拉回来
            offsetX = -(e.x - width / 2F) * (bigTimesSmall - 1)
            offsetY = -(e.y - height / 2F) * (bigTimesSmall - 1)

            fixOffsets()
            scaleAnimator.start()
        }
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean = false
}