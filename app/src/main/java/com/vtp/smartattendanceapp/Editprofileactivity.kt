package com.vtp.smartattendanceapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vtp.smartattendanceapp.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var userRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupToolbar()
        loadCurrentProfile()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadCurrentProfile() {
        showLoading(true)

        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)

                if (document.exists()) {
                    val name = document.getString("name") ?: ""
                    val department = document.getString("department") ?: ""
                    val registrationNumber = document.getString("registrationNumber") ?: ""
                    val role = document.getString("role") ?: ""

                    userRole = role

                    // Populate fields
                    binding.etName.setText(name)
                    binding.etDepartment.setText(department)
                    binding.etRegistrationNumber.setText(registrationNumber)

                    // Show/hide registration number for students only
                    if (role == "student") {
                        binding.tilRegistrationNumber.visibility = View.VISIBLE
                    } else {
                        binding.tilRegistrationNumber.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Error loading profile: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val department = binding.etDepartment.text.toString().trim()
        val registrationNumber = binding.etRegistrationNumber.text.toString().trim()

        // Validation
        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            return
        }

        if (department.isEmpty()) {
            binding.tilDepartment.error = "Department is required"
            return
        }

        if (userRole == "student" && registrationNumber.isEmpty()) {
            binding.tilRegistrationNumber.error = "Registration number is required"
            return
        }

        // Clear errors
        binding.tilName.error = null
        binding.tilDepartment.error = null
        binding.tilRegistrationNumber.error = null

        updateProfile(name, department, registrationNumber)
    }

    private fun updateProfile(name: String, department: String, registrationNumber: String) {
        showLoading(true)

        val userId = auth.currentUser?.uid ?: return

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "department" to department
        )

        // Add registration number only for students
        if (userRole == "student") {
            updates["registrationNumber"] = registrationNumber
        }

        firestore.collection("users")
            .document(userId)
            .update(updates)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(
                    this,
                    "Profile updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Failed to update profile: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
        binding.btnCancel.isEnabled = !isLoading
    }
}