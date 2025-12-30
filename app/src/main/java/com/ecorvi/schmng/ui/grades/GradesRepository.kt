package com.ecorvi.schmng.ui.grades

import android.util.Log
import com.ecorvi.schmng.ui.data.model.StudentGrade
import com.ecorvi.schmng.ui.data.model.SubjectGrade
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class GradesRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val students = db.collection("students")
    private val grades = db.collection("student_grades")

    private fun normalizeClassName(raw: String): String {
        val t = raw.trim()
        if (t.startsWith("Class ")) return t
        val num = t.replace(Regex("[^0-9]"), "")
        return if (num.isNotBlank()) "Class $num" else "Class $t"
    }

    suspend fun listStudents(className: String): List<Person> {
        val normalized = normalizeClassName(className)
        return try {
            val snapshot = students.whereEqualTo("className", normalized).get().await()
            snapshot.documents.mapNotNull { it.toObject(Person::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            Log.e("GradesRepo", "listStudents error: ${e.message}")
            emptyList()
        }
    }

    suspend fun listGrades(className: String, examType: String, academicYear: String): List<StudentGrade> {
        val normalized = normalizeClassName(className)
        val year = academicYear.trim()
        return try {
            val snapshot = grades
                .whereEqualTo("className", normalized)
                .whereEqualTo("examType", examType)
                .whereEqualTo("academicYear", year)
                .orderBy("studentName", Query.Direction.ASCENDING)
                .get().await()
            snapshot.documents.mapNotNull { it.toObject(StudentGrade::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            Log.e("GradesRepo", "listGrades error: ${e.message}")
            emptyList()
        }
    }

    fun listenGrades(
        className: String,
        examType: String,
        academicYear: String,
        onUpdate: (List<StudentGrade>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val normalized = normalizeClassName(className)
        val year = academicYear.trim()
        return grades
            .whereEqualTo("className", normalized)
            .whereEqualTo("examType", examType)
            .whereEqualTo("academicYear", year)
            .orderBy("studentName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { onError(e); return@addSnapshotListener }
                val docs = snapshot?.documents ?: emptyList()
                val list = docs.mapNotNull { it.toObject(StudentGrade::class.java)?.copy(id = it.id) }
                onUpdate(list)
            }
    }

    suspend fun saveGradesBatch(input: List<StudentGrade>, createdBy: String): Result<Unit> {
        return try {
            val batch = db.batch()
            input.forEach { g ->
                val stableId = if (g.id.isBlank()) "${g.studentId}_${normalizeClassName(g.className)}_${g.examType}_${g.academicYear.trim()}" else g.id
                val normalized = g.copy(
                    id = stableId,
                    className = normalizeClassName(g.className),
                    academicYear = g.academicYear.trim(),
                    createdBy = createdBy,
                    createdAt = if (g.id.isBlank()) System.currentTimeMillis() else g.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
                batch.set(grades.document(stableId), normalized)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GradesRepo", "saveGradesBatch error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteGrade(id: String): Result<Unit> {
        return try {
            grades.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
