package com.batz02.tradevisionai.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.batz02.tradevisionai.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class AnalysisResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val rootLayout = findViewById<View>(R.id.rootLayout)
        val ivAnalyzedImage = findViewById<ImageView>(R.id.ivAnalyzedImage)
        val tvAccuracyResult = findViewById<TextView>(R.id.tvAccuracyResult)

        val gaugeView = findViewById<ConfidenceGaugeView>(R.id.gaugeView)

        val actionButtonsContainer = findViewById<LinearLayout>(R.id.actionButtonsContainer)
        val btnShare = findViewById<Button>(R.id.btnShare)
        val btnSaveGallery = findViewById<Button>(R.id.btnSaveGallery)
        val btnBackHome = findViewById<Button>(R.id.btnBackHome)

        val imagePath = intent.getStringExtra("IMAGE_PATH")
        val label = intent.getStringExtra("PREDICTION_LABEL") ?: "Sconosciuto"
        val confidence = intent.getFloatExtra("PREDICTION_CONFIDENCE", 50f)
        val modelName = intent.getStringExtra("MODEL_NAME") ?: ""

        if (imagePath != null) {
            val imgFile = File(imagePath)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                ivAnalyzedImage.setImageBitmap(bitmap)
            }
        }

        tvAccuracyResult.text = "$modelName\n$label - ${String.format("%.1f", confidence)}%"

        val isBuy = label.contains("COMPRA", ignoreCase = true)
        gaugeView.setPrediction(isBuy, confidence)

        btnBackHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        btnShare.setOnClickListener {
            val cleanBitmap = getCleanScreenshot(rootLayout, actionButtonsContainer, btnBackHome)
            shareScreenshot(cleanBitmap)
        }

        btnSaveGallery.setOnClickListener {
            val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val isSaveEnabled = sharedPreferences.getBoolean("SAVE_TO_GALLERY", false)

            if (isSaveEnabled) {
                val cleanBitmap = getCleanScreenshot(rootLayout, actionButtonsContainer, btnBackHome)
                saveToGallery(cleanBitmap)
            } else {
                Toast.makeText(this, "Salvataggio disabilitato. Attivalo in ⚙️ Impostazioni.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getCleanScreenshot(view: View, actionButtons: View, btnHome: View): Bitmap {
        actionButtons.visibility = View.INVISIBLE
        btnHome.visibility = View.INVISIBLE

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        actionButtons.visibility = View.VISIBLE
        btnHome.visibility = View.VISIBLE

        return bitmap
    }

    private fun saveToGallery(bitmap: Bitmap) {
        try {
            val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "TradeVisionAI_$name.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TradeVisionAI")
                }
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                Toast.makeText(this, "Salvato in Galleria! 🖼️", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Errore salvataggio.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Errore durante il salvataggio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareScreenshot(bitmap: Bitmap) {
        try {
            val cachePath = File(cacheDir, "shared_images")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "shared_analysis.jpg")

            val fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()

            val uri: Uri = FileProvider.getUriForFile(
                this,
                "com.batz02.tradevisionai.fileprovider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Guarda questa analisi generata con TradeVisionAI! 🚀")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Condividi analisi tramite:"))

        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("SHARE_ERROR", "Errore condivisione: ${e.message}", e)
            Toast.makeText(this, "Errore durante la condivisione.", Toast.LENGTH_SHORT).show()
        }
    }
}