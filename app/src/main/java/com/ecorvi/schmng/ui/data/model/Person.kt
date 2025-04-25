package com.ecorvi.schmng.ui.data.model

data class Person(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val type: String = "",
    val className: String = "",
    val rollNumber: String = "",
    var gender: String = "",
    var dateOfBirth: String = "",
    var mobileNo: String = "",
    var address: String = "",
    var age: Int = 0
) {
    constructor() : this(
        id = "",
        firstName = "",
        lastName = "",
        email = "",
        password = "",
        phone = "",
        type = "",
        className = "",
        rollNumber = "",
        gender = "",
        dateOfBirth = "",
        mobileNo = "",
        address = "",
        age = 0
    )
}
