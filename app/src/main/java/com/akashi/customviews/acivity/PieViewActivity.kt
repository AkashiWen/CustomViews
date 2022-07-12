package com.akashi.customviews.acivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.akashi.customviews.R
import com.akashi.customviews.pie.PieView
import com.akashi.customviews.pie.data.PieViewData
import com.akashi.customviews.pie.data.colors

class PieViewActivity : AppCompatActivity() {

    private lateinit var pieView: PieView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_view)

        pieView = findViewById(R.id.pieView)

        listOf(
            PieViewData(
                duration = "3h",
                courseName = "数学",
                percent = 20f,
                color = colors[0]
            ),
            PieViewData(
                duration = "12h",
                courseName = "英语",
                percent = 15f,
                color = colors[1]
            ),
            PieViewData(
                duration = "6h",
                courseName = "计算机",
                percent = 13f,
                color = colors[2]
            ),
            PieViewData(
                duration = "12h",
                courseName = "日语",
                percent = 12f,
                color = colors[3]
            ),
            PieViewData(
                duration = "3h",
                courseName = "人工智能",
                percent = 10f,
                color = colors[4]
            ),
            PieViewData(
                duration = "3h",
                courseName = "人工智障",
                percent = 10f,
                color = colors[5]
            ),
            PieViewData(
                duration = "1h",
                courseName = "解刨产品",
                percent = 8f,
                color = colors[6]
            ),
            PieViewData(
                duration = "1h",
                courseName = "分解UI",
                percent = 7f,
                color = colors[7]
            ),
            PieViewData(
                duration = "1h",
                courseName = "其他",
                percent = 5f,
                color = colors[8]
            )
        ).let {
            pieView.setData(it)
        }

        pieView.startAnimator()
    }

    override fun onPause() {
        super.onPause()
        pieView.pauseAnimator()
    }

    override fun onRestart() {
        super.onRestart()
        pieView.resumeAnimator()
    }

}