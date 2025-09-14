package com.example.personalfinancetrackerapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import com.example.personalfinancetrackerapp.AddTransactionActivity
import com.example.personalfinancetrackerapp.MainActivity
import com.example.personalfinancetrackerapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
import java.text.NumberFormat
import java.util.Currency

class BudgetActivity : AppCompatActivity() {
    private lateinit var transactions: List<Transaction>
    private var currentView = "Monthly"
    private lateinit var toolbar: Toolbar
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        toolbar = findViewById(R.id.toolbar)
        val monthlyButton = findViewById<Button>(R.id.monthlyButton)
        val weeklyButton = findViewById<Button>(R.id.weeklyButton)
        val yearlyButton = findViewById<Button>(R.id.yearlyButton)
        val budgetInput = findViewById<EditText>(R.id.budgetInput)
        val saveButton = findViewById<Button>(R.id.saveBudgetButton)
        val currentBudget = findViewById<TextView>(R.id.currentBudget)
        val spentAmount = findViewById<TextView>(R.id.spentAmount)
        val overallBudgetProgress = findViewById<ProgressBar>(R.id.overallBudgetProgress)
        val totalBudgetProgress = findViewById<ProgressBar>(R.id.totalBudgetProgress)
        val totalBudgetText = findViewById<TextView>(R.id.totalBudgetText)
        val foodBudgetText = findViewById<TextView>(R.id.foodBudgetText)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val addTransactionFab = findViewById<FloatingActionButton>(R.id.addTransactionFab)

        // Update hint with current currency
        val settingsPrefs = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        val selectedCurrency = settingsPrefs.getString("currency", "USD ($)") ?: "USD ($)"
        val currencySymbol = when (selectedCurrency) {
            "EUR (€)" -> "€"
            "GBP (£)" -> "£"
            "LKR (රු)" -> "රු"
            else -> "$"
        }
        budgetInput.hint = "Set Budget ($currencySymbol)"

        transactions = loadTransactions()

        updateView(currentBudget, spentAmount, overallBudgetProgress, totalBudgetProgress, totalBudgetText, foodBudgetText)

        monthlyButton.setOnClickListener {
            currentView = "Monthly"
            updateView(currentBudget, spentAmount, overallBudgetProgress, totalBudgetProgress, totalBudgetText, foodBudgetText)
        }

        weeklyButton.setOnClickListener {
            currentView = "Weekly"
            updateView(currentBudget, spentAmount, overallBudgetProgress, totalBudgetProgress, totalBudgetText, foodBudgetText)
        }

        yearlyButton.setOnClickListener {
            currentView = "Yearly"
            updateView(currentBudget, spentAmount, overallBudgetProgress, totalBudgetProgress, totalBudgetText, foodBudgetText)
        }

        saveButton.setOnClickListener {
            val budget = budgetInput.text.toString().toFloatOrNull()
            if (budget == null) {
                return@setOnClickListener
            }
            val sharedPreferences = getSharedPreferences("BudgetPrefs", MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                when (currentView) {
                    "Monthly" -> putFloat("monthlyBudget", budget)
                    "Weekly" -> putFloat("weeklyBudget", budget)
                    "Yearly" -> putFloat("yearlyBudget", budget)
                }
                apply()
            }
            updateView(currentBudget, spentAmount, overallBudgetProgress, totalBudgetProgress, totalBudgetText, foodBudgetText)
        }

        setupBottomNavigation(bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_budget

        addTransactionFab.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    private fun setupBottomNavigation(bottomNavigation: BottomNavigationView) {
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.bottom_navigation_menu)

        bottomNavigation.selectedItemId = R.id.navigation_budget

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
                R.id.navigation_budget -> true
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

    private fun updateView(
        currentBudget: TextView,
        spentAmount: TextView,
        overallBudgetProgress: ProgressBar,
        totalBudgetProgress: ProgressBar,
        totalBudgetText: TextView,
        foodBudgetText: TextView
    ) {
        toolbar.title = "$currentView Budget"

        val sharedPreferences = getSharedPreferences("BudgetPrefs", MODE_PRIVATE)
        val settingsPrefs = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        val selectedCurrency = settingsPrefs.getString("currency", "USD ($)") ?: "USD ($)"
        
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

        val budget = when (currentView) {
            "Monthly" -> sharedPreferences.getFloat("monthlyBudget", 0f)
            "Weekly" -> sharedPreferences.getFloat("weeklyBudget", 0f)
            "Yearly" -> sharedPreferences.getFloat("yearlyBudget", 0f)
            else -> 0f
        }

        val totalExpenses = calculateExpenses()
        val percentage = if (budget > 0) (totalExpenses / budget * 100).toInt() else 0

        currentBudget.text = "Current Budget: ${currencyFormat.format(budget)}"
        spentAmount.text = "Spent: ${currencyFormat.format(totalExpenses)} ($percentage%)"
        totalBudgetText.text = currencyFormat.format(budget)
        foodBudgetText.text = currencyFormat.format(budget * 0.25) // Example: 25% of total budget for food
        
        overallBudgetProgress.progress = percentage
        totalBudgetProgress.progress = percentage

        if (currentView == "Monthly") {
            checkBudget(budget, totalExpenses)
        }
    }

    private fun calculateExpenses(): Float {
        val currentDate = Date()
        return when (currentView) {
            "Monthly" -> {
                val currentMonth = SimpleDateFormat("MM", Locale.getDefault()).format(currentDate)
                transactions.filter {
                    it.type == TransactionType.EXPENSE && 
                    SimpleDateFormat("MM", Locale.getDefault()).format(it.date) == currentMonth
                }.sumOf { it.amount }.toFloat()
            }

            "Weekly" -> {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val startOfWeek = calendar.time
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val endOfWeek = calendar.time
                transactions.filter {
                    it.type == TransactionType.EXPENSE && 
                    it.date.after(startOfWeek) && 
                    it.date.before(endOfWeek)
                }.sumOf { it.amount }.toFloat()
            }

            "Yearly" -> {
                val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(currentDate)
                transactions.filter {
                    it.type == TransactionType.EXPENSE && 
                    SimpleDateFormat("yyyy", Locale.getDefault()).format(it.date) == currentYear
                }.sumOf { it.amount }.toFloat()
            }

            else -> 0f
        }
    }

    private fun checkBudget(budget: Float, totalExpenses: Float) {
        if (budget > 0) {
            if (totalExpenses >= budget) {
                sendNotification(getString(R.string.budget_exceeded))
            } else if (totalExpenses >= budget * 0.9) {
                sendNotification(getString(R.string.budget_warning))
            }
        }
    }

    private fun sendNotification(message: String) {
        val channelId = "budget_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Budget Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Budget Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
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