package com.akashi.customviews.pie.data

import com.akashi.customviews.R
import com.akashi.customviews.getOriginColor

/**
 * Created by Akashi on 2020/11/26.
 */

/**
 * 九种饼图颜色
 */
val colors = arrayOf(
    R.color.blue_0088f0,
    R.color.green_14c5d0,
    R.color.green_50e3c2,
    R.color.marigold,
    R.color.orange_fb9e45,
    R.color.red_fd7591,
    R.color.purple_e968f8,
    R.color.purple_b264ed,
    R.color.black_c7cfd6
)

/**
 * 时长文字颜色
 */
val colorDuration = getOriginColor(R.color.black_333333)

/**
 * 课程分类文字颜色
 */
val colorClassification = getOriginColor(R.color.gray_999999)