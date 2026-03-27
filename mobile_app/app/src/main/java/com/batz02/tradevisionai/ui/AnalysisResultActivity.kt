package com.batz02.tradevisionai.ui

import android.graphics.BitmapFactory
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.batz02.tradevisionai.R
import java.io.File

class AnalysisResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val ivAnalyzedImage = findViewById<ImageView>(R.id.ivAnalyzedImage)
        val tvAccuracyResult = findViewById<TextView>(R.id.tvAccuracyResult)
        val btnBackHome = findViewById<Button>(R.id.btnBackHome)
        val gaugeView = findViewById<ConfidenceGaugeView>(R.id.gaugeView)

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
    }
}