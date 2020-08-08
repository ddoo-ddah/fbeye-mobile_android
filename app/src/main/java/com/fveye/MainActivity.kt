package com.fveye

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fveye.network.CoroutineClient
import com.fveye.qr.QrScanner
import com.fveye.qr.Snapshotor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : AppCompatActivity() {

    companion object {
        private val PERMISSIONS = arrayOf(android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_NETWORK_STATE,
                android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private const val REQUEST_CODE = 1
    }

    private lateinit var client: CoroutineClient
    private lateinit var snapshotor: Snapshotor
    private lateinit var outputDir : File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        outputDir = getOutputDirectory()

        snapshotor = Snapshotor(this, preview, this, outputDir)
        client = CoroutineClient(server_message_display_textView)
        var qrScanner = QrScanner(this, outputDir)

        if (checkPermissionIsGranted()) {
            client.startClient()
            snapshotor.startCamera()
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE)
        }

        send_button.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch { client.write(user_input_editText.text.toString()) }
        }
        snapshot_btn.setOnClickListener {
            Thread {
                snapshotor.takePhoto()
                qrScanner.detect()
            }.start()
        }
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
                client.startClient()
                snapshotor.startCamera()
            } else {
                val alertBuilder = AlertDialog.Builder(this)
                alertBuilder.apply {
                    setTitle("Permissions are rejected")
                    setMessage("Please Allow Permissions")
                    setPositiveButton("Yes"){
                        _,_ ->  ActivityCompat.requestPermissions(this@MainActivity, PERMISSIONS, REQUEST_CODE)
                    }
                }.create().apply {
                    setCanceledOnTouchOutside(false)
                }.run { show() }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.disconnect()
        val photoFile = File(
                outputDir,
                "qrPhoto"+".jpg")
        if(photoFile.exists()){
            photoFile.delete()
        }
    }

}