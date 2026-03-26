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

        val imagePath = intent.getStringExtra("IMAGE_PATH")
        val resultText = intent.getStringExtra("ACCURACY_RESULT")

        if (imagePath != null) {
            val imgFile = File(imagePath)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                ivAnalyzedImage.setImageBitmap(bitmap)
            }
        }

        tvAccuracyResult.text = resultText ?: "Nessun risultato ricevuto."

        btnBackHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            startActivity(intent)
            finish()
        }
    }
}