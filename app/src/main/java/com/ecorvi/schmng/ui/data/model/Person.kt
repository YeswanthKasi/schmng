package com.ecorvi.schmng.ui.data.model

data class Person(
    var id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val type: String = "", // student, teacher, staff, parent
    val className: String = "",
    val section: String = "", // Added section field
    val rollNumber: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val mobileNo: String = "",
    val address: String = "",
    val age: Int = 0,
    val designation: String = "",
    val department: String = "",
    val password: String = "", // Added for registration, not stored in Firestore
    // Admissions-related fields (students only; harmless defaults for others)
    val admissionNumber: String = "",
    val admissionDate: String = "",
    val academicYear: String = "",
    val aadharNumber: String = "",
    val aaparId: String = "",
    val caste: String = "",
    val category: String = "",
    val subCaste: String = "",
    val feeStructure: Double = 0.0,
    val feePaid: Double = 0.0,
    val feeRemaining: Double = 0.0,
    val modeOfTransport: String = "", // Bus, Own Transport, etc.
    
    // For students: their parent's info
    val parentInfo: ParentInfo? = null,
    
    // For parents: their child's info
    val childInfo: ChildInfo? = null
) {
    constructor() : this(
        id = "",
        firstName = "",
        lastName = "",
        email = "",
        phone = "",
        type = "",
        className = "",
        section = "",
        rollNumber = "",
        gender = "",
        dateOfBirth = "",
        mobileNo = "",
        address = "",
        age = 0,
        designation = "",
        department = "",
        password = "",
        admissionNumber = "",
        admissionDate = "",
        academicYear = "",
        aadharNumber = "",
        aaparId = "",
        caste = "",
        category = "",
        subCaste = "",
        feeStructure = 0.0,
        feePaid = 0.0,
        feeRemaining = 0.0,
        modeOfTransport = "",
        parentInfo = null,
        childInfo = null
    )
}

// Separate data class for parent information
data class ParentInfo(
    val id: String,
    val name: String,
    val email: String,
    val phone: String
) {
    constructor() : this("", "", "", "")
}

// Separate data class for child information
data class ChildInfo(
    val id: String,
    val name: String,
    val className: String,
    val section: String = "", // Added section field
    val rollNumber: String
) {
    constructor() : this("", "", "", "", "")
}
