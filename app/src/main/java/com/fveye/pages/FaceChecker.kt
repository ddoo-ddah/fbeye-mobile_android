package com.fveye.pages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.fveye.R
import com.fveye.network.CoroutineClient
import org.json.JSONObject

class FaceChecker : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.face_checker_layout)

        waitingForStart()
    }

    private fun waitingForStart(){
        Thread{
            while (true) {
                var bytes = CoroutineClient.getInstance().readTest()
                var jsonData = JSONObject(String(bytes))
                if (jsonData.getString("data") == "ok") {
                    break
                }
            }
            val intent = Intent(this, ExamPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
            finish()
        }.start()
    }
}