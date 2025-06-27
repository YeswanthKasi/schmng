package com.ecorvi.schmng.ui.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ecorvi.schmng.models.AttendanceRecord
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.ClassEvent as ModelClassEvent
import com.ecorvi.schmng.models.UserType
import com.ecorvi.schmng.models.LeaveApplication
import com.ecorvi.schmng.models.Student
import com.ecorvi.schmng.models.User
import com.ecorvi.schmng.ui.data.model.AdminProfile
import com.ecorvi.schmng.ui.data.model.AttendanceSummary
import com.ecorvi.schmng.ui.data.model.ChatMessage
import com.ecorvi.schmng.ui.data.model.ChildInfo
import com.ecorvi.schmng.ui.data.model.Event
import com.ecorvi.schmng.ui.data.model.Fee
import com.ecorvi.schmng.ui.data.model.Notice
import com.ecorvi.schmng.ui.data.model.ParentInfo
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.data.model.Schedule
import com.ecorvi.schmng.ui.data.model.SchoolProfile
import com.ecorvi.schmng.ui.data.model.Timetable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import id.zelory.compressor.Compressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.runBlocking
import android.graphics.Bitmap
import id.zelory.compressor.constraint.default
import id.zelory.compressor.constraint.size

object FirestoreDatabase {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val functions = Firebase.functions
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val storageRef: StorageReference = storage.reference
    
    private val schedulesCollection = db.collection("schedules")
    private val feesCollection = db.collection("fees")
    private val timetablesCollection = db.collection("timetables")
    private val classEventsCollection = db.collection("class_events")

    val studentsCollection: CollectionReference = db.collection("students")
    val teachersCollection: CollectionReference = db.collection("teachers")
    val staffCollection: CollectionReference = db.collection("non_teaching_staff")
    val pendingFeesCollection: CollectionReference = db.collection("pending_fees")
    val parentsCollection: CollectionReference = db.collection("parents")

    private var teacherListener: ListenerRegistration? = null
    private var studentListener: ListenerRegistration? = null
    private var staffListener: ListenerRegistration? = null

    // Time slots collection
    private val timeSlotsCollection = db.collection("timeSlots")
    private val subjectsCollection = db.collection("subjects")

    // Constants
    private const val PROFILE_PHOTOS_PATH = "profile_photos"
    private const val DOCUMENTS_PATH = "documents"
    private const val MAX_PHOTO_SIZE = 1024 * 1024 // 1MB

