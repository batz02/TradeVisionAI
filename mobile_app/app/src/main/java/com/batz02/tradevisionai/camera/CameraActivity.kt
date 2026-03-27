package com.batz02.tradevisionai.camera

import android.content.ContentValues
import android.graphics.ImageDecoder
import android.os.Build
import androidx.camera.core.ImageCaptureException
import java.text.SimpleDateFormat
import java.util.Locale
import com.batz02.tradevisionai.BuildConfig
import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.Matrix
import android.media.ExifInterface

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
            Toast.makeText(this, "Permesso fotocamera necessario per l'analisi.", Toast.LENGTH_LONG).show()
            finish()
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

        Toast.makeText(this, "Acquisizione e analisi...", Toast.LENGTH_SHORT).show()

        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val saveToGallery = sharedPreferences.getBoolean("SAVE_TO_GALLERY", false)

        if (saveToGallery) {
            val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TradeVisionAI")
                }
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = outputFileResults.savedUri
                        if (savedUri != null) {
                            Toast.makeText(this@CameraActivity, "Foto salvata in galleria", Toast.LENGTH_SHORT).show()
                            try {
                                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    val source = ImageDecoder.createSource(contentResolver, savedUri)
                                    ImageDecoder.decodeBitmap(source)
                                } else {
                                    @Suppress("DEPRECATION")
                                    MediaStore.Images.Media.getBitmap(contentResolver, savedUri)
                                }
                                val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                                processImageAndNavigate(mutableBitmap)
                            } catch (e: Exception) {
                                Log.e("CAMERA_TEST", "Errore: ${e.message}")
                            }
                        }
                    }
                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(this@CameraActivity, "Errore salvataggio foto.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } else {
            val tempFile = File(cacheDir, "temp_capture.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        try {
                            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)

                            val exif = ExifInterface(tempFile.absolutePath)
                            val orientation = exif.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_UNDEFINED
                            )

                            val rotationAngle = when (orientation) {
                                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                                else -> 0f
                            }

                            val rotatedBitmap = if (rotationAngle != 0f) {
                                val matrix = Matrix()
                                matrix.postRotate(rotationAngle)
                                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            } else {
                                bitmap
                            }

                            val mutableBitmap = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)

                            tempFile.delete()

                            processImageAndNavigate(mutableBitmap)
                        } catch (e: Exception) {
                            Log.e("CAMERA_TEST", "Errore nel caricamento del bitmap interno: ${e.message}")
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CAMERA_TEST", "Errore nello scatto: ${exception.message}")
                        Toast.makeText(this@CameraActivity, "Errore acquisizione foto.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            if (imageUri != null) {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    Toast.makeText(this, "Analisi in corso...", Toast.LENGTH_SHORT).show()
                    processImageAndNavigate(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun processImageAndNavigate(bitmap: Bitmap) {
        val selectedModel = spinnerModels.selectedItem as ModelInfo

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

                    if (cloudResultStr.contains("Errore", ignoreCase = true) ||
                        cloudResultStr.contains("Failed", ignoreCase = true) ||
                        (!cloudResultStr.contains("COMPRA", ignoreCase = true) && !cloudResultStr.contains("VENDI", ignoreCase = true))) {

                        throw Exception("Errore API: $cloudResultStr")
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

                Toast.makeText(this@CameraActivity, "Analisi completata", Toast.LENGTH_SHORT).show()

                val intent = Intent(this@CameraActivity, AnalysisResultActivity::class.java)
                intent.putExtra("IMAGE_PATH", savedFile.absolutePath)
                intent.putExtra("PREDICTION_LABEL", labelResult)
                intent.putExtra("PREDICTION_CONFIDENCE", confidenceResult)
                intent.putExtra("MODEL_NAME", selectedModel.displayName)

                startActivity(intent)

            } catch (e: Exception) {
                Log.e("AWS_ERROR", "Errore: ${e.message}")
                Toast.makeText(this@CameraActivity, "Modello non disponibile.", Toast.LENGTH_SHORT).show()
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