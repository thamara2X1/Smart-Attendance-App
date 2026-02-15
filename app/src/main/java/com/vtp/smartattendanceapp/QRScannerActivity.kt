package com.vtp.smartattendanceapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.vtp.smartattendanceapp.databinding.ActivityQrScannerBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var cameraExecutor: ExecutorService

    private var isProcessing = false
    private var currentLocation: Location? = null

    companion object {
        private const val TAG = "QRScanner"
        private const val MAX_DISTANCE_METERS = 100f // Max allowed distance from teacher
    }

    // Permission launchers
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkLocationPermission()
        } else {
            Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
            startCamera()
        } else {
            Toast.makeText(this, "Location permission is required for attendance verification", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupToolbar()
        setupUI()
        checkCameraPermission()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupUI() {
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                checkLocationPermission()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
                startCamera()
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val cancellationToken = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                currentLocation = location
                if (location != null) {
                    binding.tvLocationStatus.text = "📍 Location acquired"
                    binding.tvLocationStatus.setTextColor(
                        ContextCompat.getColor(this, R.color.success)
                    )
                } else {
                    binding.tvLocationStatus.text = "⚠️ Unable to get location"
                    binding.tvLocationStatus.setTextColor(
                        ContextCompat.getColor(this, R.color.warning)
                    )
                }
            }
            .addOnFailureListener {
                binding.tvLocationStatus.text = "⚠️ Location error"
                binding.tvLocationStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.error)
                )
            }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            // Image analysis use case for QR scanning
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImage(imageProxy)
            }

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImage(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        if (barcode.valueType == Barcode.TYPE_TEXT ||
                            barcode.valueType == Barcode.TYPE_UNKNOWN
                        ) {
                            val rawValue = barcode.rawValue ?: continue
                            isProcessing = true
                            handleScannedQRCode(rawValue)
                            break
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Barcode scanning failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleScannedQRCode(qrContent: String) {
        runOnUiThread {
            showProcessing(true)
            binding.tvScanStatus.text = "QR Code detected! Validating..."
        }

        // Parse QR code data
        val qrData = QRCodeData.fromJson(qrContent)

        if (qrData == null || qrData.sessionId.isEmpty()) {
            runOnUiThread {
                showProcessing(false)
                showError("Invalid QR code. Please scan a valid attendance QR code.")
                isProcessing = false
            }
            return
        }

        // Run all validations
        validateAndMarkAttendance(qrData)
    }

    private fun validateAndMarkAttendance(qrData: QRCodeData) {
        val userId = auth.currentUser?.uid ?: run {
            showError("User not authenticated")
            return
        }

        // VALIDATION 1: Check if QR code has expired
        if (qrData.isExpired()) {
            runOnUiThread {
                showProcessing(false)
                showError("This QR code has expired. Please ask your teacher to generate a new one.")
                isProcessing = false
            }
            return
        }

        // VALIDATION 2: Check duplicate attendance
        firestore.collection("attendance")
            .whereEqualTo("studentId", userId)
            .whereEqualTo("sessionId", qrData.sessionId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Already marked
                    runOnUiThread {
                        showProcessing(false)
                        showError("You have already marked attendance for this session.")
                        isProcessing = false
                    }
                    return@addOnSuccessListener
                }

                // VALIDATION 3: Verify student enrollment
                verifyEnrollmentAndProceed(userId, qrData)
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    showProcessing(false)
                    showError("Error checking attendance: ${e.message}")
                    isProcessing = false
                }
            }
    }

    private fun verifyEnrollmentAndProceed(userId: String, qrData: QRCodeData) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    runOnUiThread {
                        showProcessing(false)
                        showError("User profile not found.")
                        isProcessing = false
                    }
                    return@addOnSuccessListener
                }

                val studentDepartment = document.getString("department") ?: ""
                val studentName = document.getString("name") ?: ""
                val studentEmail = document.getString("email") ?: ""
                val registrationNumber = document.getString("registrationNumber") ?: ""

                // Check if student's department matches the session department
                if (studentDepartment != qrData.department) {
                    runOnUiThread {
                        showProcessing(false)
                        showError("You are not enrolled in this department's session.\nYour department: $studentDepartment\nSession department: ${qrData.department}")
                        isProcessing = false
                    }
                    return@addOnSuccessListener
                }

                // VALIDATION 4: GPS location verification
                validateLocationAndMark(userId, studentName, studentEmail, registrationNumber, qrData)
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    showProcessing(false)
                    showError("Error verifying enrollment: ${e.message}")
                    isProcessing = false
                }
            }
    }

    private fun validateLocationAndMark(
        userId: String,
        studentName: String,
        studentEmail: String,
        registrationNumber: String,
        qrData: QRCodeData
    ) {
        val location = currentLocation

        if (location == null) {
            runOnUiThread {
                showProcessing(false)
                showError("Unable to verify your location. Please enable GPS and try again.")
                isProcessing = false
            }
            return
        }

        // Calculate distance between student and teacher location
        val teacherLocation = Location("teacher").apply {
            latitude = qrData.latitude
            longitude = qrData.longitude
        }
        val distance = location.distanceTo(teacherLocation)

        if (distance > MAX_DISTANCE_METERS) {
            runOnUiThread {
                showProcessing(false)
                showError(
                    "You are too far from the classroom.\n" +
                            "Distance: ${String.format("%.0f", distance)}m\n" +
                            "Maximum allowed: ${MAX_DISTANCE_METERS.toInt()}m"
                )
                isProcessing = false
            }
            return
        }

        // ALL VALIDATIONS PASSED — Mark attendance
        markAttendance(userId, studentName, studentEmail, registrationNumber, qrData, distance)
    }

    private fun markAttendance(
        userId: String,
        studentName: String,
        studentEmail: String,
        registrationNumber: String,
        qrData: QRCodeData,
        distance: Float
    ) {
        val record = AttendanceRecord(
            studentId = userId,
            studentName = studentName,
            studentEmail = studentEmail,
            registrationNumber = registrationNumber,
            sessionId = qrData.sessionId,
            subjectCode = qrData.subjectCode,
            subjectName = qrData.subjectName,
            teacherId = qrData.teacherId,
            department = qrData.department,
            markedAt = System.currentTimeMillis(),
            latitude = currentLocation?.latitude ?: 0.0,
            longitude = currentLocation?.longitude ?: 0.0,
            distanceFromTeacher = distance,
            status = if (qrData.getRemainingSeconds() < 60) "late" else "present"
        )

        firestore.collection("attendance")
            .add(record)
            .addOnSuccessListener { documentRef ->
                runOnUiThread {
                    showProcessing(false)

                    // Navigate to success screen
                    val intent = Intent(this, ScanResultActivity::class.java).apply {
                        putExtra("success", true)
                        putExtra("subjectName", qrData.subjectName)
                        putExtra("subjectCode", qrData.subjectCode)
                        putExtra("status", record.status)
                        putExtra("distance", distance)
                        putExtra("attendanceId", documentRef.id)
                    }
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    showProcessing(false)
                    showError("Failed to mark attendance: ${e.message}")
                    isProcessing = false
                }
            }
    }

    private fun showError(message: String) {
        binding.tvScanStatus.text = message
        binding.tvScanStatus.setTextColor(ContextCompat.getColor(this, R.color.error))

        // Reset after 3 seconds to allow re-scan
        binding.tvScanStatus.postDelayed({
            binding.tvScanStatus.text = "Point your camera at the QR code"
            binding.tvScanStatus.setTextColor(ContextCompat.getColor(this, R.color.white))
            isProcessing = false
        }, 3000)
    }

    private fun showProcessing(show: Boolean) {
        binding.processingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}