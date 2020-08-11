package com.fveye.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
    private val testIdentifier = "TES"
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
                        inputBuffer = ByteArray(20)
                        client.inputStream.read(inputBuffer)
                    }
                }.run { startHandshake() }
            }
        }
    }

    fun setMoveNext(run: Consumer<String>) {
        this.moveNexPage = run
    }

    fun readData() {
        runBlocking {
            withContext(Dispatchers.IO) {
                client.inputStream.apply {
                    inputBuffer = ByteArray(20)
                    read(inputBuffer)
                    close()
                }
            }
        }
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
            client.inputStream.apply {
                inputBuffer = ByteArray(20)
                read(inputBuffer)
                identifyMessage(inputBuffer)
                close()
            }
        }
    }

    private fun identifyMessage(bytes: ByteArray) {
        val identifier = ByteArray(3)
        bytes.copyInto(identifier, 0, 0, 3)
        when (String(identifier)) {
            pcResponseIdentifier -> doSomeThingWithCase(bytes)
            qrIdentifier -> qrTask(identifier)
            errorIdentifier -> doSomeThingWithCase(bytes)
            testIdentifier -> doSomeThingWithCase(bytes)
        }
    }

    private fun doSomeThingWithCase(bytes: ByteArray) {}

    private fun qrTask(bytes: ByteArray) {
        moveNexPage.accept(String(bytes))
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