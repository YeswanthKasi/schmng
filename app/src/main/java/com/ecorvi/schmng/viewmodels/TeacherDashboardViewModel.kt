package com.ecorvi.schmng.viewmodels

import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class TeacherDashboardViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val _teacherData = MutableStateFlow<TeacherData?>(null)
    val teacherData: StateFlow<TeacherData?> = _teacherData
    
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _timeSlots = MutableStateFlow<List<String>>(emptyList())
    val timeSlots: StateFlow<List<String>> = _timeSlots
    
    private val _timetables = MutableStateFlow<List<Timetable>>(emptyList())
    val timetables: StateFlow<List<Timetable>> = _timetables

    private var timeSlotsListener: ListenerRegistration? = null

    init {
        loadTeacherData()
        setupTimeSlotsListener()
    }

    override fun onCleared() {
        super.onCleared()
        timeSlotsListener?.remove()
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                timeSlotsListener?.remove()
                auth.signOut()
            } catch (e: Exception) {
                Log.e("TeacherDashboard", "Error during logout: ${e.message}")
            }
        }
    }

    private fun loadTeacherData() {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _error.value = "User not authenticated"
                    return@launch
                }
                
                FirestoreDatabase.getTeacherByUserId(
                    userId = userId,
                    onComplete = { teacher ->
                        if (teacher != null) {
                            val teacherName = "${teacher.firstName} ${teacher.lastName}"
                            _teacherData.value = TeacherData(
                                id = teacher.id,
                                firstName = teacher.firstName,
                                lastName = teacher.lastName,
                                email = teacher.email,
                                subjects = emptyList()
                            )
                            
                            // Fetch timetables for the teacher
                            FirestoreDatabase.fetchTimetablesForTeacher(
                                teacherName = teacherName,
                                onComplete = { fetchedTimetables ->
                                    _timetables.value = fetchedTimetables.sortedBy { parseTimeSlot(it.timeSlot) }
                                    _loading.value = false
                                },
                                onFailure = { e ->
                                    _error.value = e.message
                                    _loading.value = false
                                }
                            )
                        } else {
                            _error.value = "Teacher data not found"
                            _loading.value = false
                        }
                    },
                    onFailure = { e ->
                        _error.value = e.message ?: "Failed to load teacher data"
                        _loading.value = false
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load teacher data"
                _loading.value = false
            }
        }
    }

    private fun setupTimeSlotsListener() {
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

    private fun getCurrentDay(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Monday" // Default to Monday for Sunday
        }
    }
} 