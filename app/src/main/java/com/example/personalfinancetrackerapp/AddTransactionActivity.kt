package com.example.personalfinancetrackerapp

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Date

class AddTransactionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        val amountInputLayout = findViewById<TextInputLayout>(R.id.amountInputLayout)
        val amountInput = amountInputLayout.editText as TextInputEditText
        val categoryInputLayout = findViewById<TextInputLayout>(R.id.categoryInputLayout)
        val categorySpinner = categoryInputLayout.editText as AutoCompleteTextView
        val typeInputLayout = findViewById<TextInputLayout>(R.id.typeInputLayout)
        val typeSpinner = typeInputLayout.editText as AutoCompleteTextView
        val dateInputLayout = findViewById<TextInputLayout>(R.id.dateInputLayout)
        val dateInput = dateInputLayout.editText as TextInputEditText
        val saveButton = findViewById<MaterialButton>(R.id.saveButton)
        val cancelButton = findViewById<MaterialButton>(R.id.cancelButton)

        // Setup dropdowns
        val categories = arrayOf("Food", "Transport", "Entertainment", "Shopping", "Home", "Pets", "Other")
        categorySpinner.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories))

        val types = arrayOf("Income", "Expense")
        typeSpinner.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types))

        // Set default date
        dateInput.setText(SimpleDateFormat("MM/dd/yyyy").format(Date()))

        saveButton.setOnClickListener {
            val amountText = amountInput.text.toString()
            val category = categorySpinner.text.toString()
            val type = typeSpinner.text.toString()
            val date = dateInput.text.toString()

            // Validate amount
            if (amountText.isEmpty()) {
                amountInputLayout.error = "Please enter an amount"
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                amountInputLayout.error = "Please enter a valid number"
                return@setOnClickListener
            }

            if (amount <= 0) {
                amountInputLayout.error = "Amount must be greater than 0"
                return@setOnClickListener
            }

            // Validate category
            if (category.isEmpty()) {
                categoryInputLayout.error = "Please select a category"
                return@setOnClickListener
            }

            // Validate type
            if (type.isEmpty()) {
                typeInputLayout.error = "Please select a type"
                return@setOnClickListener
            }

            // Validate date
            if (date.isEmpty()) {
                dateInputLayout.error = "Please enter a date"
                return@setOnClickListener
            }

            val parsedDate = try {
                SimpleDateFormat("MM/dd/yyyy").parse(date)
            } catch (e: Exception) {
                dateInputLayout.error = "Please enter a valid date (MM/DD/YYYY)"
                return@setOnClickListener
            }

            val transaction = Transaction(
                id = System.currentTimeMillis(),
                amount = amount,
                category = category,
                date = parsedDate,
                type = if (type == "Income") TransactionType.INCOME else TransactionType.EXPENSE
            )

            if (saveTransaction(transaction)) {
                setResult(RESULT_OK)
                finish()
            }
        }

        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun saveTransaction(transaction: Transaction): Boolean {
        return try {
            val file = java.io.File(filesDir, "transactions.json")
            val transactions = mutableListOf<String>()
            if (file.exists()) {
                file.readLines().forEach { transactions.add(it) }
            }
            transactions.add("${transaction.id},${transaction.category},${transaction.amount},${transaction.type},${transaction.date.time}")
            file.writeText(transactions.joinToString("\n"))
            Toast.makeText(this, "Transaction saved successfully", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving transaction: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
}