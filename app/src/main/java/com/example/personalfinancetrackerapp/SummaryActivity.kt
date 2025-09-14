package com.example.personalfinancetrackerapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class SummaryActivity : AppCompatActivity() {
    private lateinit var categoryAdapter: CategoryAdapter
    private val categories = mutableListOf<Category>()
    private var currentDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val monthTitle = findViewById<TextView>(R.id.monthTitle)
        val prevMonthButton = findViewById<ImageButton>(R.id.prevMonthButton)
        val nextMonthButton = findViewById<ImageButton>(R.id.nextMonthButton)
        val totalIncomeText = findViewById<TextView>(R.id.totalIncomeText)
        val totalExpenseText = findViewById<TextView>(R.id.totalExpenseText)
        val totalBalanceText = findViewById<TextView>(R.id.totalBalanceText)
        val categoryRecyclerView = findViewById<RecyclerView>(R.id.categoryRecyclerView)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val addTransactionFab = findViewById<FloatingActionButton>(R.id.addTransactionFab)

        // Setup RecyclerView
        categoryAdapter = CategoryAdapter(categories)
        categoryRecyclerView.layoutManager = LinearLayoutManager(this)
        categoryRecyclerView.adapter = categoryAdapter

        // Update month title
        updateMonthTitle(monthTitle)

        // Setup month navigation
        prevMonthButton.setOnClickListener {
            currentDate.add(Calendar.MONTH, -1)
            updateMonthTitle(monthTitle)
            updateSummary(totalIncomeText, totalExpenseText, totalBalanceText)
        }

        nextMonthButton.setOnClickListener {
            currentDate.add(Calendar.MONTH, 1)
            updateMonthTitle(monthTitle)
            updateSummary(totalIncomeText, totalExpenseText, totalBalanceText)
        }

        // Initial summary update
        updateSummary(totalIncomeText, totalExpenseText, totalBalanceText)

        // Setup bottom navigation
        setupBottomNavigation(bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_profile

        // Setup FAB
        addTransactionFab.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val totalIncomeText = findViewById<TextView>(R.id.totalIncomeText)
        val totalExpenseText = findViewById<TextView>(R.id.totalExpenseText)
        val totalBalanceText = findViewById<TextView>(R.id.totalBalanceText)
        updateSummary(totalIncomeText, totalExpenseText, totalBalanceText)
    }

    private fun updateMonthTitle(monthTitle: TextView) {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthTitle.text = dateFormat.format(currentDate.time)
    }

    private fun updateSummary(
        totalIncomeText: TextView,
        totalExpenseText: TextView,
        totalBalanceText: TextView
    ) {
        val transactions = loadTransactions()
        var totalIncome = 0.0
        var totalExpense = 0.0
        val categoryMap = mutableMapOf<String, Double>()

        // Filter transactions for current month
        val currentMonth = currentDate.get(Calendar.MONTH)
        val currentYear = currentDate.get(Calendar.YEAR)
        val filteredTransactions = transactions.filter { transaction ->
            val calendar = Calendar.getInstance()
            calendar.time = transaction.date
            calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
        }

        // Calculate totals and category breakdown
        filteredTransactions.forEach { transaction ->
            if (transaction.type == TransactionType.INCOME) {
                totalIncome += transaction.amount
            } else {
                totalExpense += transaction.amount
                categoryMap[transaction.category] = (categoryMap[transaction.category] ?: 0.0) + transaction.amount
            }
        }

        val totalBalance = totalIncome - totalExpense
        val currencyFormat = NumberFormat.getCurrencyInstance()

        totalIncomeText.text = currencyFormat.format(totalIncome)
        totalExpenseText.text = currencyFormat.format(totalExpense)
        totalBalanceText.text = currencyFormat.format(totalBalance)

        // Update category breakdown
        categories.clear()
        categoryMap.forEach { (category, amount) ->
            categories.add(Category(category, amount))
        }
        categories.sortByDescending { it.amount }
        categoryAdapter.notifyDataSetChanged()
    }

    private fun setupBottomNavigation(bottomNavigation: BottomNavigationView) {
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.bottom_navigation_menu)

        bottomNavigation.selectedItemId = R.id.navigation_profile

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_charts -> {
                    startActivity(Intent(this, ChartsActivity::class.java))
                    true
                }
                R.id.navigation_budget -> {
                    startActivity(Intent(this, BudgetActivity::class.java))
                    true
                }
                R.id.navigation_profile -> true
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadTransactions(): List<Transaction> {
        val file = java.io.File(filesDir, "transactions.json")
        if (!file.exists()) return emptyList()

        return try {
            file.readLines().mapNotNull { line ->
                val parts = line.split(",")
                if (parts.size == 5) {
                    Transaction(
                        id = parts[0].toLong(),
                        amount = parts[2].toDouble(),
                        category = parts[1],
                        date = Date(parts[4].toLong()),
                        type = if (parts[3] == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}