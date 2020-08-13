package com.fveye.pages

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import com.fveye.R
import kotlinx.android.synthetic.main.testing_page_layout.*

class TestingPage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.testing_page_layout)

        testing_page_testButton.apply {
            setOnClickListener {
                testing_page_background.setBackgroundColor(Color.parseColor("#ff000000"))
                isClickable = false
                visibility = View.INVISIBLE
            }
        }
    }
}