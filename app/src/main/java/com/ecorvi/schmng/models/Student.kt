package com.ecorvi.schmng.models

data class Student(
    val id: String = "",
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val className: String = "",
    val rollNumber: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val parentName: String = "",
    val parentPhone: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val admissionNumber: String = "",
    val admissionDate: String = "",
    val isActive: Boolean = true
) {
    fun toStudentInfo(attendancePercentage: Float = 0f): StudentInfo {
        return StudentInfo(
            id = id,
            firstName = firstName,
            lastName = lastName,
            className = className,
            rollNumber = rollNumber,
            attendancePercentage = attendancePercentage
        )
    }

    companion object {
        fun fromStudentInfo(info: StudentInfo, existingStudent: Student? = null): Student {
            return Student(
                id = info.id,
                userId = existingStudent?.userId ?: "",
                firstName = info.firstName,
                lastName = info.lastName,
                className = info.className,
                rollNumber = info.rollNumber,
                email = existingStudent?.email ?: "",
                phoneNumber = existingStudent?.phoneNumber ?: "",
                address = existingStudent?.address ?: "",
                parentName = existingStudent?.parentName ?: "",
                parentPhone = existingStudent?.parentPhone ?: "",
                dateOfBirth = existingStudent?.dateOfBirth ?: "",
                gender = existingStudent?.gender ?: "",
                admissionNumber = existingStudent?.admissionNumber ?: "",
                admissionDate = existingStudent?.admissionDate ?: "",
                isActive = existingStudent?.isActive ?: true
            )
        }
    }
} 