package com.vtp.smartattendanceapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vtp.smartattendanceapp.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var selectedRole: String = "student"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupRoleSpinner()
        setupClickListeners()
    }

    private fun setupRoleSpinner() {
        val roles = arrayOf("Student", "Teacher")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        binding.spinnerRole.adapter = adapter

        binding.spinnerRole.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRole = roles[position].lowercase()
                updateUIForRole()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun updateUIForRole() {
        // Show/hide registration number based on role
        binding.tilRegistrationNumber.visibility = if (selectedRole == "student") {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvSignIn.setOnClickListener {
            finish() // Go back to login
        }

        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val department = binding.etDepartment.text.toString().trim()
        val registrationNumber = binding.etRegistrationNumber.text.toString().trim()

        // Validation
        if (!validateInputs(name, email, password, confirmPassword, department, registrationNumber)) {
            return
        }

        showLoading(true)

        // Create user in Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                // Send email verification
                authResult.user?.sendEmailVerification()

                // Create user document in Firestore
                createUserProfile(userId, name, email, department, registrationNumber)
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Registration failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun createUserProfile(
        userId: String,
        name: String,
        email: String,
        department: String,
        registrationNumber: String
    ) {
        val userMap = hashMapOf(
            "userId" to userId,
            "name" to name,
            "email" to email,
            "role" to selectedRole,
            "department" to department,
            "registrationNumber" to registrationNumber,
            "profileImageUrl" to "",
            "institutionId" to "",
            "createdAt" to Date(),
            "isActive" to true
        )

        firestore.collection("users")
            .document(userId)
            .set(userMap)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(
                    this,
                    "Registration successful! Please verify your email.",
                    Toast.LENGTH_LONG
                ).show()

                // Sign out and go to login
                auth.signOut()
                finish()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Failed to create profile: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()

                // Delete the auth user if profile creation fails
                auth.currentUser?.delete()
            }
    }

    private fun validateInputs(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        department: String,
        registrationNumber: String
    ): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.tilFullName.error = "Name is required"
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email address"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords don't match"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        if (department.isEmpty()) {
            binding.tilDepartment.error = "Department is required"
            isValid = false
        } else {
            binding.tilDepartment.error = null
        }

        if (selectedRole == "student" && registrationNumber.isEmpty()) {
            binding.tilRegistrationNumber.error = "Registration number is required"
            isValid = false
        } else {
            binding.tilRegistrationNumber.error = null
        }

        return isValid
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
    }
}