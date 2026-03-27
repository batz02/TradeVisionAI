package com.batz02.tradevisionai.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStock(stock: StockEntity)

    @Query("SELECT * FROM stocks WHERE ticker = :ticker LIMIT 1")
    fun getStock(ticker: String): StockEntity?

    // --- AGGIORNATO: Ora usano Flow per essere reattivi ---
    @Query("SELECT * FROM stocks WHERE inHistory = 1 ORDER BY addedAt DESC")
    fun getHistory(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks WHERE inWatchlist = 1 ORDER BY addedAt DESC")
    fun getWatchlist(): Flow<List<StockEntity>>
    // ------------------------------------------------------

    @Query("UPDATE stocks SET inHistory = 0 WHERE ticker = :ticker")
    fun hideFromHistory(ticker: String)

    @Query("UPDATE stocks SET inWatchlist = :status WHERE ticker = :ticker")
    fun updateWatchlistStatus(ticker: String, status: Boolean)

    @Query("DELETE FROM stocks WHERE inHistory = 0 AND inWatchlist = 0")
    fun cleanUpOrphans()

    @Query("DELETE FROM stocks WHERE ticker = :ticker")
    fun deleteStock(ticker: String)

    @Query("UPDATE stocks SET inHistory = 0")
    fun clearAllHistory()

    @Query("DELETE FROM stocks")
    fun clearAll()

    // Ottiene tutte le azioni salvate nel database
    @Query("SELECT * FROM stocks")
    fun getAllStocks(): List<StockEntity>

    // Aggiorna solo il prezzo di un'azione specifica
    @Query("UPDATE stocks SET price = :newPrice WHERE ticker = :ticker")
    fun updateStockPrice(ticker: String, newPrice: String)
}