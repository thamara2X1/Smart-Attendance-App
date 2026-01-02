package com.vtp.smartattendanceapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vtp.smartattendanceapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInputs(email, password)) {
                loginUser(email, password)
            }
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email address"
            return false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return false
        }

        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            return false
        }

        binding.tilEmail.error = null
        binding.tilPassword.error = null
        return true
    }

    private fun loginUser(email: String, password: String) {
        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener
                getUserRoleAndNavigate(userId)
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Login failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun getUserRoleAndNavigate(userId: String) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)

                if (document.exists()) {
                    val role = document.getString("role") ?: "student"

                    when (role.lowercase()) {
                        "teacher", "admin" -> {
                            startActivity(Intent(this, TeacherDashboardActivity::class.java))
                            finish()
                        }
                        "student" -> {
                            startActivity(Intent(this, StudentDashboardActivity::class.java))
                            finish()
                        }
                        else -> {
                            Toast.makeText(this, "Invalid user role", Toast.LENGTH_SHORT).show()
                            auth.signOut()
                        }
                    }
                } else {
                    Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Error fetching user data: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showForgotPasswordDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        val input = android.widget.EditText(this)
        input.hint = "Enter your email"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        builder.setTitle("Reset Password")
            .setMessage("Enter your email to receive password reset link")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    sendPasswordResetEmail(email)
                } else {
                    Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Password reset email sent. Check your inbox.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Failed to send reset email: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.tvSignUp.isEnabled = !isLoading
    }
}