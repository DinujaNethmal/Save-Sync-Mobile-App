package com.example.personalfinancetrackerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryText: TextView = view.findViewById(R.id.categoryText)
        val amountText: TextView = view.findViewById(R.id.amountText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val editButton: ImageButton = view.findViewById(R.id.editButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val context = holder.itemView.context
        
        // Get the selected currency from settings
        val sharedPreferences = context.getSharedPreferences("SettingsPrefs", android.content.Context.MODE_PRIVATE)
        val selectedCurrency = sharedPreferences.getString("currency", "USD ($)") ?: "USD ($)"
        
        // Configure currency format
        val currencyFormat = NumberFormat.getCurrencyInstance()
        currencyFormat.currency = Currency.getInstance(
            when (selectedCurrency) {
                "EUR (€)" -> "EUR"
                "GBP (£)" -> "GBP"
                "LKR (රු)" -> "LKR"
                else -> "USD"
            }
        )

        holder.categoryText.text = transaction.category
        holder.amountText.text = currencyFormat.format(transaction.amount)
        holder.amountText.setTextColor(
            if (transaction.type == TransactionType.EXPENSE) 
                holder.itemView.context.getColor(R.color.error)
            else 
                holder.itemView.context.getColor(R.color.success)
        )
        holder.dateText.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            .format(transaction.date)

        holder.editButton.setOnClickListener { onEditClick(transaction) }
        holder.deleteButton.setOnClickListener { onDeleteClick(transaction) }
    }

    override fun getItemCount() = transactions.size
}