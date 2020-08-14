package com.fveye.pages

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fveye.R
import com.fveye.network.CoroutineClient
import kotlinx.android.synthetic.main.testing_page_layout.*

class ExamPage : AppCompatActivity() {

    private var isTesting = true

    /**
     * 1. 시험 시작의 카운팅이 끝난 뒤 시험 시작 코드를 서버로부터 받고 화면 제어 시작 == TestingPage 클래스 시작
     * 2. 지금은 임의코드 TES1이 오면 종료
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.testing_page_layout)

        checkNowTesting()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                if (isTesting){
                    Toast.makeText(this, "Do not touch screen ", Toast.LENGTH_SHORT).show()
                    hideSystemUI()
                }
                else{
                    Toast.makeText(this, "Exam is finish", Toast.LENGTH_SHORT).show()
                }
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

    private fun checkNowTesting(){
        Thread{
            while(isTesting){
                if(CoroutineClient.getInstance().getExamWord() == "TES"){
                    isTesting = false
                }
            }
        }.start()
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