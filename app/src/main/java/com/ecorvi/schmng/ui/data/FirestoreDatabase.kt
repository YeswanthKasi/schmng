package com.ecorvi.schmng.ui.data

import android.util.Log
import com.ecorvi.schmng.ui.data.model.AttendanceRecord
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.data.model.Schedule
import com.ecorvi.schmng.ui.data.model.Fee
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

object FirestoreDatabase {
    private val db = FirebaseFirestore.getInstance()
    private val schedulesCollection = db.collection("schedules")
    private val feesCollection = db.collection("fees")

    val studentsCollection: CollectionReference = db.collection("students")
    val teachersCollection: CollectionReference = db.collection("teachers")
    val pendingFeesCollection: CollectionReference = db.collection("pending_fees")
    val attendanceCollection: CollectionReference = db.collection("attendance")

    // Add a student to Firestore
    fun addStudent(student: Person, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val newStudentRef = studentsCollection.document()
        val studentWithId = student.copy(id = newStudentRef.id)

        newStudentRef.set(studentWithId)
            .addOnSuccessListener {
                Log.d("Firestore", "Student added with ID: ${newStudentRef.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding student: ${e.message}")
                onFailure(e)
            }
    }

    // Add a teacher to Firestore
    fun addTeacher(teacher: Person, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val newTeacherRef = teachersCollection.document()
        val teacherWithId = teacher.copy(id = newTeacherRef.id)

        newTeacherRef.set(teacherWithId)
            .addOnSuccessListener {
                Log.d("Firestore", "Teacher added with ID: ${newTeacherRef.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding teacher: ${e.message}")
                onFailure(e)
            }
    }

    fun fetchStudentCount(onComplete: (Int) -> Unit) {
        FirebaseFirestore.getInstance().collection("students")
            .get()
            .addOnSuccessListener { result ->
                onComplete(result.size()) // returns the number of documents
            }
            .addOnFailureListener {
                onComplete(0) // fallback if error
            }
    }

    fun fetchTeacherCount(onComplete: (Int) -> Unit) {
        FirebaseFirestore.getInstance().collection("teachers")
            .get()
            .addOnSuccessListener { result ->
                onComplete(result.size())
            }
            .addOnFailureListener {
                onComplete(0)
            }
    }

    // Add a schedule to Firestore
    fun addSchedule(
        schedule: Schedule,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        schedulesCollection.add(schedule)
            .addOnSuccessListener { 
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // Add a pending fee to Firestore
    fun addPendingFee(fee: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        pendingFeesCollection.add(mapOf("fee" to fee))
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "Pending fee added with ID: ${documentReference.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding pending fee: ${e.message}")
                onFailure(e)
            }
    }

