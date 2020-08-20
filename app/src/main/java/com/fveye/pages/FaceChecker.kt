package com.fveye.pages

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fveye.R
import com.fveye.network.CoroutineClient

class FaceChecker : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.face_checker_layout)

        waitingForStart()
    }

    private fun waitingForStart(){
        Thread{
            while(true){
                if (CoroutineClient.getInstance().getAnswer() == "ok"){
                    break
                }
            }
            val intent = Intent(this, ExamPage::class.java)
            startActivity(intent)
        }.start()
    }
}