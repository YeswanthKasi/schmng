package com.ecorvi.schmng.ui.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Model for student grades/exams
 * Supports Indian government school standards: FA1, FA2, FA3, FA4, SA1, SA2
 */
@IgnoreExtraProperties
data class StudentGrade(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val className: String = "",
    val academicYear: String = "",
    val examType: String = "", // FA1, FA2, FA3, FA4, SA1, SA2
    val examDate: String = "",
    val subjects: Map<String, SubjectGrade> = emptyMap(), // Subject name -> SubjectGrade
    val totalMarks: Double = 0.0,
    val obtainedMarks: Double = 0.0,
    val percentage: Double = 0.0,
    val grade: String = "", // A+, A, B+, B, C, etc.
    val rank: Int = 0,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    constructor() : this(
        id = "",
        studentId = "",
        studentName = "",
        className = "",
        academicYear = "",
        examType = "",
        examDate = "",
        subjects = emptyMap(),
        totalMarks = 0.0,
        obtainedMarks = 0.0,
        percentage = 0.0,
        grade = "",
        rank = 0,
        createdBy = "",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Grade for a specific subject
 */
@IgnoreExtraProperties
data class SubjectGrade(
    val subjectName: String = "",
    val maxMarks: Double = 0.0,
    val obtainedMarks: Double = 0.0,
    val grade: String = "",
    val remarks: String = ""
) {
    constructor() : this(
        subjectName = "",
        maxMarks = 0.0,
        obtainedMarks = 0.0,
        grade = "",
        remarks = ""
    )
}

/**
 * Standard exam types for Indian government schools
 */
object ExamTypes {
    const val FA1 = "FA1" // Formative Assessment 1
    const val FA2 = "FA2" // Formative Assessment 2
    const val FA3 = "FA3" // Formative Assessment 3
    const val FA4 = "FA4" // Formative Assessment 4
    const val SA1 = "SA1" // Summative Assessment 1
    const val SA2 = "SA2" // Summative Assessment 2
    
    val ALL = listOf(FA1, FA2, FA3, FA4, SA1, SA2)
}

/**
 * Standard subjects for Indian government schools
 */
object StandardSubjects {
    val PRIMARY = listOf(
        "English",
        "Mathematics",
        "Hindi",
        "Environmental Studies",
        "Telugu"
    )
    
    val SECONDARY = listOf(
        "English",
        "Mathematics",
        "Hindi",
        "Science",
        "Social Studies",
        "Telugu"
    )
    
    val HIGHER_SECONDARY = listOf(
        "English",
        "Mathematics",
        "Physics",
        "Chemistry",
        "Biology",
        "Social Studies",
        "Telugu"
    )
}

/**
 * Grade calculation based on percentage
 */
object GradeCalculator {
    fun calculateGrade(percentage: Double): String {
        return when {
            percentage >= 90 -> "A+"
            percentage >= 80 -> "A"
            percentage >= 70 -> "B+"
            percentage >= 60 -> "B"
            percentage >= 50 -> "C+"
            percentage >= 40 -> "C"
            percentage >= 35 -> "D"
            else -> "F"
        }
    }
}

