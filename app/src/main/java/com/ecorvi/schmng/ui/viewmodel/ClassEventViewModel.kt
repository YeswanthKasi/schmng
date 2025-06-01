package com.ecorvi.schmng.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecorvi.schmng.models.ClassEvent
import com.ecorvi.schmng.repository.ClassEventRepository
import com.ecorvi.schmng.services.EventNotificationService
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ClassEventViewModel : ViewModel() {
    private val repository = ClassEventRepository()
    private val notificationService = EventNotificationService()

    private val _events = MutableStateFlow<List<ClassEvent>>(emptyList())
    val events: StateFlow<List<ClassEvent>> = _events

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadTeacherEvents(teacherId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get teacher's assigned class
                val teacherClass = FirestoreDatabase.getTeacherAssignedClass(teacherId)
                if (teacherClass != null) {
                    // Load events for the teacher's class
                    loadEventsForClass(teacherClass)
                } else {
                    _error.value = "No class assigned to teacher"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadEventsForClass(className: String) {
        try {
            val result = repository.getEventsForClass(className)
            if (result.isSuccess) {
                _events.value = result.getOrNull() ?: emptyList()
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    fun createEvent(
        title: String,
        description: String,
        eventDate: Long,
        targetClass: String,
        teacherId: String,
        priority: String = "normal",
        type: String = "general"
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Verify teacher is assigned to this class
                val teacherClass = FirestoreDatabase.getTeacherAssignedClass(teacherId)
                if (teacherClass != targetClass) {
                    _error.value = "You can only create events for your assigned class"
                    return@launch
                }

                val event = ClassEvent(
                    title = title,
                    description = description,
                    eventDate = eventDate,
                    targetClass = targetClass,
                    createdBy = teacherId,
                    priority = priority,
                    type = type
                )

                val result = repository.createEvent(event)
                if (result.isSuccess) {
                    // Notify students about the new event
                    notificationService.notifyClassAboutEvent(
                        targetClass,
                        title,
                        description,
                        priority
                    )

                    // Schedule a reminder if the event is in the future
                    val currentTime = System.currentTimeMillis()
                    if (eventDate > currentTime) {
                        notificationService.scheduleEventReminder(
                            result.getOrNull()?.id ?: return@launch,
                            targetClass,
                            title,
                            eventDate
                        )
                    }

                    // Reload events
                    loadEventsForClass(targetClass)
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateEvent(event: ClassEvent) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.updateEvent(event)
                if (result.isSuccess) {
                    // Notify students about the updated event
                    notificationService.notifyClassAboutEvent(
                        event.targetClass,
                        event.title,
                        event.description,
                        event.priority
                    )

                    // Reload events
                    loadEventsForClass(event.targetClass)
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteEvent(event: ClassEvent) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.deleteEvent(event.id)
                if (result.isSuccess) {
                    // Cancel any scheduled notifications
                    notificationService.cancelEventNotifications(event.id)
                    
                    // Reload events
                    loadEventsForClass(event.targetClass)
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
} 