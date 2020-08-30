package com.fveye.pages

import android.content.Context
import android.content.Intent
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
import java.net.URI
import java.util.*
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.R)
class ExamPage : AppCompatActivity() {

    private var isRunning = true
    private var imageClient : ImageClient? = null
    private lateinit var snapshotor: Snapshotor
    private val executor = Executors.newFixedThreadPool(2)
    private var wakeLock: PowerManager.WakeLock? = null
    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.testing_page_layout)




        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        executor.submit(this::workWhileExam)

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                if (isRunning) {
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
        connectToImageServer()
    }

    private fun connectToImageServer(){
        // get uri from qrData and connect image server
        val intent = Intent()
        val qrString = intent.getStringExtra("QR")
        if (Objects.isNull(qrString)){
            return
        }
        val qrData = JSONObject(qrString)
        qrData.get("")
        val uri = URI("")
        imageClient = ImageClient()
        imageClient!!.startClient(uri)
    }


    private fun workWhileExam() {
        while (isRunning) {
            val bytes = Client.getInstance().readData()
            val jsonData = JSONObject(String(bytes))
            workWithType(jsonData)
        }
    }

    private fun workWithType(json: JSONObject) {
        val data = json.get("data").toString()
        when (json.get("type")) {
            "RES" -> finishExam(data)
            "REQ" -> sendImage()
        }
    }

    private fun finishExam(isFinish : String) {
        if(!Objects.isNull(imageClient)){
            imageClient!!.destroy()
        }
        if(isFinish == "ok"){
            isRunning = false
            runOnUiThread {
                exam_page_finishTextView.visibility = View.VISIBLE
            }
        }
    }

    private fun sendImage() {
        if(Objects.isNull(imageClient)){
            return
        }
        executor.execute{
            bitmap = exam_page_preivew.bitmap
            imageClient!!.write(bitmap)
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
        if (!Objects.isNull(wakeLock)) {
            wakeLock!!.release()
        }
        if(!Objects.isNull(imageClient)){
            imageClient!!.destroy()
        }
        snapshotor.destroy()
        executor.shutdownNow()
        Client.getInstance().disconnect()
    }
}