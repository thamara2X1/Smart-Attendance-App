package com.vtp.smartattendanceapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vtp.smartattendanceapp.databinding.ActivityTeacherDashboardBinding

class TeacherDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherDashboardBinding.inflate(layoutInflater)
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
                    val name = document.getString("name") ?: "Teacher"
                    val department = document.getString("department") ?: ""

                    // Update welcome text
                    binding.tvWelcome.text = "Welcome, $name!"

                    // Update subtitle with department info
                    if (department.isNotEmpty()) {
                        binding.tvSubtitle.text = "$department - Manage courses and track student attendance"
                    }

                    Toast.makeText(this, "Welcome back, $name!", Toast.LENGTH_SHORT).show()
                } else {
                    binding.tvWelcome.text = "Welcome, Teacher!"
                    Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                binding.tvWelcome.text = "Welcome, Teacher!"
                Toast.makeText(
                    this,
                    "Error loading profile: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun setupClickListeners() {
        // My Courses
        binding.cardMyCourses.setOnClickListener {
            Toast.makeText(this, "My Courses - Coming Soon!", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to Courses Activity
        }

        // Generate QR Code
        binding.cardGenerateQR.setOnClickListener {
            Toast.makeText(this, "Generate QR Code - Coming Soon!", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to QR Generator Activity
        }

        // Attendance Records
        binding.cardAttendanceRecords.setOnClickListener {
            Toast.makeText(this, "Attendance Records - Coming Soon!", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to Attendance Records Activity
        }

        // Profile
        binding.cardProfile.setOnClickListener {
            Toast.makeText(this, "Profile - Coming Soon!", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to Profile Activity
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        android.app.AlertDialog.Builder(this)
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
        // Prevent going back to splash/login
        // Show exit dialog instead
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