package com.fveye

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fveye.network.CoroutineClient
import com.fveye.qr.Snapshotor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.snapshot_test_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

//TODO QR Scanner 붙이기

class MainActivity : AppCompatActivity() {

    companion object {
        private val PERMISSIONS = arrayOf(android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_NETWORK_STATE,
                android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private const val REQUEST_CODE = 1
    }

    private lateinit var client: CoroutineClient
    private lateinit var outputDirectory: File
    private lateinit var snapshotor: Snapshotor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        snapshotor = Snapshotor(this, viewFinder, this, getOutputDirectory())
        client = CoroutineClient(server_message_display_textView)

        if (checkPermissionIsGranted()) {
            client.startClient()
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE)
        }

        send_button.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch { client.write(user_input_editText.text.toString()) }
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
            } else {
                //TODO Display why user allow this permission and request again
                Toast.makeText(this@MainActivity, "not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.disconnect()
    }

}