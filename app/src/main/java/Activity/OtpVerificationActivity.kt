package Activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.seriousmode.R
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private lateinit var otpEditText: EditText
    private lateinit var verifyButton: Button
    private lateinit var resendTextView: TextView
    private lateinit var messageTextView: TextView

    private var countDownTimer: CountDownTimer? = null
    private val resendCooldownMillis = 60000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        otpEditText = findViewById(R.id.editTextOtp)
        verifyButton = findViewById(R.id.buttonVerifyOtp)
        resendTextView = findViewById(R.id.textResendCode)
        messageTextView = findViewById(R.id.textOtpMessage)

        verificationId = intent.getStringExtra("verificationId")
        resendToken = intent.getParcelableExtra("resendToken")
        val phoneNumber = intent.getStringExtra("phoneNumber")

        messageTextView.text = "A 6-digit code was sent to ${phoneNumber ?: "your number"}."

        verifyButton.setOnClickListener {
            val code = otpEditText.text.toString().trim()

            if (code.length == 6 && verificationId != null) {
                val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
                verifyOtpForCurrentStudent(credential)
            } else {
                Toast.makeText(this, "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        resendTextView.setOnClickListener {
            if (resendToken != null && phoneNumber != null) {
                resendVerificationCode(phoneNumber, resendToken!!)
            }
        }

        startResendTimer()
    }

    private fun verifyOtpForCurrentStudent(credential: PhoneAuthCredential) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "No logged-in student session found.", Toast.LENGTH_SHORT).show()
            return
        }

        verifyButton.isEnabled = false

        currentUser.linkWithCredential(credential)
            .addOnSuccessListener {
                Log.d("OtpVerification", "Phone linked to UID: ${currentUser.uid}")
                saveOtpVerifiedState(currentUser.uid)
                fetchAndSaveUserData(currentUser.uid)
            }
            .addOnFailureListener { e ->
                val errorMessage = e.message.orEmpty()

                if (
                    errorMessage.contains("already linked", ignoreCase = true) ||
                    errorMessage.contains("provider-already-linked", ignoreCase = true) ||
                    errorMessage.contains("credential-already-in-use", ignoreCase = true)
                ) {
                    Log.d("OtpVerification", "Phone already linked. Continuing with UID: ${currentUser.uid}")
                    saveOtpVerifiedState(currentUser.uid)
                    fetchAndSaveUserData(currentUser.uid)
                } else if (e is FirebaseAuthInvalidCredentialsException) {
                    verifyButton.isEnabled = true
                    Toast.makeText(this, "Invalid OTP.", Toast.LENGTH_SHORT).show()
                } else {
                    verifyButton.isEnabled = true
                    Toast.makeText(this, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveOtpVerifiedState(uid: String) {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("otp_verified", true)
            .putString("otp_verified_uid", uid)
            .apply()

        Log.d("OtpVerification", "Saved OTP verified state for UID: $uid")
    }

    private fun fetchAndSaveUserData(uid: String) {
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
                        .apply()

                    Log.d("OtpVerification", "Saved user_name: $fullName")
                    Log.d("OtpVerification", "Saved student_first_name: $displayFirstName")
                    Log.d("OtpVerification", "Saved student_id: $studentId")
                }

                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                openNextActivity()
            }
            .addOnFailureListener { e ->
                verifyButton.isEnabled = true
                Toast.makeText(this, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openNextActivity() {
        val intent = Intent(this, NextActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("role", "Student")
        }
        startActivity(intent)
        finish()
    }

    private fun resendVerificationCode(
        phoneNumber: String,
        token: PhoneAuthProvider.ForceResendingToken
    ) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber,
            60,
            TimeUnit.SECONDS,
            this,
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    verifyOtpForCurrentStudent(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(
                        this@OtpVerificationActivity,
                        "Resend failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onCodeSent(
                    verId: String,
                    newToken: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = verId
                    resendToken = newToken
                    Toast.makeText(this@OtpVerificationActivity, "New code sent", Toast.LENGTH_SHORT).show()
                    startResendTimer()
                }
            },
            token
        )
    }

    private fun startResendTimer() {
        resendTextView.isEnabled = false
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(resendCooldownMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                resendTextView.text = "Resend code in ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                resendTextView.text = "Resend Code"
                resendTextView.isEnabled = true
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}