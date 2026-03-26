package com.batz02.tradevisionai.network

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class AwsApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun analyzeImageOnCloud(imageFile: File, apiUrl: String, modelId: String): String {
        return try {
            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.name, requestFile)
                .build()

            val urlWithParam = "$apiUrl?model_id=$modelId"

            val request = Request.Builder()
                .url(urlWithParam)
                .addHeader("X-API-KEY", "API")
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
}