// FaceDataActivity.kt
package com.ml.quaterion.facenetdetection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class FaceDataActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_data)

        val listView = findViewById<ListView>(R.id.faceDataListView)
        val backButton = findViewById<Button>(R.id.backButton)
        val deleteAllButton = findViewById<Button>(R.id.deleteAllButton)

        val sharedPreferences = getSharedPreferences("FaceData", Context.MODE_PRIVATE)
        val allEntries = sharedPreferences.all

        // Create a list to hold the data
        val dataList = mutableListOf<String>()
        for ((key, value) in allEntries) {
            if (key.startsWith("face_data_")) {
                // Extract the name and timestamp from the saved string
                val data = value.toString().split(",")
                if (data.size == 2) {
                    val name = data[0]
                    val timestamp = data[1]
                    // Add formatted string to list
                    dataList.add("Name: $name\nTimestamp: $timestamp")
                }
            }
        }

        // Create an ArrayAdapter and set it to the ListView
        val adapter = ArrayAdapter(this, R.layout.item_face_data, R.id.itemFaceName, dataList)
        listView.adapter = adapter

        backButton.setOnClickListener {
            val intent = Intent(this, StartingActivity::class.java)
            startActivity(intent)
            finish()
        }

        deleteAllButton.setOnClickListener {
            // Remove all face data from SharedPreferences
            val editor = sharedPreferences.edit()
            for (key in sharedPreferences.all.keys) {
                if (key.startsWith("face_data_")) {
                    editor.remove(key)
                }
            }
            editor.apply()

            // Refresh the ListView
            updateListView()
        }
    }

    private fun updateListView() {
        val listView = findViewById<ListView>(R.id.faceDataListView)
        val sharedPreferences = getSharedPreferences("FaceData", Context.MODE_PRIVATE)
        val allEntries = sharedPreferences.all

        // Create a list to hold the data
        val dataList = mutableListOf<String>()
        for ((key, value) in allEntries) {
            if (key.startsWith("face_data_")) {
                // Extract the name and timestamp from the saved string
                val data = value.toString().split(",")
                if (data.size == 2) {
                    val name = data[0]
                    val timestamp = data[1]
                    // Add formatted string to list
                    dataList.add("Name: $name\nTimestamp: $timestamp")
                }
            }
        }

        // Create an ArrayAdapter and set it to the ListView
        val adapter = ArrayAdapter(this, R.layout.item_face_data, R.id.itemFaceName, dataList)
        listView.adapter = adapter
    }
}
