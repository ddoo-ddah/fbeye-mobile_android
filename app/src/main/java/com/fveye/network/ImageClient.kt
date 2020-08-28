package com.fveye.network

import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import io.socket.client.IO
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.Executors

class ImageClient {

    private var client: io.socket.client.Socket? = null
    private val executor = Executors.newFixedThreadPool(3)


    fun startClient(){
        executor.submit{
            client = IO.socket("http://fbeye.sysbot32.com:3000")
            client!!.connect()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun write(data: Bitmap?){
        if(Objects.isNull(data)){
            return
        }
        if(Objects.isNull(client)){
            startClient()
            return
        }
        executor.submit {
            val resizedBitmap = Bitmap.createScaledBitmap(data!!, data.width/4, data.height/4, false)
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

            val base64Data = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
            client!!.emit("EYE", base64Data)
        }
    }

    fun destroy(){
        if (!Objects.isNull(client)){
            Thread{
                client!!.disconnect()
                client!!.close()
            }.start()
        }
        executor.shutdownNow()
    }

}