package com.ecorvi.schmng.ui.data

import android.net.Uri
import android.util.Log
import com.ecorvi.schmng.models.AttendanceRecord
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.UserType
import com.ecorvi.schmng.ui.data.FirestoreDatabase.db
import com.ecorvi.schmng.ui.data.model.AdminProfile
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.data.model.Schedule
import com.ecorvi.schmng.ui.data.model.Fee
import com.ecorvi.schmng.ui.data.model.SchoolProfile
import com.ecorvi.schmng.ui.data.model.Timetable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

import java.util.*
import com.ecorvi.schmng.models.LeaveApplication
import java.text.SimpleDateFormat

object FirestoreDatabase {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    private val schedulesCollection = db.collection("schedules")
    private val feesCollection = db.collection("fees")
    private val timetablesCollection = db.collection("timetables")

    val studentsCollection: CollectionReference = db.collection("students")
    val teachersCollection: CollectionReference = db.collection("teachers")
    val staffCollection: CollectionReference = db.collection("non_teaching_staff")
    val pendingFeesCollection: CollectionReference = db.collection("pending_fees")

    private var teacherListener: ListenerRegistration? = null
    private var studentListener: ListenerRegistration? = null
    private var staffListener: ListenerRegistration? = null

    // Time slots collection
    private val timeSlotsCollection = db.collection("timeSlots")
    private val subjectsCollection = db.collection("subjects")

