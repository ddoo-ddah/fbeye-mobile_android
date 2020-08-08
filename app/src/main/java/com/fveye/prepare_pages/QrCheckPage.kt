package com.fveye.prepare_pages

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.fveye.R
import com.fveye.network.CoroutineClient
import com.fveye.qr.QrScanner
import com.fveye.qr.Snapshotor
import kotlinx.android.synthetic.main.qr_check_layout.*
import java.io.File

/**
 * 테스트 페이지 순서
 * 1. QrCheckPage = 앱이 실행되자마자 나타날 페이지
 * 2. Check Face = 얼굴 인식
 * 3. Check EyeTracking = 트래킹 체크
 * 4. 시험 시작 전 대기 회면
 * 5. 시험 시작 후 pip 검은화면
 */

class QrCheckPage : AppCompatActivity() {

    private lateinit var client : CoroutineClient
    private lateinit var snapshotor: Snapshotor
    private lateinit var outputDir : File
    private lateinit var qrScanner: QrScanner
    //TODO client를 싱글턴으로 만들어 버릴까

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.qr_check_layout)

        outputDir = getOutputDirectory()

        qrScanner = QrScanner(this, outputDir)
        snapshotor = Snapshotor(this, qr_check_preview, this, outputDir).apply {
            startCamera()
        }
        client = CoroutineClient().apply {
            startClient()
        }

        //찍어서 보낸 후 반응을 받은 다음이 문제네
        qr_check_snapshot_button.setOnClickListener {
            snapshotor.takePhoto()
            val result = qrScanner.detect()
            //Send result to Server
        }
//        qr_check_snapshot_button.visibility = View.INVISIBLE
//        qr_check_snapshot_button.isClickable = false
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }
}