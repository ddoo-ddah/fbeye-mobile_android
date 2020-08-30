package com.fveye.pages

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import androidx.appcompat.app.AppCompatActivity
import com.fveye.R
import com.fveye.network.Client
import org.json.JSONObject
import java.util.*

class FaceChecker : AppCompatActivity() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.face_checker_layout)

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FBEye::FaceWakeLock").apply {
                acquire()
            }
        }

        waitingForStart()
    }

    private fun waitingForStart(){
        Thread{
            while (true) {
                var bytes = Client.getInstance().readData()
                var jsonData = JSONObject(String(bytes))
                if (jsonData.getString("data") == "ok") {
                    break
                }
            }
            val intent = Intent(this, ExamPage::class.java)
            intent.putExtra("QR", intent.getStringExtra("QR"))
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
            finish()
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!Objects.isNull(wakeLock)){
            wakeLock!!.release()
        }
    }
}