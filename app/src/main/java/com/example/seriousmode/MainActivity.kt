package com.example.seriousmode

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import Activity.NextActivity
import Activity.RegisterActivity
import Activity.ForgotPasswordActivity
import Activity.OtpVerificationActivity
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.PhoneAuthOptions

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
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            auth.signInWithCredential(credential).addOnCompleteListener {
                 if(it.isSuccessful){
                     val intent = Intent(this@MainActivity, NextActivity::class.java)
                     intent.putExtra("role", "Student")
                     startActivity(intent)
                     finish()
                 }
            }
        }

        override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
            Toast.makeText(this@MainActivity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and create a credential
            // with the code and verification ID.
            val intent = Intent(this@MainActivity, OtpVerificationActivity::class.java).apply {
                putExtra("verificationId", verificationId)
                putExtra("resendToken", token)
                putExtra("phoneNumber", auth.currentUser?.phoneNumber)
            }
            startActivity(intent)
        }
    }
}
