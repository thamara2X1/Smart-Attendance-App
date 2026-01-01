package com.vtp.smartattendanceapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Splash screen delay - 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthState()
        }, 2000)
    }

    private fun checkAuthState() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // User is logged in, check their role and navigate accordingly
            getUserRole(currentUser.uid)
        } else {
            // User is not logged in, navigate to LoginActivity
            navigateToLogin()
        }
    }

    private fun getUserRole(userId: String) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role") ?: "student"
                    when (role.lowercase()) {
                        "teacher", "admin" -> navigateToTeacherDashboard()
                        "student" -> navigateToStudentDashboard()
                        else -> navigateToLogin()
                    }
                } else {
                    navigateToLogin()
                }
            }
            .addOnFailureListener {
                navigateToLogin()
            }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun navigateToTeacherDashboard() {
        startActivity(Intent(this, TeacherDashboardActivity::class.java))
        finish()
    }

    private fun navigateToStudentDashboard() {
        startActivity(Intent(this, StudentDashboardActivity::class.java))
        finish()
    }
}