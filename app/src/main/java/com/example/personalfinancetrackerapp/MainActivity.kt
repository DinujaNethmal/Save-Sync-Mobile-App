package com.example.personalfinancetrackerapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var transactionAdapter: TransactionAdapter
    private var transactions = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val totalBalanceText = findViewById<TextView>(R.id.totalBalanceText)
        val totalIncomeText = findViewById<TextView>(R.id.totalIncomeText)
        val totalExpenseText = findViewById<TextView>(R.id.totalExpenseText)
        val transactionRecyclerView = findViewById<RecyclerView>(R.id.transactionRecyclerView)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val addTransactionFab = findViewById<FloatingActionButton>(R.id.addTransactionFab)

        // Setup RecyclerView
        transactions.addAll(loadTransactions())
        transactionAdapter = TransactionAdapter(
            transactions = transactions,
            onEditClick = { transaction ->
                val intent = Intent(this, AddTransactionActivity::class.java).apply {
                    putExtra("transaction_id", transaction.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { transaction ->
                transactions.removeAll { it.id == transaction.id }
                saveTransactions()
                transactionAdapter.notifyDataSetChanged()
                // Update balance information after deletion
                updateBalanceInfo(totalBalanceText, totalIncomeText, totalExpenseText)
            }
        )
        transactionRecyclerView.layoutManager = LinearLayoutManager(this)
        transactionRecyclerView.adapter = transactionAdapter

        // Update balance information
        updateBalanceInfo(totalBalanceText, totalIncomeText, totalExpenseText)

        // Setup bottom navigation
        setupBottomNavigation(bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_home

        // Setup FAB
        addTransactionFab.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload transactions and update UI when returning to this activity
        transactions.clear()
        transactions.addAll(loadTransactions())
        transactionAdapter.notifyDataSetChanged()
        
        val totalBalanceText = findViewById<TextView>(R.id.totalBalanceText)
        val totalIncomeText = findViewById<TextView>(R.id.totalIncomeText)
        val totalExpenseText = findViewById<TextView>(R.id.totalExpenseText)
        updateBalanceInfo(totalBalanceText, totalIncomeText, totalExpenseText)
    }

    private fun updateBalanceInfo(
        totalBalanceText: TextView,
        totalIncomeText: TextView,
        totalExpenseText: TextView
    ) {
        var totalIncome = 0.0
        var totalExpense = 0.0

        transactions.forEach { transaction ->
            if (transaction.type == TransactionType.INCOME) {
                totalIncome += transaction.amount
            } else {
                totalExpense += transaction.amount
            }
        }

        val totalBalance = totalIncome - totalExpense
        val currencyFormat = NumberFormat.getCurrencyInstance()
        
        // Get the selected currency from settings
        val sharedPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        val selectedCurrency = sharedPreferences.getString("currency", "USD ($)") ?: "USD ($)"
        
        // Set the currency symbol based on selection
        val currencySymbol = when (selectedCurrency) {
            "EUR (€)" -> "€"
            "GBP (£)" -> "£"
            "LKR (රු)" -> "රු"
            else -> "$"
        }
        currencyFormat.currency = Currency.getInstance(
            when (selectedCurrency) {
                "EUR (€)" -> "EUR"
                "GBP (£)" -> "GBP"
                "LKR (රු)" -> "LKR"
                else -> "USD"
            }
        )

        totalBalanceText.text = currencyFormat.format(totalBalance)
        totalIncomeText.text = currencyFormat.format(totalIncome)
        totalExpenseText.text = currencyFormat.format(totalExpense)
    }

    private fun setupBottomNavigation(bottomNavigation: BottomNavigationView) {
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.bottom_navigation_menu)

        bottomNavigation.selectedItemId = R.id.navigation_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_charts -> {
                    startActivity(Intent(this, ChartsActivity::class.java))
                    true
                }
                R.id.navigation_budget -> {
                    startActivity(Intent(this, BudgetActivity::class.java))
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadTransactions(): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        try {
            val file = File(filesDir, "transactions.json")
            if (file.exists()) {
                file.readLines().forEach {
                    val parts = it.split(",")
                    if (parts.size == 5) {
                        transactions.add(Transaction(
                            id = parts[0].toLong(),
                            amount = parts[2].toDouble(),
                            category = parts[1],
                            date = Date(parts[4].toLong()),
                            type = if (parts[3] == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
        return transactions
    }

    private fun saveTransactions() {
        try {
            val file = File(filesDir, "transactions.json")
            file.writeText(transactions.joinToString("\n") { transaction ->
                "${transaction.id},${transaction.category},${transaction.amount},${transaction.type},${transaction.date.time}"
            })
        } catch (e: Exception) {
            // Handle error
        }
    }
}