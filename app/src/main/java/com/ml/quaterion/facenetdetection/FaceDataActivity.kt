package com.ml.quaterion.facenetdetection

import FaceDataAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FaceDataActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var faceDataAdapter: FaceDataAdapter
    private var faceDataList: MutableList<FaceData> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_data)

        recyclerView = findViewById(R.id.recyclerViewFaceData)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize face data list and adapter
        faceDataList = loadFaceData().toMutableList()
        faceDataAdapter = FaceDataAdapter(faceDataList)
        recyclerView.adapter = faceDataAdapter

        // Set up Delete button
        val deleteButton: Button = findViewById(R.id.buttonDelete)
        deleteButton.setOnClickListener {
            deleteAllFaceData()
        }

        // Set up Back button
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
                if (parts.size >= 4) {
                    val name = parts[0].substringAfter("Name: ")
                    val date = parts[1].substringAfter("Date: ")
                    val registIn = parts[2].substringAfter("Regist In: ")
                    val registOut = parts[3].substringAfter("Regist Out: ")
                    faceDataList.add(FaceData(name, date, registIn, registOut))
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

        // Refresh the data in the adapter
        faceDataList.clear()
        faceDataAdapter.notifyDataSetChanged()
    }
}
