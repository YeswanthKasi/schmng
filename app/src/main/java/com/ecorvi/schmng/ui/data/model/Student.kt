package com.ecorvi.schmng.ui.data.model

data class Student(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val className: String = "",
    val rollNumber: String = "",
    val parentName: String = "",
    val parentEmail: String = "",
    val parentPhone: String = "",
    val address: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val admissionNumber: String = "",
    val admissionDate: String = "",
    val status: String = "active"
) 