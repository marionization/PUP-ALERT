package com.example.seriousmode

import Activity.ForgotPasswordActivity
import Activity.NextActivity
import Activity.OtpVerificationActivity
import Activity.RegisterActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var roleRadioGroup: RadioGroup
    private lateinit var signInButton: Button
    private lateinit var registerTextView: TextView
    private lateinit var forgotPasswordTextView: TextView

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val adminEmail = "admin"
    private val adminPassword = "admin01"

    private var pendingPhoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContentView(R.layout.activity_main)

        initViews()
        setupDefaultState()
        setupListeners()
    }

    private fun initViews() {
        roleRadioGroup = findViewById(R.id.radioGroup)
        signInButton = findViewById(R.id.buttonSignIn)
        registerTextView = findViewById(R.id.textRegister)
        forgotPasswordTextView = findViewById(R.id.textForgotPassword)

        emailInputLayout = findViewById(R.id.emailInputLayout)
        passwordInputLayout = findViewById(R.id.passwordInputLayout)
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
    }

    private fun setupDefaultState() {
        updateUiForSelectedRole(getSelectedRole())
    }

    private fun setupListeners() {
        roleRadioGroup.setOnCheckedChangeListener { _, _ ->
            clearInputErrors()
            updateUiForSelectedRole(getSelectedRole())
        }

        signInButton.setOnClickListener {
            handleLogin()
        }

        registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        forgotPasswordTextView.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun getSelectedRole(): String {
        return if (roleRadioGroup.checkedRadioButtonId == R.id.radioStudent) {
            "Student"
        } else {
            "Administrator"
        }
    }

    private fun updateUiForSelectedRole(role: String) {
        val isStudent = role == "Student"

        signInButton.text = if (isStudent) "Sign In as Student" else "Sign In as Admin"
        registerTextView.visibility = if (isStudent) View.VISIBLE else View.GONE
        forgotPasswordTextView.visibility = if (isStudent) View.VISIBLE else View.GONE

        emailInputLayout.helperText = if (isStudent) {
            "Use your PUP email address"
        } else {
            "Enter admin username"
        }
    }

    private fun handleLogin() {
        clearInputErrors()

        val email = emailEditText.text?.toString()?.trim().orEmpty()
        val password = passwordEditText.text?.toString()?.trim().orEmpty()
        val selectedRole = getSelectedRole()

        if (selectedRole == "Administrator") {
            handleAdminLogin(email, password)
        } else {
            handleStudentLogin(email, password)
        }
    }

    private fun handleAdminLogin(email: String, password: String) {
        if (email.isEmpty()) {
            emailInputLayout.error = "Admin username is required"
            emailEditText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordInputLayout.error = "Password is required"
            passwordEditText.requestFocus()
            return
        }

        if (email == adminEmail && password == adminPassword) {
            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            prefs.edit()
                .putString("user_name", "Administrator")
                .putString("student_first_name", "Administrator")
                .putString("student_id", "")
                .putString("role", "Administrator")
                .apply()

            Toast.makeText(this, "Admin login successful!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, NextActivity::class.java).apply {
                putExtra("role", "Administrator")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } else {
            passwordInputLayout.error = "Invalid admin credentials"
            passwordEditText.requestFocus()
        }
    }

    private fun handleStudentLogin(email: String, password: String) {
        if (!validateStudentInputs(email, password)) return

        setLoadingState(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid

                if (uid == null) {
                    setLoadingState(false)
                    Toast.makeText(this, "Login failed, please try again.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val otpVerified = prefs.getBoolean("otp_verified", false)
                val verifiedUid = prefs.getString("otp_verified_uid", null)

                if (otpVerified && verifiedUid == uid) {
                    fetchAndSaveUserData(uid) {
                        updateFcmToken(uid)
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, NextActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("role", "Student")
                        }
                        startActivity(intent)
                        finish()
                    }
                } else {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val phoneNumber = doc.getString("phone")

                            if (phoneNumber.isNullOrBlank()) {
                                setLoadingState(false)
                                Toast.makeText(
                                    this,
                                    "Phone number not found for this user.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@addOnSuccessListener
                            }

                            pendingPhoneNumber = phoneNumber
                            startPhoneNumberVerification(phoneNumber)
                        }
                        .addOnFailureListener {
                            setLoadingState(false)
                            Toast.makeText(
                                this,
                                "Failed to fetch user details.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                setLoadingState(false)
                passwordInputLayout.error = "Login failed: ${e.message}"
                passwordEditText.requestFocus()
            }
    }

    private fun validateStudentInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            emailInputLayout.error = "Email is required"
            emailEditText.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Enter a valid email address"
            emailEditText.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            passwordInputLayout.error = "Password is required"
            passwordEditText.requestFocus()
            return false
        }

        if (password.length < 6) {
            passwordInputLayout.error = "Password must be at least 6 characters"
            passwordEditText.requestFocus()
            return false
        }

        return true
    }

    private fun clearInputErrors() {
        emailInputLayout.error = null
        passwordInputLayout.error = null
        emailInputLayout.isErrorEnabled = false
        passwordInputLayout.isErrorEnabled = false
    }

    private fun setLoadingState(isLoading: Boolean) {
        signInButton.isEnabled = !isLoading
        signInButton.text = if (isLoading) {
            "Signing In..."
        } else {
            if (getSelectedRole() == "Student") "Sign In as Student" else "Sign In as Admin"
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber,
            60,
            TimeUnit.SECONDS,
            this,
            callbacks
        )
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                currentUser.linkWithCredential(credential)
                    .addOnSuccessListener {
                        Log.d("MainActivity", "Auto verification linked successfully for UID: ${currentUser.uid}")
                    }
                    .addOnFailureListener { e ->
                        Log.d("MainActivity", "Auto verification link skipped/failed: ${e.message}")
                    }
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            setLoadingState(false)
            Toast.makeText(
                this@MainActivity,
                "Verification failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            setLoadingState(false)
            val intent = Intent(this@MainActivity, OtpVerificationActivity::class.java).apply {
                putExtra("verificationId", verificationId)
                putExtra("resendToken", token)
                putExtra("phoneNumber", pendingPhoneNumber)
            }
            startActivity(intent)
        }
    }

    private fun fetchAndSaveUserData(uid: String, onComplete: () -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val studentId = document.getString("studentId") ?: ""

                    val fullName = if (firstName.isNotBlank() || lastName.isNotBlank()) {
                        "$firstName $lastName".trim()
                    } else {
                        document.getString("name") ?: "Student"
                    }

                    val displayFirstName = if (firstName.isNotBlank()) {
                        firstName
                    } else {
                        fullName.substringBefore(" ").ifBlank { "Student" }
                    }

                    val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    prefs.edit()
                        .putString("user_name", fullName)
                        .putString("student_first_name", displayFirstName)
                        .putString("student_id", studentId)
                        .putString("role", "Student")
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
        if (uid.isNullOrBlank()) return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            db.collection("users").document(uid).update("fcmToken", token)
                .addOnFailureListener { e ->
                    Log.w("FCM", "Failed to update FCM token", e)
                }
        }
    }

    override fun onResume() {
        super.onResume()
        setLoadingState(false)

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            updateFcmToken(uid)
        }
    }
}