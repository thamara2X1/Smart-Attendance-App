package com.vtp.smartattendanceapp

import org.json.JSONObject

data class QRCodeData(
    val sessionId: String = "",
    val subjectCode: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val department: String = "",
    val timestamp: Long = 0L,
    val expiryMinutes: Int = 5,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    companion object {
        fun fromJson(json: String): QRCodeData? {
            return try {
                val obj = JSONObject(json)
                QRCodeData(
                    sessionId = obj.optString("sessionId", ""),
                    subjectCode = obj.optString("subjectCode", ""),
                    subjectName = obj.optString("subjectName", ""),
                    teacherId = obj.optString("teacherId", ""),
                    department = obj.optString("department", ""),
                    timestamp = obj.optLong("timestamp", 0L),
                    expiryMinutes = obj.optInt("expiryMinutes", 5),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0)
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun toJson(): String {
        val obj = JSONObject()
        obj.put("sessionId", sessionId)
        obj.put("subjectCode", subjectCode)
        obj.put("subjectName", subjectName)
        obj.put("teacherId", teacherId)
        obj.put("department", department)
        obj.put("timestamp", timestamp)
        obj.put("expiryMinutes", expiryMinutes)
        obj.put("latitude", latitude)
        obj.put("longitude", longitude)
        return obj.toString()
    }

    fun isExpired(): Boolean {
        val currentTime = System.currentTimeMillis()
        val expiryTime = timestamp + (expiryMinutes * 60 * 1000L)
        return currentTime > expiryTime
    }

    fun getRemainingSeconds(): Long {
        val expiryTime = timestamp + (expiryMinutes * 60 * 1000L)
        val remaining = (expiryTime - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining else 0
    }
}