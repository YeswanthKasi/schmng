package com.ecorvi.schmng.services

import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlin.collections.mapNotNull

class EventNotificationService {
    private val functions = Firebase.functions

    suspend fun notifyClassAboutEvent(
        classId: String,
        eventTitle: String,
        eventDescription: String,
        priority: String = "normal"
    ) {
        try {
            // Get all students in the class using FirestoreDatabase
            val students = FirestoreDatabase.getStudentsByClass(classId)
            val studentTokens = students.mapNotNull { student ->
                FirestoreDatabase.getUserFCMToken(student.userId)
            }

            if (studentTokens.isEmpty()) return

            // Prepare notification data
            val notificationData = hashMapOf(
                "tokens" to studentTokens,
                "notification" to hashMapOf(
                    "title" to eventTitle,
                    "body" to eventDescription,
                    "priority" to priority
                ),
                "data" to hashMapOf(
                    "type" to "class_event",
                    "classId" to classId
                )
            )

            // Call Firebase Cloud Function to send notifications
            functions
                .getHttpsCallable("sendMulticastNotification")
                .call(notificationData)
                .await()

        } catch (e: Exception) {
            // Log error but don't throw - notification failure shouldn't break the app
            e.printStackTrace()
        }
    }

    suspend fun scheduleEventReminder(
        eventId: String,
        classId: String,
        eventTitle: String,
        eventTime: Long,
        reminderMinutes: Int = 30
    ) {
        try {
            val reminderData = hashMapOf(
                "eventId" to eventId,
                "classId" to classId,
                "title" to eventTitle,
                "eventTime" to eventTime,
                "reminderMinutes" to reminderMinutes
            )

            functions
                .getHttpsCallable("scheduleEventReminder")
                .call(reminderData)
                .await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun cancelEventNotifications(eventId: String) {
        try {
            val cancelData = hashMapOf(
                "eventId" to eventId
            )

            functions
                .getHttpsCallable("cancelEventNotifications")
                .call(cancelData)
                .await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 