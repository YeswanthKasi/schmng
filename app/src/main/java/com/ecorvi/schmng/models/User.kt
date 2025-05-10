package com.ecorvi.schmng.models

data class User(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val mobileNo: String = "",
    val phone: String = "",
    val address: String = "",
    val age: Int = 0,
    val gender: String = "",
    val dateOfBirth: String = "",
    val className: String = "",
    val rollNumber: String = "",
    val department: String = "",
    val designation: String = "",
    val type: String = "",
    val password: String = ""
) {
    // Empty constructor for Firestore
    constructor() : this(id = "")
    
    val fullName: String
        get() = "$firstName $lastName"
        
    fun getUserType(): UserType {
        return when (type.lowercase()) {
            "staff" -> UserType.STAFF
            "teacher" -> UserType.TEACHER
            else -> UserType.STUDENT
        }
    }
} 