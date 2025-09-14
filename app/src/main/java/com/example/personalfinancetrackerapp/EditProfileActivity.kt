package com.example.personalfinancetrackerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditProfileActivity : AppCompatActivity() {
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var locationEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE)
        val userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        // Initialize views
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        locationEditText = findViewById(R.id.locationEditText)
        saveButton = findViewById(R.id.saveButton)

        // Load existing data
        val registeredName = userPrefs.getString("name", null)
        val registeredEmail = userPrefs.getString("email", null)

        nameEditText.setText(registeredName ?: sharedPreferences.getString("name", ""))
        emailEditText.setText(registeredEmail ?: sharedPreferences.getString("email", ""))
        phoneEditText.setText(sharedPreferences.getString("phone", ""))
        locationEditText.setText(sharedPreferences.getString("location", ""))

        // Save button click listener
        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val phone = phoneEditText.text.toString()
            val location = locationEditText.text.toString()

            // Save to both UserProfile and UserPrefs
            sharedPreferences.edit().apply {
                putString("name", name)
                putString("email", email)
                putString("phone", phone)
                putString("location", location)
                apply()
            }

            userPrefs.edit().apply {
                putString("name", name)
                putString("email", email)
                apply()
            }

            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}