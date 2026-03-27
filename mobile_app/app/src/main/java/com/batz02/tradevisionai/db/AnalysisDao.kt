package com.batz02.tradevisionai.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AnalysisDao {
    @Insert
    fun insertAnalysis(analysis: AnalysisEntity)

    @Query("SELECT * FROM analysis_history ORDER BY timestamp DESC")
    fun getAllHistory(): List<AnalysisEntity>

    @Delete
    fun deleteAnalysis(analysis: AnalysisEntity)

    // --- NUOVA QUERY ---
    @Query("DELETE FROM analysis_history")
    fun clearAllAnalysis()
}