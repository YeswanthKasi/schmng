package com.ecorvi.schmng.ui.data.model

data class Person(
    var id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val type: String = "", // student, teacher, staff, parent
    val className: String = "",
    val section: String = "", // Added section field
    val rollNumber: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val mobileNo: String = "",
    val address: String = "",
    val age: Int = 0,
    val designation: String = "",
    val department: String = "",
    val password: String = "", // Added for registration, not stored in Firestore
    
    // For students: their parent's info
    val parentInfo: ParentInfo? = null,
    
    // For parents: their child's info
    val childInfo: ChildInfo? = null
) {
    constructor() : this(
        id = "",
        firstName = "",
        lastName = "",
        email = "",
        phone = "",
        type = "",
        className = "",
        section = "",
        rollNumber = "",
        gender = "",
        dateOfBirth = "",
        mobileNo = "",
        address = "",
        age = 0,
        designation = "",
        department = "",
        password = "",
        parentInfo = null,
        childInfo = null
    )
}

// Separate data class for parent information
data class ParentInfo(
    val id: String,
    val name: String,
    val email: String,
    val phone: String
) {
    constructor() : this("", "", "", "")
}

// Separate data class for child information
data class ChildInfo(
    val id: String,
    val name: String,
    val className: String,
    val section: String = "", // Added section field
    val rollNumber: String
) {
    constructor() : this("", "", "", "", "")
}
