package com.ecorvi.schmng.repository

import com.ecorvi.schmng.models.AttendanceRecord
import com.ecorvi.schmng.models.UserType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.*

class AttendanceRepository {
    private val db: FirebaseFirestore = Firebase.firestore
    private val attendanceCollection = db.collection("attendance")

    suspend fun markAttendance(record: AttendanceRecord): Result<Unit> {
        return try {
            attendanceCollection.document(record.id).set(record).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAttendance(recordId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            attendanceCollection.document(recordId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAttendanceByDate(date: Long, userType: UserType): Query {
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfDay = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return attendanceCollection
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThanOrEqualTo("date", endOfDay)
            .whereEqualTo("userType", userType.name)
    }

    fun getAttendanceByUser(userId: String): Query {
        return attendanceCollection
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
    }

    suspend fun deleteAttendanceRecord(recordId: String): Result<Unit> {
        return try {
            attendanceCollection.document(recordId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 