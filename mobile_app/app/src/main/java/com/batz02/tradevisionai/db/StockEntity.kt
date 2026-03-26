package com.batz02.tradevisionai.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val ticker: String,
    val companyName: String,
    val price: String,
    val currency: String,
    val addedAt: Long,
    val inWatchlist: Boolean = false,
    val inHistory: Boolean = true
)