package com.ecorvi.schmng.ui.data.model

data class Person(
    var id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val type: String = "", // student, teacher, staff
    val className: String = "",
    val rollNumber: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val mobileNo: String = "",
    val address: String = "",
    val age: Int = 0,
    val designation: String = "",
    val department: String = "",
    val password: String = "" // Added for registration, not stored in Firestore
) {
    constructor() : this(
        id = "",
        firstName = "",
        lastName = "",
        email = "",
        phone = "",
        type = "",
        className = "",
        rollNumber = "",
        gender = "",
        dateOfBirth = "",
        mobileNo = "",
        address = "",
        age = 0,
        designation = "",
        department = "",
        password = ""
    )
}
