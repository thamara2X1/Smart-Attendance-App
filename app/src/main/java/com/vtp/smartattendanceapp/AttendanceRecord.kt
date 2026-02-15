package com.vtp.smartattendanceapp

data class AttendanceRecord(
    val studentId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val registrationNumber: String = "",
    val sessionId: String = "",
    val subjectCode: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val department: String = "",
    val markedAt: Long = System.currentTimeMillis(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceFromTeacher: Float = 0f,
    val status: String = "present" // present, late
)