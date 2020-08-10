package com.fveye

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fveye.network.CoroutineClient
import com.fveye.prepare_pages.FaceChecker
import com.fveye.prepare_pages.QrChecker
import com.fveye.feature.QrScanner
import com.fveye.feature.Snapshotor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

//TODO 다음주 할 일
/* *
 * 핸드폰은 조작을 하면 안됨 -- pc도 조작해야되는데 유저가 너무 힘듬
 * 다음화면으로 넘어가는거 자동화 처리
 * 전부 자동화로 넘어가야됨
 * 버튼이 있으면 안됨
 * 코루틴 안되면 그냥 thread
 * 스샷을 주기적으로 찍으면서 완료가 되면 다음 화면으로 자동으로 넘어가게끔 --> 동영상을 찍으면서 확인하는 거랑 비슷함
 * 가격 확인
 * 카메라 안정성 확보
 * 시험 시작 후 pip 검은화면 -- google 내부적 pip 쓰면 안됨 너무 작게나옴 youtube pip 사이즈 나옴 -- 확인 해보기 like 블루 라이트 필터
 * ------------------------ 1순위 자동화 ------------------------
 * ------------------------ 2순위 pip --------------------------
*/

/**
 * EyeTracking 은 벡터 두개로 넘어 올거임
 */

/**
 * 우선 계속 스크린샷 찍어보자 -- 완료
 * 주기적으로 qr 검사를 해보자 -- 완료
 * 자동으로 화면이 넘어가게끔 만들어 보자
 *      a. 일단 화면을 넣어두자
 */

class MainActivity : AppCompatActivity() {

    companion object {
        private val PERMISSIONS = arrayOf(android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_NETWORK_STATE,
                android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private const val REQUEST_CODE = 1
    }

    private lateinit var snapshotor: Snapshotor
    private lateinit var outputDir: File
    private lateinit var backGroundThread: HandlerThread
    private lateinit var backGroundHandler: Handler
    private lateinit var qrScanner : QrScanner

    private var classSet = mutableMapOf(Pair("AUT", QrChecker::class.java), Pair("ho", FaceChecker::class.java))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        CoroutineClient.getInstance().setMoveNext(this::moveNext)

        outputDir = getOutputDirectory()

        snapshotor = Snapshotor(this, preview, this, outputDir)

        qrScanner = QrScanner(this, outputDir)

        if (checkPermissionIsGranted()) {
            CoroutineClient.getInstance().startClient()
            snapshotor.startCamera()
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE)
        }

        send_button.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch { CoroutineClient.getInstance().write(user_input_editText.text.toString()) }
        }

        snapshot_btn.setOnClickListener {
//            snapshotWithDelay()
//            detectWithDelay()
            snapshotor.takePhoto()
            qrScanner.detect()
        }
    }

    private fun moveNext(nextClass : String){
        val intent = Intent(this, classSet.get(nextClass))
        startActivity(intent)
    }

    private fun snapshot() {
        snapshotor.takePhoto()
        snapshotWithDelay()
    }

    private fun detect(){
        qrScanner.detect()
        detectWithDelay()
    }

    private fun detectWithDelay(){
        backGroundHandler.postDelayed(this::detect, 10000)
    }

    private fun snapshotWithDelay() {
        backGroundHandler.postDelayed(this::snapshot, 10000)
    }

    private fun startBackgroundHandler() {
        backGroundThread = HandlerThread("Back")
        backGroundThread.start()
        backGroundHandler = Handler(backGroundThread.looper)
    }

    private fun checkPermissionIsGranted() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE) {
            if (checkPermissionIsGranted()) {
                CoroutineClient.getInstance().startClient()
                snapshotor.startCamera()
            } else {
                val alertBuilder = AlertDialog.Builder(this)
                alertBuilder.apply {
                    setTitle("Permissions are rejected")
                    setMessage("Please Allow Permissions")
                    setPositiveButton("Yes") { _, _ ->
                        ActivityCompat.requestPermissions(this@MainActivity, PERMISSIONS, REQUEST_CODE)
                    }
                }.create().apply {
                    setCanceledOnTouchOutside(false)
                }.run { show() }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        startBackgroundHandler()
    }

    override fun onDestroy() {
        super.onDestroy()
        CoroutineClient.getInstance().disconnect()
        val photoFile = File(
                outputDir,
                "qrPhoto"+".jpg")
        if(photoFile.exists()){
            photoFile.delete()
        }
    }

}