// FaceDataAdapter.kt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ml.quaterion.facenetdetection.FaceData
import com.ml.quaterion.facenetdetection.R

class FaceDataAdapter(private val faceDataList: List<FaceData>) : RecyclerView.Adapter<FaceDataAdapter.FaceDataViewHolder>() {

    inner class FaceDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.textName)
        val textDate: TextView = itemView.findViewById(R.id.textDate)
        val textRegistIn: TextView = itemView.findViewById(R.id.textRegistIn)
        val textRegistOut: TextView = itemView.findViewById(R.id.textRegistOut)
        val textKantor: TextView = itemView.findViewById(R.id.textKantor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaceDataViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_face_data, parent, false)
        return FaceDataViewHolder(view)
    }

    override fun onBindViewHolder(holder: FaceDataViewHolder, position: Int) {
        val faceData = faceDataList[position]
        holder.textName.text = faceData.name
        holder.textDate.text = faceData.date
        holder.textRegistIn.text = faceData.registIn
        holder.textRegistOut.text = faceData.registOut
        holder.textKantor.text = faceData.kantor
    }

    override fun getItemCount(): Int = faceDataList.size
}
