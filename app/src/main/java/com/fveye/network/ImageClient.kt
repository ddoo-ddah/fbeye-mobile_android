package com.fveye.network

import android.graphics.*
import android.media.Image
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageProxy
import io.socket.client.IO
import io.socket.emitter.Emitter
import java.io.ByteArrayOutputStream
import java.util.*

class ImageClient {

    private lateinit var client: io.socket.client.Socket

    fun startClient(){
        Thread{
            client = IO.socket("http://fbeye.sysbot32.com:3000")
            client.connect()
        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun write(data: Bitmap?){
        if(Objects.isNull(data)){
            return
        }
        Thread{
            val resizedBitmap = Bitmap.createScaledBitmap(data!!, data.width/4, data.height/4, false)
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

            val base64Data = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
            client.emit("EYE", base64Data)
            client.on("EYE", listner)
        }.start()
    }

    private val listner : Emitter.Listener = Emitter.Listener {
        Log.d("Receive", "Received!")
    }

    fun destroy(){
        Thread{
            client.disconnect()
            client.close()
        }.start()
    }

}