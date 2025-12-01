package com.example.seriousmode

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import Activity.NextActivity

class MainActivity : AppCompatActivity() {

    private lateinit var roleRadioGroup: RadioGroup
    private lateinit var signInButton: Button
    private lateinit var registerTextView: TextView

    // fixed admin credentials (do not change)
    private val adminEmail = "admin"
    private val adminPassword = "admin01"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        roleRadioGroup = findViewById(R.id.radioGroup)
        signInButton = findViewById(R.id.buttonSignIn)
        registerTextView = findViewById(R.id.textRegister)

        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)

        // default selection: Student, show Register link
        registerTextView.visibility = View.VISIBLE

        roleRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioStudent) {
                signInButton.text = "Sign In as Student"
                registerTextView.visibility = View.VISIBLE
            } else {
                signInButton.text = "Sign In as Admin"
                registerTextView.visibility = View.GONE
            }
        }

        signInButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val selectedRole =
                if (roleRadioGroup.checkedRadioButtonId == R.id.radioStudent) "Student"
                else "Administrator"

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isValidLogin = when (selectedRole) {
                "Administrator" -> {
                    // admin must match fixed credentials
                    email == adminEmail && password == adminPassword
                }
                else -> {
                    // Student: for now accept any non-empty credentials
                    // later you can validate against Firebase or your own user database
                    true
                }
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

        registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
