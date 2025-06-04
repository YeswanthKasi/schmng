package com.ecorvi.schmng.models

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class AttendanceRecord(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",

    @get:PropertyName("userType") @set:PropertyName("userType")
    var userType: UserType = UserType.STUDENT,

    @get:PropertyName("date") @set:PropertyName("date")
    var date: Long = System.currentTimeMillis(),

    @get:PropertyName("status") @set:PropertyName("status")
    var status: AttendanceStatus = AttendanceStatus.ABSENT,

    @get:PropertyName("markedBy") @set:PropertyName("markedBy")
    var markedBy: String = "",

    @get:PropertyName("remarks") @set:PropertyName("remarks")
    var remarks: String = "",

    @get:PropertyName("lastModified") @set:PropertyName("lastModified")
    var lastModified: Long = System.currentTimeMillis(),
    
    @get:PropertyName("classId") @set:PropertyName("classId")
    var classId: String = "",
    
    @get:PropertyName("className") @set:PropertyName("className")
    var className: String = ""
) {
    // Required for Firestore
    constructor() : this(
        id = "",
        userId = "",
        userType = UserType.STUDENT,
        date = System.currentTimeMillis(),
        status = AttendanceStatus.ABSENT,
        markedBy = "",
        remarks = "",
        lastModified = System.currentTimeMillis(),
        classId = "",
        className = ""
    )
}

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