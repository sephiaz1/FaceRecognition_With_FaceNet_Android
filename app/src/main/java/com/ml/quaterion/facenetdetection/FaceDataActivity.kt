package com.ml.quaterion.facenetdetection

import FaceDataAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FaceDataActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var faceDataAdapter: FaceDataAdapter
    private var faceDataList: MutableList<FaceData> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_data)
        logSharedPreferences()
        recyclerView = findViewById(R.id.recyclerViewFaceData)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize face data list and adapter
        faceDataList = loadFaceData().toMutableList()
        faceDataAdapter = FaceDataAdapter(faceDataList)
        recyclerView.adapter = faceDataAdapter

        val deleteButton: Button = findViewById(R.id.buttonDelete)
        deleteButton.setOnClickListener {
            deleteAllFaceData()
        }

        val backButton: Button = findViewById(R.id.buttonBack)
        backButton.setOnClickListener {
            val intent = Intent(this, StartingActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadFaceData(): List<FaceData> {
        val sharedPreferences = getSharedPreferences("FaceData", MODE_PRIVATE)
        val faceDataList = mutableListOf<FaceData>()
        val keys = sharedPreferences.all.keys

        for (key in keys) {
            val data = sharedPreferences.getString(key, null)
            if (data != null) {
                val parts = data.split(", ")
                if (parts.size == 5) {
                    try {
                        val name = parts[0].substringAfter("Name: ")
                        val date = parts[1].substringAfter("Date: ")
                        val kantor = parts[2].substringAfter("Kantor:")
                        val registIn = parts[3].substringAfter("Regist In: ")
                        val registOut = parts[4].substringAfter("Regist Out: ")
                        faceDataList.add(FaceData(name, date, kantor, registIn, registOut))
                    } catch (e: Exception) {
                        Log.e("FaceDataActivity", "Error parsing data for key: $key", e)
                    }
                } else {
                    Log.e("FaceDataActivity", "Unexpected data format for key: $key")
                }
            }
        }
        return faceDataList
    }

    private fun deleteAllFaceData() {
        val sharedPreferences = getSharedPreferences("FaceData", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        Log.d("SharedPreferences", "SP Deleted")

        // Refresh the data in the adapter
        faceDataList.clear()
        faceDataAdapter.notifyDataSetChanged()
    }
    private fun logSharedPreferences() {
        val sharedPreferences = getSharedPreferences("FaceData", MODE_PRIVATE)
        val allEntries = sharedPreferences.all

        for ((key, value) in allEntries) {
            Log.d("SharedPreferences", "Key: $key, Value: $value")
        }
    }
}
