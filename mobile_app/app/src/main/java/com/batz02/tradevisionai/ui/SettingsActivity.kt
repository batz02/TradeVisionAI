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

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnClearHistory = findViewById<Button>(R.id.btnClearHistory)
        val switchTheme = findViewById<SwitchCompat>(R.id.switchTheme)

        val dao = AppDatabase.getDatabase(this).stockDao()
        btnClearHistory.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    dao.clearAllHistory()
                    dao.cleanUpOrphans()
                }
                Toast.makeText(this@SettingsActivity, "Cronologia pulita!", Toast.LENGTH_SHORT).show()
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