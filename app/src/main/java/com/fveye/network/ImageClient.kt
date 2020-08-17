package com.fveye.network

import android.annotation.SuppressLint
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

class ImageClient {

    private val ip = "15.165.77.217"
    private val port = 3000
    private lateinit var client: SSLSocket


    fun startServer() {
        connectToServer()
    }

    private fun connectToServer() {
        Thread {
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
            client = sslContext.socketFactory.createSocket(ip, port) as SSLSocket
            client.startHandshake()
        }.start()
    }

    fun write(type: String, data: String) {
        if (!client.isConnected) {
            connectToServer()
        }
        Thread {
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
        }.start()
    }

    fun destroy() {
        Thread {
            if (!Objects.isNull(client) && !client.isClosed) {
                client.close()
            }
        }.start()
    }
}