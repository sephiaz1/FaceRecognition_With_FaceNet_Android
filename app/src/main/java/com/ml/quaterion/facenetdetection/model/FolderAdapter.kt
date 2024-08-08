package com.ml.quaterion.facenetdetection

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FolderAdapter(
    private val folders: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]
        holder.bind(folder, onClick)
    }

    override fun getItemCount() = folders.size

    class FolderViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val folderName: TextView = itemView.findViewById(R.id.folderName)

        fun bind(folder: String, onClick: (String) -> Unit) {
            folderName.text = folder
            itemView.setOnClickListener { onClick(folder) }
        }
    }
}