    // Fetch schedules from Firestore
    fun fetchSchedules(onComplete: (List<Schedule>) -> Unit, onFailure: (Exception) -> Unit) {
        schedulesCollection.get()
            .addOnSuccessListener { snapshot ->
                val schedulesList = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Schedule::class.java)?.apply { id = doc.id }
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error converting document to Schedule: ${e.message}")
                        null
                    }
                }
                Log.d("Firestore", "Fetched Schedules: $schedulesList")
                onComplete(schedulesList)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching schedules: ${e.message}")
                onFailure(e)
            }
    }

    // Fetch pending fees from Firestore
    fun fetchPendingFees(onComplete: (List<Fee>) -> Unit, onFailure: (Exception) -> Unit) {
        pendingFeesCollection.get()
            .addOnSuccessListener { snapshot ->
                val feesList = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Fee::class.java)?.apply { id = doc.id }
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error converting document to Fee: ${e.message}")
                        null
                    }
                }
                Log.d("Firestore", "Fetched Pending Fees: $feesList")
                onComplete(feesList)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching pending fees: ${e.message}")
                onFailure(e)
            }
    }

    // Real-time updates for students
    fun listenForStudentUpdates(
        onUpdate: (List<Person>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return studentsCollection.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e("Firestore", "Error fetching students: ${exception.message}")
                onError(exception)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val students = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Person::class.java)?.copy(id = doc.id)
                }
                Log.d("Firestore", "Fetched Students: $students")
                onUpdate(students)
            }
        }
    }

    // Real-time updates for teachers
    fun listenForTeacherUpdates(
        onUpdate: (List<Person>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return teachersCollection.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e("Firestore", "Error fetching teachers: ${exception.message}")
                onError(exception)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val teachers = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Person::class.java)?.copy(id = doc.id)
                }
                Log.d("Firestore", "Fetched Teachers: $teachers")
                onUpdate(teachers)
            }
        }
    }

    // Delete student by ID
    fun deleteStudent(studentId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (studentId.isBlank()) {
            Log.e("Firestore", "Error: Invalid student ID")
            onFailure(Exception("Invalid student ID"))
            return
        }
        studentsCollection.document(studentId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Student with ID: $studentId deleted successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error deleting student: ${e.message}")
                onFailure(e)
            }
    }

    // Delete teacher by ID
    fun deleteTeacher(teacherId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        teachersCollection.document(teacherId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Teacher with ID: $teacherId deleted successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error deleting teacher: ${e.message}")
                onFailure(e)
            }
    }

    // Add attendance record to Firestore
    fun addAttendanceRecord(personId: String, attendanceOption: String, date: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val newAttendanceRef = attendanceCollection.document()
        val attendanceRecord = AttendanceRecord(personId, attendanceOption, date)

        newAttendanceRef.set(attendanceRecord)
            .addOnSuccessListener {
                Log.d("Firestore", "Attendance record added with ID: ${newAttendanceRef.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding attendance record: ${e.message}")
                onFailure(e)
            }
    }

    // In FirestoreDatabase.kt
    fun fetchAttendanceForPerson(personId: String, onComplete: (List<AttendanceRecord>) -> Unit, onFailure: (Exception) -> Unit) {
        // Assuming you have a collection for attendance records
        val attendanceCollection = db.collection("attendance")

        attendanceCollection.whereEqualTo("personId", personId)
            .get()
            .addOnSuccessListener { result ->
                val attendanceRecords = result.documents.mapNotNull { doc ->
                    doc.toObject(AttendanceRecord::class.java)
                }
                onComplete(attendanceRecords)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // Fetch all attendance records
    fun fetchAllAttendanceRecords(onComplete: (List<AttendanceRecord>) -> Unit, onFailure: (Exception) -> Unit) {
        attendanceCollection.get()
            .addOnSuccessListener { snapshot ->
                val attendanceRecords = snapshot.documents.mapNotNull { it.toObject(AttendanceRecord::class.java) }
                onComplete(attendanceRecords)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching attendance records: ${e.message}")
                onFailure(e)
            }
    }

    // Fetch schedule with ID
    fun fetchScheduleWithId(onComplete: (Map<String, String>) -> Unit, onFailure: (Exception) -> Unit) {
        schedulesCollection.get()
            .addOnSuccessListener { snapshot ->
                val schedulesMap = snapshot.documents.associate { 
                    it.id to (it.getString("schedule") ?: "")
                }
                onComplete(schedulesMap)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching schedules: ${e.message}")
                onFailure(e)
            }
    }

    // Delete schedule by ID
    fun deleteSchedule(
        scheduleId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        schedulesCollection.document(scheduleId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure }
    }

    // Fetch pending fees with ID
    fun fetchPendingFeesWithId(onComplete: (Map<String, String>) -> Unit, onFailure: (Exception) -> Unit) {
        pendingFeesCollection.get()
            .addOnSuccessListener { snapshot ->
                val feesMap = snapshot.documents.associate { 
                    it.id to (it.getString("fee") ?: "")
                }
                onComplete(feesMap)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching fees: ${e.message}")
                onFailure(e)
            }
    }

    // Delete pending fee by ID
    fun deletePendingFee(feeId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (feeId.isBlank()) {
            onFailure(Exception("Invalid fee ID"))
            return
        }
        pendingFeesCollection.document(feeId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Pending fee deleted successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error deleting fee: ${e.message}")
                onFailure(e)
            }
    }

    // Add real-time listener for schedules
    fun listenForScheduleUpdates(
        onUpdate: (List<Schedule>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return schedulesCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val schedules = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Schedule::class.java)?.apply { id = doc.id }
                    } catch (e: Exception) {
                        onError(e)
                        null
                    }
                } ?: emptyList()
                onUpdate(schedules)
            }
    }

    // Add real-time listener for fees
    fun listenForFeeUpdates(
        onUpdate: (List<Fee>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return feesCollection
            .orderBy("dueDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val fees = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Fee::class.java)?.apply { id = doc.id }
                    } catch (e: Exception) {
                        onError(e)
                        null
                    }
                } ?: emptyList()
                onUpdate(fees)
            }
    }

    // Delete fee by ID
    fun deleteFee(
        feeId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        feesCollection.document(feeId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure }
    }

    fun addFee(
        fee: Fee,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        feesCollection.add(fee)
            .addOnSuccessListener { 
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

}