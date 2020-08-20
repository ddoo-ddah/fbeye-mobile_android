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

        exam_page_finishTextView.visibility = View.INVISIBLE

        hideSystemUI()
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
    }

    //TODO while , if 순서가 바뀔 가능성이 있음 - 자바 최적화 문제 - isTesting이 변경 안될 수 있음
    // * 고쳐야됨
    private fun checkNowTesting(){
        Thread{
            while(isTesting){
                if(CoroutineClient.getInstance().getAnswer() == "ok"){
                    isTesting = false
                    exam_page_finishTextView.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}