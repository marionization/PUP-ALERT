package com.example.seriousmode

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import Activity.NextActivity  // <-- Add this import for your NextActivity

class MainActivity : AppCompatActivity() {
    private lateinit var roleRadioGroup: RadioGroup
    private lateinit var signInButton: Button

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
            if (
                email == "jonerickjamesvimperial@iskolamgbayan.pup.edu.ph" &&
                password == "dikoalam@2"
            ) {
                Toast.makeText(this, "Demo login successful!", Toast.LENGTH_SHORT).show()

                // Determine the selected role from the radio group
                val selectedRole = if (roleRadioGroup.checkedRadioButtonId == R.id.radioStudent) {
                    "Student"
                } else {
                    "Administrator"
                }

                // Pass the role to NextActivity using intent
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
