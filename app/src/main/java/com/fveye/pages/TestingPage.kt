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
                hideSystemUI()
            }
        }
    }

    //참조 https://developer.android.com/training/system-ui/immersive
    //ui변경 있을 시는 콜백 구현하면됨 이걸로 화면 변경 감지하면 될듯 함

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}