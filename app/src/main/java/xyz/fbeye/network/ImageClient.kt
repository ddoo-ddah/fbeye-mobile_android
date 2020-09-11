package xyz.fbeye.network

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONObject
import xyz.fbeye.feature.EyeGazeFinder
import xyz.fbeye.pages.ExamPage
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.*
import java.util.concurrent.Executors

class ImageClient {

    private var client: Socket? = null
    private val executor = Executors.newFixedThreadPool(3)

    fun startClient() {
        executor.submit {
            val option = IO.Options()
            option.forceNew = false
            option.transports = Array(1) { WebSocket.NAME }
            client = IO.socket("https://fbeye.xyz", option)
            client!!.on(Socket.EVENT_CONNECT) {
                client!!.emit("mobile-welcome", ExamPage.qrData!!.get("userCode"))
            }
            read()
            client!!.connect()
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

            val byteArrayOutputStream = ByteArrayOutputStream()

            data!!.compress(Bitmap.CompressFormat.PNG, 95, byteArrayOutputStream)

            val base64Data = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)

            client!!.emit("eye", base64Data)

            byteArrayOutputStream.close()
            data.recycle()
        }
    }

    fun read() {
        client!!.on("request-data") { checkForStarting(it) }
        client!!.on("stop-data") { EyeGazeFinder.instance.requestBitmap.set(false) }
        client!!.on("mobile-disconnect") { EyeGazeFinder.instance.requestBitmap.set(false) }
    }

    private fun checkForStarting(it: Array<Any?>) {
        val jsonData = JSONObject(it[0].toString())
        if (jsonData.get("type") == "RES" && Objects.nonNull(Client.getInstance().userCode) && jsonData.get("userCode") == Client.getInstance().userCode) {
            EyeGazeFinder.instance.requestBitmap.set(true)
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