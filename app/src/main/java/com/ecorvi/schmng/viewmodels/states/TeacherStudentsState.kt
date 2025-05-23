package com.ecorvi.schmng.viewmodels.states

import com.ecorvi.schmng.models.StudentInfo

data class TeacherStudentsState(
    val students: List<StudentInfo> = emptyList(),
    val classes: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
) 