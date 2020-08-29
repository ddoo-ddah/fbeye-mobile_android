package com.fveye.pages

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.fveye.R
import com.fveye.feature.Snapshotor
import com.fveye.network.Client
import com.fveye.network.ImageClient
import kotlinx.android.synthetic.main.testing_page_layout.*
import org.json.JSONObject
import java.util.*
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.R)
class ExamPage : AppCompatActivity() {

    private var isTesting = true
    private val imageClient = ImageClient()
    private lateinit var snapshotor: Snapshotor
    private val executor = Executors.newFixedThreadPool(2)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.testing_page_layout)

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FBEye::ExamWakeLock").apply {
                acquire()
            }
        }

        val display: Display? = this.display
        val point = Point()
        display!!.getRealSize(point)
        snapshotor = Snapshotor(this as Context, exam_page_preivew, this as LifecycleOwner, point)
        snapshotor.startCameraWithAnalysis()

        exam_page_finishTextView.visibility = View.INVISIBLE

        hideSystemUI()
        executor.submit(this::checkNowTesting)

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                if (isTesting) {
                    Toast.makeText(this, "Do not touch screen ", Toast.LENGTH_SHORT).show()
                    hideSystemUI()
                } else {
                    Toast.makeText(this, "Exam is finish", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        imageClient.startClient()
        executor.submit(this::sendImage)
    }

    //TODO while , if 순서가 바뀔 가능성이 있음 - 자바 최적화 문제 - isTesting이 변경 안될 수 있음
    // * 고쳐야됨
    private fun checkNowTesting() {
        while (isTesting) {
            var bytes = Client.getInstance().readData()
            var jsonData = JSONObject(String(bytes))
            if (jsonData.getString("data") == "ok") {
                isTesting = false
                imageClient.destroy()
                runOnUiThread {
                    exam_page_finishTextView.visibility = View.VISIBLE
                }
            }

        }
    }

    private var bitmap : Bitmap? = null

    private fun sendImage() {
        while (isTesting) {
            bitmap = exam_page_preivew.bitmap
            imageClient.write(bitmap)
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!Objects.isNull(wakeLock)){
            wakeLock!!.release()
        }
        snapshotor.destroy()
        imageClient.destroy()
        executor.shutdownNow()
        Client.getInstance().disconnect()
    }
}