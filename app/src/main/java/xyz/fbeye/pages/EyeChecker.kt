package xyz.fbeye.pages

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import xyz.fbeye.R
import xyz.fbeye.feature.Snapshotor
import xyz.fbeye.network.Client
import kotlinx.android.synthetic.main.eye_checker_layout.*
import org.json.JSONObject
import java.util.*

class EyeChecker : AppCompatActivity() {

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var snapshotor: Snapshotor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.eye_checker_layout)

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FBEye::FaceWakeLock").apply {
                acquire(200*60*1000L /*200 minutes*/)
            }
        }

        snapshotor = Snapshotor(this, face_checker_preview, this as LifecycleOwner)
        snapshotor.startFrontCamera(face_checker_preview)

        waitingForStart()
    }

    //TODO ok사인 물어보고 바꾸기 = 만들고 알려준다고함
    private fun waitingForStart(){
        Thread{
            while (true) {
                var bytes = Client.getInstance().readData()
                var jsonData = JSONObject(String(bytes))
                if (jsonData.getString("data") == "startExam") {
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
        if(Objects.nonNull(wakeLock)){
            wakeLock!!.release()
        }
        snapshotor.destroy()
    }
}