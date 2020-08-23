package com.fveye

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class Splash : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        startActivity(intent)
        finish()
    }
}