package com.ecorvi.schmng.ui.data.model

import com.ecorvi.schmng.ui.data.model.AttendanceOption

data class Person(
    var id: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    var gender: String = "",
    var dateOfBirth: String = "",
    var mobileNo: String = "",
    var address: String = "",
    var className: String = "",
    var age: Int = 0,
    var attendance: AttendanceOption = AttendanceOption.NONE
) {
    constructor() : this("", "", "", "", "", "", "", "", "", 0)
}
