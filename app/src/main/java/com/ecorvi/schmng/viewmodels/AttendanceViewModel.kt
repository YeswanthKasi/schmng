package com.ecorvi.schmng.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecorvi.schmng.models.AttendanceRecord
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.User
import com.ecorvi.schmng.models.UserType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AttendanceViewModel : ViewModel() {
    private val db = Firebase.firestore.apply {
        // Enable Firestore logging for debugging
        FirebaseFirestore.setLoggingEnabled(true)
        // Verify Firestore instance
        println("Firestore instance initialized: $this")
    }
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users
    
    private val _attendanceRecords = MutableStateFlow<Map<String, AttendanceRecord>>(emptyMap())
    val attendanceRecords: StateFlow<Map<String, AttendanceRecord>> = _attendanceRecords

    private val _pendingChanges = MutableStateFlow<Map<String, AttendanceStatus>>(emptyMap())
    val pendingChanges: StateFlow<Map<String, AttendanceStatus>> = _pendingChanges
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentDate = MutableStateFlow(System.currentTimeMillis())
    val currentDate: StateFlow<Long> = _currentDate

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges

    // Count properties from cloud data only
    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount

    private val _presentCount = MutableStateFlow(0)
    val presentCount: StateFlow<Int> = _presentCount

    private val _absentCount = MutableStateFlow(0)
    val absentCount: StateFlow<Int> = _absentCount

    private val _permissionCount = MutableStateFlow(0)
    val permissionCount: StateFlow<Int> = _permissionCount

    private var attendanceListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null

    init {
        viewModelScope.launch {
            attendanceRecords.collect { records ->
                updateCounts(records)
            }
        }

        viewModelScope.launch {
            users.collect { usersList ->
                _totalCount.value = usersList.size
            }
        }
    }

    private fun getFormattedDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
    }

    private fun getUserTypePath(userType: UserType): String {
        return when (userType) {
            UserType.STUDENT -> "students"
            UserType.TEACHER -> "teachers"
            UserType.STAFF -> "staff"
        }
    }

    private fun generateAttendanceId(userId: String, date: Long): String {
        val formattedDate = getFormattedDate(date)
        return "${userId}_${formattedDate}"
    }

    private fun updateCounts(records: Map<String, AttendanceRecord>) {
        _presentCount.value = records.values.count { it.status == AttendanceStatus.PRESENT }
        _absentCount.value = records.values.count { it.status == AttendanceStatus.ABSENT }
        _permissionCount.value = records.values.count { it.status == AttendanceStatus.PERMISSION }
    }

    fun updateAttendance(userId: String, userType: UserType, status: AttendanceStatus) {
        println("Updating attendance for user: $userId with status: $status")
        _pendingChanges.update { current ->
            current.toMutableMap().apply {
                this[userId] = status
            }
        }
        _hasUnsavedChanges.value = true
        println("Updated pending changes: ${_pendingChanges.value}")
        println("Has unsaved changes: ${_hasUnsavedChanges.value}")
    }

    fun loadUsers(userType: UserType) {
        _loading.value = true
        _error.value = null

        usersListener?.remove()

        val collectionPath = when (userType) {
            UserType.STUDENT -> "students"
            UserType.TEACHER -> "teachers"
            UserType.STAFF -> "non_teaching_staff"
        }

        usersListener = db.collection(collectionPath)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    _error.value = "Error loading users: ${exception.message}"
                    _loading.value = false
                    return@addSnapshotListener
                }

                try {
                    val usersList = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }?.sortedBy { 
                        when (userType) {
                            UserType.STUDENT -> it.rollNumber
                            else -> "${it.firstName} ${it.lastName}"
                        }
                    } ?: emptyList()
                    
                    _users.value = usersList
                } catch (e: Exception) {
                    _error.value = "Error parsing user data: ${e.message}"
                } finally {
                    _loading.value = false
                }
            }
    }

    fun loadAttendance(date: Long, userType: UserType) {
        _loading.value = true
        _error.value = null
        _currentDate.value = date
        _pendingChanges.value = emptyMap()
        _hasUnsavedChanges.value = false

        val formattedDate = getFormattedDate(date)
        println("Loading attendance for date: $formattedDate, userType: ${userType.name}")

        attendanceListener?.remove()
        attendanceListener = db.collection("attendance")
            .document(formattedDate)
            .collection(userType.name.lowercase())
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    println("Error loading attendance: ${exception.message}")
                    exception.printStackTrace()
                    _error.value = "Error loading attendance: ${exception.message}"
                    _loading.value = false
                    return@addSnapshotListener
                }

                try {
                    val records = snapshot?.documents?.mapNotNull { doc ->
                        println("Processing document: ${doc.id}")
                        doc.toObject(AttendanceRecord::class.java)
                    } ?: emptyList()
                    
                    println("Loaded ${records.size} attendance records")
                    
                    _attendanceRecords.value = records.associateBy { it.userId }
                    updateCounts(records.associateBy { it.userId })
                    
                    // Print the current state
                    println("Current attendance state:")
                    println("Total records: ${_attendanceRecords.value.size}")
                    println("Present: ${_presentCount.value}")
                    println("Absent: ${_absentCount.value}")
                    println("Permission: ${_permissionCount.value}")
                } catch (e: Exception) {
                    println("Error parsing attendance data: ${e.message}")
                    e.printStackTrace()
                    _error.value = "Error parsing attendance data: ${e.message}"
                } finally {
                    _loading.value = false
                }
            }
    }

    fun submitAttendance(userType: UserType) {
        println("submitAttendance called with userType: $userType")
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                // Log the current state
                println("Starting attendance submission")
                println("Current date: ${getFormattedDate(_currentDate.value)}")
                println("User type: ${userType.name}")
                println("Pending changes: ${_pendingChanges.value}")
                println("Current user: ${FirebaseAuth.getInstance().currentUser?.uid}")

                // Verify Firestore connection
                try {
                    val testDoc = db.collection("test").document()
                    testDoc.set(mapOf("test" to true)).await()
                    testDoc.delete().await()
                    println("Firestore connection test successful")
                } catch (e: Exception) {
                    println("Firestore connection test failed: ${e.message}")
                    e.printStackTrace()
                    _error.value = "Database connection failed: ${e.message}"
                    return@launch
                }

                val formattedDate = getFormattedDate(_currentDate.value)
                println("Saving attendance for date: $formattedDate, userType: ${userType.name}")
                
                if (_pendingChanges.value.isEmpty()) {
                    println("No changes to save")
                    _error.value = "No changes to save"
                    return@launch
                }

                // Create a batch for all operations
                val batch = db.batch()
                var operationsCount = 0

                _pendingChanges.value.forEach { (userId, status) ->
                    // Create a document reference with a predictable path
                    val docRef = db.collection("attendance")
                        .document(formattedDate)
                        .collection(userType.name.lowercase())
                        .document(userId)

                    println("Preparing to save attendance for user: $userId with status: $status")
                    println("Document path: ${docRef.path}")

                    val record = AttendanceRecord(
                        id = userId,
                        userId = userId,
                        userType = userType,
                        date = _currentDate.value,
                        status = status,
                        markedBy = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                        remarks = "",
                        lastModified = System.currentTimeMillis()
                    )

                    println("Created attendance record: $record")

                    // Set with merge to update existing or create new
                    batch.set(docRef, record)
                    operationsCount++

                    println("Added operation for user $userId to batch")
                }

                println("Attempting to commit batch with $operationsCount operations")
                
                try {
                    batch.commit().await()
                    println("Batch commit successful")
                    
                    // Verify the save by reading back
                    val savedRecords = db.collection("attendance")
                        .document(formattedDate)
                        .collection(userType.name.lowercase())
                        .get()
                        .await()
                    
                    println("Verification - Found ${savedRecords.documents.size} records after save")
                    println("Saved records: ${savedRecords.documents.map { it.id }}")
                    
                    if (savedRecords.documents.isEmpty()) {
                        throw Exception("No records found after save")
                    }
                    
                    // Clear pending changes only if save was successful
                    _pendingChanges.value = emptyMap()
                    _hasUnsavedChanges.value = false
                    println("Cleared pending changes and reset hasUnsavedChanges")

                    // Reload attendance to refresh the UI
                    loadAttendance(_currentDate.value, userType)
                    println("Reloaded attendance data")
                    
                    println("Save operation completed successfully")
                } catch (e: Exception) {
                    println("Batch commit failed: ${e.message}")
                    e.printStackTrace()
                    _error.value = "Failed to save attendance: ${e.message}"
                }
            } catch (e: Exception) {
                println("Error in submitAttendance: ${e.message}")
                e.printStackTrace()
                _error.value = "Error saving attendance: ${e.message}"
            } finally {
                _loading.value = false
                println("Save operation finished, loading set to false")
            }
        }
    }

    fun markAllAttendance(userType: UserType, status: AttendanceStatus) {
        println("Marking all attendance as $status for $userType")
        val users = _users.value
        users.forEach { user ->
            updateAttendance(user.id, userType, status)
        }
        println("Marked all ${users.size} users as $status")
    }

    fun resetChanges() {
        println("Resetting changes")
        _pendingChanges.value = emptyMap()
        _hasUnsavedChanges.value = false
        println("Changes reset, hasUnsavedChanges: ${_hasUnsavedChanges.value}")
    }

    override fun onCleared() {
        super.onCleared()
        attendanceListener?.remove()
        usersListener?.remove()
    }
} 