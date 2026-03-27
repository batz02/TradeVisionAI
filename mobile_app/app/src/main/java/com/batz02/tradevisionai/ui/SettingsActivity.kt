package com.batz02.tradevisionai.ui

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.batz02.tradevisionai.R
import com.batz02.tradevisionai.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnClearHistory = findViewById<Button>(R.id.btnClearHistory)
        val btnClearAIHistory = findViewById<Button>(R.id.btnClearAIHistory)
        val switchTheme = findViewById<SwitchCompat>(R.id.switchTheme)

        val db = AppDatabase.getDatabase(this)
        val stockDao = db.stockDao()
        val analysisDao = db.analysisDao()

        btnClearHistory.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    stockDao.clearAllHistory()
                    stockDao.cleanUpOrphans()
                }
                Toast.makeText(this@SettingsActivity, "Cronologia titoli pulita!", Toast.LENGTH_SHORT).show()
            }
        }

        btnClearAIHistory.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val allAnalyses = analysisDao.getAllHistory()

                    for (analysis in allAnalyses) {
                        val imgFile = File(analysis.imagePath)
                        if (imgFile.exists()) {
                            imgFile.delete()
                        }
                    }

                    analysisDao.clearAllAnalysis()
                }
                Toast.makeText(this@SettingsActivity, "Storico AI e immagini eliminati!", Toast.LENGTH_SHORT).show()
            }
        }

        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("DARK_MODE", true)

        switchTheme.isChecked = isDarkMode

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("DARK_MODE", isChecked).apply()

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }
}