package com.fveye

import com.fveye.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

class CoroutineClient {

    companion object{
        const val IP = "192.168.200.144"
        const val PORT = 10101
    }

    private lateinit var client : SSLSocket

    private val testBuffer = ByteArray(20)

    fun startClient(){
        connectToServer()
    }

    private fun connectToServer(){
        runBlocking {
            withContext(Dispatchers.IO){
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
                        write("Client Hi")
                        read()
                    }
                }.run { startHandshake() }
            }
        }
    }

    fun read(){
        runBlocking {
            withContext(Dispatchers.IO){
                client.inputStream.apply {
                    read(testBuffer)
                }
            }
        }
    }

    fun testMethod(binding : ActivityMainBinding){
        CoroutineScope(Dispatchers.Main).launch {
            read()
            binding.serverMessageDisplayTextView.text = String(testBuffer)
        }
    }

    fun write(data : String){
        runBlocking {
            withContext(Dispatchers.IO){
                client.outputStream.apply {
                    write(data.toByteArray())
                    flush()
                    close()
                }
            }
        }
    }

    fun disconnect(){
        write("bye")
        runBlocking {
            withContext(Dispatchers.IO){
                client.close()
            }
        }
    }
}