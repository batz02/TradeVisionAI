package com.batz02.tradevisionai.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.batz02.tradevisionai.R
import com.batz02.tradevisionai.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchlistActivity : AppCompatActivity() {

    private lateinit var adapter: StockAdapter
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watchlist)

        tvStatus = findViewById(R.id.tvWatchlistStatus)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewWatchlistOnly)

        val dao = AppDatabase.getDatabase(this).stockDao()

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StockAdapter(emptyList(),
            onItemClick = { stockSelezionato ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("TICKER", stockSelezionato.ticker)
                val nomePulito = stockSelezionato.companyName.split("  |")[0]
                intent.putExtra("COMPANY_NAME", nomePulito)
                startActivity(intent)
            },
            onDeleteClick = { stockDaRimuovere ->
                // NOTA: Non serve più chiamare caricaWatchlist() alla fine!
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        dao.updateWatchlistStatus(stockDaRimuovere.ticker, false)
                        dao.cleanUpOrphans()
                    }
                }
            }
        )
        recyclerView.adapter = adapter

        // --- ASCOLTO REATTIVO DELLA WATCHLIST ---
        lifecycleScope.launch {
            dao.getWatchlist().collect { lista ->
                tvStatus.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                adapter.updateData(lista)
            }
        }
        // ----------------------------------------
    }
}