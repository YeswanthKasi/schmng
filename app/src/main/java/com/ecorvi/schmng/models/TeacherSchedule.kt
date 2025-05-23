package com.ecorvi.schmng.models

data class TeacherSchedule(
    val id: String,
    val classes: List<String>,
    val schedule: List<ScheduleItem> = emptyList()
)

data class ScheduleItem(
    val subject: String,
    val timeSlot: String,
    val className: String,
    val roomNumber: String? = null
) 