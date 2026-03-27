package com.batz02.tradevisionai.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.batz02.tradevisionai.R
import com.batz02.tradevisionai.db.AnalysisDao
import com.batz02.tradevisionai.db.AnalysisEntity
import com.batz02.tradevisionai.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AnalysisHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerAIHistory: RecyclerView
    private lateinit var tvEmptyAIHistory: TextView
    private lateinit var adapter: AIHistoryAdapter
    private lateinit var dao: AnalysisDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_history)

        recyclerAIHistory = findViewById(R.id.recyclerAIHistory)
        tvEmptyAIHistory = findViewById(R.id.tvEmptyAIHistory)

        recyclerAIHistory.layoutManager = LinearLayoutManager(this)
        dao = AppDatabase.getDatabase(this).analysisDao()

        adapter = AIHistoryAdapter(
            historyList = emptyList(),
            onItemClick = { selectedItem ->
                val intent = Intent(this@AnalysisHistoryActivity, AnalysisResultActivity::class.java)
                intent.putExtra("IMAGE_PATH", selectedItem.imagePath)
                intent.putExtra("PREDICTION_LABEL", selectedItem.label)
                intent.putExtra("PREDICTION_CONFIDENCE", selectedItem.confidence)
                intent.putExtra("MODEL_NAME", selectedItem.modelName)
                startActivity(intent)
            },
            onItemLongClick = { itemDaCancellare ->
                mostraDialogCancellazione(itemDaCancellare)
            }
        )

        recyclerAIHistory.adapter = adapter

        caricaCronologia()
    }

    private fun caricaCronologia() {
        lifecycleScope.launch {
            val historyList = withContext(Dispatchers.IO) {
                dao.getAllHistory()
            }

            adapter.updateData(historyList)

            if (historyList.isEmpty()) {
                tvEmptyAIHistory.visibility = View.VISIBLE
                recyclerAIHistory.visibility = View.GONE
            } else {
                tvEmptyAIHistory.visibility = View.GONE
                recyclerAIHistory.visibility = View.VISIBLE
            }
        }
    }

    private fun mostraDialogCancellazione(item: AnalysisEntity) {
        AlertDialog.Builder(this)
            .setTitle("Elimina Analisi")
            .setMessage("Vuoi davvero eliminare questa analisi dalla cronologia?")
            .setPositiveButton("Elimina") { _, _ ->
                eliminaAnalisi(item)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun eliminaAnalisi(item: AnalysisEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val imgFile = File(item.imagePath)
                if (imgFile.exists()) {
                    imgFile.delete()
                }

                dao.deleteAnalysis(item)
            }

            Toast.makeText(this@AnalysisHistoryActivity, "Analisi eliminata", Toast.LENGTH_SHORT).show()
            caricaCronologia()
        }
    }
}