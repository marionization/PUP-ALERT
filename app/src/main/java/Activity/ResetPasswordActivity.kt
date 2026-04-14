package Activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.seriousmode.R
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var actionCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        auth = FirebaseAuth.getInstance()

        // Get the action code from the intent
        actionCode = intent.getStringExtra("actionCode")

        if (actionCode == null) {
            Toast.makeText(this, "Invalid link. Please try again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val newPasswordEditText = findViewById<EditText>(R.id.editTextNewPassword)
        val confirmPasswordEditText = findViewById<EditText>(R.id.editTextConfirmPassword)
        val confirmButton = findViewById<Button>(R.id.buttonConfirmReset)

        confirmButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (newPassword.isEmpty() || newPassword.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verify the action code and reset the password
            auth.verifyPasswordResetCode(actionCode!!)
                .addOnSuccessListener {
                    auth.confirmPasswordReset(actionCode!!, newPassword)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Password has been reset successfully", Toast.LENGTH_LONG).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error resetting password: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Invalid or expired link: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
