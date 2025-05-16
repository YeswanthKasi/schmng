package com.ecorvi.schmng.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecorvi.schmng.models.AttendanceRecord
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.UserType
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "TeacherAttendanceVM"

data class TeacherInfo(
    val name: String = "",
    val id: String = "",
    val isReplacement: Boolean = false
)

sealed class TeacherAttendanceState {
    object Loading : TeacherAttendanceState()
    object NotTakenYet : TeacherAttendanceState()
    data class Error(val message: String) : TeacherAttendanceState()
    data class Success(
        val regularTeacher: TeacherInfo,
        val attendanceStatus: AttendanceStatus?,
        val replacementTeacher: TeacherInfo? = null
    ) : TeacherAttendanceState()
}

class TeacherAttendanceViewModel : ViewModel() {
    private val _attendanceState = MutableStateFlow<TeacherAttendanceState>(TeacherAttendanceState.Loading)
    val attendanceState: StateFlow<TeacherAttendanceState> = _attendanceState

    fun fetchTeacherAttendance(className: String) {
        viewModelScope.launch {
            _attendanceState.value = TeacherAttendanceState.Loading
            Log.d(TAG, "Fetching teacher attendance for class: $className")
            
            try {
                // First get the class teacher for this class
                val formattedClassName = if (!className.startsWith("Class ")) "Class $className" else className
                Log.d(TAG, "Searching for teacher with formatted class name: $formattedClassName")
                
                val teachersSnapshot = FirebaseFirestore.getInstance()
                    .collection("teachers")
                    .whereEqualTo("className", formattedClassName)
                    .get()
                    .await()

                if (teachersSnapshot.isEmpty) {
                    // Try without "Class" prefix
                    val classNumber = className.replace("Class ", "")
                    Log.d(TAG, "No teacher found with formatted name, trying with class number: $classNumber")
                    
                    val altTeachersSnapshot = FirebaseFirestore.getInstance()
                        .collection("teachers")
                        .whereEqualTo("className", classNumber)
                        .get()
                        .await()
                        
                    if (altTeachersSnapshot.isEmpty) {
                        Log.e(TAG, "No class teacher found for any class format")
                        _attendanceState.value = TeacherAttendanceState.Error("No class teacher found")
                        return@launch
                    } else {
                        val teacher = altTeachersSnapshot.documents.firstOrNull()?.toObject(Person::class.java)
                        if (teacher != null) {
                            Log.d(TAG, "Found teacher with class number format: ${teacher.firstName} ${teacher.lastName}")
                            handleTeacherFound(teacher, classNumber)
                        } else {
                            Log.e(TAG, "Failed to convert teacher document to Person object")
                            _attendanceState.value = TeacherAttendanceState.Error("Failed to get teacher information")
                        }
                    }
                } else {
                    val teacher = teachersSnapshot.documents.firstOrNull()?.toObject(Person::class.java)
                    if (teacher != null) {
                        Log.d(TAG, "Found teacher with formatted class name: ${teacher.firstName} ${teacher.lastName}")
                        handleTeacherFound(teacher, formattedClassName)
                    } else {
                        Log.e(TAG, "Failed to convert teacher document to Person object")
                        _attendanceState.value = TeacherAttendanceState.Error("Failed to get teacher information")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching teacher attendance: ${e.message}", e)
                _attendanceState.value = TeacherAttendanceState.Error("Error: ${e.message}")
            }
        }
    }

    private suspend fun handleTeacherFound(teacher: Person?, className: String) {
        if (teacher == null) {
            Log.e(TAG, "Could not convert teacher document to Person object")
            _attendanceState.value = TeacherAttendanceState.Error("Could not get teacher information")
            return
        }

        Log.d(TAG, "Found class teacher: ${teacher.firstName} ${teacher.lastName} (ID: ${teacher.id})")

        val regularTeacher = TeacherInfo(
            name = "${teacher.firstName} ${teacher.lastName}",
            id = teacher.id
        )

        try {
            // Get today's date in yyyy-MM-dd format
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            Log.d(TAG, "Checking attendance for date: $today")

            // Get regular teacher's attendance
            val attendanceDoc = FirebaseFirestore.getInstance()
                .collection("attendance")
                .document(today)
                .collection("teacher")
                .document(teacher.id)
                .get()
                .await()

            // If attendance document doesn't exist, it means attendance hasn't been taken
            if (!attendanceDoc.exists()) {
                Log.d(TAG, "No attendance record found for today")
                _attendanceState.value = TeacherAttendanceState.NotTakenYet
                return
            }

            val record = attendanceDoc.toObject(AttendanceRecord::class.java)
            val status = record?.status
            Log.d(TAG, "Teacher attendance status: $status")

            // If teacher is absent or on leave, check for replacement
            if (status == AttendanceStatus.ABSENT || status == AttendanceStatus.PERMISSION) {
                Log.d(TAG, "Teacher is absent/on leave, checking for replacement")
                
                // Get the teacher's class name from their document and standardize it
                val teacherDoc = FirebaseFirestore.getInstance()
                    .collection("teachers")
                    .document(teacher.id)
                    .get()
                    .await()
                
                val rawClassName = teacherDoc.getString("className") ?: className
                val standardizedClassName = if (!rawClassName.startsWith("Class ")) "Class $rawClassName" else rawClassName
                Log.d(TAG, "Teacher's standardized class name: $standardizedClassName")
                Log.d(TAG, "Teacher's raw class name: $rawClassName")
                Log.d(TAG, "Original class name parameter: $className")
                Log.d(TAG, "Teacher document data: ${teacherDoc.data}")
                Log.d(TAG, "Checking replacement for teacher ID: ${teacher.id} in class: $standardizedClassName")
                
                try {
                    // Check for replacement teacher using the standardized class name
                    val replacementPath = "substitute_teachers/$today/$standardizedClassName/assigned_teacher"
                    Log.d(TAG, "Attempting to fetch replacement from path: $replacementPath")
                    
                    // First check if the date document exists
                    val dateDoc = FirebaseFirestore.getInstance()
                        .collection("substitute_teachers")
                        .document(today)
                        .get()
                        .await()
                    
                    Log.d(TAG, "Date document exists: ${dateDoc.exists()}")
                    if (dateDoc.exists()) {
                        Log.d(TAG, "Date document data: ${dateDoc.data}")
                    }
                    
                    // Then check if the class collection exists
                    val classCollection = FirebaseFirestore.getInstance()
                        .collection("substitute_teachers")
                        .document(today)
                        .collection(standardizedClassName)
                        .get()
                        .await()
                    
                    Log.d(TAG, "Class collection exists: ${!classCollection.isEmpty()}")
                    if (!classCollection.isEmpty()) {
                        val docIds = classCollection.documents.joinToString(", ") { doc -> doc.id }
                        val docData = classCollection.documents.joinToString(", ") { doc -> doc.data.toString() }
                        Log.d(TAG, "Class collection documents: $docIds")
                        Log.d(TAG, "Class collection data: $docData")
                    }
                    
                    val replacementDoc = FirebaseFirestore.getInstance()
                        .collection("substitute_teachers")
                        .document(today)
                        .collection(standardizedClassName)
                        .document("assigned_teacher")
                        .get()
                        .await()

                    Log.d(TAG, "Replacement document exists: ${replacementDoc.exists()}")
                    if (replacementDoc.exists()) {
                        Log.d(TAG, "Replacement document data: ${replacementDoc.data}")
                        val teacherId = replacementDoc.getString("teacherId")
                        val originalTeacherId = replacementDoc.getString("originalTeacherId")
                        Log.d(TAG, "Found replacement teacher ID: $teacherId for original teacher: $originalTeacherId")
                        
                        if (originalTeacherId == teacher.id) {
                            handleReplacementTeacher(replacementDoc, regularTeacher, status)
                        } else {
                            Log.d(TAG, "Replacement exists but for different teacher. Expected: ${teacher.id}, Found: $originalTeacherId")
                            _attendanceState.value = TeacherAttendanceState.Success(
                                regularTeacher = regularTeacher,
                                attendanceStatus = status
                            )
                        }
                    } else {
                        // No replacement found
                        Log.d(TAG, "No replacement teacher document found at path: $replacementPath")
                        _attendanceState.value = TeacherAttendanceState.Success(
                            regularTeacher = regularTeacher,
                            attendanceStatus = status
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching replacement teacher: ${e.message}", e)
                    _attendanceState.value = TeacherAttendanceState.Error("Error fetching replacement: ${e.message}")
                }
                return
            }

            // If we reach here, teacher is present
            Log.d(TAG, "Teacher is present, no replacement needed")
            _attendanceState.value = TeacherAttendanceState.Success(
                regularTeacher = regularTeacher,
                attendanceStatus = status
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleTeacherFound: ${e.message}", e)
            _attendanceState.value = TeacherAttendanceState.Error("Error: ${e.message}")
        }
    }

    private suspend fun handleReplacementTeacher(
        replacementDoc: com.google.firebase.firestore.DocumentSnapshot,
        regularTeacher: TeacherInfo,
        status: AttendanceStatus?
    ) {
        try {
            val replacementTeacherId = replacementDoc.getString("teacherId")
            Log.d(TAG, "Processing replacement teacher ID: $replacementTeacherId")
            
            if (replacementTeacherId != null) {
                try {
                    // Fetch replacement teacher details
                    val replacementTeacherDoc = FirebaseFirestore.getInstance()
                        .collection("teachers")
                        .document(replacementTeacherId)
                        .get()
                        .await()

                    Log.d(TAG, "Fetching replacement teacher from: ${replacementTeacherDoc.reference.path}")
                    Log.d(TAG, "Document exists: ${replacementTeacherDoc.exists()}")

                    if (!replacementTeacherDoc.exists()) {
                        Log.e(TAG, "Replacement teacher document does not exist")
                        _attendanceState.value = TeacherAttendanceState.Error("Replacement teacher not found")
                        return
                    }

                    val replacementTeacher = replacementTeacherDoc.toObject(Person::class.java)
                    if (replacementTeacher != null) {
                        Log.d(TAG, "Successfully found replacement teacher: ${replacementTeacher.firstName} ${replacementTeacher.lastName}")
                        Log.d(TAG, "Replacement teacher class: ${replacementTeacher.className}")
                        _attendanceState.value = TeacherAttendanceState.Success(
                            regularTeacher = regularTeacher,
                            attendanceStatus = status,
                            replacementTeacher = TeacherInfo(
                                name = "${replacementTeacher.firstName} ${replacementTeacher.lastName}",
                                id = replacementTeacher.id,
                                isReplacement = true
                            )
                        )
                    } else {
                        Log.e(TAG, "Could not convert replacement teacher document to Person object")
                        _attendanceState.value = TeacherAttendanceState.Success(
                            regularTeacher = regularTeacher,
                            attendanceStatus = status
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching replacement teacher details: ${e.message}")
                    if (e.message?.contains("PERMISSION_DENIED") == true) {
                        Log.e(TAG, "Permission denied while fetching replacement teacher. Check Firestore rules.")
                        _attendanceState.value = TeacherAttendanceState.Error(
                            "Unable to access replacement teacher information. Please contact administrator."
                        )
                    } else {
                        _attendanceState.value = TeacherAttendanceState.Error("Error: ${e.message}")
                    }
                }
            } else {
                Log.e(TAG, "Replacement document exists but no teacherId field found")
                _attendanceState.value = TeacherAttendanceState.Success(
                    regularTeacher = regularTeacher,
                    attendanceStatus = status
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleReplacementTeacher: ${e.message}", e)
            _attendanceState.value = TeacherAttendanceState.Error("Error fetching replacement teacher: ${e.message}")
        }
    }
} 