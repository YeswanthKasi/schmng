package com.ecorvi.schmng.models

data class TeacherData(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val subjects: List<String> = emptyList(),
    val classes: List<String> = emptyList()
) 