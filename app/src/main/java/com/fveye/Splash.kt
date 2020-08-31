package com.fveye

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fbeye.EyeGazeFinder
import com.fveye.pages.QrChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Splash : AppCompatActivity() {

    companion object {
        private val PERMISSIONS = arrayOf(android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_NETWORK_STATE,
                android.Manifest.permission.CAMERA, android.Manifest.permission.WAKE_LOCK)
        private const val REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkPermissionIsGranted()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE)
        }
        else{
            //모델 먼저 불러오기 시간이 좀 걸림
            EyeGazeFinder.instance.init(assets.openFd("model.tflite"))
            start()
        }
    }

    private fun start(){
        val intent = Intent(this, QrChecker::class.java)
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
            finish()
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
                        ActivityCompat.requestPermissions(this@Splash, PERMISSIONS, REQUEST_CODE)
                    }
                }.create().apply {
                    setCanceledOnTouchOutside(false)
                }.run { show() }
            }
            else{
                start()
            }
        }
    }
}