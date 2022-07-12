package com.akashi.customviews.pie

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.akashi.customviews.dp
import com.akashi.customviews.getOriginColor
import com.akashi.customviews.pie.data.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.properties.Delegates

private val TEXT_MIN_HEIGHT = 20.dp

// 外层圆环距离饼图margin值
private val OUTER_CIRCLE_MARGIN = 10.dp

// 衍生线起点的小圆点半径
private val STROKE_CAP_OVAL_RADIUS = 3.dp

// 两行文字最小间隔距离
private val MIN_SPACE_BETWEEN_TWO_LINES = 34.dp

private const val MAX_ANGLE = 360
private const val MAX_ALPHA = 255
private const val MAX_PROGRESS = 100

/**
 * 饼图渲染，包括缓存渲染、实时渲染
 * 1. Arc
 * 2. Line
 * 3. Text
 * Created by Akashi on 2020/9/14.
 */
class PieRender {

    val mOvalPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    val mWhiteLinePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.dp
    }

    private val mPiePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val mLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeCap = Paint.Cap.BUTT
        strokeWidth = 1.dp
    }

    private val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12.dp
    }

    var mCenterX by Delegates.notNull<Float>()
    var mCenterY by Delegates.notNull<Float>()
    var mRadius by Delegates.notNull<Float>()
    var mCenterOvalRadius by Delegates.notNull<Float>()

    // 数据源
    private val mArcDataList: MutableList<ArcData> = mutableListOf()
    private val mLineDataList: MutableList<LineData> = mutableListOf()
    private val mWhiteLineDataList: MutableList<WhiteLineData> = mutableListOf()

    // 缓存
    private val mArcCacheList = mutableListOf<ArcCache>()
    private val mLineCacheMap = mutableMapOf<Int, LineData>()
    private val mTextCacheMap = mutableMapOf<Int, TextCache>()

    /**
     * 正在绘制第几个饼图
     */
    var mDrawingIndex: Int = 0
        set(value) {
            isSwitchToNext = field != value
            field = value
        }

    /**
     * 当前饼图的绘制进度
     * 由此可得到线条绘制长度
     * 0.0 ~ 100.0
     */
    var mDrawingProgress: Float = 0f
        set(value) {
            field = if (isSwitchToNext) {
                0f
            } else {
                value
            }
        }

    /**
     * 开始绘制下一个
     */
    private var isSwitchToNext = false

    /**
     * 根据当前需要绘制的角度 获取对应的ArcData
     */
    fun currentArcData(drawingAngle: Float): ArcData? = mArcDataList.find {
        it.startAngle <= drawingAngle && it.endAngle > drawingAngle
    }

    fun preArcData(index: Int): ArcData? {
        if (index < 0 || index > mArcDataList.lastIndex) {
            return null
        }
        return mArcDataList[index - 1]
    }

    fun indexOf(data: ArcData) = mArcDataList.indexOf(data)

    /**
     * 切换到下一个绘制的饼图时，修正最新缓存中的扫过角度数据
     */
    fun fixAngle(value: Float) {
        if (mArcCacheList.isNullOrEmpty()) return
        mArcCacheList.let {
            it[it.size - 1]
        }.let {
            it.swipeAngle += value - it.swipeAngle - it.startAngle
        }
    }

    /**
     * 设置Arc数据源
     */
    fun setData(dataList: List<PieViewData>) {
        mArcDataList.clear()

        dataList.forEachIndexed { index, it ->
            val startAngle = if (index > 0) {
                mArcDataList[index - 1].endAngle
            } else {
                START_ANGLE
            }
            val swipeAngle = MAX_ANGLE * it.percent / MAX_PROGRESS
            val endAngle = startAngle + swipeAngle

            // 1. 组成饼图数据源
            ArcData(
                index = index,
                startAngle = startAngle,
                endAngle = endAngle,
                swipeAngle = swipeAngle,
                textAbove = it.duration,
                textBelow = it.courseName,
                color = getOriginColor(it.color)
            ).let {
                mArcDataList.add(it)
            }
        }
    }

    fun isReadyToRender() = this.mArcDataList.isNotEmpty()

    /**
     * 设置Line数据源
     */
    fun calculateLines(
        lineLength: Float,
        marginEdge: Float,
        measuredWidth: Int
    ) {
        var startAngle = START_ANGLE

        mArcDataList.forEachIndexed { index, it ->
            val swipeAngle = it.swipeAngle.toDouble()

            // 1. 从中心射出的白色线条 覆盖到扇形上
            calculateWhiteLines(startAngle, mRadius)

            // 2. 组成线条数据源
            val fixedRadius = mRadius + OUTER_CIRCLE_MARGIN

            val startY =
                fixedRadius * sin((Math.toRadians(startAngle + swipeAngle / 2f))).toFloat() + mCenterY
            val startX =
                fixedRadius * cos((Math.toRadians(startAngle + swipeAngle / 2f))).toFloat() + mCenterX

            // 2.1 转折点坐标(turningX, turningY)
            val turningPointY =
                (lineLength + fixedRadius) * sin((Math.toRadians(startAngle + swipeAngle / 2f))).toFloat() + mCenterY
            val turningPointX =
                (lineLength + fixedRadius) * cos((Math.toRadians(startAngle + swipeAngle / 2f))).toFloat() + mCenterX

            // 2.2 终点坐标(endX, endY)
            var endX = 0f
            var endY = 0f
            when (locate(turningPointX, turningPointY)) {
                IntervalEnum.MIDDLE_TOP,
                IntervalEnum.MIDDLE_BOTTOM,
                IntervalEnum.LEFT -> {
                    endX = 0f + marginEdge
                    endY = turningPointY
                }
                IntervalEnum.RIGHT -> {
                    endX = measuredWidth - marginEdge
                    endY = turningPointY
                }
            }

            // 3. 记录每个数据间隔距离
            val dpToPre = if (index == 0) {
                0f
            } else {
                val preLineData = mLineDataList[index - 1]
                abs(turningPointY - preLineData.turningY)
            }

            LineData(
                startX = startX,
                startY = startY,
                turningX = turningPointX,
                turningY = turningPointY,
                endX = endX,
                endY = endY,
                color = it.color,
                dpToPre = dpToPre,
                isFitToRender = false
            ).run {
                mLineDataList.add(this)
            }

            // end
            startAngle += swipeAngle.toFloat()
        }

        isFitToRender(mLineDataList)
    }

    private fun isFitToRender(lineDataList: MutableList<LineData>) {
        // isFitToRender 位置是否合适
        var mTempSpace = 0f
        lineDataList.forEachIndexed { index, lineData ->
            when {
                index == 0 -> {
                    lineData.isFitToRender = true
                }
                lineData.dpToPre + mTempSpace >= MIN_SPACE_BETWEEN_TWO_LINES -> {
                    lineData.isFitToRender = true
                    mTempSpace = 0f
                }
                else -> {
                    val preLineData = lineDataList[index - 1]
                    // 前后跨了左右象限
                    val c1 = preLineData.turningX <= mCenterX && lineData.turningX > mCenterX
                    val c2 = preLineData.turningX >= mCenterX && lineData.turningX < mCenterX
                    if (c1 || c2) {
                        lineData.isFitToRender = true
                        mTempSpace = 0f
                    } else {
                        lineData.isFitToRender = false
                        mTempSpace = +lineData.dpToPre
                    }
                }
            }
        }
    }

    private fun calculateWhiteLines(angle: Float, radius: Float) {
        val x = radius * cos(Math.toRadians(angle.toDouble())) + mCenterX
        val y = radius * sin(Math.toRadians(angle.toDouble())) + mCenterY

        WhiteLineData(
            endX = x.toFloat(),
            endY = y.toFloat()
        ).let {
            mWhiteLineDataList.add(it)
        }
    }

    fun startRendering(
        canvas: Canvas,
        startAngle: Float,
        drawingAngle: Float
    ) {
        renderArc(canvas, startAngle, drawingAngle)
        renderLine(canvas)
        renderStatic(canvas)
    }

    // //////////////////// statics /////////////////////////
    private fun renderStatic(canvas: Canvas) {
        // 画一个内圆遮住饼图，形成环形效果
        canvas.drawOval(
            mCenterX - mCenterOvalRadius,
            mCenterY - mCenterOvalRadius,
            mCenterX + mCenterOvalRadius,
            mCenterY + mCenterOvalRadius,
            mOvalPaint
        )

        mWhiteLineDataList.forEach {
            canvas.drawLine(mCenterX, mCenterY, it.endX, it.endY, mWhiteLinePaint)
        }
    }

    // //////////////////// arcs /////////////////////////
    private fun renderArc(
        canvas: Canvas,
        startAngle: Float,
        drawingAngle: Float
    ) {
        if (!isPrepared()) return
        drawArcCache(canvas)

        // 饼图区域
        val pieRectF = RectF(
            mCenterX - mRadius,
            mCenterY - mRadius,
            mCenterX + mRadius,
            mCenterY + mRadius
        )

        val color = mArcDataList[mDrawingIndex].color
        mPiePaint.color = color

        canvas.drawArc(pieRectF, startAngle, drawingAngle, true, mPiePaint)

        saveArcCache(pieRectF, startAngle, drawingAngle, color)
    }

    private fun saveArcCache(rectF: RectF, startAngle: Float, drawingAngle: Float, color: Int) {
        ArcCache(
            rectF,
            startAngle,
            drawingAngle,
            color
        ).let {
            mArcCacheList.add(it)
        }
    }

    private fun drawArcCache(canvas: Canvas) {
        mArcCacheList.forEach {
            mPiePaint.color = it.color

            canvas.drawArc(it.rectF, it.startAngle, it.swipeAngle, true, mPiePaint)
        }
    }

    // //////////////////// lines /////////////////////////
    private val mLinePath = Path()

    /**
     * 渲染衍生线条
     */
    private fun renderLine(canvas: Canvas) {
        if (!isPrepared()) return

        drawLineCache(canvas)

        val lineData = mLineDataList[mDrawingIndex]

        if (lineData.isFitToRender) {
            saveLineCache(lineData)
        }

        val endX = lineData.endX
        val endY = lineData.endY
        val availableTextLength = abs(lineData.startX - lineData.endX)
        renderText(canvas, endX, endY, availableTextLength, lineData)
    }

    private fun saveLineCache(lineData: LineData) {
        lineData.alpha = (mDrawingProgress * MAX_ALPHA / MAX_PROGRESS).toInt()
        mLineCacheMap[mDrawingIndex] = lineData
    }

    private fun drawLineCache(canvas: Canvas) {
        mLineCacheMap.forEach { map ->
            val it = map.value
            mLinePaint.style = Paint.Style.STROKE
            mLinePaint.color = it.color
            mLinePaint.alpha = it.alpha

            mLinePath.run {
                rewind()
                moveTo(it.startX, it.startY)
                lineTo(it.turningX, it.turningY)
                lineTo(it.endX, it.endY)
            }

            canvas.drawPath(mLinePath, mLinePaint)

            mLinePaint.style = Paint.Style.FILL
            canvas.drawCircle(it.startX, it.startY, STROKE_CAP_OVAL_RADIUS, mLinePaint)
        }
    }

    // //////////////////// texts /////////////////////////

    /**
     * 适配文字长度
     */
    private fun adjustText(text: String, availableTextLength: Float): String {
        val realLength = mTextPaint.measureText(text)
        if (availableTextLength < realLength) {
            val fixedIndex =
                mTextPaint.breakText(text, 0, text.length, true, availableTextLength, null)
            return "${text.substring(0, fixedIndex - 1)}..."
        }

        return text
    }

    private fun renderText(
        canvas: Canvas,
        x: Float,
        y: Float,
        availableTextLength: Float,
        lineData: LineData
    ) {
        drawTextCache(canvas)

        val alpha = (mDrawingProgress * MAX_ALPHA / MAX_PROGRESS).toInt()
        val data = mArcDataList[mDrawingIndex]
        val text1 = adjustText(data.textAbove, availableTextLength)
        val text2 = adjustText(data.textBelow, availableTextLength)

//        val text1 = data.textAbove
//        val text2 = data.textBelow

        val bounds = Rect()
        mTextPaint.getTextBounds(text1, 0, text1.length, bounds)
        val baselineOffset = abs((bounds.top + bounds.bottom) / 2)

        // 透明度 颜色
        mTextPaint.color = data.color
        mTextPaint.alpha = alpha

        // 根据坐标所处区间设置文字靠齐方向
        var y1 = 0f
        var y2 = 0f
        when (locate(x, y)) {
            IntervalEnum.LEFT -> {
                mTextPaint.textAlign = Paint.Align.LEFT
                // 第一次向上偏移
                y1 = y - TEXT_MIN_HEIGHT / 2 + baselineOffset
                // 第二次向下偏移
                y2 = y + TEXT_MIN_HEIGHT / 2 + baselineOffset
            }
            IntervalEnum.RIGHT -> {
                mTextPaint.textAlign = Paint.Align.RIGHT
                // 第一次向上偏移
                y1 = y - TEXT_MIN_HEIGHT / 2 + baselineOffset
                // 第二次向下偏移
                y2 = y + TEXT_MIN_HEIGHT / 2 + baselineOffset
            }
            IntervalEnum.MIDDLE_TOP -> {
                mTextPaint.textAlign = Paint.Align.CENTER
                // 第一次向上偏移半个字体高度
                y1 = y - TEXT_MIN_HEIGHT / 2 + baselineOffset
                // 第二次向上偏移一个字体高度
                y2 = y - TEXT_MIN_HEIGHT
            }
            IntervalEnum.MIDDLE_BOTTOM -> {
                mTextPaint.textAlign = Paint.Align.CENTER
                // 第一次向下偏移半个字体高度
                y1 = y + TEXT_MIN_HEIGHT + baselineOffset
                // 第二次向下偏移一个字体高度
                y2 = y + TEXT_MIN_HEIGHT + baselineOffset * 2
            }
        }

        if (lineData.isFitToRender) {
            saveTextCache(text1, text2, x, y1, x, y2)
        }
    }

    private fun saveTextCache(
        text1: String,
        text2: String,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ) {
        TextCache(
            text1 = text1,
            text2 = text2,
            textAlign = mTextPaint.textAlign,
            color1 = colorDuration,
            color2 = colorClassification,
            alpha = mTextPaint.alpha,
            x1 = x1,
            y1 = y1,
            x2 = x2,
            y2 = y2
        ).run {
            mTextCacheMap.put(mDrawingIndex, this)
        }
    }

    private fun drawTextCache(canvas: Canvas) {
        mTextCacheMap.forEach {
            val cache = it.value
            cache.run {
                mTextPaint.textAlign = textAlign

                mTextPaint.color = color1
                mTextPaint.alpha = alpha
                canvas.drawText(text1, x1, y1, mTextPaint)
                mTextPaint.color = color2
                mTextPaint.alpha = alpha
                canvas.drawText(text2, x2, y2, mTextPaint)
            }
        }
    }

    /**
     * 定位坐标区间
     */
    private fun locate(x: Float, y: Float): IntervalEnum {
        return when {
            x < mCenterX -> {
                IntervalEnum.LEFT
            }
            x > mCenterX -> {
                IntervalEnum.RIGHT
            }
            y < mCenterY -> {
                IntervalEnum.MIDDLE_TOP
            }
            else -> {
                IntervalEnum.MIDDLE_BOTTOM
            }
        }
    }

    /**
     * 区间枚举
     */
    private enum class IntervalEnum {
        LEFT, RIGHT, MIDDLE_TOP, MIDDLE_BOTTOM
    }

    /**
     * 准备绘制前
     */
    private fun isPrepared(): Boolean {
        return mArcDataList.isNotEmpty() && mLineDataList.isNotEmpty()
    }

    fun release() {
        mArcCacheList.clear()
        mLineCacheMap.clear()
        mTextCacheMap.clear()
    }
}
