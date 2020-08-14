package com.fveye.network

import android.annotation.SuppressLint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.cert.X509Certificate
import java.util.*
import java.util.function.Consumer
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

/**
{
"type": "res",
"data": "ok"
}

{
"type": "err",
"data": 404
}

{
"type": "btn",
"data": [
{
"x": 100,
"y": 200
},
{
"x": 200,
"y": 100
}
]
}

{
"type": "eye",
"data": "아이트래킹값"
}

EYE1234567890 (아이트래킹)
AUTasdfzxcvqwer (qr 코드)
ERR12345 (에러 코드)
TES1start (테스트)
RESdesktopOk (pc 연동)
 */

class CoroutineClient private constructor() {

    companion object {
        private var instance: CoroutineClient? = null
        val eyeTrackingIdentifier = "EYE"
        val qrIdentifier = "AUT"
        val errorIdentifier = "ERR"
        val examIdentifier = "TES"
        val pcResponseIdentifier = "RES"

        fun getInstance(): CoroutineClient =
                instance ?: synchronized(this) {
                    instance ?: CoroutineClient().also {
                        instance = it
                    }
                }
    }

    private val IP = "192.168.200.144"
    private val PORT = 10101
    private lateinit var client: SSLSocket
    private lateinit var moveNexPage: Consumer<String>

    fun startClient() {
        connectToServer()
    }

    //Thread도 가능
    private fun connectToServer() {
        runBlocking {
            withContext(Dispatchers.IO) {
                val trustManager = arrayOf(object : X509TrustManager {
                    @SuppressLint("TrustAllX509TrustManager")
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    }

                    @SuppressLint("TrustAllX509TrustManager")
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }
                })
                val sslContext = SSLContext.getInstance("TLSv1.2").apply {
                    init(null, trustManager, null)
                }
                client = sslContext.socketFactory.createSocket(IP, PORT) as SSLSocket
                client.apply {
                    addHandshakeCompletedListener {
//                        inputBuffer = ByteArray(20)
//                        client.inputStream.read(inputBuffer)
                        readData()
                    }
                }.run { startHandshake() }
            }
        }
    }

    fun setMoveNext(run: Consumer<String>) {
        this.moveNexPage = run
    }

    fun write(type: String, data: String) {
        if (!client.isConnected) {
            connectToServer()
        }
        runBlocking {
            val jsonData = JSONObject()
            jsonData.apply {
                put("type", type)
                put("data", data)
            }
            client.outputStream.apply {
                write(jsonData.toString().toByteArray(StandardCharsets.UTF_8))
                flush()
                close()
            }
        }
    }

    fun readData() {
        Thread {
            while (client.isConnected) {
                var inputStream = client.inputStream
                identifyMessage(readToBuffer(inputStream))
            }
        }.start()
    }

    @Synchronized
    fun readToBuffer(inputStream: InputStream): ByteArray {
        if (!client.isConnected) {
            connectToServer()
        }
        var input = ByteArray(40)
        inputStream.apply {
            read(input)
            close()
        }
        return input
    }

    private fun identifyMessage(bytes: ByteArray) {
        if (Objects.isNull(bytes)) {
            return
        }
        val jsonData = JSONObject(String(bytes))
        when (val identifier = jsonData.getString("type")) {
            pcResponseIdentifier -> doSomeThingWithCase()
            qrIdentifier -> qrTask(identifier)
            errorIdentifier -> doSomeThingWithCase()
            examIdentifier -> testFinish(identifier)
        }
    }

    private fun doSomeThingWithCase() {}

    private fun qrTask(data: String) {
        moveNexPage.accept(data)
    }

    private var examWord: String? = null

    private fun testFinish(data: String) {
        examWord = data
    }

    fun getExamWord(): String {
        return examWord!!
    }

    fun disconnect() {
        if (client.isConnected && !client.isClosed) {
            runBlocking {
                withContext(Dispatchers.IO) {
                    client.close()
                }
            }
        }
    }
}