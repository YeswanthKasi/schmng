package com.ecorvi.schmng.models

data class AttendanceRecord(
    val id: String = "",
    val userId: String = "",
    val userType: UserType = UserType.STUDENT,
    val date: Long = System.currentTimeMillis(),
    val status: AttendanceStatus = AttendanceStatus.ABSENT,
    val markedBy: String = "",
    val remarks: String = "",
    val lastModified: Long = System.currentTimeMillis()
)

enum class UserType {
    STUDENT,
    TEACHER,
    STAFF
}

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    PERMISSION
} 