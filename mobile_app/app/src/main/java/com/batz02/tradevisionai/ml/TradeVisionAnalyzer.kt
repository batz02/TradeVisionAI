package com.batz02.tradevisionai.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TradeVisionAnalyzer(private val context: Context) {

    private val IMAGE_SIZE = 224

    private val NUM_CLASSES = 2

    private val labels = listOf("Compra", "Vendi")

    fun analyzeGraph(bitmap: Bitmap, modelFilename: String): String {
        var interpreter: Interpreter? = null
        try {
            val options = Interpreter.Options()
            options.numThreads = 4
            interpreter = Interpreter(loadModelFile(modelFilename), options)

            val startTime = System.currentTimeMillis()

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true)
            val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

            val output = Array(1) { FloatArray(NUM_CLASSES) }

            interpreter.run(byteBuffer, output)

            val probabilities = output[0]
            var maxIndex = -1
            var maxProb = -1.0f

            for (i in probabilities.indices) {
                val prob = probabilities[i] * 100
                if (prob > maxProb) {
                    maxProb = prob
                    maxIndex = i
                }
            }

            val inferenceTime = System.currentTimeMillis() - startTime
            val predictedLabel = if (maxIndex in labels.indices) labels[maxIndex] else "Sconosciuto"

            return "$predictedLabel\n(Affidabilità: ${String.format("%.1f", maxProb)}% | ${inferenceTime}ms)"

        } catch (e: Exception) {
            Log.e("ML_TEST", "Errore in inferenza con $modelFilename", e)
            return "Errore AI: ${e.message}"
        } finally {
            interpreter?.close()
        }
    }

    private fun loadModelFile(modelName: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until IMAGE_SIZE) {
            for (j in 0 until IMAGE_SIZE) {
                val value = intValues[pixel++]

                byteBuffer.putFloat(((value shr 16 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value shr 8 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((value and 0xFF) / 255.0f))
            }
        }
        return byteBuffer
    }
}