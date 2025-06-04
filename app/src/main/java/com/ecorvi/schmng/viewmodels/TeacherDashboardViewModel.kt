package com.ecorvi.schmng.viewmodels

import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecorvi.schmng.models.AttendanceRecord
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.TeacherData
import com.ecorvi.schmng.models.TeacherSchedule
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Timetable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import com.ecorvi.schmng.models.UserType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Data class to hold attendance statistics
data class AttendanceStats(
    val attendanceRate: Float = 0f,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val leaveCount: Int = 0,
    val totalCount: Int = 0
)

class TeacherDashboardViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val _teacherData = MutableStateFlow<TeacherData?>(null)
    val teacherData: StateFlow<TeacherData?> = _teacherData
    
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    
    private val _attendanceLoading = MutableStateFlow(false)
    val attendanceLoading: StateFlow<Boolean> = _attendanceLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _timeSlots = MutableStateFlow<List<String>>(emptyList())
    val timeSlots: StateFlow<List<String>> = _timeSlots
    
    private val _timetables = MutableStateFlow<List<Timetable>>(emptyList())
    val timetables: StateFlow<List<Timetable>> = _timetables
    
    private val _attendanceStats = MutableStateFlow(AttendanceStats())
    val attendanceStats: StateFlow<AttendanceStats> = _attendanceStats
    
    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords

    private var timeSlotsListener: ListenerRegistration? = null

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear

    init {
        loadTeacherData()
        setupTimeSlotsListener()
    }

    private fun loadTeacherData() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    handleError("User not authenticated")
                    return@launch
                }

                Log.d("TeacherDashboard", "Loading teacher data for user: $userId")
                
                // First get the teacher document
                val teacherDoc = db.collection("teachers").document(userId).get().await()
                if (!teacherDoc.exists()) {
                    handleError("Teacher data not found")
                    return@launch
                }

                // Map teacher data according to Firestore structure
                val teacher = TeacherData(
                    id = teacherDoc.getString("id") ?: userId,
                    firstName = teacherDoc.getString("firstName") ?: "",
                    lastName = teacherDoc.getString("lastName") ?: "",
                    email = teacherDoc.getString("email") ?: "",
                    subjects = (teacherDoc.get("subjects") as? List<String>) ?: emptyList(),
                    classes = (teacherDoc.get("classes") as? List<String>) ?: emptyList()
                )

                _teacherData.value = teacher
                Log.d("TeacherDashboard", "Teacher data loaded: ${teacher.firstName} ${teacher.lastName}")

                // Get class information from classes list
                val teacherClasses = teacher.classes
                if (teacherClasses.isNotEmpty()) {
                    Log.d("TeacherDashboard", "Teacher's assigned classes: $teacherClasses")
                }

                // Get assigned classes from teacher document
                val assignedClasses = teacherDoc.get("classes") as? List<String> ?: emptyList()
                val className = teacherDoc.getString("className")
                
                // Combine both class sources
                val allAssignedClasses = if (className != null && className.isNotEmpty()) {
                    assignedClasses + listOf(className)
                } else {
                    assignedClasses
                }
                
                Log.d("TeacherDashboard", "Assigned classes from document: $allAssignedClasses")

                // Also check the "users" collection for class information
                val userDoc = db.collection("users").document(userId).get().await()
                if (userDoc.exists()) {
                    val userClassName = userDoc.getString("className")
                    if (userClassName != null && userClassName.isNotEmpty()) {
                        Log.d("TeacherDashboard", "Found class in user document: $userClassName")
                    }
                }

                // Fetch timetables for the teacher
                FirestoreDatabase.fetchTimetablesForTeacher(
                    teacherName = "${teacher.firstName} ${teacher.lastName}",
                    onComplete = { fetchedTimetables ->
                        _timetables.value = fetchedTimetables.sortedBy { parseTimeSlot(it.timeSlot) }
                        Log.d("TeacherDashboard", "Timetables loaded: ${fetchedTimetables.size} entries")
                        
                        // Extract classes from timetables
                        val timetableClasses = fetchedTimetables.map { it.classGrade }.distinct()
                        Log.d("TeacherDashboard", "Classes from timetables: $timetableClasses")
                        
                        // After timetables are loaded, fetch attendance
                        viewModelScope.launch {
                            val calendar = Calendar.getInstance()
                            fetchMonthlyAttendance(
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH)
                            )
                        }
                        _loading.value = false
                    },
                    onFailure = { e ->
                        Log.e("TeacherDashboard", "Failed to load timetables: ${e.message}")
                        handleError("Failed to load timetables: ${e.message}")
                        _loading.value = false
                    }
                )
            } catch (e: Exception) {
                Log.e("TeacherDashboard", "Error in loadTeacherData: ${e.message}")
                handleError("Error loading teacher data: ${e.message}")
                _loading.value = false
            }
        }
    }

    fun fetchMonthlyAttendance(year: Int, month: Int) {
        viewModelScope.launch {
            try {
                _attendanceLoading.value = true
                _error.value = null
                
                val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
                Log.d("TeacherAttendance", "Fetching monthly attendance for user: $userId, year: $year, month: $month")
                
                // Get month start and end dates
                val calendar = Calendar.getInstance()
                calendar.set(year, month, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.timeInMillis
                
                calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val monthEnd = calendar.timeInMillis

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                Log.d("TeacherAttendance", "Fetching attendance from ${dateFormat.format(Date(monthStart))} to ${dateFormat.format(Date(monthEnd))}")
                
                // Use the common date range fetching function
                fetchAttendanceForDateRange(monthStart, monthEnd)
            } catch (e: Exception) {
                Log.e("TeacherAttendance", "Error fetching monthly attendance: ${e.message}", e)
                handleError("Failed to fetch monthly attendance data: ${e.message}")
                _attendanceLoading.value = false
            }
        }
    }

    // Function to create test attendance records for debugging
    private fun createTestAttendanceRecords(classes: List<String>): List<AttendanceRecord> {
        val testRecords = mutableListOf<AttendanceRecord>()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Create 20 students per class
        classes.forEach { rawClassId ->
            // Format class ID as "Class X" if it's not already in that format
            val classId = if (!rawClassId.startsWith("Class ", ignoreCase = true)) {
                "Class $rawClassId"
            } else {
                rawClassId
            }
            
            for (studentId in 1..20) {
                // Create records for the last 10 days
                for (day in 1..10) {
                    calendar.set(currentYear, currentMonth, day)
                    
                    // Randomly assign attendance status
                    val status = when ((0..10).random()) {
                        in 0..7 -> AttendanceStatus.PRESENT // 80% present
                        in 8..9 -> AttendanceStatus.ABSENT  // 20% absent
                        else -> AttendanceStatus.PERMISSION  // 10% permission
                    }
                    
                    val record = AttendanceRecord(
                        id = "test_${classId}_${studentId}_${day}",
                        userId = "student_$studentId",
                        userType = UserType.STUDENT,
                        date = calendar.timeInMillis,
                        status = status,
                        markedBy = auth.currentUser?.uid ?: "unknown",
                        remarks = "",
                        lastModified = System.currentTimeMillis(),
                        classId = classId,
                        className = classId
                    )
                    
                    testRecords.add(record)
                }
            }
        }
        
        return testRecords
    }

    fun handleError(errorMessage: String) {
        Log.e("TeacherDashboard", errorMessage)
        _error.value = errorMessage
        _loading.value = false
        _attendanceLoading.value = false
    }

    fun clearError() {
        _error.value = null
    }

    fun setupTimeSlotsListener() {
        timeSlotsListener = FirestoreDatabase.listenForTimeSlotUpdates(
            onUpdate = { slots ->
                _timeSlots.value = slots.sortedBy { parseTimeSlot(it) }
            },
            onError = { e ->
                Log.e("TeacherDashboard", "Error listening for time slots: ${e.message}")
                _error.value = "Failed to load time slots: ${e.message}"
            }
        )
    }

    private fun parseTimeSlot(timeSlot: String): Int {
        return try {
            val startTime = timeSlot.split("-")[0].trim()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
            timeFormat.isLenient = false
            val date = timeFormat.parse(startTime) ?: return 0
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        } catch (e: Exception) {
            0
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _teacherData.value = null
                _timetables.value = emptyList()
                _timeSlots.value = emptyList()
                _attendanceStats.value = AttendanceStats()
                _attendanceRecords.value = emptyList()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to sign out: ${e.message}"
            }
        }
    }

    fun fetchWeeklyAttendance() {
        viewModelScope.launch {
            try {
                _attendanceLoading.value = true
                _error.value = null
                
                // Get current week start and end dates
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.timeInMillis
                
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val weekEnd = calendar.timeInMillis
                
                fetchAttendanceForDateRange(weekStart, weekEnd)
            } catch (e: Exception) {
                _error.value = "Failed to fetch weekly attendance: ${e.message}"
                _attendanceLoading.value = false
            }
        }
    }

    // Function to normalize class ID format
    private fun normalizeClassId(classId: String): String {
        // Remove extra spaces and convert to lowercase for comparison
        val normalized = classId.trim().lowercase().replace("\\s+".toRegex(), " ")
        
        // If it's just a number, format as "Class X"
        if (normalized.matches("\\d+".toRegex())) {
            return "Class $normalized"
        }
        
        // If it starts with "class" (case insensitive), ensure proper format
        if (normalized.startsWith("class")) {
            val number = normalized.substring(5).trim()
            if (number.isNotEmpty()) {
                return "Class $number"
            }
        }
        
        return classId
    }

    // Function to check if a class ID matches any of the teacher's classes
    private fun isTeacherClass(classId: String, teacherClasses: List<String>): Boolean {
        val normalizedInput = normalizeClassId(classId)
        val normalizedTeacherClasses = teacherClasses.map { normalizeClassId(it) }
        
        return normalizedTeacherClasses.contains(normalizedInput)
    }

    private suspend fun fetchAttendanceForDateRange(startTime: Long, endTime: Long) {
        try {
            _attendanceLoading.value = true
            _error.value = null
            
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            // Get teacher's assigned class - Cache this value
            val normalizedClass = withContext(Dispatchers.IO) {
                val teacherDoc = db.collection("teachers").document(userId).get().await()
                val assignedClass = teacherDoc.getString("className")
                
                if (assignedClass == null) {
                    val userDoc = db.collection("users").document(userId).get().await()
                    val userClassName = userDoc.getString("className")
                    if (userClassName == null) {
                        _attendanceStats.value = AttendanceStats()
                        _attendanceRecords.value = emptyList()
                        return@withContext null
                    }
                    userClassName
                } else {
                    assignedClass
                }?.let { className ->
                    if (!className.startsWith("Class ", ignoreCase = true)) {
                        "Class $className"
                    } else {
                        className
                    }
                }
            } ?: run {
                _attendanceLoading.value = false
                return
            }

            // Get all dates between start and end time
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val records = mutableListOf<AttendanceRecord>()
            
            calendar.timeInMillis = startTime
            val endCalendar = Calendar.getInstance()
            endCalendar.timeInMillis = endTime

            // Fetch student IDs for the class first
            val studentsInClass = withContext(Dispatchers.IO) {
                db.collection("students")
                    .whereEqualTo("className", normalizedClass)
                    .get()
                    .await()
                    .documents
                    .map { it.id }
                    .toSet()
            }
            
            // Batch fetch attendance records for all dates
            withContext(Dispatchers.IO) {
                while (calendar.timeInMillis <= endTime) {
                    val dateId = dateFormat.format(calendar.time)
                    
                    try {
                        val snapshot = db.collection("attendance")
                            .document(dateId)
                            .collection("student")
                            .whereIn("userId", studentsInClass.toList())
                            .get()
                            .await()
                        
                        if (!snapshot.isEmpty) {
                            snapshot.documents.mapNotNull { doc ->
                                try {
                                    AttendanceRecord(
                                        id = doc.getString("id") ?: doc.id,
                                        userId = doc.getString("userId") ?: "",
                                        userType = UserType.valueOf(doc.getString("userType") ?: "STUDENT"),
                                        date = doc.getLong("date") ?: calendar.timeInMillis,
                                        status = AttendanceStatus.valueOf(doc.getString("status") ?: "ABSENT"),
                                        markedBy = doc.getString("markedBy") ?: "",
                                        remarks = doc.getString("remarks") ?: "",
                                        lastModified = doc.getLong("lastModified") ?: System.currentTimeMillis(),
                                        classId = normalizedClass,
                                        className = normalizedClass
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }.let { records.addAll(it) }
                        }
                    } catch (e: Exception) {
                        Log.e("TeacherAttendance", "Error fetching records for date $dateId: ${e.message}")
                    }
                    
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            
            _attendanceRecords.value = records
            
            // Calculate statistics
            val totalRecords = records.size
            val presentCount = records.count { it.status == AttendanceStatus.PRESENT }
            val absentCount = records.count { it.status == AttendanceStatus.ABSENT }
            val leaveCount = records.count { it.status == AttendanceStatus.PERMISSION }
            val attendanceRate = if (totalRecords > 0) (presentCount.toFloat() / totalRecords) * 100 else 0f
            
            _attendanceStats.value = AttendanceStats(
                attendanceRate = attendanceRate,
                presentCount = presentCount,
                absentCount = absentCount,
                leaveCount = leaveCount,
                totalCount = totalRecords
            )
            
            _attendanceLoading.value = false
        } catch (e: Exception) {
            Log.e("TeacherAttendance", "Error fetching attendance: ${e.message}", e)
            handleError("Failed to fetch attendance data: ${e.message}")
            _attendanceLoading.value = false
        }
    }
    
    // Function to create test weekly attendance records for debugging
    private fun createTestWeeklyAttendanceRecords(classes: List<String>, startTime: Long, endTime: Long): List<AttendanceRecord> {
        val testRecords = mutableListOf<AttendanceRecord>()
        val startCalendar = Calendar.getInstance()
        startCalendar.timeInMillis = startTime
        val endCalendar = Calendar.getInstance()
        endCalendar.timeInMillis = endTime
        
        // Create 20 students per class
        classes.forEach { rawClassId ->
            // Format class ID as "Class X" if it's not already in that format
            val classId = if (!rawClassId.startsWith("Class ", ignoreCase = true)) {
                "Class $rawClassId"
            } else {
                rawClassId
            }
            
            for (studentId in 1..20) {
                // Create records for each day in the week
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = startTime
                
                while (calendar.timeInMillis <= endTime) {
                    // Randomly assign attendance status
                    val status = when ((0..10).random()) {
                        in 0..7 -> AttendanceStatus.PRESENT // 80% present
                        in 8..9 -> AttendanceStatus.ABSENT  // 20% absent
                        else -> AttendanceStatus.PERMISSION  // 10% permission
                    }
                    
                    val record = AttendanceRecord(
                        id = "test_weekly_${classId}_${studentId}_${calendar.get(Calendar.DAY_OF_WEEK)}",
                        userId = "student_$studentId",
                        userType = UserType.STUDENT,
                        date = calendar.timeInMillis,
                        status = status,
                        markedBy = auth.currentUser?.uid ?: "unknown",
                        remarks = "",
                        lastModified = System.currentTimeMillis(),
                        classId = classId,
                        className = classId
                    )
                    
                    testRecords.add(record)
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }
        
        return testRecords
    }

    fun updateSelectedDate(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
        fetchMonthlyAttendance(year, month)
    }

    fun selectPreviousMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, _selectedYear.value)
        calendar.set(Calendar.MONTH, _selectedMonth.value)
        calendar.add(Calendar.MONTH, -1)
        
        _selectedYear.value = calendar.get(Calendar.YEAR)
        _selectedMonth.value = calendar.get(Calendar.MONTH)
        fetchMonthlyAttendance(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
    }

    fun selectNextMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, _selectedYear.value)
        calendar.set(Calendar.MONTH, _selectedMonth.value)
        calendar.add(Calendar.MONTH, 1)
        
        // Don't allow selecting future months
        val currentCalendar = Calendar.getInstance()
        if (calendar.timeInMillis <= currentCalendar.timeInMillis) {
            _selectedYear.value = calendar.get(Calendar.YEAR)
            _selectedMonth.value = calendar.get(Calendar.MONTH)
            fetchMonthlyAttendance(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
        }
    }
} 
