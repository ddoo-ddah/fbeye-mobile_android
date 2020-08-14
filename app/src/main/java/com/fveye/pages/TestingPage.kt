package com.fveye.pages

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fveye.R
import kotlinx.android.synthetic.main.testing_page_layout.*

class TestingPage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.testing_page_layout)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                Toast.makeText(this, "Do not touch screen ", Toast.LENGTH_SHORT).show()
                hideSystemUI()
            }
        }

        testing_page_testButton.apply {
            setOnClickListener {
                testing_page_background.setBackgroundColor(Color.parseColor("#ff110000"))
                isClickable = false
                visibility = View.INVISIBLE
                hideSystemUI()
            }
        }
    }

    //참조 https://developer.android.com/training/system-ui/immersive

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}