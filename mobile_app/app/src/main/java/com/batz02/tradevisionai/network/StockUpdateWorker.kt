package com.batz02.tradevisionai.network

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.batz02.tradevisionai.db.AppDatabase
import com.batz02.tradevisionai.network.StockApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StockUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WORKER_TEST", "Inizio aggiornamento prezzi...")

                val dao = AppDatabase.getDatabase(applicationContext).stockDao()
                val apiClient = StockApiClient()

                val stocks = dao.getAllStocks()

                for (stock in stocks) {
                    val newPrice = apiClient.getStockPrice(stock.ticker)

                    if (newPrice != "Errore") {
                        dao.updateStockPrice(stock.ticker, newPrice)
                        Log.d("WORKER_TEST", "Aggiornato ${stock.ticker}: $newPrice")
                    }
                }

                Log.d("WORKER_TEST", "Aggiornamento prezzi completato.")
                Result.success()

            } catch (e: Exception) {
                Log.e("WORKER_TEST", "Errore durante l'aggiornamento: ${e.message}")
                Result.retry()
            }
        }
    }
}