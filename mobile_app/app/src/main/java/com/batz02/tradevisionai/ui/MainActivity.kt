package com.batz02.tradevisionai.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.batz02.tradevisionai.R
import com.batz02.tradevisionai.camera.CameraActivity
import com.batz02.tradevisionai.db.AppDatabase
import com.batz02.tradevisionai.network.StockUpdateWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: StockAdapter
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {

        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("DARK_MODE", true)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        val btnOpenAI = findViewById<Button>(R.id.btnOpenAI)
        val btnAIHistory = findViewById<Button>(R.id.btnAIHistory)
        val btnGoToSearch = findViewById<Button>(R.id.btnGoToSearch)
        val btnGoToWatchlist = findViewById<Button>(R.id.btnGoToWatchlist)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewWatchlist)

        val dao = AppDatabase.getDatabase(this).stockDao()

        val btnSettings = findViewById<Button>(R.id.btnSettings)
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = StockAdapter(emptyList(),
            onItemClick = { stockSelezionato ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("TICKER", stockSelezionato.ticker)
                val nomePulito = stockSelezionato.companyName.split("  |")[0]
                intent.putExtra("COMPANY_NAME", nomePulito)
                startActivity(intent)
            },
            onDeleteClick = { stockDaCancellare ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        dao.hideFromHistory(stockDaCancellare.ticker)
                        dao.cleanUpOrphans()
                    }
                }
            },

            onItemLongClick = { stock ->
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Aggiungi alla Watchlist")
                    .setMessage("Vuoi aggiungere ${stock.ticker} alla tua Watchlist?")
                    .setPositiveButton("Sì") { _, _ ->
                        if (stock.inWatchlist) {
                            Toast.makeText(this@MainActivity, "${stock.ticker} è già presente!", Toast.LENGTH_SHORT).show()
                        } else {
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    dao.updateWatchlistStatus(stock.ticker, true)
                                }
                                Toast.makeText(this@MainActivity, "${stock.ticker} aggiunto alla Watchlist!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Annulla", null)
                    .show()
            }
        )
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            dao.getHistory().collect { lista ->
                tvStatus.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                adapter.updateData(lista)
            }
        }

        btnOpenAI.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        btnAIHistory.setOnClickListener {
            startActivity(Intent(this, AnalysisHistoryActivity::class.java))
        }

        btnGoToSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        btnGoToWatchlist.setOnClickListener {
            startActivity(Intent(this, WatchlistActivity::class.java))
        }

        setupPeriodicStockUpdates()
    }

    private fun setupPeriodicStockUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val updateRequest = PeriodicWorkRequestBuilder<StockUpdateWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "STOCK_PRICE_UPDATE",
            ExistingPeriodicWorkPolicy.REPLACE,
            updateRequest
        )
    }
}