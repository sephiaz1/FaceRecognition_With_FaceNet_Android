package com.ml.quaterion.facenetdetection

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity

class StartingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starting)

        val registerButton = findViewById<RelativeLayout>(R.id.registerButtonContainer)
        val registInButton = findViewById<Button>(R.id.checkInButton)
        val registOutButton = findViewById<Button>(R.id.checkOutButton)
        val checkAttendanceButton = findViewById<RelativeLayout>(R.id.checkAttendanceButtonContainer)

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
