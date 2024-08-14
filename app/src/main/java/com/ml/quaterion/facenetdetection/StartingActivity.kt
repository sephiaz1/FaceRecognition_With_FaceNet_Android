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
        val registInButton = findViewById<Button>(R.id.registInButton)
        val registOutButton = findViewById<Button>(R.id.registOutButton)

        val checkAttendanceButton = findViewById<Button>(R.id.checkAttendanceButton)

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterFaceDataActivity::class.java)
            startActivity(intent)
        }

        registInButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("isRegistIn", true) // Pass true for "regist in"
            startActivity(intent)
        }


        registOutButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("isRegistIn", false) // Pass false for "regist out"
            startActivity(intent)
        }

        checkAttendanceButton.setOnClickListener {
            val intent = Intent(this, FaceDataActivity::class.java)
            startActivity(intent)
        }
    }
}
