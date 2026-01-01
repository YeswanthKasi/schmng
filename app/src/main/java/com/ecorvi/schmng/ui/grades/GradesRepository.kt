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
        Log.d("GradesRepo", "Fetching students for class: $normalized")
        return try {
            // First try with className field
            val snapshot = students.whereEqualTo("className", normalized).get().await()
            Log.d("GradesRepo", "Found ${snapshot.documents.size} students")
            val result = snapshot.documents.mapNotNull { doc ->
                try {
                    // Map document fields directly to Person object
                    val data = doc.data ?: return@mapNotNull null
                    Person(
                        id = doc.id,
                        firstName = data["firstName"] as? String ?: "",
                        lastName = data["lastName"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        phone = data["phone"] as? String ?: data["phoneNumber"] as? String ?: "",
                        type = data["type"] as? String ?: "student",
                        className = data["className"] as? String ?: "",
                        section = data["section"] as? String ?: "",
                        rollNumber = data["rollNumber"] as? String ?: "",
                        gender = data["gender"] as? String ?: "",
                        dateOfBirth = data["dateOfBirth"] as? String ?: "",
                        mobileNo = data["mobileNo"] as? String ?: "",
                        address = data["address"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("GradesRepo", "Error mapping student doc ${doc.id}: ${e.message}")
                    null
                }
            }
            Log.d("GradesRepo", "Mapped ${result.size} students successfully")
            result
        } catch (e: Exception) {
            Log.e("GradesRepo", "listStudents error: ${e.message}")
            emptyList()
        }
    }

    suspend fun listGrades(className: String, examType: String, academicYear: String): List<StudentGrade> {
        val normalized = normalizeClassName(className)
        val year = academicYear.trim()
        Log.d("GradesRepo", "Fetching grades: class=$normalized, exam=$examType, year=$year")
        return try {
            // Simpler query without orderBy to avoid index issues
            val snapshot = grades
                .whereEqualTo("className", normalized)
                .whereEqualTo("examType", examType)
                .whereEqualTo("academicYear", year)
                .get().await()
            Log.d("GradesRepo", "Found ${snapshot.documents.size} grade documents")
            val result = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(StudentGrade::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("GradesRepo", "Error mapping grade doc ${doc.id}: ${e.message}")
                    null
                }
            }.sortedBy { it.studentName }
            Log.d("GradesRepo", "Mapped ${result.size} grades successfully")
            result
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
        Log.d("GradesRepo", "Setting up listener: class=$normalized, exam=$examType, year=$year")
        // Simpler query without orderBy to avoid index issues
        return grades
            .whereEqualTo("className", normalized)
            .whereEqualTo("examType", examType)
            .whereEqualTo("academicYear", year)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { 
                    Log.e("GradesRepo", "Listener error: ${e.message}")
                    onError(e)
                    return@addSnapshotListener 
                }
                val docs = snapshot?.documents ?: emptyList()
                Log.d("GradesRepo", "Listener received ${docs.size} documents")
                val list = docs.mapNotNull { doc ->
                    try {
                        doc.toObject(StudentGrade::class.java)?.copy(id = doc.id)
                    } catch (ex: Exception) {
                        Log.e("GradesRepo", "Error mapping grade in listener: ${ex.message}")
                        null
                    }
                }.sortedBy { it.studentName }
                onUpdate(list)
            }
    }

    suspend fun saveGradesBatch(input: List<StudentGrade>, createdBy: String): Result<Unit> {
        Log.d("GradesRepo", "Saving ${input.size} grades")
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
                Log.d("GradesRepo", "Saving grade: id=$stableId, student=${g.studentName}")
                batch.set(grades.document(stableId), normalized)
            }
            batch.commit().await()
            Log.d("GradesRepo", "Batch save successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GradesRepo", "saveGradesBatch error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteGrade(id: String): Result<Unit> {
        Log.d("GradesRepo", "Deleting grade: $id")
        return try {
            grades.document(id).delete().await()
            Log.d("GradesRepo", "Delete successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GradesRepo", "Delete error: ${e.message}")
            Result.failure(e)
        }
    }
}
