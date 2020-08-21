package com.fveye

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fveye.network.CoroutineClient
import com.fveye.pages.QrChecker
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*
import kotlin.system.exitProcess

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

//TODO
// cameraX rotation 변경
/* enum 쓰지 말 것 - 용량 늘어남
 * constraint layout guide line 사용, 비율 맘대로 조절 가능 
 * oriantaion 변경
 * 실행 주기랑 실행 시간 실행 횟수 체크
 * ---------------사진을 여러장 찍어서 전송 --> data queue 넣고 보내고  -------------------
 * ---------------1순위 실행 주기랑 실행 시간 실행 횟수 체크 ------------------- --> 최대한 빨리 //오늘 안으로 보내기 - 어느부분이 어느정도 걸린다 가능한 상세히
 * ---------------2순위 완성도 높이기 (QRChecker, ExamPage, 화면 넘기기, 에러 핸들링) ------------------
 * ---------------3순위 서버연동 ----------------- 입출력 하는놈, 중간에서 연계해주는 놈 , interface 만들어서 상속받으면 데이터 받을 수 있게끔 (eventQueue 참고)
 * ---------------참고만 하라고  eyeTracker 붙이기 -------------------- (frontCamera 에 붙여야 됨, 가능한 한 원본 이미지 넘기기 - viewfinder 하나 깔고 안보이게 해서 넘기면됨)
*/

class MainActivity : AppCompatActivity() {

    companion object {
        private val PERMISSIONS = arrayOf(android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_NETWORK_STATE,
                android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private const val REQUEST_CODE = 1
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        CoroutineClient.getInstance()

        if (!checkPermissionIsGranted()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE)
        }

        main_access_button.setOnClickListener {
            CoroutineClient.getInstance().startClient()
            val intent = Intent(this, QrChecker::class.java)
            startActivity(intent)
        }

        main_exit_button.setOnClickListener {
            exitProcess(0)
        }
    }

    private fun checkPermissionIsGranted() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE) {
            if (!checkPermissionIsGranted()) {
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

    override fun onDestroy() {
        super.onDestroy()
        CoroutineClient.getInstance().disconnect()
    }
}