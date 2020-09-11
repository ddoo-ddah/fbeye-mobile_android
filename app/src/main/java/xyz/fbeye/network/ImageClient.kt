package xyz.fbeye.network

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import io.socket.client.IO
import io.socket.emitter.Emitter
import org.json.JSONObject
import xyz.fbeye.feature.EyeGazeFinder
import xyz.fbeye.pages.ExamPage
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.*
import java.util.concurrent.Executors
import java.util.function.Supplier

class ImageClient {

    private var client: io.socket.client.Socket? = null
    private val executor = Executors.newFixedThreadPool(3)
    private var uri = URI("https://fbeye.xyz/")

    fun startClient() {
        executor.submit {
            client = IO.socket(uri)
            read()
            client!!.connect()
            client!!.emit("mobile-welcome", ExamPage.qrData!!.get("userCode"))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun write(data: Bitmap?) {
        if (Objects.isNull(data)) {
            return
        }
        if (Objects.isNull(client)) {
            startClient()
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

    fun read() {
        client!!.on("request-data") { checkForStarting(it) }
        client!!.on("stop-data") { EyeGazeFinder.instance.requestBitmap = false }
        client!!.on("mobile-disconnect") { EyeGazeFinder.instance.requestBitmap = false }
    }

    private fun checkForStarting(it :Array<Any?>){
        val jsonData = JSONObject(it[0].toString())
        if (jsonData.get("type") == "RES" && Objects.nonNull(Client.getInstance().userCode) && jsonData.get("userCode") == Client.getInstance().userCode){
            EyeGazeFinder.instance.requestBitmap = true
        }
    }

    fun destroy() {
        if (Objects.nonNull(client)) {
            Thread {
                client!!.disconnect()
                client!!.close()
            }.start()
        }
        executor.shutdownNow()
    }

}