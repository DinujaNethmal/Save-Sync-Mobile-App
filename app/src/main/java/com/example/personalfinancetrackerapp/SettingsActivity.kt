package com.example.personalfinancetrackerapp

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.example.personalfinancetrackerapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.text.NumberFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var darkModeSwitch: SwitchCompat
    private lateinit var currencySpinner: Spinner
    private lateinit var transactionAlertsSwitch: SwitchCompat
    private lateinit var budgetWarningsSwitch: SwitchCompat
    private lateinit var exportButton: Button
    private lateinit var restoreButton: Button
    private var transactions: List<Transaction> = emptyList()  // Initialize with empty list

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        try {
            // Request storage permissions for Android 10 and below
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    1
                )
            }

            sharedPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)

            val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
            val addTransactionFab = findViewById<FloatingActionButton>(R.id.addTransactionFab)

            // Initialize views
            darkModeSwitch = findViewById(R.id.darkModeSwitch)
            currencySpinner = findViewById(R.id.currencySpinner)
            transactionAlertsSwitch = findViewById(R.id.transactionAlertsSwitch)
            budgetWarningsSwitch = findViewById(R.id.budgetWarningsSwitch)
            exportButton = findViewById(R.id.exportButton)
            restoreButton = findViewById(R.id.restoreButton)

            // Load saved settings first
            loadSettings()

            // Setup listeners
            setupListeners()

            // Setup bottom navigation
            setupBottomNavigation(bottomNavigation)

            // Setup currency spinner
            setupCurrencySpinner()

            // Setup dark mode
            setupDarkMode()

            // Try to load transactions
            try {
                transactions = loadTransactions()
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to load transactions", Toast.LENGTH_SHORT).show()
                transactions = emptyList()
            }

            // Setup export and restore buttons
            exportButton.setOnClickListener {
                if (transactions.isNotEmpty()) {
                    saveTransactions(transactions)
                } else {
                    Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show()
                }
            }

            restoreButton.setOnClickListener {
                try {
                    restoreTransactions()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error restoring: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            // Setup FAB
            addTransactionFab.setOnClickListener {
                startActivity(Intent(this, AddTransactionActivity::class.java))
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing settings: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupDarkMode() {
        darkModeSwitch.isChecked = sharedPreferences.getBoolean("dark_mode", false)
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            with(sharedPreferences.edit()) {
                putBoolean("dark_mode", isChecked)
                apply()
            }
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun setupCurrencySpinner() {
        val currencies = arrayOf("USD ($)", "EUR (€)", "GBP (£)", "LKR (රු)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, currencies)
        currencySpinner.adapter = adapter

        val savedCurrency = sharedPreferences.getString("currency", "USD ($)")
        val currencyIndex = currencies.indexOf(savedCurrency)
        if (currencyIndex != -1) {
            currencySpinner.setSelection(currencyIndex)
        }

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                with(sharedPreferences.edit()) {
                    putString("currency", currencies[position])
                    apply()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadTransactions(): List<Transaction> {
        try {
            val file = File(filesDir, "transactions.json")
            if (!file.exists()) return emptyList()

            return file.readLines().mapNotNull { line ->
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
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun saveTransactions(transactions: List<Transaction>) {
        try {
            // Save to internal storage
            val internalFile = File(filesDir, "transactions.json")
            internalFile.writeText(transactions.joinToString("\n") { transaction ->
                "${transaction.id},${transaction.category},${transaction.amount},${transaction.type},${transaction.date.time}"
            })

            // Save to external storage for visibility
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val externalFile = File(downloadsDir, "finance_tracker_backup.json")
            externalFile.writeText(internalFile.readText())
            
            Toast.makeText(this, "Data exported to Downloads folder: finance_tracker_backup.json", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreTransactions() {
        try {
            // Try to restore from external storage first
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val externalFile = File(downloadsDir, "finance_tracker_backup.json")
            
            if (!externalFile.exists()) {
                Toast.makeText(this, "No backup file found in Downloads folder", Toast.LENGTH_SHORT).show()
                return
            }

            // Read transactions from backup
            val restoredTransactions = externalFile.readLines().mapNotNull { line ->
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

            if (restoredTransactions.isNotEmpty()) {
                // Save restored transactions to internal storage
                val internalFile = File(filesDir, "transactions.json")
                internalFile.writeText(externalFile.readText())
                
                // Restart MainActivity to refresh the data
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
                Toast.makeText(this, R.string.restore_success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No valid transactions found in backup file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error restoring: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSettings() {
        try {
            // Load notification settings
            transactionAlertsSwitch.isChecked = sharedPreferences.getBoolean("transaction_alerts", true)
            budgetWarningsSwitch.isChecked = sharedPreferences.getBoolean("budget_warnings", true)
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        try {
            transactionAlertsSwitch.setOnCheckedChangeListener { _, isChecked ->
                with(sharedPreferences.edit()) {
                    putBoolean("transaction_alerts", isChecked)
                    apply()
                }
            }

            budgetWarningsSwitch.setOnCheckedChangeListener { _, isChecked ->
                with(sharedPreferences.edit()) {
                    putBoolean("budget_warnings", isChecked)
                    apply()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up listeners: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation(bottomNavigation: BottomNavigationView) {
        try {
            bottomNavigation.menu.clear()
            bottomNavigation.inflateMenu(R.menu.bottom_navigation_menu)

            bottomNavigation.selectedItemId = R.id.navigation_settings

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
                    R.id.navigation_profile -> {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        true
                    }
                    R.id.navigation_settings -> true
                    else -> false
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up navigation: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private val currencies = arrayOf(
            "USD ($)",
            "EUR (€)",
            "GBP (£)",
            "JPY (¥)",
            "AUD (A$)",
            "CAD (C$)",
            "CHF (Fr)",
            "CNY (¥)",
            "INR (₹)",
            "LKR (රු)"
        )
    }
}