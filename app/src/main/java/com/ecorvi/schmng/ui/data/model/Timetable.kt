package com.ecorvi.schmng.ui.data.model

data class Timetable(
    var id: String = "",
    var classGrade: String = "", // 1st to 10th
    var dayOfWeek: String = "", // Monday, Tuesday, etc.
    var timeSlot: String = "", // e.g., "09:00 AM - 10:00 AM"
    var subject: String = "",
    var teacher: String = "",
    var roomNumber: String = "",
    var isActive: Boolean = true
) {
    constructor() : this("", "", "", "", "", "", "", true)
} 