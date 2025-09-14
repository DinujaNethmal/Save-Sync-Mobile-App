package com.example.personalfinancetrackerapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class ChartsActivity : AppCompatActivity() {
    private lateinit var transactions: List<Transaction>
    private var currentView = "Monthly"
    private lateinit var toolbar: Toolbar
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var lineChart: LineChart
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    private val TAG = "ChartsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charts)

        toolbar = findViewById(R.id.toolbar)
        val monthlyButton = findViewById<Button>(R.id.monthlyButton)
        val weeklyButton = findViewById<Button>(R.id.weeklyButton)
        val yearlyButton = findViewById<Button>(R.id.yearlyButton)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        lineChart = findViewById(R.id.lineChart)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val addTransactionFab = findViewById<FloatingActionButton>(R.id.addTransactionFab)

        // Load transactions from file
        transactions = loadTransactions()
        Log.d(TAG, "Loaded ${transactions.size} transactions")
        
        setupCharts()
        updateCharts()

        monthlyButton.setOnClickListener {
            currentView = "Monthly"
            updateCharts()
        }

        weeklyButton.setOnClickListener {
            currentView = "Weekly"
            updateCharts()
        }

        yearlyButton.setOnClickListener {
            currentView = "Yearly"
            updateCharts()
        }

        setupBottomNavigation(bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_charts

        addTransactionFab.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    private fun loadTransactions(): List<Transaction> {
        val file = java.io.File(filesDir, "transactions.json")
        if (!file.exists()) {
            Log.d(TAG, "Transactions file does not exist")
            return emptyList()
        }

        return try {
            val transactions = file.readLines().mapNotNull { line ->
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
            Log.d(TAG, "Successfully loaded ${transactions.size} transactions from file")
            transactions
        } catch (e: Exception) {
            Log.e(TAG, "Error loading transactions from file", e)
            emptyList()
        }
    }

    private fun setupCharts() {
        // Setup Pie Chart
        pieChart.apply {
            description.isEnabled = false
            setDrawHoleEnabled(true)
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400)
            legend.isEnabled = true
            legend.textSize = 12f
            legend.textColor = getColor(R.color.text_primary)
        }

        // Setup Bar Chart
        barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            isHighlightFullBarEnabled = false
            animateY(1000)
            legend.isEnabled = true
            legend.textSize = 12f
            legend.textColor = getColor(R.color.text_primary)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = getColor(R.color.text_primary)
                textSize = 12f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                textColor = getColor(R.color.text_primary)
                textSize = 12f
            }
            
            axisRight.apply {
                isEnabled = false
            }
        }

        // Setup Line Chart
        lineChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            animateY(1000)
            legend.isEnabled = true
            legend.textSize = 12f
            legend.textColor = getColor(R.color.text_primary)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = getColor(R.color.text_primary)
                textSize = 12f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                textColor = getColor(R.color.text_primary)
                textSize = 12f
            }
            
            axisRight.apply {
                isEnabled = false
            }
        }
    }

    private fun updateCharts() {
        toolbar.title = "$currentView Expense Analysis"
        updatePieChart()
        updateBarChart()
        updateLineChart()
    }

    private fun updatePieChart() {
        val filteredTransactions = filterTransactions()
        Log.d(TAG, "Filtered ${filteredTransactions.size} transactions for pie chart")
        
        val categoryMap = filteredTransactions.groupBy { it.category }
        val entries = categoryMap.map { PieEntry(it.value.sumOf { t -> t.amount }.toFloat(), it.key) }

        if (entries.isEmpty()) {
            Log.d(TAG, "No data available for pie chart")
            pieChart.clear()
            pieChart.setNoDataText("No expense data available")
            pieChart.setNoDataTextColor(getColor(R.color.text_primary))
            return
        }

        val dataSet = PieDataSet(entries, "Expenses by Category")
        dataSet.colors = listOf(
            getColor(R.color.primary_green_light),
            getColor(R.color.primary_green_dark),
            getColor(R.color.accent_brown),
            getColor(R.color.primary_blue),
            getColor(R.color.primary_orange)
        )
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueFormatter = PercentFormatter(pieChart)

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate()
    }

    private fun updateBarChart() {
        val filteredTransactions = filterTransactions()
        Log.d(TAG, "Filtered ${filteredTransactions.size} transactions for bar chart")
        
        val categoryMap = filteredTransactions.groupBy { it.category }
        val entries = categoryMap.map { BarEntry(categoryMap.keys.indexOf(it.key).toFloat(), it.value.sumOf { t -> t.amount }.toFloat()) }

        if (entries.isEmpty()) {
            Log.d(TAG, "No data available for bar chart")
            barChart.clear()
            barChart.setNoDataText("No expense data available")
            barChart.setNoDataTextColor(getColor(R.color.text_primary))
            return
        }

        val dataSet = BarDataSet(entries, "Expenses by Category")
        dataSet.color = getColor(R.color.primary_green_light)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = getColor(R.color.text_primary)

        val data = BarData(dataSet)
        barChart.data = data
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(categoryMap.keys.toList())
        barChart.invalidate()
    }

    private fun updateLineChart() {
        val filteredTransactions = filterTransactions()
        Log.d(TAG, "Filtered ${filteredTransactions.size} transactions for line chart")
        
        val dailyExpenses = filteredTransactions.groupBy { 
            val calendar = Calendar.getInstance()
            calendar.time = it.date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.time
        }

        val entries = dailyExpenses.map { 
            Entry(dailyExpenses.keys.indexOf(it.key).toFloat(), it.value.sumOf { t -> t.amount }.toFloat())
        }.sortedBy { it.x }

        if (entries.isEmpty()) {
            Log.d(TAG, "No data available for line chart")
            lineChart.clear()
            lineChart.setNoDataText("No expense data available")
            lineChart.setNoDataTextColor(getColor(R.color.text_primary))
            return
        }

        val dataSet = LineDataSet(entries, "Daily Expenses")
        dataSet.color = getColor(R.color.primary_green_light)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = getColor(R.color.text_primary)
        dataSet.setDrawCircles(true)
        dataSet.setDrawValues(true)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setCircleColor(getColor(R.color.primary_green_light))

        val data = LineData(dataSet)
        lineChart.data = data
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(dailyExpenses.keys.map { dateFormat.format(it) }.toTypedArray())
        lineChart.invalidate()
    }

    private fun filterTransactions(): List<Transaction> {
        val currentDate = Date()
        val filtered = when (currentView) {
            "Monthly" -> {
                val currentMonth = SimpleDateFormat("MM", Locale.getDefault()).format(currentDate)
                transactions.filter {
                    it.type == TransactionType.EXPENSE && 
                    SimpleDateFormat("MM", Locale.getDefault()).format(it.date) == currentMonth
                }
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
                }
            }
            "Yearly" -> {
                val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(currentDate)
                transactions.filter {
                    it.type == TransactionType.EXPENSE && 
                    SimpleDateFormat("yyyy", Locale.getDefault()).format(it.date) == currentYear
                }
            }
            else -> emptyList()
        }
        Log.d(TAG, "Filtered ${filtered.size} transactions for $currentView view")
        return filtered
    }

    private fun setupBottomNavigation(bottomNavigation: BottomNavigationView) {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_charts -> {
                    // Already in charts
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

    override fun onResume() {
        super.onResume()
        // Reload transactions and update charts when returning to this activity
        transactions = loadTransactions()
        updateCharts()
    }
}