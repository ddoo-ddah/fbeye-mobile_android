package xyz.fbeye.pages

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
import xyz.fbeye.R
import xyz.fbeye.feature.Snapshotor
import xyz.fbeye.network.Client
import kotlinx.android.synthetic.main.qr_check_layout.*
import org.json.JSONObject
import xyz.fbeye.feature.EyeGazeFinder
import java.util.*

@RequiresApi(Build.VERSION_CODES.R)
class QrChecker : AppCompatActivity() {

    private lateinit var snapshotor: Snapshotor
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.qr_check_layout)

        EyeGazeFinder.instance.setEyeDataWriter(Client.getInstance()::writeEyeData)
        //가끔 서버에 접속을 2번 혹은 그 이상 접속 해서 null check 함
        if(Objects.isNull(savedInstanceState)){
            Client.getInstance().startClient()
        }

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FBEye::QrWakeLock").apply {
                acquire()
            }
        }

        sendQrData()
        checkOk()
    }

    private fun sendQrData() {
        val display: Display? = this.display
        val point = Point()
        display!!.getRealSize(point)
        snapshotor = Snapshotor(this, qr_check_preview, this as LifecycleOwner)
        snapshotor.startCameraWithAnalysis(point)
    }

    private fun checkOk() {
        Thread {
            while (true) {
                var bytes = Client.getInstance().readData()
                var jsonData = JSONObject(String(bytes))
                if (jsonData.getString("data") == "desktopOk") {
                    break
                }
            }
            val intent = Intent(this, EyeChecker::class.java)
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