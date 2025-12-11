package com.example.seriousmode

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import Activity.NextActivity
import Activity.RegisterActivity
import Activity.ForgotPasswordActivity

class MainActivity : AppCompatActivity() {

    private lateinit var roleRadioGroup: RadioGroup
    private lateinit var signInButton: Button
    private lateinit var registerTextView: TextView
    private lateinit var forgotPasswordTextView: TextView
    private lateinit var auth: FirebaseAuth

    // fixed admin credentials (do not change)
    private val adminEmail = "admin"
    private val adminPassword = "admin01"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        roleRadioGroup = findViewById(R.id.radioGroup)
        signInButton = findViewById(R.id.buttonSignIn)
        registerTextView = findViewById(R.id.textRegister)
        forgotPasswordTextView = findViewById(R.id.textForgotPassword)

        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)

        // default selection: Student, show Register link
        registerTextView.visibility = View.VISIBLE
        forgotPasswordTextView.visibility = View.VISIBLE

        roleRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioStudent) {
                signInButton.text = "Sign In as Student"
                registerTextView.visibility = View.VISIBLE
                forgotPasswordTextView.visibility = View.VISIBLE
            } else {
                signInButton.text = "Sign In as Admin"
                registerTextView.visibility = View.GONE
                forgotPasswordTextView.visibility = View.GONE
            }
        }

        signInButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val selectedRole =
                if (roleRadioGroup.checkedRadioButtonId == R.id.radioStudent) "Student"
                else "Administrator"

            if (selectedRole == "Administrator") {
                if (email == adminEmail && password == adminPassword) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, NextActivity::class.java)
                    intent.putExtra("role", "Administrator")
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Invalid admin credentials", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Student Login via Firebase
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, NextActivity::class.java)
                        intent.putExtra("role", "Student")
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        forgotPasswordTextView.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}
