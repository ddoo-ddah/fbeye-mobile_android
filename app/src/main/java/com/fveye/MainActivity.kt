package com.fveye

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fveye.databinding.ActivityMainBinding
import com.fveye.network.CoroutineClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//TODO QR Scanner 붙이기

class MainActivity : AppCompatActivity() {

    companion object{
        private val PERMISSIONS = arrayOf(android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_NETWORK_STATE)
        private const val REQUEST_CODE = 1
    }

    private lateinit var binding : ActivityMainBinding
    private lateinit var client : CoroutineClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        client = CoroutineClient(binding)

        if(checkPermissionIsGranted()){
            client.startClient()
        }
        else{
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE)
        }

        binding.sendButton.setOnClickListener{
            CoroutineScope(Dispatchers.IO).launch{client.write(binding.userInputEditText.text.toString())}
        }
    }

    private fun checkPermissionIsGranted() = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
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