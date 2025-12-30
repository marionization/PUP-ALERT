package com.example.seriousmode

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import Activity.NextActivity
import Activity.RegisterActivity
import Activity.ForgotPasswordActivity
import Activity.OtpVerificationActivity
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var roleRadioGroup: RadioGroup
    private lateinit var signInButton: Button
    private lateinit var registerTextView: TextView
    private lateinit var forgotPasswordTextView: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // fixed admin credentials (do not change)
    private val adminEmail = "admin"
    private val adminPassword = "admin01"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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
                    updateFcmToken(auth.currentUser?.uid) // Admin does not have a real account, you might need a different logic for admin tokens
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

                // Step 1: Sign in with email and password
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid
                        if (uid != null) {
                            // Step 2: Get phone number from Firestore
                            db.collection("users").document(uid).get()
                                .addOnSuccessListener { doc ->
                                    val phoneNumber = doc.getString("phone")
                                    if (phoneNumber != null) {
                                        // Step 3: Start phone verification
                                        startPhoneNumberVerification(phoneNumber)
                                    } else {
                                        Toast.makeText(this, "Phone number not found for this user.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                             Toast.makeText(this, "Login failed, please try again.", Toast.LENGTH_SHORT).show()
                        }
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

    private fun startPhoneNumberVerification(phoneNumber: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber,          // Phone number to verify
            60,                 // Timeout duration
            TimeUnit.SECONDS,
            this,               // Activity
            callbacks
        )
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
            Toast.makeText(this@MainActivity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            val intent = Intent(this@MainActivity, OtpVerificationActivity::class.java).apply {
                putExtra("verificationId", verificationId)
                putExtra("resendToken", token)
            }
            startActivity(intent)
        }
    }
    
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid != null) {
                    fetchAndSaveUserData(uid) {
                        updateFcmToken(uid)
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, NextActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("role", "Student")
                        }
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(this, "Login failed after verification.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchAndSaveUserData(uid: String, onComplete: () -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name") ?: "Student"
                    val studentId = document.getString("studentId") ?: ""
                    
                    val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    prefs.edit()
                        .putString("user_name", name)
                        .putString("student_id", studentId)
                        .apply()
                }
                onComplete()
            }
            .addOnFailureListener { 
                Toast.makeText(this, "Failed to fetch user data.", Toast.LENGTH_SHORT).show()
                onComplete()
            }
    }

    private fun updateFcmToken(uid: String?) {
        if (uid == null) return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            db.collection("users").document(uid).update("fcmToken", token)
        }
    }
}
