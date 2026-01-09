package com.vtp.smartattendanceapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vtp.smartattendanceapp.databinding.ActivityProfileBinding
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var userRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupToolbar()
        loadUserProfile()
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

    private fun loadUserProfile() {
        showLoading(true)

        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)

                if (document.exists()) {
                    // Get user data
                    val name = document.getString("name") ?: ""
                    val email = document.getString("email") ?: ""
                    val role = document.getString("role") ?: ""
                    val department = document.getString("department") ?: ""
                    val registrationNumber = document.getString("registrationNumber") ?: ""

                    userRole = role

                    // Display data
                    binding.tvName.text = name
                    binding.tvEmail.text = email
                    binding.tvRole.text = role.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                    binding.tvDepartment.text = department

                    // Show/hide registration number based on role
                    if (role == "student" && registrationNumber.isNotEmpty()) {
                        binding.layoutRegistrationNumber.visibility = View.VISIBLE
                        binding.tvRegistrationNumber.text = registrationNumber
                    } else {
                        binding.layoutRegistrationNumber.visibility = View.GONE
                    }

                    // Update profile initial
                    if (name.isNotEmpty()) {
                        binding.tvProfileInitial.text = name.first().toString().uppercase()
                    }

                } else {
                    Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show()
                    finish()
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
        // Edit Profile
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Change Password
        binding.cardChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Logout
        binding.cardLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Delete Account
        binding.cardDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showChangePasswordDialog() {
        val email = auth.currentUser?.email ?: return

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setMessage("A password reset link will be sent to:\n$email")
            .setPositiveButton("Send") { _, _ ->
                sendPasswordResetEmail(email)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendPasswordResetEmail(email: String) {
        showLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(
                    this,
                    "Password reset email sent to $email",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Failed to send email: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                confirmDeleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteAccount() {
        // Second confirmation
        AlertDialog.Builder(this)
            .setTitle("Final Confirmation")
            .setMessage("This will permanently delete your account and all associated data. Continue?")
            .setPositiveButton("Yes, Delete") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        showLoading(true)

        val userId = auth.currentUser?.uid ?: return
        val user = auth.currentUser ?: return

        // Delete Firestore data first
        firestore.collection("users")
            .document(userId)
            .delete()
            .addOnSuccessListener {
                // Then delete auth account
                user.delete()
                    .addOnSuccessListener {
                        showLoading(false)
                        Toast.makeText(
                            this,
                            "Account deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navigate to login
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { exception ->
                        showLoading(false)
                        Toast.makeText(
                            this,
                            "Failed to delete account: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Failed to delete data: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.scrollView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        // Reload profile when returning from edit
        loadUserProfile()
    }
}