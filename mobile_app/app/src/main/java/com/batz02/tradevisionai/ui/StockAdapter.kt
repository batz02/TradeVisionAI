package com.batz02.tradevisionai.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.batz02.tradevisionai.R
import com.batz02.tradevisionai.db.StockEntity

class StockAdapter(
    private var stockList: List<StockEntity>,
    private val onItemClick: (StockEntity) -> Unit,
    private val onDeleteClick: (StockEntity) -> Unit
) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTicker: TextView = itemView.findViewById(R.id.tvItemTicker)
        val tvPrice: TextView = itemView.findViewById(R.id.tvItemPrice)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteSingle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stock, parent, false)
        return StockViewHolder(view)
    }


    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val stock = stockList[position]
        holder.tvTicker.text = stock.ticker


        holder.tvPrice.text = "${stock.companyName}  |  ${stock.price} ${stock.currency}"

        holder.itemView.setOnClickListener { onItemClick(stock) }
        holder.btnDelete.setOnClickListener { onDeleteClick(stock) }
    }



    override fun getItemCount(): Int = stockList.size

    fun updateData(newList: List<StockEntity>) {
        stockList = newList
        notifyDataSetChanged()
    }
}