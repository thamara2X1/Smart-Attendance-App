package com.vtp.smartattendanceapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vtp.smartattendanceapp.databinding.ActivityScanResultBinding

class ScanResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isSuccess = intent.getBooleanExtra("success", false)
        val subjectName = intent.getStringExtra("subjectName") ?: ""
        val subjectCode = intent.getStringExtra("subjectCode") ?: ""
        val status = intent.getStringExtra("status") ?: "present"
        val distance = intent.getFloatExtra("distance", 0f)

        if (isSuccess) {
            showSuccess(subjectName, subjectCode, status, distance)
        } else {
            showFailure()
        }

        binding.btnDone.setOnClickListener { finish() }
    }

    private fun showSuccess(subjectName: String, subjectCode: String, status: String, distance: Float) {
        binding.ivResultIcon.setImageResource(R.drawable.ic_badge) // Use a check icon if available
        binding.ivResultIcon.setColorFilter(ContextCompat.getColor(this, R.color.success))

        binding.tvResultTitle.text = "Attendance Marked!"
        binding.tvResultTitle.setTextColor(ContextCompat.getColor(this, R.color.success))

        val statusText = if (status == "late") "Late" else "Present"
        binding.tvResultMessage.text = "You have been marked as $statusText for:\n\n" +
                "📚 $subjectName ($subjectCode)\n" +
                "📍 Distance: ${String.format("%.0f", distance)}m from classroom"

        binding.btnDone.text = "Done"
    }

    private fun showFailure() {
        binding.ivResultIcon.setImageResource(R.drawable.ic_person) // Use an error icon if available
        binding.ivResultIcon.setColorFilter(ContextCompat.getColor(this, R.color.error))

        binding.tvResultTitle.text = "Attendance Failed"
        binding.tvResultTitle.setTextColor(ContextCompat.getColor(this, R.color.error))

        binding.tvResultMessage.text = "Unable to mark your attendance. Please try again."

        binding.btnDone.text = "Go Back"
    }
}