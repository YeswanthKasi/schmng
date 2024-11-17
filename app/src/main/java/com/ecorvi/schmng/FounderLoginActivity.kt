package com.ecorvi.schmng

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.ecorvi.schmng.ForgotPasswordActivity
import com.ecorvi.schmng.RegisterActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var founderLoginButton: Button
    private lateinit var adminLoginButton: Button
    private lateinit var teacherLoginButton: Button
    private lateinit var parentLoginButton: Button
    private lateinit var forgotPasswordTextView: TextView
    private lateinit var signUpTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_founder_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize UI elements
        usernameEditText = findViewById(R.id.username_edittext)
        passwordEditText = findViewById(R.id.password_edittext)
        founderLoginButton = findViewById(R.id.Founder_login_button)
        adminLoginButton = findViewById(R.id.administrator_login_button)
        teacherLoginButton = findViewById(R.id.staff_login_button)
        parentLoginButton = findViewById(R.id.Login_as_Parent)
        forgotPasswordTextView = findViewById(R.id.forgot_password_textview)
        signUpTextView = findViewById(R.id.signup_textview)

        // Login for different roles
        founderLoginButton.setOnClickListener { loginUser("Founder") }
        adminLoginButton.setOnClickListener { loginUser("Administrator") }
        teacherLoginButton.setOnClickListener { loginUser("Teacher") }
        parentLoginButton.setOnClickListener { loginUser("Parent") }

        // Forgot password
        forgotPasswordTextView.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Sign up
        signUpTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(role: String) {
        val email = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "$role Login Successful", Toast.LENGTH_SHORT).show()
                // Navigate to respective role's dashboard
            } else {
                Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

