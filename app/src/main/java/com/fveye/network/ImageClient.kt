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

class ImageClient {

    private lateinit var client: io.socket.client.Socket

    fun startClient(){
        Thread{
            client = IO.socket("http://fbeye.sysbot32.com:3000")
            client.connect()
        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun write(data: Image){
        Thread{

            val yBuffer = data.planes[0].buffer // Y
            val uBuffer = data.planes[1].buffer // U
            val vBuffer = data.planes[2].buffer // V

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val bytes = ByteArray(ySize + uSize + vSize)

            yBuffer.get(bytes, 0, ySize)
            uBuffer.get(bytes, 0, uSize)
            vBuffer.get(bytes, 0, vSize)

            val yuvImage = YuvImage(bytes, ImageFormat.NV21, data.width, data.height, null)

            var baos : ByteArrayOutputStream = ByteArrayOutputStream()

            yuvImage.compressToJpeg(Rect(0,0,yuvImage.width, yuvImage.height), 75, baos)

            var imageBytes = baos.toByteArray()

            val bitmapOptions = BitmapFactory.Options()
            bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, bitmapOptions)

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

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