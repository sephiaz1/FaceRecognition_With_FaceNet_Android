package com.ml.quaterion.facenetdetection

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class FaceDataAdapter(context: Context, private val faceDataList: List<Pair<String, String>>) :
    ArrayAdapter<Pair<String, String>>(context, 0, faceDataList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_face_data, parent, false)

        val faceData = faceDataList[position]
        val faceNameTextView: TextView = view.findViewById(R.id.itemFaceName)
        val timestampTextView: TextView = view.findViewById(R.id.itemTimestamp)

        faceNameTextView.text = faceData.first ?: "Unknown"
        timestampTextView.text = faceData.second ?: "N/A"

        return view
    }
}
