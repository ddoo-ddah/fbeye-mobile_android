package com.fveye.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.cert.X509Certificate
import java.util.function.Consumer
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

class CoroutineClient private constructor() {

    companion object {
        private var instance: CoroutineClient? = null

        fun getInstance(): CoroutineClient =
                instance ?: synchronized(this) {
                    instance ?: CoroutineClient().also {
                        instance = it
                    }
                }
    }

    private val IP = "192.168.200.144"
    private val PORT = 10101
    private val eyeTrackingIdentifier = "EYE"
    private val qrIdentifier = "AUT"
    private val errorIdentifier = "ERR"
    private val examIdentifier = "TES"
    private val pcResponseIdentifier = "RES"
    private var inputBuffer = ByteArray(20)

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
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    }

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

    fun readData() {
        Thread{
            while(client.isConnected){
                var inputStream = client.inputStream
                identifyMessage(readToBuffer(inputStream))
            }
        }.start()
    }

    @Synchronized fun readToBuffer(inputStream : InputStream) : ByteArray{
        if (!client.isConnected){
            connectToServer()
        }
        var input = ByteArray(20)
        inputStream.apply {
            read(input)
            close()
        }
        return input
    }

    fun write(data: String) {
        if (!client.isConnected) {
            connectToServer()
        }
        runBlocking {
            client.outputStream.apply {
                write(data.toByteArray())
                flush()
                close()
            }
//            identifyMessage(readToBuffer(client.inputStream))
        }
    }

    private fun identifyMessage(bytes: ByteArray) {
        val identifier = ByteArray(3)
        bytes.copyInto(identifier, 0, 0, 3)
        Log.d("identifier", String(identifier))
        when (String(identifier)) {
            pcResponseIdentifier -> doSomeThingWithCase(bytes)
            qrIdentifier -> qrTask(identifier)
            errorIdentifier -> doSomeThingWithCase(bytes)
            examIdentifier -> testFinish(identifier)
        }
    }

    private fun doSomeThingWithCase(bytes: ByteArray) {}

    private fun qrTask(bytes: ByteArray) {
        moveNexPage.accept(String(bytes))
    }

    private var examWord = ByteArray(3)

    private fun testFinish(bytes: ByteArray){
        examWord = bytes.copyOf()
        Log.d("testFinish", String())
    }

    fun getExamWord():String{
        return String()
    }

    fun disconnect() {
        if (client.isConnected && !client.isClosed) {
            runBlocking {
                withContext(Dispatchers.IO) {
                    write("bye")
                    client.close()
                }
            }
        }
    }
}