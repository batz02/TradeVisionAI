package com.batz02.tradevisionai.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.batz02.tradevisionai.R
import com.batz02.tradevisionai.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchSaveGallery: SwitchCompat

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        if (isGranted) {
            sharedPreferences.edit().putBoolean("SAVE_TO_GALLERY", true).apply()
            switchSaveGallery.isChecked = true
            Toast.makeText(this, "Salvataggio automatico abilitato", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permesso negato. Impossibile salvare in galleria.", Toast.LENGTH_LONG).show()
            sharedPreferences.edit().putBoolean("SAVE_TO_GALLERY", false).apply()
            switchSaveGallery.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnClearHistory = findViewById<Button>(R.id.btnClearHistory)
        val btnClearAIHistory = findViewById<Button>(R.id.btnClearAIHistory)
        val switchTheme = findViewById<SwitchCompat>(R.id.switchTheme)
        switchSaveGallery = findViewById(R.id.switchSaveGallery)

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

        val saveToGallery = sharedPreferences.getBoolean("SAVE_TO_GALLERY", false)
        switchSaveGallery.isChecked = saveToGallery

        switchSaveGallery.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) return@setOnCheckedChangeListener

            if (isChecked) {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        switchSaveGallery.isChecked = false
                        requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        return@setOnCheckedChangeListener
                    }
                }
                sharedPreferences.edit().putBoolean("SAVE_TO_GALLERY", true).apply()
                Toast.makeText(this, "Salvataggio automatico abilitato", Toast.LENGTH_SHORT).show()
            } else {
                sharedPreferences.edit().putBoolean("SAVE_TO_GALLERY", false).apply()
            }
        }
    }
}