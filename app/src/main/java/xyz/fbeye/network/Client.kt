package xyz.fbeye.network

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

class Client private constructor() {

    companion object {
        private var instance: Client? = null

        fun getInstance(): Client =
                instance ?: synchronized(this) {
                    instance ?: Client().also {
                        instance = it
                    }
                }
    }

    private val ip = "fbeye.xyz"
    private val port = 10101
    private var client: SSLSocket? = null
    var userCode:String? = null

    fun startClient() {
        connectToServer()
    }

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
                client = sslContext.socketFactory.createSocket(ip, port) as SSLSocket
                client!!.run { startHandshake() }
            }
        }
    }

    fun writeEyeData(dataList : List<Float>){
        if (!client!!.isConnected || Objects.isNull(client)) {
            connectToServer()
        }
        runBlocking {
            val jsonData = JSONObject()
            jsonData.apply {
                put("type", "EYE")
                put("data", dataList.toFloatArray().contentToString())
            }
            val newData = jsonData.toString()
                    .replace("\"[", "[").replace("]\"", "]")
            try {
                client!!.outputStream.apply {
                    write(newData.toByteArray(StandardCharsets.UTF_8))
                    flush()
                    close()
                }
            } catch (e: SSLException) {
                return@runBlocking
            } catch (e1: SocketException) {
                return@runBlocking
            }
        }
    }

    fun write(type: String, data: String) {
        if (!client!!.isConnected || Objects.isNull(client)) {
            connectToServer()
        }
        runBlocking {
            val jsonData = JSONObject()
            jsonData.apply {
                put("type", type)
                put("data", data)
            }
            var newData = jsonData.toString().replace("\\", "").replace("\"{", "{").replace("}\"", "}")
            try {
                client!!.outputStream.apply {
                    write(newData.toByteArray(StandardCharsets.UTF_8))
                    flush()
                    close()
                }
            } catch (e: SSLException) {
                return@runBlocking
            } catch (e1: SocketException) {
                return@runBlocking
            }
        }
    }

    fun readData(): ByteArray {
        if (!client!!.isConnected || Objects.isNull(client) || client!!.isClosed) {
            connectToServer()
        }
        return readToBuffer(client!!.inputStream)
    }

    @Synchronized
    private fun readToBuffer(inputStream: InputStream): ByteArray {
        return try {
            val input = ByteArray(40)
            inputStream.apply {
                read(input)
                close()
            }
            input
        } catch (e: Exception) {
            val errorJson = JSONObject()
            errorJson.apply {
                put("type", "ERR")
                put("data", "client is not connected")
            }.run { toString().toByteArray() }.also { client!!.close() }
        }
    }

    fun disconnect() {
        if (client!!.isConnected && Objects.nonNull(client)) {
            runBlocking {
                withContext(Dispatchers.IO) {
                    client!!.close()
                }
            }
        }
    }
}