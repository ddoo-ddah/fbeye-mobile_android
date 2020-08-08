package com.fveye.network

import kotlinx.coroutines.*
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

class CoroutineClient private constructor(){

    companion object {
        private var instance : CoroutineClient? = null

        fun getInstance() : CoroutineClient =
                instance ?: synchronized(this) {
                    instance ?: CoroutineClient().also {
                        instance = it
                    }
                }
    }

    object Info {
        val IP = "192.168.200.144"
        val PORT = 10101
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
                client = sslContext.socketFactory.createSocket(Info.IP, Info.PORT) as SSLSocket
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
            Info.pcResponseIdentifier -> doSomeThingWithCase()
            Info.qrIdentifier -> doSomeThingWithCase()
            Info.errorIdentifier -> doSomeThingWithCase()
            Info.testIdentifier -> doSomeThingWithCase()
        }
    }

    /**
     * qr코드의 인식 값을 보낸 후 연동 ok 를 받아서 다음 화면으로 가야하는 걸 콜백으로?
     * 성공 시 다음 화면
     * 실패 시 재도전 권유 --> 이것도 콜백? 너무 더러워지는데
     * RunBlocking 을 사용해서 결과를 boolean 으로 반환
     * 버튼을 하나더 만들어서 true 일시만 활성화
     */
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