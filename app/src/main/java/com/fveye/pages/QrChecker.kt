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
import com.fveye.R
import com.fveye.feature.Snapshotor
import com.fveye.network.Client
import kotlinx.android.synthetic.main.qr_check_layout.*
import org.json.JSONObject
import java.util.*

/**
 * 테스트 페이지 순서
 * 1. QrCheckPage = 앱이 실행되자마자 나타날 페이지
 * 2. Check Face = 얼굴 인식
 * 3. Check EyeTracking = 트래킹 체크
 * 4. 시험 시작 전 대기 회면
 * 5. 시험 시작 후 pip 검은화면
 */

@RequiresApi(Build.VERSION_CODES.R)
class QrChecker : AppCompatActivity() {

    private lateinit var snapshotor: Snapshotor
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.qr_check_layout)
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                acquire()
            }
        }
        Client.getInstance().startClient()
        sendQrData()
        checkOk()
    }

    private fun sendQrData() {
        val display: Display? = this.display
        val point = Point()
        display!!.getRealSize(point)
        snapshotor = Snapshotor(this, qr_check_preview, this as LifecycleOwner, point)
        snapshotor.startCameraWithAnalysis()
    }

    private fun checkOk() {
        Thread {
            while (true) {
                var bytes = Client.getInstance().readData()
                var jsonData = JSONObject(String(bytes))
                if (jsonData.getString("data") == "ok") {
                    break
                }
            }
            val intent = Intent(this, FaceChecker::class.java)
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