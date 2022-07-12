package com.akashi.customviews.pie

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Keep
import com.akashi.customviews.R
import com.akashi.customviews.dp
import com.akashi.customviews.getOriginColor
import com.akashi.customviews.pie.data.PieViewData
import kotlin.properties.Delegates

/**
 * 衍生线长度
 */
private val LINE_LENGTH = 10.dp

/**
 * 边缘留白距离
 */
private val MARGIN_SCREEN_EDGE = 0.dp

/**
 * 起始绘制角度
 */
const val START_ANGLE = -90f

/**
 * 饼图
 * Created by Akashi on 2020/9/2.
 */
@Keep
class PieView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val mRender = PieRender()

    // PieView的实际高宽
    private var mWidth by Delegates.notNull<Int>()
    private var mHeight by Delegates.notNull<Int>()

    /**
     * 这一次需要绘制的角度
     */
    private var mDrawingAngle: Float = 0f
        set(value) {
            // 找出处于哪个扇形 计算mStartAngle
            mRender.currentArcData(value)?.let {
                // 1. 找出是第几个扇形，取上一个的终点角度作为起始角度
                val index = mRender.indexOf(it)
                mStartAngle = if (index == 0) {
                    START_ANGLE
                } else {
                    mRender.preArcData(index)?.endAngle ?: 0f
                }
                // 2. 正在绘制第几个
                mRender.mDrawingIndex = index

                // 3. 计算当前饼图绘制进度
                mRender.mDrawingProgress = (value - mStartAngle) / it.swipeAngle * 100
            }

            field = value - mStartAngle

            invalidate()
        }

    /**
     * 这一次起始绘制角度
     */
    private var mStartAngle: Float = START_ANGLE
        set(value) {
            if (value != field) {
                mRender.fixAngle(value)
                field = value
            }
        }

    init {
        // 获取xml背景色background
        if (background is ColorDrawable) {
            (background as ColorDrawable).color
        } else {
            getOriginColor(R.color.white_255)
        }.let {
            mRender.mOvalPaint.color = it
            mRender.mWhiteLinePaint.color = it
        }
    }

    fun setData(dataList: List<PieViewData>) {
        mRender.setData(dataList)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // 默认width是match_parent
        mWidth = measuredWidth
        val minHeight = measuredWidth * 89 / 111
        mHeight = minHeight

        setMeasuredDimension(mWidth, mHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // 在半径测量完毕后 生成线条数据
        mRender.calculateLines(LINE_LENGTH, MARGIN_SCREEN_EDGE, mWidth)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mRender.release()

        mRender.mCenterX = (mWidth / 2f)
        mRender.mCenterY = (mHeight / 2f)

        // 半径
        ((mWidth) * 8 / 17f / 2).let {
            mRender.mRadius = it
            mRender.mCenterOvalRadius = (it * 9 / 16)
        }
    }

    private val mCanvasDrawFilter = PaintFlagsDrawFilter(
        0,
        Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
    )

    override fun onDraw(canvas: Canvas) {

        // canvas抗锯齿
        canvas.drawFilter = mCanvasDrawFilter
        mRender.startRendering(canvas, mStartAngle, mDrawingAngle)
    }

    private var mAnimator: ObjectAnimator? = null
    private var mTempPlayTime: Long = 0L

    fun startAnimator(startDelay: Long = 200, duration: Long = 1000) {
        if (!mRender.isReadyToRender()) return
        val startAngle = START_ANGLE
        val swipeAngle = START_ANGLE + 360f
        mAnimator = ObjectAnimator.ofFloat(this, "mDrawingAngle", startAngle, swipeAngle).also {
            it.startDelay = startDelay
            it.duration = duration
            it.start()
        }
    }

    /**
     * 暂停动画
     */
    fun pauseAnimator() {
        mTempPlayTime = mAnimator?.currentPlayTime ?: 0
        mAnimator?.cancel()
    }

    /**
     * 恢复动画
     */
    fun resumeAnimator() {
        if (mTempPlayTime <= 0) return
        mAnimator?.start()
        mAnimator?.currentPlayTime = mTempPlayTime
    }
}
