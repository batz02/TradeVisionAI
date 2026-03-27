package com.batz02.tradevisionai.camera

import com.batz02.tradevisionai.BuildConfig
import android.Manifest
import android.app.Activity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.batz02.tradevisionai.R
import com.batz02.tradevisionai.ml.TradeVisionAnalyzer
import com.batz02.tradevisionai.network.AwsApiClient
import com.batz02.tradevisionai.ui.AnalysisResultActivity
import com.batz02.tradevisionai.db.AppDatabase
import com.batz02.tradevisionai.db.AnalysisEntity
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class ModelInfo(
    val id: String,
    val displayName: String,
    val type: ModelType,
    val assetName: String? = null
) {
    override fun toString(): String = displayName
}

enum class ModelType { LOCAL_TFLITE, CLOUD_AWS }

class CameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var spinnerModels: Spinner

    private lateinit var analyzer: TradeVisionAnalyzer

    private val availableModels = listOf(
        ModelInfo("local_v2", "MobileNet V2 Quant (On-device)", ModelType.LOCAL_TFLITE, "mobilenet_v2_quant.tflite"),
        ModelInfo("local_v3", "MobileNet V3 Quant (On-device)", ModelType.LOCAL_TFLITE, "mobilenet_v3_small_quant.tflite"),
        ModelInfo("inception", "Inception V3 (Cloud)", ModelType.CLOUD_AWS),
        ModelInfo("mobilenet", "MobileNet V2 (Cloud)", ModelType.CLOUD_AWS)
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Permesso fotocamera negato.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        viewFinder = findViewById(R.id.viewFinder)
        spinnerModels = findViewById(R.id.spinnerModels)
        val btnCapture = findViewById<Button>(R.id.btnCapture)
        val btnGallery = findViewById<Button>(R.id.btnGallery)

        cameraExecutor = Executors.newSingleThreadExecutor()
        analyzer = TradeVisionAnalyzer(this)

        val adapter = ArrayAdapter(this, R.layout.spinner_item, availableModels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModels.adapter = adapter

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnCapture.setOnClickListener {
            takePhoto()
        }

        btnGallery.setOnClickListener {
            val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CAMERA_TEST", "Errore nell'avvio della fotocamera", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        Toast.makeText(this, "Acquisizione e Analisi...", Toast.LENGTH_SHORT).show()

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {

                override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                    var bitmap = image.toBitmap()

                    val rotationDegrees = image.imageInfo.rotationDegrees
                    image.close()

                    if (rotationDegrees != 0) {
                        val matrix = android.graphics.Matrix()
                        matrix.postRotate(rotationDegrees.toFloat())
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }

                    processImageAndNavigate(bitmap)
                }

                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                    Log.e("CAMERA_TEST", "Errore nello scatto: ${exception.message}", exception)
                }
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            if (imageUri != null) {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    processImageAndNavigate(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }


    private fun processImageAndNavigate(bitmap: Bitmap) {
        val selectedModel = spinnerModels.selectedItem as ModelInfo

        Toast.makeText(this, "Analisi in corso...", Toast.LENGTH_LONG).show()

        lifecycleScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val savedFile = java.io.File(filesDir, "analysis_$timestamp.jpg")

                withContext(Dispatchers.IO) {
                    val outputStream = java.io.FileOutputStream(savedFile)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.flush()
                    outputStream.close()
                }

                var labelResult = "Sconosciuto"
                var confidenceResult = 50f

                if (selectedModel.type == ModelType.LOCAL_TFLITE) {
                    val modelFilename = selectedModel.assetName ?: "mobilenet_v2_quant.tflite"
                    val prediction = analyzer.analyzeGraph(bitmap, modelFilename)
                    labelResult = prediction.label
                    confidenceResult = prediction.confidence
                } else {
                    val cloudResultStr = withContext(Dispatchers.IO) {
                        val awsClient = AwsApiClient()
                        val modelId = selectedModel.id
                        val baseUrl = BuildConfig.AWS_API_URL
                        awsClient.analyzeImageOnCloud(savedFile, baseUrl, modelId)
                    }
                    labelResult = if (cloudResultStr.contains("COMPRA", ignoreCase = true)) "COMPRA" else "VENDI"
                    val regex = Regex("(\\d+\\.?\\d*)")
                    val match = regex.find(cloudResultStr)
                    confidenceResult = match?.value?.toFloatOrNull() ?: 50f
                }

                withContext(Dispatchers.IO) {
                    val dao = AppDatabase.getDatabase(this@CameraActivity).analysisDao()
                    val entity = AnalysisEntity(
                        imagePath = savedFile.absolutePath,
                        modelName = selectedModel.displayName,
                        label = labelResult,
                        confidence = confidenceResult,
                        timestamp = timestamp
                    )
                    dao.insertAnalysis(entity)
                }

                val intent = Intent(this@CameraActivity, AnalysisResultActivity::class.java)
                intent.putExtra("IMAGE_PATH", savedFile.absolutePath)
                intent.putExtra("PREDICTION_LABEL", labelResult)
                intent.putExtra("PREDICTION_CONFIDENCE", confidenceResult)
                intent.putExtra("MODEL_NAME", selectedModel.displayName)

                startActivity(intent)

            } catch (e: Exception) {
                Log.e("AWS_ERROR", "Errore: ${e.message}")
                Toast.makeText(this@CameraActivity, "Errore durante l'elaborazione.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 2
    }
}