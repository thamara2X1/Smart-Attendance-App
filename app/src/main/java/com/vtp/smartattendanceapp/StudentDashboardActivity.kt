package com.vtp.smartattendanceapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vtp.smartattendanceapp.databinding.ActivityStudentDashboardBinding
import androidx.appcompat.app.AlertDialog

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        loadUserData()
        setupClickListeners()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "Student"
                    val department = document.getString("department") ?: ""

                    // Update welcome text
                    binding.tvWelcome.text = "Welcome, $name!"

                    // Update subtitle with department info
                    if (department.isNotEmpty()) {
                        binding.tvSubtitle.text = "$department - Scan QR codes to mark your attendance"
                    }

                    Toast.makeText(this, "Welcome back, $name!", Toast.LENGTH_SHORT).show()
                } else {
                    binding.tvWelcome.text = "Welcome, Student!"
                    Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                binding.tvWelcome.text = "Welcome, Student!"
                Toast.makeText(
                    this,
                    "Error loading profile: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun setupClickListeners() {
        // Scan QR Code
        binding.cardScanQR.setOnClickListener {
            Toast.makeText(this, "QR Scanner - Coming Soon!", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to QR Scanner Activity
        }

        // Attendance History
        binding.cardHistory.setOnClickListener {
            Toast.makeText(this, "Attendance History - Coming Soon!", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to Attendance History Activity
        }

        // Profile
        binding.cardProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        auth.signOut()

        // Navigate to LoginActivity and clear back stack
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Prevent going back to splash/login
        // Do nothing or show exit dialog
        showExitDialog()
    }

    private fun showExitDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Do you want to exit the app?")
            .setPositiveButton("Yes") { _, _ ->
                finishAffinity() // Close all activities
            }
            .setNegativeButton("No", null)
            .show()
    }
}