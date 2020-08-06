package com.fveye.network

import com.fveye.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

class CoroutineClient(private val binding: ActivityMainBinding) {

    companion object {
        const val IP = "192.168.200.144"
        const val PORT = 10101
    }

    private lateinit var client: SSLSocket

    private var testBuffer = ByteArray(20)

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
//                        readData()
                        testBuffer = ByteArray(20)
                        client.inputStream.read(testBuffer)
                        CoroutineScope(Dispatchers.Main).launch {binding.serverMessageDisplayTextView.text = String(testBuffer)}
                    }
                }.run { startHandshake() }
            }
        }
    }

    fun readData() {
        runBlocking {
            withContext(Dispatchers.IO) {
                client.inputStream.apply {
                    testBuffer = ByteArray(20)
                    read(testBuffer)
                }
            }
            changeDisplayedWord()
        }
    }

    private suspend fun changeDisplayedWord() {
        withContext(Dispatchers.Main) {
            binding.serverMessageDisplayTextView.text = String(testBuffer)
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
                testBuffer = ByteArray(20)
                read(testBuffer)
            }
            changeDisplayedWord()
        }
    }

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