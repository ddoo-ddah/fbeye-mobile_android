package xyz.fbeye.pages

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
import kotlinx.android.synthetic.main.exam_page_layout.*
import org.json.JSONObject
import xyz.fbeye.R
import xyz.fbeye.feature.EyeGazeFinder
import xyz.fbeye.feature.Snapshotor
import xyz.fbeye.network.Client
import xyz.fbeye.network.ImageClient
import java.net.URI
import java.util.*
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.R)
class ExamPage : AppCompatActivity() {

    private var isRunning = true
    private var imageClient: ImageClient? = null
    private lateinit var snapshotor: Snapshotor
    private val executor = Executors.newFixedThreadPool(3)
    private var wakeLock: PowerManager.WakeLock? = null
    private var bitmap: Bitmap? = null
    private var qrData: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.exam_page_layout)

        keepScreenOn()

        initSnapshotor()

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

    private fun initSnapshotor() {
        val display: Display? = this.display
        val point = Point()
        display!!.getRealSize(point)
        snapshotor = Snapshotor(this as Context, exam_page_preivew, this as LifecycleOwner)
        snapshotor.apply {
            setQrCallback(this@ExamPage::setQrData)
        }.run {
            startCameraWithAnalysis(point)
            startFrontCamera(exam_page_preivew_front)
        }
    }

    private fun keepScreenOn() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FBEye::ExamWakeLock").apply {
                acquire()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        executor.execute(this::connectToImageServer)
    }

    private fun setQrData(data: JSONObject) {
        this.qrData = data
    }

    private fun connectToImageServer() {
        while (isRunning) {
            if (Objects.nonNull(qrData)) {
                break
            }
        }
        // get uri from qrData and connect image server
        imageClient = ImageClient()
        imageClient!!.startClient()
        EyeGazeFinder.instance.setBitmapWriter(imageClient!!::write)
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

    //TODO ok사인 물어보고 바꾸기 = 만들고 알려준다고함
    private fun finishExam(isFinish: String) {
        if (isFinish == "endExam") {
            isRunning = false
            runOnUiThread {
                exam_page_finishTextView.visibility = View.VISIBLE
            }
        }
    }

    private fun sendImage() {
        if (Objects.isNull(imageClient)) {
            return
        }
        EyeGazeFinder.instance.requestBitmap = true
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
        if (Objects.nonNull(wakeLock)) {
            wakeLock!!.release()
        }
        if (Objects.nonNull(imageClient)) {
            imageClient!!.destroy()
        }
        snapshotor.destroy()
        executor.shutdownNow()
        Client.getInstance().disconnect()
    }
}