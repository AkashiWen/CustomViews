package com.akashi.customviews

import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import com.akashi.customviews.views.CircleView
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cv = findViewById<CircleView>(R.id.cv)

        var width = 10.dp.toInt()

        thread {
            while (width < 600) {
                runOnUiThread {
                    cv.layoutParams = cv.layoutParams.apply {
                        this.height = width
                        this.width = width
                    }
                }
                width++
                SystemClock.sleep(100)
            }
        }
    }

}
