package com.batz02.tradevisionai.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_history")
data class AnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val modelName: String,
    val resultText: String,
    val timestamp: Long
)