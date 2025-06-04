package com.ecorvi.schmng.repository

import com.ecorvi.schmng.models.ClassEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class ClassEventRepository {
    private val db = FirebaseFirestore.getInstance()
    private val eventsCollection = db.collection("class_events")

    suspend fun createEvent(event: ClassEvent): Result<ClassEvent> = try {
        val eventId = java.util.UUID.randomUUID().toString()
        val newEvent = event.copy(
            id = eventId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        eventsCollection.document(eventId).set(newEvent).await()
        Result.success(newEvent)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateEvent(event: ClassEvent): Result<ClassEvent> = try {
        val updatedEvent = event.copy(updatedAt = System.currentTimeMillis())
        eventsCollection.document(event.id).set(updatedEvent).await()
        Result.success(updatedEvent)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> = try {
        eventsCollection.document(eventId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getEventsForClass(className: String): Result<List<ClassEvent>> = try {
        val snapshot = eventsCollection
            .whereEqualTo("targetClass", className)
            .whereEqualTo("status", "active")
            .orderBy("eventDate", Query.Direction.DESCENDING)
            .get()
            .await()

        val events = snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(ClassEvent::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
        Result.success(events)
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }

    fun getEventsByTeacher(teacherId: String): Flow<List<ClassEvent>> = flow {
        try {
            val snapshot = eventsCollection
                .whereEqualTo("createdBy", teacherId)
                .orderBy("eventDate", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val events = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ClassEvent::class.java)
            }
            emit(events)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun getEventById(eventId: String): Result<ClassEvent> = try {
        val doc = eventsCollection.document(eventId).get().await()
        val event = doc.toObject(ClassEvent::class.java)
        if (event != null) {
            Result.success(event)
        } else {
            Result.failure(NoSuchElementException("Event not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getUpcomingEvents(className: String): Flow<List<ClassEvent>> = flow {
        try {
            val currentTime = System.currentTimeMillis()
            val snapshot = eventsCollection
                .whereEqualTo("targetClass", className)
                .whereGreaterThan("eventDate", currentTime)
                .orderBy("eventDate", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val events = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ClassEvent::class.java)
            }
            emit(events)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
} 