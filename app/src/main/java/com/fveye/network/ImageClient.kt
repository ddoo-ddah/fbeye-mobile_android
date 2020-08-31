package com.fveye.network

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import io.socket.client.IO
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.*
import java.util.concurrent.Executors

class ImageClient {

    private var client: io.socket.client.Socket? = null
    private val executor = Executors.newFixedThreadPool(3)
    private var uri: URI? = null

    fun startClient(uri: URI) {
        this.uri = uri
        executor.submit {
//            client = IO.socket("http://fbeye.sysbot32.com:3000")
            client = IO.socket(uri)
            client!!.connect()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun write(data: Bitmap?) {
        if (Objects.isNull(data)) {
            return
        }
        if (Objects.isNull(client) && !Objects.isNull(uri)) {
            startClient(uri!!)
            data!!.recycle()
            return
        }
        executor.execute {

            val matrix = Matrix()
            matrix.postScale(720.toFloat() / data!!.width, 720.toFloat() / data.height)

            val resizedBitmap = Bitmap.createBitmap(data, 0, 0, data.width, data.height, matrix, false)
            val byteArrayOutputStream = ByteArrayOutputStream()

            resizedBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

            val base64Data = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)

            client!!.emit("eye", base64Data)

            resizedBitmap.recycle()
            byteArrayOutputStream.close()
            data.recycle()
        }
    }

    fun destroy() {
        if (!Objects.isNull(client)) {
            Thread {
                client!!.disconnect()
                client!!.close()
            }.start()
        }
        executor.shutdownNow()
    }

}