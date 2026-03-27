package com.batz02.tradevisionai.network

import android.util.Base64
import com.batz02.tradevisionai.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class TickerAnalysisResult(
    val label: String,
    val confidence: Float,
    val imageBytes: ByteArray
)

class AwsApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun analyzeImageOnCloud(imageFile: File, baseUrl: String, modelId: String): String {
        return try {
            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.name, requestFile)
                .build()

            val urlWithParam = "$baseUrl/predict?model_id=$modelId"

            val request = Request.Builder()
                .url(urlWithParam)
                .addHeader("X-API-KEY", BuildConfig.AWS_API_KEY)
                .addHeader("accept", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val prediction = json.getString("prediction")
                val confidence = json.getDouble("confidence")
                "$prediction\n($confidence%)"
            } else {
                "Errore ${response.code}: ${responseBody ?: "Nessun dettaglio"}"
            }
        } catch (e: Exception) {
            "Errore di rete: ${e.message}"
        }
    }

    fun analyzeTickerOnCloud(baseUrl: String, ticker: String, modelId: String): TickerAnalysisResult {
        val url = "$baseUrl/analyze_ticker"

        val jsonRequest = JSONObject().apply {
            put("ticker", ticker)
            put("model_id", modelId)
        }

        val requestBody = jsonRequest.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .addHeader("X-API-KEY", BuildConfig.AWS_API_KEY)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val responseData = response.body?.string() ?: throw Exception("Risposta server vuota")
            val jsonResponse = JSONObject(responseData)

            val label = jsonResponse.getString("label")
            val confidence = jsonResponse.getDouble("confidence").toFloat()
            val base64Image = jsonResponse.getString("image_base64")

            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)

            return TickerAnalysisResult(label, confidence, imageBytes)
        } else {
            val errorBody = response.body?.string() ?: "Nessun dettaglio"
            throw Exception("Errore Server: ${response.code} - $errorBody")
        }
    }
}