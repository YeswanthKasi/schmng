package com.ecorvi.schmng.models

import java.util.Date

data class LeaveApplication(
    val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val fromDate: Long = 0L,
    val toDate: Long = 0L,
    val reason: String = "",
    val status: String = STATUS_PENDING, // "pending", "approved", "rejected"
    val appliedAt: Long = 0L,
    val reviewedBy: String = "",
    val reviewedAt: Long = 0L,
    val adminRemarks: String = ""
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "teacherId" to teacherId,
        "teacherName" to teacherName,
        "fromDate" to fromDate,
        "toDate" to toDate,
        "reason" to reason,
        "status" to status,
        "appliedAt" to appliedAt,
        "reviewedBy" to reviewedBy,
        "reviewedAt" to reviewedAt,
        "adminRemarks" to adminRemarks
    )
} 