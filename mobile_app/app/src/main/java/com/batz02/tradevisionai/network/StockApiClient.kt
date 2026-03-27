package com.batz02.tradevisionai.network

import com.batz02.tradevisionai.BuildConfig
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class SearchResult(val ticker: String, val name: String)
data class NewsItem(val headline: String, val url: String)

class StockApiClient {

    private val client = OkHttpClient()
    private val API_KEY = BuildConfig.FINNHUB_API_KEY

    fun getStockPrice(ticker: String): String {
        val url = "https://finnhub.io/api/v1/quote?symbol=$ticker&token=$API_KEY"
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string()
                if (jsonString != null) {
                    val jsonObject = JSONObject(jsonString)
                    return jsonObject.getDouble("c").toString()
                }
            }
        } catch (e: Exception) {
            Log.e("NETWORK_TEST", "Eccezione: ${e.message}")
        }
        return "Errore"
    }

    fun searchStock(query: String): List<SearchResult> {
        val url = "https://finnhub.io/api/v1/search?q=$query&token=$API_KEY"
        val request = Request.Builder().url(url).build()
        val results = mutableListOf<SearchResult>()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string()
                if (jsonString != null) {
                    val jsonObject = JSONObject(jsonString)
                    val count = jsonObject.getInt("count")

                    if (count > 0) {
                        val resultJSONArray = jsonObject.getJSONArray("result")

                        val maxResults = if (resultJSONArray.length() > 10) 10 else resultJSONArray.length()

                        for (i in 0 until maxResults) {
                            val item = resultJSONArray.getJSONObject(i)
                            val ticker = item.getString("displaySymbol")
                            val name = item.getString("description")

                            results.add(SearchResult(ticker, name))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NETWORK_TEST", "Errore ricerca: ${e.message}")
        }

        return results
    }

    fun getCompanyNews(ticker: String): List<NewsItem> {
        val results = mutableListOf<NewsItem>()
        try {
            val today = java.time.LocalDate.now().toString()
            val lastWeek = java.time.LocalDate.now().minusDays(7).toString()

            val url = "https://finnhub.io/api/v1/company-news?symbol=$ticker&from=$lastWeek&to=$today&token=$API_KEY"
            val request = Request.Builder().url(url).build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string()
                if (jsonString != null) {
                    val jsonArray = org.json.JSONArray(jsonString)
                    val maxNews = if (jsonArray.length() > 5) 5 else jsonArray.length()

                    for (i in 0 until maxNews) {
                        val item = jsonArray.getJSONObject(i)
                        val headline = item.getString("headline")
                        val newsUrl = item.getString("url")
                        results.add(NewsItem(headline, newsUrl))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NETWORK_TEST", "Errore News: ${e.message}")
        }
        return results
    }
}