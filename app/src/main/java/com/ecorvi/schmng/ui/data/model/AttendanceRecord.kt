package com.ecorvi.schmng.ui.data.model

data class AttendanceRecord(
    val personId: String,
    val attendanceOption: String, // e.g., "Present", "Absent", "Permission"
    val date: String // Store date in a format like "2025-04-15"
)
