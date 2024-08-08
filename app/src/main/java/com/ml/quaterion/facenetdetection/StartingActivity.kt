package com.ml.quaterion.facenetdetection

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starting)

        val registerButton = findViewById<Button>(R.id.registerButton)
        val attendanceButton = findViewById<Button>(R.id.attendanceButton)

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterFaceDataActivity::class.java)
            startActivity(intent)
        }

        attendanceButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
