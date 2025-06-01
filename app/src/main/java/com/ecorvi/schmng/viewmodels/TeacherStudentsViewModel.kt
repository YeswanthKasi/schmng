package com.ecorvi.schmng.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecorvi.schmng.models.Student
import com.ecorvi.schmng.models.StudentInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TeacherStudentsState(
    val students: List<StudentInfo> = emptyList(),
    val classes: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TeacherStudentsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val _studentsState = MutableStateFlow(TeacherStudentsState())
    val studentsState: StateFlow<TeacherStudentsState> = _studentsState
    
    private var allStudents = emptyList<StudentInfo>()

    init {
        refreshStudents()
    }

    fun refreshStudents() {
        viewModelScope.launch {
            try {
                _studentsState.value = _studentsState.value.copy(isLoading = true, error = null)
                
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _studentsState.value = _studentsState.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                    return@launch
                }

                // Get teacher's class (single class as string)
                val teacherDoc = db.collection("teachers").document(userId).get().await()
                val className = teacherDoc.getString("className")
                val teacherClasses = if (!className.isNullOrBlank()) listOf(className) else emptyList()

                // Get all students in teacher's classes
                if (teacherClasses.isNotEmpty()) {
                    val studentsQuery = db.collection("students")
                        .whereIn("className", teacherClasses)
                        .get()
                        .await()

                    allStudents = studentsQuery.documents.mapNotNull { doc ->
                        doc.toObject(Student::class.java)?.let { student ->
                            student.copy(id = doc.id).toStudentInfo()
                        }
                    }
                } else {
                    allStudents = emptyList()
                }

                _studentsState.value = _studentsState.value.copy(
                    students = allStudents,
                    classes = allStudents.map { it.className }.toSet(),
                    isLoading = false,
                    error = if (teacherClasses.isEmpty()) "No classes assigned to this teacher." else null
                )
            } catch (e: Exception) {
                _studentsState.value = _studentsState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load students"
                )
            }
        }
    }

    fun filterByClass(className: String?) {
        _studentsState.value = _studentsState.value.copy(
            students = if (className == null) allStudents
                      else allStudents.filter { it.className == className }
        )
    }
} 