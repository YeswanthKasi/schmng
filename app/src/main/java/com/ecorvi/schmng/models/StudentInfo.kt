package com.ecorvi.schmng.models

data class StudentInfo(
    val id: String,
    val firstName: String,
    val lastName: String,
    val className: String,
    val rollNumber: String = "",
    val attendancePercentage: Float = 0f
) 