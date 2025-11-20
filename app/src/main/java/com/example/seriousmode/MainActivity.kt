package com.example.seriousmode

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import Activity.NextActivity  // <-- Add this import for your NextActivity

class MainActivity : AppCompatActivity() {
    private lateinit var roleRadioGroup: RadioGroup
    private lateinit var signInButton: Button

    // Separate credentials for student and admin
    private val studentEmail = "jonerickjamesvimperial@iskolarngbayan.pup.edu.ph"
    private val studentPassword = "dikoalam@2"

    private val adminEmail = "admin"
    private val adminPassword = "admin01"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        roleRadioGroup = findViewById(R.id.radioGroup)
        signInButton = findViewById(R.id.buttonSignIn)
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)

        roleRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioStudent) {
                signInButton.text = "Sign In as Student"
            } else {
                signInButton.text = "Sign In as Admin"
            }
        }

        signInButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val selectedRole = if (roleRadioGroup.checkedRadioButtonId == R.id.radioStudent) {
                "Student"
            } else {
                "Administrator"
            }

            // Role-specific credential check
            val isValidLogin = when (selectedRole) {
                "Student" -> (email == studentEmail && password == studentPassword)
                "Administrator" -> (email == adminEmail && password == adminPassword)
                else -> false
            }

            if (isValidLogin) {
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, NextActivity::class.java)
                intent.putExtra("role", selectedRole)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Invalid login credentials", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
