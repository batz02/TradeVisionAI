package com.batz02.tradevisionai.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.batz02.tradevisionai.R
import com.batz02.tradevisionai.db.AppDatabase
import com.batz02.tradevisionai.db.StockEntity
import com.batz02.tradevisionai.network.StockApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity() {

    private var datiValidi = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val tvDetailTicker = findViewById<TextView>(R.id.tvDetailTicker)
        val tvDetailName = findViewById<TextView>(R.id.tvDetailName)
        val tvDetailPrice = findViewById<TextView>(R.id.tvDetailPrice)
        val btnSaveWatchlist = findViewById<Button>(R.id.btnSaveWatchlist)
        val containerNews = findViewById<LinearLayout>(R.id.containerNews)

        val ticker = intent.getStringExtra("TICKER") ?: return
        val companyName = intent.getStringExtra("COMPANY_NAME") ?: "Azienda Sconosciuta"

        tvDetailTicker.text = ticker
        tvDetailName.text = companyName

        var currentPriceStr = "0.0"
        val dao = AppDatabase.getDatabase(this).stockDao()

        lifecycleScope.launch {
            val price = withContext(Dispatchers.IO) {
                StockApiClient().getStockPrice(ticker)
            }

            val valuta = if (ticker.contains(".")) "€" else "$"

            if (price != "Errore" && price != "0.0" && price != "0") {
                currentPriceStr = price
                tvDetailPrice.text = "$price $valuta"
                datiValidi = true
            } else {
                currentPriceStr = "N/D"
                datiValidi = false

                if (ticker.contains(".")) {
                    tvDetailPrice.text = "API Free: Solo mercato USA"
                    tvDetailPrice.textSize = 20f
                    tvDetailPrice.setTextColor(ContextCompat.getColor(this@DetailActivity, android.R.color.holo_orange_light))
                } else {
                    tvDetailPrice.text = "Errore Rete / Dati N/D"
                }

                btnSaveWatchlist.isEnabled = false
                btnSaveWatchlist.alpha = 0.5f
            }

            val newsList = withContext(Dispatchers.IO) {
                StockApiClient().getCompanyNews(ticker)
            }

            containerNews.removeAllViews()
            if (newsList.isNotEmpty()) {
                for (news in newsList) {
                    val tvSingleNews = TextView(this@DetailActivity).apply {
                        text = "📰 ${news.headline}\n"
                        textSize = 14f
                        setTextColor(ContextCompat.getColor(context, android.R.color.white))
                        setPadding(0, 16, 0, 16)
                        setOnClickListener {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(news.url)))
                        }
                    }
                    containerNews.addView(tvSingleNews)
                }
            } else {
                val tvNoNews = TextView(this@DetailActivity).apply {
                    text = if (ticker.contains(".")) {
                        "📰 Le news per i mercati europei richiedono un piano API Premium."
                    } else {
                        "Nessuna notizia recente trovata per questo titolo."
                    }
                    setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                    setPadding(0, 16, 0, 16)
                }
                containerNews.addView(tvNoNews)
            }

            if (datiValidi) {
                withContext(Dispatchers.IO) {
                    val azioneEsistente = dao.getStock(ticker)
                    val statoPreferito = azioneEsistente?.inWatchlist ?: false

                    val entity = StockEntity(
                        ticker = ticker,
                        companyName = companyName,
                        price = currentPriceStr,
                        currency = valuta,
                        addedAt = System.currentTimeMillis(),
                        inWatchlist = statoPreferito,
                        inHistory = true
                    )
                    dao.insertStock(entity)
                }
            }
        }

        btnSaveWatchlist.setOnClickListener {
            if (!datiValidi) {
                Toast.makeText(this@DetailActivity, "Impossibile salvare un titolo senza dati.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val giaPresente = withContext(Dispatchers.IO) {
                    dao.getStock(ticker)?.inWatchlist == true
                }

                if (giaPresente) {
                    Toast.makeText(this@DetailActivity, "$ticker è già presente!", Toast.LENGTH_SHORT).show()
                } else {
                    withContext(Dispatchers.IO) {
                        dao.updateWatchlistStatus(ticker, true)
                    }
                    Toast.makeText(this@DetailActivity, "$ticker aggiunto alla Watchlist!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}