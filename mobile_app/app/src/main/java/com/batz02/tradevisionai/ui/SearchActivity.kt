package com.batz02.tradevisionai.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.batz02.tradevisionai.R
import com.batz02.tradevisionai.network.StockApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val etSearchQuery = findViewById<EditText>(R.id.etSearchQuery)
        val btnDoSearch = findViewById<Button>(R.id.btnDoSearch)
        val listViewResults = findViewById<ListView>(R.id.listViewResults)

        btnDoSearch.setOnClickListener {
            val query = etSearchQuery.text.toString()
            if (query.isNotEmpty()) {
                lifecycleScope.launch {
                    val results = withContext(Dispatchers.IO) {
                        StockApiClient().searchStock(query)
                    }

                    if (results.isNotEmpty()) {

                        val displayList = results.map { "${it.ticker} - ${it.name}" }

                        val adapter = ArrayAdapter(this@SearchActivity, R.layout.item_search_result, displayList)
                        listViewResults.adapter = adapter

                        listViewResults.setOnItemClickListener { _, _, position, _ ->
                            val selectedItem = results[position]
                            val intent = Intent(this@SearchActivity, DetailActivity::class.java)

                            intent.putExtra("TICKER", selectedItem.ticker)
                            intent.putExtra("COMPANY_NAME", selectedItem.name)
                            startActivity(intent)
                        }
                    } else {
                        Toast.makeText(this@SearchActivity, "Nessun risultato", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}