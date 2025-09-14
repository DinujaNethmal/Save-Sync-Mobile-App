package com.example.personalfinancetrackerapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalfinancetrackerapp.databinding.ActivityHomeBinding
import java.util.Date

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFloatingActionButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Home"
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            transactions,
            onEditClick = { transaction ->
                // Handle edit action
                val intent = Intent(this, AddTransactionActivity::class.java).apply {
                    putExtra("transaction", transaction)
                }
                startActivity(intent)
            },
            onDeleteClick = { transaction ->
                // Handle delete action
                AlertDialog.Builder(this)
                    .setTitle("Delete Transaction")
                    .setMessage("Are you sure you want to delete this transaction?")
                    .setPositiveButton("Delete") { _, _ ->
                        transactions.remove(transaction)
                        transactionAdapter.notifyDataSetChanged()
                        // TODO: Update database
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = transactionAdapter
        }
    }

    private fun setupFloatingActionButton() {
        binding.fabAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // TODO: Load transactions from database
        // For now, using sample data
        transactions.clear()
        transactions.addAll(getSampleTransactions())
        transactionAdapter.notifyDataSetChanged()
    }

    private fun getSampleTransactions(): List<Transaction> {
        return listOf(
            Transaction(
                id = 1,
                amount = 100.0,
                category = "Salary",
                date = Date(),
                type = TransactionType.INCOME
            ),
            Transaction(
                id = 2,
                amount = 50.0,
                category = "Groceries",
                date = Date(),
                type = TransactionType.EXPENSE
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 