package Activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.seriousmode.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val studentIdEditText = findViewById<EditText>(R.id.editTextStudentId)
        val resetPasswordButton = findViewById<Button>(R.id.buttonResetPassword)

        resetPasswordButton.setOnClickListener {
            val studentId = studentIdEditText.text.toString().trim()

            if (studentId.isEmpty()) {
                Toast.makeText(this, "Please enter your Student ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Find user by Student ID
            db.collection("users")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(this, "Student ID not found", Toast.LENGTH_SHORT).show()
                    } else {
                        // Assuming one user per student ID
                        val userDoc = documents.documents[0]
                        val email = userDoc.getString("email")

                        if (email != null) {
                            auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Password reset link sent to $email", Toast.LENGTH_LONG).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to send reset email: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "No email found for this user", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error finding user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
