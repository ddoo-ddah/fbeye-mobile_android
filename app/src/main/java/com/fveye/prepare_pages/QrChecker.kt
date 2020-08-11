package com.fveye.prepare_pages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.fveye.R
import com.fveye.feature.QrScanner
import com.fveye.feature.Snapshotor
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


class QrChecker : AppCompatActivity() {

    private lateinit var snapshotor: Snapshotor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.qr_check_layout)

        snapshotor = Snapshotor(this, qr_check_preview, this as LifecycleOwner).apply {
            startCamera()
        }
    }
}