package com.ecorvi.schmng.services

import com.ecorvi.schmng.models.Student
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EventNotificationService {
    suspend fun notifyClassAboutEvent(
        classId: String,
        eventTitle: String,
        eventDescription: String,
        priority: String = "normal"
    ) = withContext(Dispatchers.IO) {
        FirestoreDatabase.notifyClassAboutEvent(classId, eventTitle, eventDescription, priority)
    }

    suspend fun scheduleEventReminder(
        eventId: String,
        classId: String,
        eventTitle: String,
        eventTime: Long,
        reminderMinutes: Int = 30
    ) = withContext(Dispatchers.IO) {
        FirestoreDatabase.scheduleEventReminder(eventId, classId, eventTitle, eventTime, reminderMinutes)
    }

    suspend fun cancelEventNotifications(eventId: String) = withContext(Dispatchers.IO) {
        FirestoreDatabase.cancelEventNotifications(eventId)
    }
} 