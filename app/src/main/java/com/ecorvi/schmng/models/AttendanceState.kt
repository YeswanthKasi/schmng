package com.ecorvi.schmng.models

data class AttendanceState(
    val students: List<StudentAttendance> = emptyList(),
    val classes: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class StudentAttendance(
    val id: String,
    val firstName: String,
    val lastName: String,
    val rollNumber: String,
    val className: String,
    val isPresent: Boolean = false
) 