package com.task.sm.agorademo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val videoCallBtn = findViewById<Button>(R.id.btnVideoCall)
        val audioCallBtn = findViewById<Button>(R.id.btnAudioCall)

        videoCallBtn.setOnClickListener {
            val intent = Intent(this, CallActivity::class.java)
            intent.putExtra("CALL_TYPE", "VIDEO")
            startActivity(intent)
        }

        audioCallBtn.setOnClickListener {
            val intent = Intent(this, CallActivity::class.java)
            intent.putExtra("CALL_TYPE", "AUDIO")
            startActivity(intent)
        }
    }
}