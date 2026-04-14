package Activity

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.seriousmode.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val firstNameEditText = findViewById<TextInputEditText>(R.id.editTextFirstName)
        val lastNameEditText = findViewById<TextInputEditText>(R.id.editTextLastName)
        val emailEditText = findViewById<TextInputEditText>(R.id.editTextEmail)
        val studentIdEditText = findViewById<TextInputEditText>(R.id.editTextPUP_id)
        val phoneEditText = findViewById<TextInputEditText>(R.id.editTextPhone)
        val passwordEditText = findViewById<TextInputEditText>(R.id.editTextPassword)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.editTextConfirmPassword)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        registerButton.setOnClickListener {
            val firstName = firstNameEditText.text.toString().trim()
            val lastName = lastNameEditText.text.toString().trim()
            val fullName = "$firstName $lastName".trim()
            val email = emailEditText.text.toString().trim()
            val studentId = studentIdEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (firstName.isEmpty() ||
                lastName.isEmpty() ||
                email.isEmpty() ||
                studentId.isEmpty() ||
                phone.isEmpty() ||
                password.isEmpty() ||
                confirmPassword.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pupEmailPattern = Regex("""^[A-Za-z0-9._%+-]+@iskolarngbayan\.pup\.edu\.ph$""")
            if (!pupEmailPattern.matches(email)) {
                Toast.makeText(
                    this,
                    "Please use your PUP email",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

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

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid

                    if (uid == null) {
                        Toast.makeText(this, "Registration failed, try again", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val userData = mapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "name" to fullName,
                        "email" to email,
                        "studentId" to studentId,
                        "phone" to phone,
                        "role" to "Student"
                    )

                    db.collection("users")
                        .document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                            prefs.edit()
                                .putString("user_name", fullName)
                                .putString("student_id", studentId)
                                .apply()

                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}