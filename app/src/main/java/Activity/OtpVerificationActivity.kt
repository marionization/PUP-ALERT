package Activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.seriousmode.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    private lateinit var otpEditText: EditText
    private lateinit var verifyButton: Button
    private lateinit var resendTextView: TextView
    private lateinit var messageTextView: TextView

    private var countDownTimer: CountDownTimer? = null
    private val resendCooldownMillis = 60000L // 60 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        auth = FirebaseAuth.getInstance()

        otpEditText = findViewById(R.id.editTextOtp)
        verifyButton = findViewById(R.id.buttonVerifyOtp)
        resendTextView = findViewById(R.id.textResendCode)
        messageTextView = findViewById(R.id.textOtpMessage)

        verificationId = intent.getStringExtra("verificationId")
        resendToken = intent.getParcelableExtra("resendToken")
        val phoneNumber = intent.getStringExtra("phoneNumber")

        messageTextView.text = "A 6-digit code was sent to $phoneNumber."

        verifyButton.setOnClickListener {
            val code = otpEditText.text.toString().trim()
            if (code.isNotEmpty() && verificationId != null) {
                val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
                signInWithPhoneAuthCredential(credential)
            } else {
                Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show()
            }
        }

        resendTextView.setOnClickListener {
            if (resendToken != null && phoneNumber != null) {
                resendVerificationCode(phoneNumber, resendToken!!)
            }
        }

        startResendTimer()
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, NextActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("role", "Student")
                }
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resendVerificationCode(phoneNumber: String, token: PhoneAuthProvider.ForceResendingToken) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber,          // Phone number to verify
            60,                 // Timeout duration
            java.util.concurrent.TimeUnit.SECONDS,
            this,               // Activity
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    Toast.makeText(this@OtpVerificationActivity, "Resend failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(verId: String, newToken: PhoneAuthProvider.ForceResendingToken) {
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
