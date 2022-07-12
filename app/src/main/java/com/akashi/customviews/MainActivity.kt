package com.akashi.customviews

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import com.akashi.customviews.acivity.PieViewActivity
import com.akashi.customviews.acivity.ScalableActivity
import com.akashi.customviews.views.CircleView
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<AppCompatImageButton>(R.id.ibtn_scalable).setOnClickListener {
            Intent(this, ScalableActivity::class.java).apply {
                startActivity(this)
            }
        }
        findViewById<AppCompatButton>(R.id.btn_pie_view).setOnClickListener {
            Intent(this, PieViewActivity::class.java).apply {
                startActivity(this)
            }
        }
    }

}
