package com.batz02.tradevisionai.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.batz02.tradevisionai.R
import com.batz02.tradevisionai.db.AnalysisEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AIHistoryAdapter(
    private var historyList: List<AnalysisEntity>,
    private val onItemClick: (AnalysisEntity) -> Unit,
    private val onItemLongClick: (AnalysisEntity) -> Unit
) : RecyclerView.Adapter<AIHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        val tvHistoryModel: TextView = view.findViewById(R.id.tvHistoryModel)
        val tvHistoryResult: TextView = view.findViewById(R.id.tvHistoryResult)
        val tvHistoryDate: TextView = view.findViewById(R.id.tvHistoryDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_analysis_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]

        holder.tvHistoryModel.text = item.modelName

        val cleanResult = item.resultText.substringAfter("\n\n").trim()
        holder.tvHistoryResult.text = cleanResult

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateString = sdf.format(Date(item.timestamp))
        holder.tvHistoryDate.text = dateString

        val imgFile = File(item.imagePath)
        if (imgFile.exists()) {
            holder.ivThumbnail.setImageURI(Uri.fromFile(imgFile))
        } else {
            holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount() = historyList.size

    fun updateData(newList: List<AnalysisEntity>) {
        historyList = newList
        notifyDataSetChanged()
    }
}