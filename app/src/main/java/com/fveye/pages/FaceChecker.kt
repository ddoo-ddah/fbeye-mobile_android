package com.fveye.pages

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.Display
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.fbeye.EyeGazeFinder
import com.fveye.R
import com.fveye.feature.Snapshotor
import com.fveye.network.Client
import kotlinx.android.synthetic.main.face_checker_layout.*
import kotlinx.android.synthetic.main.qr_check_layout.*
import org.json.JSONObject
import java.util.*

class FaceChecker : AppCompatActivity() {

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var snapshotor: Snapshotor

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.face_checker_layout)

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FBEye::FaceWakeLock").apply {
                acquire()
            }
        }

        snapshotor = Snapshotor(this, qr_check_preview, this as LifecycleOwner)
        snapshotor.startFrontCamera(face_checker_preview)

        EyeGazeFinder.instance.setEyeDataWriter(Client.getInstance()::writeEyeDataForTest)

        waitingForStart()
    }

    //TODO ok사인 물어보고 바꾸기 = 만들고 알려준다고함
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
        snapshotor.destroy()
    }
}