    // Add a timetable entry (Reverted signature)
    fun addTimetable(
        timetable: Timetable,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        timetablesCollection.add(timetable)
            .addOnSuccessListener { documentReference ->
                // Update the document to set the id field
                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // Fetch timetables for a specific class
    fun fetchTimetablesForClass(
        classGrade: String,
        onComplete: (List<Timetable>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        timetablesCollection
            .whereEqualTo("classGrade", classGrade)
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val timetables = snapshot.documents.mapNotNull { doc ->
                    try {
                        val timetable = Timetable()
                        timetable.id = doc.id
                        timetable.classGrade = doc.getString("classGrade") ?: ""
                        timetable.dayOfWeek = doc.getString("dayOfWeek") ?: ""
                        timetable.timeSlot = doc.getString("timeSlot") ?: ""
                        timetable.subject = doc.getString("subject") ?: ""
                        timetable.teacher = doc.getString("teacher") ?: ""
                        timetable.roomNumber = doc.getString("roomNumber") ?: ""
                        timetable.isActive = doc.getBoolean("active") ?: true
                        timetable
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error converting document to Timetable: ${e.message}")
                        null
                    }
                }
                Log.d("Firestore", "Fetched ${timetables.size} timetables for class $classGrade")
                onComplete(timetables)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching timetables: ${e.message}")
                onFailure(e)
            }
    }

    // Delete a timetable entry (Reverted signature)
    fun deleteTimetable(
        timetableId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        timetablesCollection.document(timetableId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error deleting timetable: ${e.message}")
                onFailure(e) 
            }
    }

    // Update a timetable entry (Reverted signature)
    fun updateTimetable(
        timetable: Timetable,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        timetablesCollection.document(timetable.id)
            .set(timetable)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error updating timetable: ${e.message}")
                onFailure(e)
            }
    }

    // Get a single timetable by ID
    fun getTimetableById(
        timetableId: String,
        onComplete: (Timetable?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        timetablesCollection.document(timetableId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val timetable = document.toObject(Timetable::class.java)?.apply { id = document.id }
                        onComplete(timetable)
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error converting document to Timetable: ${e.message}")
                        onComplete(null) // Treat conversion error as not found for simplicity
                    }
                } else {
                    onComplete(null) // Not found
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching timetable by ID: ${e.message}")
                onFailure(e)
            }
    }

    // Add a student to Firestore
    fun addStudent(student: Person, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Standardize class format to "Class X"
        val standardizedStudent = if (student.className.endsWith("st")) {
            student.copy(className = "Class ${student.className.replace("st", "")}")
        } else if (!student.className.startsWith("Class ")) {
            student.copy(className = "Class ${student.className}")
        } else {
            student
        }

        val newStudentRef = studentsCollection.document()
        val studentWithId = standardizedStudent.copy(id = newStudentRef.id)

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
        // Standardize class format to "Class X"
        val standardizedTeacher = if (teacher.className.endsWith("st")) {
            teacher.copy(className = "Class ${teacher.className.replace("st", "")}")
        } else if (!teacher.className.startsWith("Class ")) {
            teacher.copy(className = "Class ${teacher.className}")
        } else {
            teacher
        }

        val newTeacherRef = teachersCollection.document()
        val teacherWithId = standardizedTeacher.copy(id = newTeacherRef.id)

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
        studentListener?.remove()
        return studentsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val students = snapshot.documents.mapNotNull { doc ->
                        try {
                            Person(
                                id = doc.id,
                                firstName = doc.getString("firstName") ?: "",
                                lastName = doc.getString("lastName") ?: "",
                                email = doc.getString("email") ?: "",
                                phone = doc.getString("phone") ?: "",
                                type = "student",
                                className = doc.getString("className") ?: "",
                                rollNumber = doc.getString("rollNumber") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onUpdate(students)
                }
            }.also { studentListener = it }
    }

    // Real-time updates for teachers
    fun listenForTeacherUpdates(
        onUpdate: (List<Person>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        teacherListener?.remove()
        return teachersCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val teachers = snapshot.documents.mapNotNull { doc ->
                        try {
                            Person(
                                id = doc.id,
                                firstName = doc.getString("firstName") ?: "",
                                lastName = doc.getString("lastName") ?: "",
                                email = doc.getString("email") ?: "",
                                phone = doc.getString("phone") ?: "",
                                type = "teacher",
                                className = doc.getString("className") ?: "",
                                rollNumber = "",
                                gender = doc.getString("gender") ?: "",
                                dateOfBirth = doc.getString("dateOfBirth") ?: "",
                                mobileNo = doc.getString("mobileNo") ?: "",
                                address = doc.getString("address") ?: "",
                                age = doc.getLong("age")?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            Log.e("Firestore", "Error mapping teacher document: ${e.message}")
                            null
                        }
                    }
                    onUpdate(teachers)
                }
            }.also { teacherListener = it }
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
            .addOnFailureListener { onFailure(it) } // Keep original failure logic
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

    // Get a student by ID
    suspend fun getStudent(studentId: String): Person? {
        return try {
            val doc = studentsCollection.document(studentId).get().await()
            doc.toObject(Person::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting student: ${e.message}")
            null
        }
    }

    // Get a teacher by ID
    suspend fun getTeacher(teacherId: String): Person? {
        return try {
            val doc = teachersCollection.document(teacherId).get().await()
            doc.toObject(Person::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting teacher: ${e.message}")
            null
        }
    }

    // Update student by ID
    fun updateStudent(student: Person, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (student.id.isBlank()) {
            onFailure(Exception("Invalid student ID"))
            return
        }

        // Standardize class format to "Class X"
        val standardizedStudent = if (student.className.endsWith("st")) {
            student.copy(className = "Class ${student.className.replace("st", "")}")
        } else if (!student.className.startsWith("Class ")) {
            student.copy(className = "Class ${student.className}")
        } else {
            student
        }

        studentsCollection.document(student.id)
            .set(standardizedStudent)
            .addOnSuccessListener {
                Log.d("Firestore", "Student with ID: ${student.id} updated successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error updating student: ${e.message}")
                onFailure(e)
            }
    }

    // Update teacher in Firestore
    fun updateTeacher(teacher: Person, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Standardize class format to "Class X"
        val standardizedTeacher = if (teacher.className.endsWith("st")) {
            teacher.copy(className = "Class ${teacher.className.replace("st", "")}")
        } else if (!teacher.className.startsWith("Class ")) {
            teacher.copy(className = "Class ${teacher.className}")
        } else {
            teacher
        }

        teachersCollection.document(standardizedTeacher.id)
            .set(standardizedTeacher)
            .addOnSuccessListener {
                Log.d("Firestore", "Teacher updated successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error updating teacher: ${e.message}")
                onFailure(e)
            }
    }

    fun cleanup() {
        teacherListener?.remove()
        studentListener?.remove()
        staffListener?.remove()
    }

    // Get user role
    fun getUserRole(
        userId: String,
        onComplete: (String?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    onComplete(document.getString("role"))
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error getting user role: ${e.message}")
                onFailure(e)
            }
    }

    // Create user with role
    fun createUserWithRole(
        userId: String,
        userData: Map<String, Any>,
        person: Person,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // First create the user document
        db.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                // Then add the person data to appropriate collection
                when (userData["role"]) {
                    "student" -> {
                        studentsCollection.document(userId)
                            .set(person)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFailure(it) }
                    }
                    "teacher" -> {
                        teachersCollection.document(userId)
                            .set(person)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFailure(it) }
                    }
                    "staff" -> {
                        staffCollection.document(userId)
                            .set(person)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFailure(it) }
                    }
                    else -> {
                        onFailure(Exception("Invalid role specified"))
                    }
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun fetchStaffCount(onComplete: (Int) -> Unit) {
        FirebaseFirestore.getInstance().collection("non_teaching_staff")
            .get()
            .addOnSuccessListener { result ->
                onComplete(result.size())
            }
            .addOnFailureListener {
                onComplete(0)
            }
    }

    // Add a staff member to Firestore
    fun addStaffMember(staff: Person, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val newStaffRef = staffCollection.document()
        val staffWithId = staff.copy(id = newStaffRef.id)

        newStaffRef.set(staffWithId)
            .addOnSuccessListener {
                Log.d("Firestore", "Staff member added with ID: ${newStaffRef.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding staff member: ${e.message}")
                onFailure(e)
            }
    }

    // Delete staff member by ID
    fun deleteStaffMember(staffId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        staffCollection.document(staffId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Staff member with ID: $staffId deleted successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error deleting staff member: ${e.message}")
                onFailure(e)
            }
    }

    // Real-time updates for staff members
    fun listenForStaffUpdates(
        onUpdate: (List<Person>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        staffListener?.remove()
        return staffCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val staffMembers = snapshot.documents.mapNotNull { doc ->
                        try {
                            Person(
                                id = doc.id,
                                firstName = doc.getString("firstName") ?: "",
                                lastName = doc.getString("lastName") ?: "",
                                email = doc.getString("email") ?: "",
                                phone = doc.getString("phone") ?: "",
                                type = "staff",
                                className = "",
                                rollNumber = "",
                                gender = doc.getString("gender") ?: "",
                                dateOfBirth = doc.getString("dateOfBirth") ?: "",
                                mobileNo = doc.getString("mobileNo") ?: "",
                                address = doc.getString("address") ?: "",
                                age = doc.getLong("age")?.toInt() ?: 0,
                                designation = doc.getString("designation") ?: "",
                                department = doc.getString("department") ?: ""
                            )
                        } catch (e: Exception) {
                            Log.e("Firestore", "Error mapping staff document: ${e.message}")
                            null
                        }
                    }
                    onUpdate(staffMembers)
                }
            }.also { staffListener = it }
    }

    // Add this method to get staff member
    suspend fun getStaffMember(staffId: String): Person? {
        return try {
            val doc = staffCollection.document(staffId).get().await()
            doc.toObject(Person::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting staff member: ${e.message}")
            null
        }
    }

    // Get schedule by ID
    fun getScheduleById(
        scheduleId: String,
        onComplete: (Schedule?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        schedulesCollection.document(scheduleId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val schedule = document.toObject(Schedule::class.java)
                    schedule?.id = document.id
                    onComplete(schedule)
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error getting schedule: ${e.message}")
                onFailure(e)
            }
    }

    // Update schedule
    fun updateSchedule(
        schedule: Schedule,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (schedule.id.isBlank()) {
            onFailure(Exception("Invalid schedule ID"))
            return
        }

        schedulesCollection.document(schedule.id)
            .set(schedule)
            .addOnSuccessListener {
                Log.d("Firestore", "Schedule updated successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error updating schedule: ${e.message}")
                onFailure(e)
            }
    }

    // Delete schedule
    fun deleteSchedule(
        scheduleId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (scheduleId.isBlank()) {
            onFailure(Exception("Invalid schedule ID"))
            return
        }

        schedulesCollection.document(scheduleId)
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Schedule deleted successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error deleting schedule: ${e.message}")
                onFailure(e)
            }
    }

    fun getPersonById(id: String, type: String): Person? {
        val collection = when (type.lowercase()) {
            "student" -> studentsCollection
            "teacher" -> teachersCollection
            else -> throw IllegalArgumentException("Invalid type: $type")
        }
        val document = collection.document(id).get().result
        return document?.toObject(Person::class.java)
    }

    // Fetch timetables for a specific teacher
    fun fetchTimetablesForTeacher(
        teacherName: String,
        onComplete: (List<Timetable>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        timetablesCollection
            .whereEqualTo("teacher", teacherName)
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val timetables = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val timetable = Timetable()
                        timetable.id = doc.id
                        timetable.classGrade = doc.getString("classGrade") ?: ""
                        timetable.dayOfWeek = doc.getString("dayOfWeek") ?: ""
                        timetable.timeSlot = doc.getString("timeSlot") ?: ""
                        timetable.subject = doc.getString("subject") ?: ""
                        timetable.teacher = doc.getString("teacher") ?: ""
                        timetable.roomNumber = doc.getString("roomNumber") ?: ""
                        timetable.isActive = doc.getBoolean("active") ?: true
                        timetable
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error converting document to Timetable: ${e.message}")
                        null
                    }
                }.sortedBy { it.timeSlot }
                onComplete(timetables)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching timetables: ${e.message}")
                onFailure(e)
            }
    }

    // Get a teacher by user ID
    fun getTeacherByUserId(
        userId: String,
        onComplete: (Person?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        teachersCollection
            .whereEqualTo("id", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    val teacher = doc.toObject(Person::class.java)
                    teacher?.id = doc.id
                    onComplete(teacher)
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error getting teacher by user ID: ${e.message}")
                onFailure(e)
            }
    }

    // Time slots methods
    fun addTimeSlot(
        timeSlot: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        timeSlotsCollection.document()
            .set(mapOf("timeSlot" to timeSlot))
            .addOnSuccessListener {
                Log.d("Firestore", "Time slot added successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding time slot: ${e.message}")
                onFailure(e)
            }
    }

    fun deleteTimeSlot(
        timeSlot: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        timeSlotsCollection
            .whereEqualTo("timeSlot", timeSlot)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onFailure(Exception("Time slot not found"))
                    return@addOnSuccessListener
                }
                
                // Delete the first matching document
                documents.documents[0].reference
                    .delete()
                    .addOnSuccessListener {
                        Log.d("Firestore", "Time slot deleted successfully")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error deleting time slot: ${e.message}")
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error finding time slot: ${e.message}")
                onFailure(e)
            }
    }

    fun fetchTimeSlots(
        onComplete: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        timeSlotsCollection
            .get()
            .addOnSuccessListener { documents ->
                val timeSlots = documents.mapNotNull { doc ->
                    doc.getString("timeSlot")
                }
                onComplete(timeSlots)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching time slots: ${e.message}")
                onFailure(e)
            }
    }

    fun listenForTimeSlotUpdates(
        onUpdate: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return timeSlotsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error listening for time slot updates: ${error.message}")
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val timeSlots = snapshot.documents.mapNotNull { doc ->
                        doc.getString("timeSlot")
                    }
                    onUpdate(timeSlots)
                }
            }
    }

