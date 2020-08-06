package com.fveye

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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

    fun startClient(){

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
            }
        }
    }

    fun read(){

    }

    fun send(data : String){
        
    }

    fun disconnect(){
        send("bye")
        client.close()
    }
}