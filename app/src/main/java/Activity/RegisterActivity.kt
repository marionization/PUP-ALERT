package com.example.seriousmode

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val nameEditText = findViewById<EditText>(R.id.editTextName)
        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val studentIdEditText = findViewById<EditText>(R.id.editTextPUP_id)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val confirmPasswordEditText = findViewById<EditText>(R.id.editTextConfirmPassword)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val studentId = studentIdEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (name.isEmpty() ||
                email.isEmpty() ||
                studentId.isEmpty() ||
                password.isEmpty() ||
                confirmPassword.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // PUP email must end with @iskolarngbayan.pup.edu.ph
            val pupEmailPattern = Regex("""^[A-Za-z0-9._%+-]+@iskolarngbayan\.pup\.edu\.ph$""")
            if (!pupEmailPattern.matches(email)) {
                Toast.makeText(
                    this,
                    "Please use your PUP email (example: jonerickjamesvimperial@iskolarngbayan.pup.edu.ph)",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // student ID must be like 2023-00226-PQ-0  (YYYY-#####-AA-#)
            val studentIdPattern = Regex("""\d{4}-\d{5}-[A-Za-z]{2}-\d""")
            if (!studentIdPattern.matches(studentId)) {
                Toast.makeText(
                    this,
                    "Student Number must be in the format 2023-00226-PQ-0",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save name and student number
            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            prefs.edit()
                .putString("user_name", name)
                .putString("student_id", studentId)
                .apply()

            // TODO save new user in your backend or Firebase
            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
            finish()  // go back to login
        }
    }
}