    // Subjects methods
    fun addSubject(
        subject: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        subjectsCollection
            .document(subject)
            .set(mapOf("subject" to subject))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteSubject(
        subject: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        subjectsCollection
            .document(subject)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun fetchSubjects(
        onComplete: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        subjectsCollection
            .get()
            .addOnSuccessListener { documents ->
                val subjects = documents.mapNotNull { it.getString("subject") }
                onComplete(subjects)
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Fetch timetables for a specific teacher and class
    fun fetchTimetablesForTeacherAndClass(
        teacherName: String,
        classGrade: String,
        onComplete: (List<Timetable>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        timetablesCollection
            .whereEqualTo("teacher", teacherName)
            .whereEqualTo("classGrade", classGrade)
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val timetables = snapshot.documents.mapNotNull { doc ->
                    try {
                        val timetable = Timetable()
                        timetable.id = doc.id
                        timetable.classGrade = doc.getString("classGrade") ?: ""
                        timetable.dayOfWeek = doc.getString("dayOfWeek") ?: ""
                        timetable.timeSlot = doc.getString("timeSlot") ?: ""
                        timetable.subject = doc.getString("subject") ?: ""
                        timetable.teacher = doc.getString("teacher") ?: ""
                        timetable.roomNumber = doc.getString("roomNumber") ?: ""
                        timetable.isActive = doc.getBoolean("active") ?: true
                        timetable
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error converting document to Timetable: ${e.message}")
                        null
                    }
                }
                Log.d("Firestore", "Fetched ${timetables.size} timetables for teacher $teacherName in class $classGrade")
                onComplete(timetables)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching timetables: ${e.message}")
                onFailure(e)
            }
    }

    // Admin Profile Methods
    suspend fun getAdminProfile(adminId: String): AdminProfile? {
        return try {
            val doc = db.collection("admin_profiles")
                .document(adminId)
                .get()
                .await()
            
            if (doc != null && doc.exists()) {
                doc.toObject(AdminProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting admin profile: ${e.message}")
            null
        }
    }

    suspend fun updateAdminProfile(adminId: String, profile: AdminProfile): Boolean {
        return try {
            db.collection("admin_profiles")
                .document(adminId)
                .set(profile)
                .await()
            true
        } catch (e: Exception) {
            Log.e("Firestore", "Error updating admin profile: ${e.message}")
            false
        }
    }

    // School Profile Methods
    suspend fun getSchoolProfile(): SchoolProfile? {
        return try {
            val doc = db.collection("school_profile")
                .document("main")
                .get()
                .await()
            
            if (doc != null && doc.exists()) {
                doc.toObject(SchoolProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting school profile: ${e.message}")
            null
        }
    }

    suspend fun updateSchoolProfile(profile: SchoolProfile): Boolean {
        return try {
            db.collection("school_profile")
                .document("main")
                .set(profile)
                .await()
            true
        } catch (e: Exception) {
            Log.e("Firestore", "Error updating school profile: ${e.message}")
            false
        }
    }

    fun fetchAttendanceByDate(date: String, userType: UserType, onComplete: (List<AttendanceRecord>) -> Unit) {
        try {
            val db = FirebaseFirestore.getInstance()
            Log.d("Firestore", "Starting attendance fetch for date: $date, userType: ${userType.name}")
            
            if (date.isBlank()) {
                Log.e("Firestore", "Invalid date provided")
                onComplete(emptyList())
                return
            }

            db.collection("attendance")
                .document(date)
                .collection(userType.name.lowercase())
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        Log.d("Firestore", "Got snapshot with ${snapshot.documents.size} documents")
                        val records = snapshot.documents.mapNotNull { doc ->
                            try {
                                Log.d("Firestore", "Processing document: ${doc.id}")
                                val record = doc.toObject(AttendanceRecord::class.java)
                                
                                // Validate record
                                if (record == null) {
                                    Log.w("Firestore", "Failed to deserialize document ${doc.id}")
                                    return@mapNotNull null
                                }
                                
                                // Ensure required fields are present
                                if (record.id.isBlank() || record.userId.isBlank()) {
                                    Log.w("Firestore", "Invalid record found in ${doc.id}: missing id or userId")
                                    return@mapNotNull null
                                }
                                
                                // Validate date and userType
                                if (record.date <= 0L) {
                                    Log.w("Firestore", "Invalid date in record ${doc.id}")
                                    record.date = System.currentTimeMillis()
                                }
                                
                                // Ensure status is valid
                                if (record.status !in AttendanceStatus.values()) {
                                    Log.w("Firestore", "Invalid status in record ${doc.id}")
                                    record.status = AttendanceStatus.ABSENT
                                }
                                
                                record
                            } catch (e: Exception) {
                                Log.e("Firestore", "Error processing document ${doc.id}: ${e.message}")
                                e.printStackTrace()
                                null
                            }
                        }
                        
                        Log.d("Firestore", "Successfully processed ${records.size} valid records")
                        onComplete(records)
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error processing snapshot: ${e.message}")
                        e.printStackTrace()
                        onComplete(emptyList())
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error fetching attendance: ${e.message}")
                    e.printStackTrace()
                    onComplete(emptyList())
                }
        } catch (e: Exception) {
            Log.e("Firestore", "Critical error in fetchAttendanceByDate: ${e.message}")
            e.printStackTrace()
            onComplete(emptyList())
        }
    }

    fun submitLeave(
        leave: LeaveApplication,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val appliedAt = leave.appliedAt
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(appliedAt))
        val leaveWithDateKey = leave.copy()
        val leaveMap = leaveWithDateKey.toMap().toMutableMap()
        leaveMap["dateKey"] = dateKey
        db.collection("leave_applications")
            .add(leaveMap)
            .addOnSuccessListener { docRef ->
                docRef.update("id", docRef.id)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun listenToTeacherLeaves(
        teacherId: String,
        onUpdate: (List<LeaveApplication>) -> Unit,
        onError: (Exception) -> Unit
    ) = db.collection("leave_applications")
        .whereEqualTo("teacherId", teacherId)
        .addSnapshotListener { snapshot, e ->
            if (e != null) {
                onError(e)
                return@addSnapshotListener
            }
            onUpdate(snapshot?.documents?.mapNotNull { it.toObject(LeaveApplication::class.java)?.copy(id = it.id) } ?: emptyList())
        }

    fun getLeaveDetails(
        leaveId: String,
        onSuccess: (LeaveApplication?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("leave_applications").document(leaveId)
            .get()
            .addOnSuccessListener { doc ->
                onSuccess(doc.toObject(LeaveApplication::class.java)?.copy(id = doc.id))
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun listenToAllLeaves(
        status: String?,
        onUpdate: (List<LeaveApplication>) -> Unit,
        onError: (Exception) -> Unit
    ) = db.collection("leave_applications")
        .let { if (status != null && status != "all") it.whereEqualTo("status", status) else it }
        .addSnapshotListener { snapshot, e ->
            if (e != null) {
                onError(e)
                return@addSnapshotListener
            }
            onUpdate(snapshot?.documents?.mapNotNull { it.toObject(LeaveApplication::class.java)?.copy(id = it.id) } ?: emptyList())
        }

    fun updateLeaveStatus(
        leaveId: String,
        status: String,
        adminRemarks: String,
        adminId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("leave_applications").document(leaveId)
            .update(
                mapOf(
                    "status" to status,
                    "adminRemarks" to adminRemarks,
                    "reviewedBy" to adminId,
                    "reviewedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}