    private suspend fun cleanupOldProfilePhotos(userId: String) = withContext(Dispatchers.IO) {
        try {
            val photoRef = storageRef.child("$PROFILE_PHOTOS_PATH/$userId")
            val files = photoRef.listAll().await()
            
            // Keep only the most recent photo
            if (files.items.size > 1) {
                files.items.dropLast(1).forEach { oldPhoto ->
                    try {
                        oldPhoto.delete().await()
                    } catch (e: Exception) {
                        Log.w("FirestoreDatabase", "Failed to delete old photo: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreDatabase", "Error cleaning up old profile photos", e)
        }
    }

    // Storage Functions
    suspend fun uploadProfilePhoto(
        userId: String,
        photoUri: Uri,
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        try {
            // Create a reference to the photo location
            val photoRef = storageRef.child("$PROFILE_PHOTOS_PATH/$userId/${UUID.randomUUID()}")
            
            // Convert Uri to File and compress
            val originalFile = File(context.cacheDir, "profile_photo")
            context.contentResolver.openInputStream(photoUri)?.use { input ->
                originalFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Compress the image using Compressor
            val compressedFile = Compressor.compress(context, originalFile, Dispatchers.IO) {
                default(width = 1024, format = Bitmap.CompressFormat.JPEG)
                size(MAX_PHOTO_SIZE.toLong())
            }

            // Upload the file
            val uploadTask = photoRef.putFile(Uri.fromFile(compressedFile))

            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount
                onProgress(progress)
            }

            // Wait for upload to complete
            uploadTask.await()

            // Get download URL
            val downloadUrl = photoRef.downloadUrl.await().toString()
            
            // Clean up temporary files
            originalFile.delete()
            compressedFile.delete()

            // Clean up old profile photos
            cleanupOldProfilePhotos(userId)

            // Update the appropriate collection with the photo URL
            // First try users collection for admin
            var userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                db.collection("users").document(userId).update("profilePhoto", downloadUrl).await()
                return@withContext downloadUrl
            }

            // Try teachers collection
            userDoc = teachersCollection.document(userId).get().await()
            if (userDoc.exists()) {
                teachersCollection.document(userId).update("profilePhoto", downloadUrl).await()
                return@withContext downloadUrl
            }

            // Try students collection
            userDoc = studentsCollection.document(userId).get().await()
            if (userDoc.exists()) {
                studentsCollection.document(userId).update("profilePhoto", downloadUrl).await()
                return@withContext downloadUrl
            }

            // Try staff collection
            userDoc = staffCollection.document(userId).get().await()
            if (userDoc.exists()) {
                staffCollection.document(userId).update("profilePhoto", downloadUrl).await()
                return@withContext downloadUrl
            }

            throw Exception("User document not found in any collection")
        } catch (e: Exception) {
            Log.e("FirestoreDatabase", "Error uploading profile photo", e)
            throw e
        }
    }

    suspend fun uploadDocument(
        userId: String,
        documentUri: Uri,
        documentType: String,
        onProgress: (Float) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        try {
            val documentRef = storageRef.child("$DOCUMENTS_PATH/$userId/$documentType/${UUID.randomUUID()}")
            
            val uploadTask = documentRef.putFile(documentUri)

            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount
                onProgress(progress)
            }

            uploadTask.await()
            return@withContext documentRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("FirestoreDatabase", "Error uploading document", e)
            throw e
        }
    }

    suspend fun deleteFile(fileUrl: String) = withContext(Dispatchers.IO) {
        try {
            val fileRef = storage.getReferenceFromUrl(fileUrl)
            fileRef.delete().await()
        } catch (e: Exception) {
            Log.e("FirestoreDatabase", "Error deleting file", e)
            throw e
        }
    }

    suspend fun getProfilePhotoUrl(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            // Try each collection in sequence
            val collections = listOf(
                "students",
                "teachers",
                "non_teaching_staff",
                "users" // Fallback for admin
            )

            for (collection in collections) {
                val doc = db.collection(collection).document(userId).get().await()
                if (doc.exists() && doc.contains("profilePhoto")) {
                    return@withContext doc.getString("profilePhoto")
                }
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e("FirestoreDatabase", "Error getting profile photo URL", e)
            return@withContext null
        }
    }

    suspend fun updateProfilePhoto(userId: String, photoUrl: String, userType: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            // Determine the collection based on user type
            val collection = when (userType?.lowercase()) {
                "student" -> "students"
                "teacher" -> "teachers"
                "non_teaching_staff" -> "non_teaching_staff"
                else -> {
                    // If user type not specified, try to find the correct collection
                    val collections = listOf("students", "teachers", "non_teaching_staff", "users")
                    var foundCollection: String? = null
                    
                    for (coll in collections) {
                        if (db.collection(coll).document(userId).get().await().exists()) {
                            foundCollection = coll
                            break
                        }
                    }
                    foundCollection ?: "users" // Default to users if not found
                }
            }

            // Update the profile photo in the correct collection
            db.collection(collection).document(userId)
                .update("profilePhoto", photoUrl)
                .await()

            return@withContext true
        } catch (e: Exception) {
            Log.e("FirestoreDatabase", "Error updating profile photo", e)
            return@withContext false
        }
    }

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
    fun addStudent(student: Student, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Validate required fields
        when {
            student.className.isBlank() -> {
                onFailure(IllegalArgumentException("Student must have a class name"))
                return
            }
            student.firstName.isBlank() -> {
                onFailure(IllegalArgumentException("Student must have a first name"))
                return
            }
            student.lastName.isBlank() -> {
                onFailure(IllegalArgumentException("Student must have a last name"))
                return
            }
            student.email.isBlank() -> {
                onFailure(IllegalArgumentException("Student must have an email"))
                return
            }
        }

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
                val schedulesMap = mutableMapOf<String, String>()
                snapshot.documents.forEach { doc ->
                    doc.getString("schedule")?.let { schedule ->
                        schedulesMap[doc.id] = schedule
                    }
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
                val feesMap = mutableMapOf<String, String>()
                snapshot.documents.forEach { doc ->
                    doc.getString("fee")?.let { fee ->
                        feesMap[doc.id] = fee
                    }
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

    // Get a student by ID with parent information
    suspend fun getStudent(studentId: String): Person? {
        return try {
            Log.d("Firestore", "Fetching student with ID: $studentId")
            
            // 1. Get student document
            val studentDoc = studentsCollection.document(studentId).get().await()
            if (!studentDoc.exists()) {
                Log.d("Firestore", "Student document does not exist")
                return null
            }

            // 2. Get basic student data
            val student = studentDoc.toObject(Person::class.java)?.copy(id = studentId)
            if (student == null) {
                Log.d("Firestore", "Failed to convert student document to Person object")
                return null
            }

            // 3. Get parent info from student document
            val parentId = studentDoc.getString("parentId")
            Log.d("Firestore", "Found parentId in student document: $parentId")

            if (!parentId.isNullOrBlank()) {
                // 4. Get parent document
                val parentDoc = parentsCollection.document(parentId).get().await()
                if (parentDoc.exists()) {
                    val parent = parentDoc.toObject(Person::class.java)
                    if (parent != null) {
                        Log.d("Firestore", "Found parent: ${parent.firstName} ${parent.lastName}")
                        return student.copy(
                            parentInfo = ParentInfo(
                                id = parentId,
                                name = "${parent.firstName} ${parent.lastName}".trim(),
                                email = parent.email,
                                phone = parent.phone
                            )
                        )
                    }
                } else {
                    // 5. If parent document doesn't exist, try getting info from student document
                    Log.d("Firestore", "Parent document not found, using info from student document")
                    return student.copy(
                        parentInfo = ParentInfo(
                            id = parentId,
                            name = studentDoc.getString("parentName") ?: "",
                            email = studentDoc.getString("parentEmail") ?: "",
                            phone = studentDoc.getString("parentPhone") ?: ""
                        )
                    )
                }
            } else {
                // 6. Try to find parent through relationship collection
                Log.d("Firestore", "No parentId in student document, checking relationships")
                val relationshipQuery = db.collection("parent_student_relationships")
                    .whereEqualTo("studentId", studentId)
                    .get()
                    .await()

                if (!relationshipQuery.isEmpty) {
                    val relationshipDoc = relationshipQuery.documents[0]
                    val relParentId = relationshipDoc.getString("parentId")
                    
                    if (relParentId != null) {
                        val parentDoc = parentsCollection.document(relParentId).get().await()
                        if (parentDoc.exists()) {
                            val parent = parentDoc.toObject(Person::class.java)
                            if (parent != null) {
                                Log.d("Firestore", "Found parent through relationship: ${parent.firstName} ${parent.lastName}")
                                return student.copy(
                                    parentInfo = ParentInfo(
                                        id = relParentId,
                                        name = "${parent.firstName} ${parent.lastName}".trim(),
                                        email = parent.email,
                                        phone = parent.phone
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 7. If no parent info found anywhere, return student as is
            Log.d("Firestore", "No parent information found for student")
            student

        } catch (e: Exception) {
            Log.e("Firestore", "Error getting student with parent info: ${e.message}")
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
        // First create the user document with profile photo field
        val userDataWithPhoto = userData.toMutableMap().apply {
            put("profilePhoto", "") // Initialize empty profile photo
        }
        
        db.collection("users")
            .document(userId)
            .set(userDataWithPhoto)
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
        .whereEqualTo("userId", teacherId)
        .whereEqualTo("userType", LeaveApplication.TYPE_TEACHER)
        .addSnapshotListener { snapshot, e ->
            if (e != null) {
                onError(e)
                return@addSnapshotListener
            }
            onUpdate(snapshot?.documents?.mapNotNull { it.toObject(LeaveApplication::class.java)?.copy(id = it.id) } ?: emptyList())
        }

    fun listenToStaffLeaves(
        staffId: String,
        onUpdate: (List<LeaveApplication>) -> Unit,
        onError: (Exception) -> Unit
    ) = db.collection("leave_applications")
        .whereEqualTo("userId", staffId)
        .whereEqualTo("userType", LeaveApplication.TYPE_STAFF)
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

    // Get students by class with improved error handling
    suspend fun getStudentsByClass(classId: String): List<Student> = withContext(Dispatchers.IO) {
        if (classId.isBlank()) {
            Log.e("Firestore", "Empty classId provided")
            return@withContext emptyList()
        }

        try {
            studentsCollection
                .whereEqualTo("className", classId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        doc.toObject(Student::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error converting document to Student: ${e.message}")
                        null
                    }
                }
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting students by class: ${e.message}")
            emptyList()
        }
    }

    // Get user's FCM token
    suspend fun getUserFCMToken(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection("user_tokens")
                .document(userId)
                .get()
                .await()
            
            doc.getString("token")
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting user FCM token: ${e.message}")
            null
        }
    }

    suspend fun getTeacherAssignedClass(teacherId: String): String? {
        return try {
            val doc = teachersCollection.document(teacherId).get().await()
            doc.getString("className")
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting teacher's assigned class: ${e.message}")
            null
        }
    }

    suspend fun getStudentClass(
        studentId: String,
        onComplete: (String?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val doc = studentsCollection.document(studentId).get().await()
            onComplete(doc.getString("className"))
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    suspend fun getClassEvents(
        className: String,
        onComplete: (List<ModelClassEvent>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            Log.d("Firestore", "Fetching class events for class: $className with status: active")
            // Simplify the query to match index exactly
            val snapshot = classEventsCollection
                .whereEqualTo("targetClass", className)
                .whereEqualTo("status", "active")
                .orderBy("eventDate", Query.Direction.DESCENDING)  // Only order by eventDate
                .get()
                .await()
            
            Log.d("Firestore", "Found ${snapshot.documents.size} events")
            val events = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(ModelClassEvent::class.java)?.also { event ->
                        event.id = doc.id
                        Log.d("Firestore", "Event details - Title: ${event.title}, Class: ${event.targetClass}, Status: ${event.status}, Date: ${event.eventDate}")
                    }
                } catch (e: Exception) {
                    Log.e("Firestore", "Error converting document to ClassEvent: ${e.message}", e)
                    null
                }
            }
            Log.d("Firestore", "Successfully processed ${events.size} events")
            onComplete(events)
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching class events: ${e.message}", e)
            e.printStackTrace()
            onFailure(e)
        }
    }

    // Notification and Event Functions with improved validation
    suspend fun notifyClassAboutEvent(
        classId: String,
        eventTitle: String,
        eventDescription: String,
        priority: String = "normal"
    ) = withContext(Dispatchers.IO) {
        try {
            // Validate input parameters
            when {
                classId.isBlank() -> {
                    Log.e("EventNotification", "Invalid parameter: classId is empty")
                    return@withContext
                }
                eventTitle.isBlank() -> {
                    Log.e("EventNotification", "Invalid parameter: eventTitle is empty")
                    return@withContext
                }
                eventDescription.isBlank() -> {
                    Log.e("EventNotification", "Invalid parameter: eventDescription is empty")
                    return@withContext
                }
                !setOf("normal", "high", "low").contains(priority.lowercase()) -> {
                    Log.e("EventNotification", "Invalid parameter: priority must be normal, high, or low")
                    return@withContext
                }
            }

            val students = getStudentsByClass(classId)
            if (students.isEmpty()) {
                Log.w("EventNotification", "No students found in class: $classId")
                return@withContext
            }

            val studentTokens = mutableListOf<String>()
            students.forEach { student ->
                getUserFCMToken(student.userId)?.let { token ->
                    studentTokens.add(token)
                }
            }

            if (studentTokens.isEmpty()) {
                Log.w("EventNotification", "No FCM tokens found for students in class: $classId")
                return@withContext
            }

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

            functions
                .getHttpsCallable("sendMulticastNotification")
                .call(notificationData)
                .await()

            Log.d("EventNotification", "Successfully sent notification to ${studentTokens.size} students")
        } catch (e: Exception) {
            Log.e("EventNotification", "Error sending notifications: ${e.message}")
            e.printStackTrace()
        }
    }

    // Schedule event reminder with improved validation
    suspend fun scheduleEventReminder(
        eventId: String,
        classId: String,
        eventTitle: String,
        eventTime: Long,
        reminderMinutes: Int = 30
    ) = withContext(Dispatchers.IO) {
        try {
            // Validate input parameters
            when {
                eventId.isBlank() -> {
                    Log.e("EventReminder", "Invalid parameter: eventId is empty")
                    return@withContext
                }
                classId.isBlank() -> {
                    Log.e("EventReminder", "Invalid parameter: classId is empty")
                    return@withContext
                }
                eventTitle.isBlank() -> {
                    Log.e("EventReminder", "Invalid parameter: eventTitle is empty")
                    return@withContext
                }
                eventTime <= System.currentTimeMillis() -> {
                    Log.e("EventReminder", "Invalid parameter: eventTime must be in the future")
                    return@withContext
                }
                reminderMinutes <= 0 -> {
                    Log.e("EventReminder", "Invalid parameter: reminderMinutes must be positive")
                    return@withContext
                }
            }

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

            Log.d("EventReminder", "Successfully scheduled reminder for event: $eventId")
        } catch (e: Exception) {
            Log.e("EventReminder", "Error scheduling reminder: ${e.message}")
            e.printStackTrace()
        }
    }

    // Cancel event notifications with improved validation
    suspend fun cancelEventNotifications(eventId: String) = withContext(Dispatchers.IO) {
        try {
            if (eventId.isBlank()) {
                Log.e("EventNotification", "Invalid parameter: eventId is empty")
                return@withContext
            }

            val cancelData = hashMapOf(
                "eventId" to eventId
            )

            functions
                .getHttpsCallable("cancelEventNotifications")
                .call(cancelData)
                .await()

            Log.d("EventNotification", "Successfully cancelled notifications for event: $eventId")
        } catch (e: Exception) {
            Log.e("EventNotification", "Error canceling notifications: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getMonthlyAttendance(studentId: String, month: String): AttendanceSummary? {
        return try {
            val db = FirebaseFirestore.getInstance()
            runBlocking {
                // Get all attendance records for the month
                val records = db.collection("attendance")
                    .whereEqualTo("userId", studentId)
                    .whereGreaterThanOrEqualTo("date", getMonthStartTimestamp(month))
                    .whereLessThan("date", getMonthEndTimestamp(month))
                    .get()
                    .await()

                if (!records.isEmpty) {
                    var present = 0
                    var absent = 0
                    var leave = 0

                    records.forEach { doc ->
                        when (doc.getString("status")?.uppercase()) {
                            "PRESENT" -> present++
                            "ABSENT" -> absent++
                            "LEAVE" -> leave++
                        }
                    }

                    AttendanceSummary(
                        present = present,
                        absent = absent,
                        leave = leave
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching monthly attendance: ${e.message}")
            null
        }
    }

    private fun getMonthStartTimestamp(monthStr: String): Long {
        val (year, month) = monthStr.split("-").map { it.toInt() }
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getMonthEndTimestamp(monthStr: String): Long {
        val (year, month) = monthStr.split("-").map { it.toInt() }
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MONTH, 1)
        return calendar.timeInMillis
    }

    suspend fun getParentMessages(parentId: String): List<ChatMessage>? = withContext(Dispatchers.IO) {
        try {
            val messages = db.collection("messages")
                .whereEqualTo("recipientId", parentId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                }
            messages
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getApprovedNotices(): List<Notice>? = withContext(Dispatchers.IO) {
        try {
            val notices = db.collection("notices")
                .whereEqualTo("status", "approved")
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(Notice::class.java)
                }
            notices
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getClassEvents(className: String): List<Event>? = withContext(Dispatchers.IO) {
        try {
            val events = db.collection("events")
                .whereEqualTo("className", className)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(Event::class.java)
                }
            events
        } catch (e: Exception) {
            null
        }
    }

    fun updateParentProfile(
        parentId: String,
        phone: String,
        address: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("parents")
                .document(parentId)
                .update(
                    mapOf(
                        "phone" to phone,
                        "address" to address
                    )
                )
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Get parent by child ID using the relationship collection
    fun getParentByChildId(childId: String, onSuccess: (Person?) -> Unit, onFailure: (Exception) -> Unit) {
        Log.d("Firestore", "Fetching parent for child: $childId")
        
        db.collection("student_parent_relationships")
            .whereEqualTo("studentId", childId)
            .get()
            .addOnSuccessListener { relationshipQuery ->
                if (!relationshipQuery.isEmpty) {
                    val relationshipDoc = relationshipQuery.documents[0]
                    val parentId = relationshipDoc.getString("parentId")
                    
                    if (parentId != null) {
                        // First get the student details
                        studentsCollection.document(childId).get()
                            .addOnSuccessListener { studentDoc ->
                                val student = studentDoc.toObject(Person::class.java)
                                
                                // Then get the parent details
                                parentsCollection.document(parentId).get()
                                    .addOnSuccessListener { parentDoc ->
                                        if (parentDoc.exists() && student != null) {
                                            val parent = parentDoc.toObject(Person::class.java)?.copy(
                                                id = parentDoc.id,
                                                childInfo = ChildInfo(
                                                    id = childId,
                                                    name = "${student.firstName} ${student.lastName}".trim(),
                                                    className = student.className,
                                                    rollNumber = student.rollNumber
                                                )
                                            )
                                            Log.d("Firestore", "Found parent with child info: ${parent?.firstName} ${parent?.lastName}")
                                            onSuccess(parent)
                                        } else {
                                            Log.d("Firestore", "Parent document not found or student is null")
                                            onSuccess(null)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firestore", "Error getting parent document: ${e.message}")
                                        onFailure(e)
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Error getting student document: ${e.message}")
                                onFailure(e)
                            }
                    } else {
                        Log.d("Firestore", "No parentId in relationship document")
                        onSuccess(null)
                    }
                } else {
                    Log.d("Firestore", "No relationship found for child: $childId")
                    onSuccess(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error querying relationships: ${e.message}")
                onFailure(e)
            }
    }

    // Get student details for parent
    suspend fun getStudentForParent(parentId: String): Person? {
        return try {
            // First get parent details
            val parentDoc = parentsCollection.document(parentId).get().await()
            val parent = parentDoc.toObject(Person::class.java)

            // Then get relationship
            val relationshipQuery = db.collection("parent_student_relationships")
                .whereEqualTo("parentId", parentId)
                .get()
                .await()

            if (!relationshipQuery.isEmpty) {
                val relationshipDoc = relationshipQuery.documents[0]
                val studentId = relationshipDoc.getString("studentId")
                
                if (studentId != null) {
                    val studentDoc = studentsCollection.document(studentId).get().await()
                    if (studentDoc.exists() && parent != null) {
                        studentDoc.toObject(Person::class.java)?.copy(
                            id = studentId,
                            parentInfo = ParentInfo(
                                id = parentId,
                                name = "${parent.firstName} ${parent.lastName}".trim(),
                                email = parent.email,
                                phone = parent.phone
                            )
                        )
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting student for parent: ${e.message}")
            null
        }
    }

    // Update parent with relationships
    fun updateParent(parent: Person, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val batch = db.batch()

        try {
            // 1. Update parent document
            val parentDoc = parentsCollection.document(parent.id)
            batch.set(parentDoc, parent)

            // 2. Update relationship if child info exists
            if (parent.childInfo != null) {
                // Find existing relationship
                db.collection("parent_student_relationships")
                    .whereEqualTo("parentId", parent.id)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val relationshipDoc = querySnapshot.documents[0].reference
                            val relationshipUpdate = hashMapOf<String, Any>(
                                "studentName" to (parent.childInfo.name),
                                "studentClass" to (parent.childInfo.className),
                                "updatedAt" to com.google.firebase.Timestamp.now()
                            )
                            batch.update(relationshipDoc, relationshipUpdate)
                        }

                        // 3. Update student document
                        val studentDoc = studentsCollection.document(parent.childInfo.id)
                        val studentUpdate = hashMapOf<String, Any>(
                            "parentId" to parent.id,
                            "parentName" to "${parent.firstName} ${parent.lastName}".trim(),
                            "parentEmail" to parent.email,
                            "parentPhone" to parent.phone
                        )
                        batch.update(studentDoc, studentUpdate)

                        // Commit all changes
                        batch.commit()
                            .addOnSuccessListener {
                                Log.d("Firestore", "Successfully updated parent and relationships")
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Error updating parent: ${e.message}")
                                onFailure(e)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error finding relationship: ${e.message}")
                        onFailure(e)
                    }
            } else {
                // Just update parent document if no child info
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("Firestore", "Successfully updated parent")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error updating parent: ${e.message}")
                        onFailure(e)
                    }
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error in updateParent: ${e.message}")
            onFailure(e)
        }
    }

    // Create parent with proper relationships
    fun createParent(parent: Person, studentId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val batch = db.batch()

        try {
            // 1. Create parent document
            val parentDoc = parentsCollection.document(parent.id)
            batch.set(parentDoc, parent)

            // 2. Create parent-student relationship
            val relationshipDoc = db.collection("parent_student_relationships").document()
            val relationshipData = hashMapOf<String, Any>(
                "parentId" to parent.id,
                "studentId" to studentId,
                "studentName" to (parent.childInfo?.name ?: ""),
                "studentClass" to (parent.childInfo?.className ?: ""),
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            batch.set(relationshipDoc, relationshipData)

            // 3. Update student document with parent reference
            val studentDoc = studentsCollection.document(studentId)
            val studentUpdate = hashMapOf<String, Any>(
                "parentId" to parent.id,
                "parentName" to "${parent.firstName} ${parent.lastName}".trim(),
                "parentEmail" to parent.email,
                "parentPhone" to parent.phone
            )
            batch.update(studentDoc, studentUpdate)

            // Commit all changes
            batch.commit()
                .addOnSuccessListener {
                    Log.d("Firestore", "Successfully created parent and relationships")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error creating parent: ${e.message}")
                    onFailure(e)
                }

        } catch (e: Exception) {
            Log.e("Firestore", "Error in createParent: ${e.message}")
            onFailure(e)
        }
    }

    // Delete parent
    fun deleteParent(parentId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val batch = db.batch()

        try {
            // Delete parent document
            val parentDoc = parentsCollection.document(parentId)
            batch.delete(parentDoc)

            // Delete user document
            val userDoc = db.collection("users").document(parentId)
            batch.delete(userDoc)

            // Delete student-parent relationship
            val relationshipDoc = db.collection("student_parent_relationships").document(parentId)
            batch.delete(relationshipDoc)

            batch.commit()
                .addOnSuccessListener {
                    Log.d("Firestore", "Parent and related documents deleted successfully")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error deleting parent: ${e.message}")
                    onFailure(e)
                }
        } catch (e: Exception) {
            Log.e("Firestore", "Error in delete operation: ${e.message}")
            onFailure(e)
        }
    }

    // Get parent by ID
    suspend fun getParent(parentId: String): Person? {
        return try {
            Log.d("Firestore", "Fetching parent with ID: $parentId")
            val doc = parentsCollection.document(parentId).get().await()
            if (doc.exists()) {
                doc.toObject(Person::class.java)?.copy(id = doc.id)
            } else {
                Log.d("Firestore", "Parent document does not exist")
                null
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting parent: ${e.message}")
            null
        }
    }

    // Create auth accounts for existing staff members
    fun createAuthAccountForStaff(
        staffId: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        staffCollection.document(staffId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val staff = document.toObject(Person::class.java)
                    if (staff != null) {
                        // Create Firebase Auth account
                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(staff.email, password)
                            .addOnSuccessListener { authResult ->
                                val userId = authResult.user?.uid ?: return@addOnSuccessListener
                                
                                // Start a batch write
                                val batch = db.batch()
                                
                                // Update staff document with new ID
                                val staffRef = staffCollection.document(userId)
                                val staffDataWithId = staff.copy(id = userId, password = password)
                                batch.set(staffRef, staffDataWithId)
                                
                                // Delete old staff document
                                batch.delete(staffCollection.document(staffId))
                                
                                // Add to users collection
                                val userRef = db.collection("users").document(userId)
                                val userData = mapOf(
                                    "role" to "staff",
                                    "email" to staff.email,
                                    "name" to "${staff.firstName} ${staff.lastName}",
                                    "createdAt" to com.google.firebase.Timestamp.now(),
                                    "userId" to userId,
                                    "type" to "staff",
                                    "department" to staff.department
                                )
                                batch.set(userRef, userData)
                                
                                // Commit the batch
                                batch.commit()
                                    .addOnSuccessListener {
                                        onSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        // If Firestore fails, delete the auth user
                                        authResult.user?.delete()
                                        onFailure(e)
                                    }
                            }
                            .addOnFailureListener { e ->
                                onFailure(e)
                            }
                    } else {
                        onFailure(Exception("Staff data not found"))
                    }
                } else {
                    onFailure(Exception("Staff document not found"))
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // Get staff by ID
    fun getStaffById(
        staffId: String,
        onComplete: (User?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        staffCollection.document(staffId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val staff = document.toObject(Person::class.java)
                        if (staff != null) {
                            val user = User(
                                id = document.id,
                                firstName = staff.firstName,
                                lastName = staff.lastName,
                                email = staff.email,
                                phone = staff.phone,
                                mobileNo = staff.mobileNo,
                                address = staff.address,
                                age = staff.age,
                                gender = staff.gender,
                                dateOfBirth = staff.dateOfBirth,
                                className = "",  // Staff doesn't have a class
                                rollNumber = "", // Staff doesn't have a roll number
                                department = staff.department ?: "",
                                designation = staff.designation ?: "",
                                type = "staff",
                                password = staff.password ?: ""
                            )
                            onComplete(user)
                        } else {
                            onComplete(null)
                        }
                    } catch (e: Exception) {
                        onError(e)
                    }
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // Add these functions after the existing functions
    fun fetchGenderStatistics(
        userType: String,
        onSuccess: (Map<String, Int>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val collection = when (userType.lowercase()) {
            "students" -> db.collection("students")
            "teachers" -> db.collection("teachers")
            "staff" -> db.collection("non_teaching_staff")
            else -> throw IllegalArgumentException("Invalid user type")
        }

        collection.get()
            .addOnSuccessListener { snapshot ->
                val genderStats = mutableMapOf(
                    "male" to 0,
                    "female" to 0
                )
                
                snapshot.documents.forEach { doc ->
                    when (doc.getString("gender")?.lowercase()) {
                        "male" -> genderStats["male"] = genderStats["male"]!! + 1
                        "female" -> genderStats["female"] = genderStats["female"]!! + 1
                    }
                }
                
                onSuccess(genderStats)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    suspend fun updateStaffField(
        staffId: String,
        field: String,
        value: Any,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            db.collection("staff")
                .document(staffId)
                .update(field, value)
                .await()
            onSuccess()
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to update staff field")
        }
    }

    suspend fun deleteStaff(
        staffId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            // Delete staff document
            db.collection("staff")
                .document(staffId)
                .delete()
                .await()
            
            // Delete profile photo if exists
            try {
                storage.reference.child("profile_photos/$staffId").delete().await()
            } catch (e: Exception) {
                // Ignore if photo doesn't exist
            }
            
            onSuccess()
        } catch (e: Exception) {
            onError(e)
        }
    }
}