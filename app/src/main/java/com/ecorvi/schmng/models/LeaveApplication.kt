package com.ecorvi.schmng.models

import java.util.Date

data class LeaveApplication(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userType: String = "", // "teacher" or "staff"
    val fromDate: Long = 0L,
    val toDate: Long = 0L,
    val reason: String = "",
    val status: String = STATUS_PENDING, // "pending", "approved", "rejected"
    val appliedAt: Long = 0L,
    val reviewedBy: String = "",
    val reviewedAt: Long = 0L,
    val adminRemarks: String = "",
    val leaveType: String = "",
    val department: String = "" // For better organization in admin view
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"
        
        const val TYPE_TEACHER = "teacher"
        const val TYPE_STAFF = "staff"
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "userName" to userName,
        "userType" to userType,
        "fromDate" to fromDate,
        "toDate" to toDate,
        "reason" to reason,
        "status" to status,
        "appliedAt" to appliedAt,
        "reviewedBy" to reviewedBy,
        "reviewedAt" to reviewedAt,
        "adminRemarks" to adminRemarks,
        "leaveType" to leaveType,
        "department" to department
    )
} 