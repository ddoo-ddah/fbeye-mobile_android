package com.fveye.network

import android.util.Log
import io.socket.client.IO

class ImageClient {

    private lateinit var client: io.socket.client.Socket

    fun startClient(){
        Thread{
            client = IO.socket("http://fbeye.sysbot32.com:3000/supervise/eyes")
            client.connect()
        }.start()
    }

    fun write(data : String){
        Thread{
//            Log.d("Send", data)
            client.emit("EYE", data)
        }.start()
    }

    fun destroy(){
        Thread{
            client.disconnect()
            client.close()
        }.start()
    }
}