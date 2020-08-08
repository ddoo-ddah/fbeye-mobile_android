package com.fveye.network

import android.widget.TextView
import kotlinx.coroutines.*
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

class CoroutineClient {

    companion object {
        const val IP = "192.168.200.144"
        const val PORT = 10101
        val eyeTrackingIdentifier = "EYE"
        val qrIdentifier = "AUT"
        val errorIdentifier = "ERR"
        val testIdentifier = "TES"
        val pcResponseIdentifier = "RES"
    }

    private lateinit var client: SSLSocket

    private var inputBuffer = ByteArray(20)

    fun startClient() {
        connectToServer()
    }

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
                val sslContext = SSLContext.getInstance("TLSv1.3").apply {
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
        if(!client.isConnected){
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

    private fun identifyMessage(bytes : ByteArray){
        val identifier = ByteArray(3)
        bytes.copyInto(identifier,0,0,2)
        when(String(identifier)){
            pcResponseIdentifier -> doSomeThingWithCase()
            qrIdentifier -> doSomeThingWithCase()
            errorIdentifier -> doSomeThingWithCase()
            testIdentifier -> doSomeThingWithCase()
        }
    }

    private fun doSomeThingWithCase(){}

    fun disconnect() {
        if(client.isConnected && !client.isClosed){
            runBlocking {
                withContext(Dispatchers.IO) {
                    write("bye")
                    client.close()
                }
            }
        }
    }
}