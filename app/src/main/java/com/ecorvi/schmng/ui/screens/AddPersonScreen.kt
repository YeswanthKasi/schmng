package com.ecorvi.schmng.ui.screens

import android.app.DatePickerDialog
import android.R.attr.password
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ecorvi.schmng.ui.components.ProfilePhotoComponent
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.ChildInfo
import com.ecorvi.schmng.ui.data.model.ParentInfo
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val StudentGreen = Color(0xFF4CAF50) // Green color for student theme
private val TeacherBlue = Color(0xFF1F41BB) // Blue color for teacher theme

// Helper function to format class name
fun formatClassForStorage(className: String): String {
    return if (className.endsWith("st")) {
        "Class ${className.replace("st", "")}"
    } else if (!className.startsWith("Class ")) {
        "Class $className"
    } else {
        className
    }
}

fun enforceRoleTypeConsistency(personType: String, person: Person): Boolean {
    return when (personType) {
        "student" -> person.type.equals("student", ignoreCase = true)
        "teacher" -> person.type.equals("teacher", ignoreCase = true)
        "staff" -> person.type.equals("staff", ignoreCase = true) || person.type.equals("non_teaching_staff", ignoreCase = true)
        else -> false
    }
}

fun savePersonDirectly(
    person: Person,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    email: String,
    password: String,
    personType: String,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    // Enforce type field matches personType
    val correctedPerson = person.copy(type = personType)
    if (!enforceRoleTypeConsistency(personType, correctedPerson)) {
        onError(Exception("Role/type mismatch: $personType cannot be saved as ${correctedPerson.type}"))
        return
    }
    auth.createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener { authResult ->
            val userId = authResult.user?.uid ?: return@addOnSuccessListener

            // Create user document
            val userDocRef = db.collection("users").document(userId)
            val userData = mapOf(
                "role" to personType,
                "email" to email,
                "name" to "${correctedPerson.firstName} ${correctedPerson.lastName}",
                "createdAt" to com.google.firebase.Timestamp.now(),
                "userId" to userId,
                "type" to correctedPerson.type,
                "className" to correctedPerson.className
            )

            // Create person document
            val personDocRef = when (personType) {
                "student" -> db.collection("students").document(userId)
                "teacher" -> db.collection("teachers").document(userId)
                else -> db.collection("non_teaching_staff").document(userId)
            }

            // Start a batch write
            val batch = db.batch()
            batch.set(userDocRef, userData)
            batch.set(personDocRef, correctedPerson.copy(id = userId))

            // Commit the batch
            batch.commit()
                .addOnSuccessListener {
                    onSuccess(userId)
                }
                .addOnFailureListener { e ->
                    // If Firestore fails, delete the auth user
                    authResult.user?.delete()
                    onError(e)
                }
        }
        .addOnFailureListener { e ->
            onError(e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonScreen(
    navController: NavController,
    personType: String,
    personId: String? = null
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var gender by remember { mutableStateOf("Male") }
    var dateOfBirth by remember { mutableStateOf("") }
    var mobileNo by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(Constants.CLASS_OPTIONS.first()) }
    var rollNumber by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var personTypeValue by remember { mutableStateOf("Regular") }

    // Admissions-only (student)
    var admissionNumber by remember { mutableStateOf("") }
    var admissionDate by remember { mutableStateOf("") } // default today, editable
    var academicYear by remember { mutableStateOf(FirestoreDatabase.getCurrentAcademicYear()) }
    var aadharNumber by remember { mutableStateOf("") }
    var aaparId by remember { mutableStateOf("") }

    // Parent-related state variables
    var parentFirstName by remember { mutableStateOf("") }
    var parentLastName by remember { mutableStateOf("") }
    var parentEmail by remember { mutableStateOf("") }
    var parentPassword by remember { mutableStateOf("") }
    var showParentPassword by remember { mutableStateOf(false) }
    var parentPhone by remember { mutableStateOf("") }
    var parentAddress by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf("") }

    var showClassDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(personId != null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var isErrorDialog by remember { mutableStateOf(false) }
    val adminEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    var adminPassword by remember { mutableStateOf("") }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isEditMode = personId != null
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }

    // Define person type options
    val typeOptions = if (personType == "student") {
        listOf("Regular", "Scholarship", "Transfer")
    } else {
        listOf("Regular", "Temporary", "Visiting")
    }

    LaunchedEffect(personId) {
        if (personId != null) {
            isLoading = true
            try {
                val person = if (personType == "student") {
                    FirestoreDatabase.getStudent(personId)
                } else {
                    FirestoreDatabase.getTeacher(personId)
                }
                if (person != null) {
                    firstName = person.firstName
                    lastName = person.lastName
                    email = person.email
                    phone = person.phone
                    personTypeValue = person.type
                    selectedClass = person.className
                    rollNumber = person.rollNumber
                    gender = person.gender
                    dateOfBirth = person.dateOfBirth
                    mobileNo = person.mobileNo
                    address = person.address
                    age = if (person.age > 0) person.age.toString() else ""
                    admissionNumber = person.admissionNumber
                    admissionDate = person.admissionDate
                    academicYear = if (person.academicYear.isNotBlank()) person.academicYear else FirestoreDatabase.getCurrentAcademicYear()
                    aadharNumber = person.aadharNumber
                    aaparId = person.aaparId

                    // Edit-mode prefill for legacy students without admission fields
                    if (personType == "student" && admissionNumber.isBlank()) {
                        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                        admissionNumber = "ADM-$year-"
                    }
                    if (personType == "student" && admissionDate.isBlank()) {
                        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        admissionDate = fmt.format(java.util.Calendar.getInstance().time)
                    }

                    // If this is a student, fetch parent information
                    if (personType == "student") {
                        try {
                            FirestoreDatabase.getParentByChildId(
                                childId = personId,
                                onSuccess = { parent ->
                                    if (parent != null) {
                                        parentId = parent.id
                                        parentFirstName = parent.firstName
                                        parentLastName = parent.lastName
                                        parentEmail = parent.email
                                        parentPhone = parent.phone
                                        parentAddress = parent.address
                                    }
                                },
                                onFailure = { e ->
                                    errorMessage = "Failed to load parent data: ${e.message}"
                                    showErrorDialog = true
                                }
                            )
                        } catch (e: Exception) {
                            errorMessage = "Failed to load parent data: ${e.message}"
                            showErrorDialog = true
                        }
                    }

                    // Load profile photo if in edit mode
                    try {
                        profilePhotoUrl = FirestoreDatabase.getProfilePhotoUrl(personId)
                    } catch (e: Exception) {
                        errorMessage = "Failed to load profile photo: ${e.message}"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load data: ${e.message}"
                showErrorDialog = true
            }
            isLoading = false
        }
    }

    // Generate defaults for new student
    LaunchedEffect(personType, isEditMode) {
        if (!isEditMode && personType == "student") {
            try {
                // Preview next admission number without incrementing counter
                admissionNumber = FirestoreDatabase.previewNextAdmissionNumber()
            } catch (e: Exception) {
                admissionNumber = ""
            }
            // Set today's date in yyyy-MM-dd to match backend scripts
            val cal = java.util.Calendar.getInstance()
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            admissionDate = fmt.format(cal.time)
            if (academicYear.isBlank()) academicYear = FirestoreDatabase.getCurrentAcademicYear()
        }
    }

    @Composable
    fun ShowMessage(message: String) {
        val context = LocalContext.current
        LaunchedEffect(message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun validateInputs(): Boolean {
        var isValid = true
        var validationMessage = ""

        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || (!isEditMode && password.isBlank())) {
            validationMessage = "Please fill in all required fields"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            validationMessage = "Please enter a valid email address"
            isValid = false
        } else if (phone.isNotBlank() && !android.util.Patterns.PHONE.matcher(phone).matches()) {
            validationMessage = "Please enter a valid phone number"
            isValid = false
        } else if (age.isNotBlank() && age.toIntOrNull() == null) {
            validationMessage = "Please enter a valid age"
            isValid = false
        }

        if (!isValid) {
            errorMessage = validationMessage
            showErrorDialog = true
        }
        return isValid
    }
    
    fun createStudentWithReservedNumber(student: Person, studentId: String) {
        val auth = FirebaseAuth.getInstance()
        // Handle parent information if provided
        if (parentEmail.isNotBlank() && parentPassword.isNotBlank()) {
            // Create parent authentication account
            auth.createUserWithEmailAndPassword(parentEmail, parentPassword)
                .addOnSuccessListener { parentAuthResult ->
                    val parentId = parentAuthResult.user?.uid ?: return@addOnSuccessListener
                    
                    // Create parent document
                    val parent = Person(
                        id = parentId,
                        firstName = parentFirstName.trim(),
                        lastName = parentLastName.trim(),
                        email = parentEmail.trim(),
                        phone = parentPhone.trim(),
                        childInfo = ChildInfo(
                            id = studentId,
                            name = "${student.firstName} ${student.lastName}".trim(),
                            className = student.className,
                            rollNumber = student.rollNumber
                        )
                    )
                    
                    // Create student with parent info
                    val studentWithParent = student.copy(
                        parentInfo = ParentInfo(
                            id = parentId,
                            name = "${parent.firstName} ${parent.lastName}".trim(),
                            email = parentEmail.trim(),
                            phone = parentPhone.trim()
                        )
                    )
                    
                    // Save both student and parent
                    FirestoreDatabase.createParent(parent, studentId,
                        onSuccess = {
                            FirestoreDatabase.updateStudent(studentWithParent,
                                onSuccess = {
                                    // Add student to users collection
                                    val db = FirebaseFirestore.getInstance()
                                    val studentUserDocRef = db.collection("users").document(studentId)
                                    val studentUserData = mapOf(
                                        "role" to "student",
                                        "email" to email.trim(),
                                        "name" to "${firstName.trim()} ${lastName.trim()}",
                                        "createdAt" to com.google.firebase.Timestamp.now(),
                                        "userId" to studentId,
                                        "type" to "student",
                                        "className" to student.className
                                    )
                                    studentUserDocRef.set(studentUserData)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            navController.popBackStack()
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            errorMessage = "Error creating user document: ${e.message}"
                                            showErrorDialog = true
                                        }
                                },
                                onFailure = { e ->
                                    isLoading = false
                                    errorMessage = "Error creating student: ${e.message}"
                                    showErrorDialog = true
                                }
                            )
                        },
                        onFailure = { e ->
                            isLoading = false
                            errorMessage = "Error creating parent: ${e.message}"
                            showErrorDialog = true
                        }
                    )
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    errorMessage = "Error creating parent account: ${e.message}"
                    showErrorDialog = true
                }
        } else {
            // No parent info, just create student
            FirestoreDatabase.updateStudent(student,
                onSuccess = {
                    // Add student to users collection
                    val db = FirebaseFirestore.getInstance()
                    val studentUserDocRef = db.collection("users").document(studentId)
                    val studentUserData = mapOf(
                        "role" to "student",
                        "email" to email.trim(),
                        "name" to "${firstName.trim()} ${lastName.trim()}",
                        "createdAt" to com.google.firebase.Timestamp.now(),
                        "userId" to studentId,
                        "type" to "student",
                        "className" to student.className
                    )
                    studentUserDocRef.set(studentUserData)
                        .addOnSuccessListener {
                            isLoading = false
                            navController.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            errorMessage = "Error creating user document: ${e.message}"
                            showErrorDialog = true
                        }
                },
                onFailure = { e ->
                    isLoading = false
                    errorMessage = "Error creating student: ${e.message}"
                    showErrorDialog = true
                }
            )
        }
    }
    
    fun createPersonDirectly(person: Person, userId: String) {
        val auth = FirebaseAuth.getInstance()
        // Add to users collection
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(userId)
        val userData = mapOf(
            "role" to personType,
            "email" to email.trim(),
            "name" to "${firstName.trim()} ${lastName.trim()}",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "userId" to userId,
            "type" to personType,
            "className" to person.className
        )
        
        // Create person document
        val personDocRef = when (personType) {
            "teacher" -> db.collection("teachers").document(userId)
            else -> db.collection("non_teaching_staff").document(userId)
        }
        
        // Start a batch write
        val batch = db.batch()
        batch.set(userDocRef, userData)
        batch.set(personDocRef, person.copy(id = userId))
        
        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                isLoading = false
                navController.popBackStack()
            }
            .addOnFailureListener { e ->
                // If Firestore fails, delete the auth user
                auth.currentUser?.delete()
                isLoading = false
                errorMessage = "Error creating ${personType}: ${e.message}"
                showErrorDialog = true
            }
    }

    fun proceedWithAccountCreation() {
        isLoading = true
        val auth = FirebaseAuth.getInstance()

        // First create student account and data
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { studentAuthResult ->
                val studentId = studentAuthResult.user?.uid ?: return@addOnSuccessListener

                // Generate and reserve admission number only when student is being saved
                if (personType == "student") {
                    kotlinx.coroutines.GlobalScope.launch {
                        try {
                            // Generate and reserve the admission number (increments counter)
                            val reservedAdmissionNumber = FirestoreDatabase.generateAndReserveAdmissionNumber()
                            
                            // Create student document with reserved admission number
                            val student = Person(
                                id = studentId,
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                type = personTypeValue,
                                className = formatClassForStorage(selectedClass),
                                rollNumber = rollNumber.trim(),
                                gender = gender,
                                dateOfBirth = dateOfBirth.trim(),
                                mobileNo = mobileNo.trim(),
                                address = address.trim(),
                                age = age.toIntOrNull() ?: 0,
                                admissionNumber = reservedAdmissionNumber,
                                admissionDate = admissionDate,
                                academicYear = academicYear,
                                aadharNumber = aadharNumber,
                                aaparId = aaparId
                            )
                            
                            // Continue with student creation using the reserved admission number
                            createStudentWithReservedNumber(student, studentId)
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Error generating admission number: ${e.message}"
                            showErrorDialog = true
                        }
                    }
                } else {
                    // For non-students, create normally
                    val person = Person(
                        id = studentId,
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        email = email.trim(),
                        phone = phone.trim(),
                        type = personTypeValue,
                        className = formatClassForStorage(selectedClass),
                        rollNumber = rollNumber.trim(),
                        gender = gender,
                        dateOfBirth = dateOfBirth.trim(),
                        mobileNo = mobileNo.trim(),
                        address = address.trim(),
                        age = age.toIntOrNull() ?: 0
                    )
                    createPersonDirectly(person, studentId)
                }

            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = "Error creating user authentication: ${e.message}"
                showErrorDialog = true
            }
    }

    fun savePerson() {
        if (!validateInputs()) {
            return
        }
        
        isLoading = true
        val auth = FirebaseAuth.getInstance()

        // Validate admission number and aadhar before creating account
        if (personType == "student") {
            // Use comprehensive validation in coroutine scope
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    val admissionValidation = FirestoreDatabase.validateAdmissionNumber(admissionNumber)
                    if (!admissionValidation.isValid) {
                        isLoading = false
                        errorMessage = admissionValidation.message
                        showErrorDialog = true
                        return@launch
                    }
                    
                    val aadharValidation = FirestoreDatabase.validateAadharNumber(aadharNumber)
                    if (!aadharValidation.isValid) {
                        isLoading = false
                        errorMessage = aadharValidation.message
                        showErrorDialog = true
                        return@launch
                    }
                    
                    // If validation passes, proceed with account creation
                    proceedWithAccountCreation()
                } catch (e: Exception) {
                    isLoading = false
                    errorMessage = "Validation error: ${e.message}"
                    showErrorDialog = true
                }
            }
        } else {
            // For non-students, proceed directly
            proceedWithAccountCreation()
        }
    }

    fun savePersonWithAdmin() {
        if (!validateInputs()) return

        isLoading = true
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        // Store the current admin user
        val currentAdmin = auth.currentUser
        if (currentAdmin == null) {
            isLoading = false
            isErrorDialog = true
            errorMessage = "Admin not logged in"
            showErrorDialog = true
            return
        }

        // Prepare person object with enforced type
        val correctedPerson = Person(
            id = "",
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = email.trim(),
            password = password,
            phone = phone.trim(),
            type = personType, // enforce type
            className = formatClassForStorage(selectedClass),
            rollNumber = rollNumber.trim(),
            gender = gender,
            dateOfBirth = dateOfBirth.trim(),
            mobileNo = mobileNo.trim(),
            address = address.trim(),
            age = age.toIntOrNull() ?: 0,
            admissionNumber = admissionNumber,
            admissionDate = admissionDate,
            academicYear = academicYear,
            aadharNumber = aadharNumber,
            aaparId = aaparId
        )
        if (!enforceRoleTypeConsistency(personType, correctedPerson)) {
            isLoading = false
            isErrorDialog = true
            errorMessage = "Role/type mismatch: $personType cannot be saved as ${correctedPerson.type}"
            showErrorDialog = true
            return
        }
        // First create the authentication account
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                // Sign back in as admin immediately
                auth.signInWithEmailAndPassword(currentAdmin.email ?: "", adminPassword)
                    .addOnSuccessListener {
                        // Start a Firestore batch
                        val batch = db.batch()

                        // Create user document
                        val userDocRef = db.collection("users").document(userId)
                        val userData = mapOf(
                            "role" to personType,
                            "email" to email,
                            "name" to "$firstName $lastName",
                            "createdAt" to com.google.firebase.Timestamp.now(),
                            "userId" to userId,
                            "type" to correctedPerson.type,
                            "className" to formatClassForStorage(selectedClass)
                        )
                        batch.set(userDocRef, userData)

                        // Create person document
                        val personDocRef = when (personType) {
                            "student" -> db.collection("students").document(userId)
                            "teacher" -> db.collection("teachers").document(userId)
                            else -> db.collection("non_teaching_staff").document(userId)
                        }
                        batch.set(personDocRef, correctedPerson.copy(id = userId))

                        // Update auth user profile
                        authResult.user?.updateProfile(
                            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName("$firstName $lastName")
                                .build()
                        )

                        // Commit Firestore batch
                        batch.commit()
                            .addOnSuccessListener {
                                isLoading = false
                                isErrorDialog = false
                                errorMessage = "${personType.capitalize()} added successfully"
                                showErrorDialog = true
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("${personType.capitalize()} added successfully")
                                    navController.popBackStack()
                                }
                            }
                            .addOnFailureListener { e ->
                                // If Firestore fails, delete the auth user
                                authResult.user?.delete()
                                isLoading = false
                                isErrorDialog = true
                                errorMessage = "Error saving user data: ${e.message}"
                                showErrorDialog = true
                            }
                    }
                    .addOnFailureListener { e ->
                        // If admin re-login fails, delete the created user
                        authResult.user?.delete()
                        isLoading = false
                        isErrorDialog = true
                        errorMessage = "Error re-authenticating admin: ${e.message}"
                        showErrorDialog = true
                    }
            }
            .addOnFailureListener { e ->
                isLoading = false
                isErrorDialog = true
                errorMessage = "Error creating user authentication: ${e.message}"
                showErrorDialog = true
            }
    }

    fun validateAndSavePerson(
        person: Person,
        personType: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (personType == "student") {
            // First update the student
            FirestoreDatabase.updateStudent(
                person,
                onSuccess = {
                    // Handle parent information update
                    if (parentEmail.isNotBlank()) {
                        val auth = FirebaseAuth.getInstance()
                        val db = FirebaseFirestore.getInstance()

                        // Function to create/update parent documents and establish relationships
                        fun setupParentDocuments(parentUserId: String, isNewParent: Boolean) {
                            val batch = db.batch()
                            
                            // 1. Update/Create parent user document
                            val parentUserDoc = db.collection("users").document(parentUserId)
                            val parentUserData = mapOf(
                                "role" to "parent",
                                "email" to parentEmail,
                                "name" to "$parentFirstName $parentLastName",
                                "updatedAt" to com.google.firebase.Timestamp.now(),
                                "userId" to parentUserId,
                                "child_uid" to person.id  // Link to student
                            )
                            
                            if (isNewParent) {
                                batch.set(parentUserDoc, parentUserData)
                            } else {
                                batch.set(parentUserDoc, parentUserData, com.google.firebase.firestore.SetOptions.merge())
                            }

                            // 2. Update/Create parent document in parents collection
                            val parentDoc = db.collection("parents").document(parentUserId)
                            val parentData = Person(
                                id = parentUserId,
                                firstName = parentFirstName.trim(),
                                lastName = parentLastName.trim(),
                                email = parentEmail.trim(),
                                phone = parentPhone.trim(),
                                address = parentAddress.trim(),
                                type = "parent"
                            )
                            if (isNewParent) {
                                batch.set(parentDoc, parentData)
                            } else {
                                batch.set(parentDoc, parentData, com.google.firebase.firestore.SetOptions.merge())
                            }

                            // 3. Update student document to include parent reference
                            val studentDoc = db.collection("students").document(person.id)
                            val studentUpdate = mapOf(
                                "parentId" to parentUserId,
                                "parentName" to "$parentFirstName $parentLastName",
                                "parentEmail" to parentEmail,
                                "parentPhone" to parentPhone
                            )
                            batch.update(studentDoc, studentUpdate)

                            // 4. Create/Update parent-student relationship document
                            val relationshipDoc = db.collection("parent_student_relationships").document(parentUserId)
                            val relationshipData = mapOf(
                                "parentId" to parentUserId,
                                "studentId" to person.id,
                                "studentName" to "${person.firstName} ${person.lastName}",
                                "studentClass" to person.className,
                                "updatedAt" to com.google.firebase.Timestamp.now()
                            )
                            batch.set(relationshipDoc, relationshipData)

                            // Commit all changes
                            batch.commit()
                                .addOnSuccessListener {
                                    onSuccess()
                                }
                                .addOnFailureListener { e ->
                                    onError(Exception("Failed to setup parent-student relationship: ${e.message}"))
                                }
                        }

                        if (parentId.isNotBlank()) {
                            // Update existing parent
                            setupParentDocuments(parentId, false)
                        } else if (parentPassword.isNotBlank()) {
                            // Create new parent account and setup documents
                            auth.createUserWithEmailAndPassword(parentEmail, parentPassword)
                                .addOnSuccessListener { authResult ->
                                    val newParentId = authResult.user?.uid
                                    if (newParentId != null) {
                                        // Set initial display name for the parent
                                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                            .setDisplayName("$parentFirstName $parentLastName")
                                            .build()
                                        
                                        authResult.user?.updateProfile(profileUpdates)
                                            ?.addOnSuccessListener {
                                                setupParentDocuments(newParentId, true)
                                            }
                                            ?.addOnFailureListener { e ->
                                                onError(Exception("Failed to update parent profile: ${e.message}"))
                                            }
                                    } else {
                                        onError(Exception("Failed to create parent account: Invalid user ID"))
                                    }
                                }
                                .addOnFailureListener { e ->
                                    onError(Exception("Failed to create parent authentication: ${e.message}"))
                                }
                        } else {
                            onError(Exception("Parent password is required for new parent account"))
                        }
                    } else {
                        onSuccess()
                    }
                },
                onFailure = onError
            )
        } else {
            FirestoreDatabase.updateTeacher(
                person,
                onSuccess = onSuccess,
                onFailure = onError
            )
        }
    }

    LaunchedEffect(personId) {
        if (personId != null) {
            isLoading = true
            try {
                val person = if (personType == "student") {
                    FirestoreDatabase.getStudent(personId)
                } else {
                    FirestoreDatabase.getTeacher(personId)
                }
                if (person != null) {
                    firstName = person.firstName
                    lastName = person.lastName
                    email = person.email
                    phone = person.phone
                    personTypeValue = person.type
                    selectedClass = person.className
                    rollNumber = person.rollNumber
                    gender = person.gender
                    dateOfBirth = person.dateOfBirth
                    mobileNo = person.mobileNo
                    address = person.address
                    age = if (person.age > 0) person.age.toString() else ""

                    // If this is a student, fetch parent information
                    if (personType == "student") {
                        try {
                            FirestoreDatabase.getParentByChildId(
                                childId = personId,
                                onSuccess = { parent ->
                                    if (parent != null) {
                                        parentId = parent.id
                                        parentFirstName = parent.firstName
                                        parentLastName = parent.lastName
                                        parentEmail = parent.email
                                        parentPhone = parent.phone
                                        parentAddress = parent.address
                                    }
                                },
                                onFailure = { e ->
                                    errorMessage = "Failed to load parent data: ${e.message}"
                                    showErrorDialog = true
                                }
                            )
                        } catch (e: Exception) {
                            errorMessage = "Failed to load parent data: ${e.message}"
                            showErrorDialog = true
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load data: ${e.message}"
                showErrorDialog = true
            }
            isLoading = false
        }
    }

    @Composable
    fun ParentInformationSection(
        parentFirstName: String,
        onParentFirstNameChange: (String) -> Unit,
        parentLastName: String,
        onParentLastNameChange: (String) -> Unit,
        parentEmail: String,
        onParentEmailChange: (String) -> Unit,
        parentPassword: String,
        onParentPasswordChange: (String) -> Unit,
        parentPhone: String,
        onParentPhoneChange: (String) -> Unit,
        parentAddress: String,
        onParentAddressChange: (String) -> Unit,
        showParentPassword: Boolean,
        onShowParentPasswordChange: (Boolean) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Parent Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudentGreen
                )

                OutlinedTextField(
                    value = parentFirstName,
                    onValueChange = onParentFirstNameChange,
                    label = { Text("Parent First Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = parentLastName,
                    onValueChange = onParentLastNameChange,
                    label = { Text("Parent Last Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = parentEmail,
                    onValueChange = onParentEmailChange,
                    label = { Text("Parent Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = parentPassword,
                    onValueChange = onParentPasswordChange,
                    label = { Text("Parent Password") },
                    singleLine = true,
                    visualTransformation = if (showParentPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    trailingIcon = {
                        IconButton(onClick = { onShowParentPasswordChange(!showParentPassword) }) {
                            Icon(
                                if (showParentPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showParentPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = parentPhone,
                    onValueChange = onParentPhoneChange,
                    label = { Text("Parent Phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = parentAddress,
                    onValueChange = onParentAddressChange,
                    label = { Text("Parent Address") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isEditMode) "Edit ${personType.capitalize()}" else "Add ${personType.capitalize()}",
                        color = when (personType) {
                            "student" -> StudentGreen
                            "teacher" -> TeacherBlue
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = when (personType) {
                                "student" -> StudentGreen
                                "teacher" -> TeacherBlue
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = when (personType) {
                        "student" -> StudentGreen
                        "teacher" -> TeacherBlue
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Photo Component
                    item {
                        ProfilePhotoComponent(
                            userId = personId ?: "",
                            photoUrl = profilePhotoUrl,
                            isEditable = true,
                            themeColor = when (personType) {
                                "student" -> StudentGreen
                                "teacher" -> TeacherBlue
                                else -> MaterialTheme.colorScheme.primary
                            },
                            onPhotoUpdated = { url ->
                                profilePhotoUrl = url
                                // Update the user document with the new photo URL
                                if (personId != null) {
                                    val collection = when (personType) {
                                        "student" -> "students"
                                        "teacher" -> "teachers"
                                        else -> "non_teaching_staff"
                                    }
                                    FirebaseFirestore.getInstance()
                                        .collection(collection)
                                        .document(personId)
                                        .update("profilePhoto", url)
                                        .addOnSuccessListener {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Profile photo updated successfully")
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Failed to update profile photo: ${e.message}")
                                            }
                                        }
                                }
                            },
                            onError = { error ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            }
                        )
                    }

                    // First Name and Last Name
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = { Text("First Name") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = { Text("Last Name") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Email
                    item {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email ID") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }

                    // Password (only show for new users)
                    if (!isEditMode) {
                        item {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showPassword) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                supportingText = { Text("Minimum 6 characters required") }
                            )
                        }
                    }

                    // Gender
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text("Gender", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { gender = "Male" }
                                ) {
                                    RadioButton(
                                        selected = gender == "Male",
                                        onClick = { gender = "Male" },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = when (personType) {
                                                "student" -> StudentGreen
                                                "teacher" -> TeacherBlue
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    )
                                    Text("Male")
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { gender = "Female" }
                                ) {
                                    RadioButton(
                                        selected = gender == "Female",
                                        onClick = { gender = "Female" },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = when (personType) {
                                                "student" -> StudentGreen
                                                "teacher" -> TeacherBlue
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    )
                                    Text("Female")
                                }
                            }
                        }
                    }

                    // Date of Birth
                    item {
                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = { input ->
                                dateOfBirth = input
                                // Try to auto-calc age when DOB changes
                                val parsed = runCatching {
                                    val fmt1 = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US)
                                    fmt1.isLenient = false
                                    fmt1.parse(input)
                                }.getOrNull() ?: runCatching {
                                    val fmt2 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                    fmt2.isLenient = false
                                    fmt2.parse(input)
                                }.getOrNull()
                                if (parsed != null) {
                                    val dobCal = java.util.Calendar.getInstance().apply { time = parsed }
                                    val now = java.util.Calendar.getInstance()
                                    var years = now.get(java.util.Calendar.YEAR) - dobCal.get(java.util.Calendar.YEAR)
                                    if (now.get(java.util.Calendar.DAY_OF_YEAR) < dobCal.get(java.util.Calendar.DAY_OF_YEAR)) {
                                        years -= 1
                                    }
                                    age = years.coerceAtLeast(0).toString()
                                }
                            },
                            label = { Text("Date of Birth (mm/dd/yyyy)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    val cal = java.util.Calendar.getInstance()
                                    // Prefill from existing value if present
                                    val prefill = runCatching {
                                        val f1 = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US)
                                        f1.isLenient = false
                                        f1.parse(dateOfBirth)
                                    }.getOrNull() ?: runCatching {
                                        val f2 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                        f2.isLenient = false
                                        f2.parse(dateOfBirth)
                                    }.getOrNull()
                                    if (prefill != null) cal.time = prefill

                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val picked = java.util.Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth)
                                            }
                                            val outFmt = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US)
                                            dateOfBirth = outFmt.format(picked.time)
                                            // Update age
                                            val now = java.util.Calendar.getInstance()
                                            var years = now.get(java.util.Calendar.YEAR) - year
                                            val temp = java.util.Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                            if (now.get(java.util.Calendar.DAY_OF_YEAR) < temp.get(java.util.Calendar.DAY_OF_YEAR)) {
                                                years -= 1
                                            }
                                            age = years.coerceAtLeast(0).toString()
                                        },
                                        cal.get(java.util.Calendar.YEAR),
                                        cal.get(java.util.Calendar.MONTH),
                                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                                    ).show()
                                }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                                }
                            }
                        )
                    }

                    // Mobile No
                    item {
                        OutlinedTextField(
                            value = mobileNo,
                            onValueChange = { mobileNo = it },
                            label = { Text("Mobile No") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }

                    // Address
                    item {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }

                    // Class selection
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showClassDialog = true }
                        ) {
                            OutlinedTextField(
                                value = selectedClass,
                                onValueChange = { },
                                label = { Text("Class") },
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, "Select Class")
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = LocalContentColor.current,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                enabled = false
                            )
                        }
                    }

                    // Type selection
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTypeDialog = true }
                        ) {
                            OutlinedTextField(
                                value = personTypeValue,
                                onValueChange = { },
                                label = { Text("Type") },
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, "Select Type")
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = LocalContentColor.current,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                enabled = false
                            )
                        }
                    }

                    // Roll Number (only for students)
                    if (personType == "student") {
                        item {
                            OutlinedTextField(
                                value = rollNumber,
                                onValueChange = { rollNumber = it },
                                label = { Text("Roll Number") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Age (auto-calculated)
                    item {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { },
                            label = { Text("Age") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            enabled = false
                        )
                    }

                    // Phone
                    item {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }

                    // Student admissions fields
                    if (personType == "student") {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                OutlinedTextField(
                                    value = admissionNumber,
                                    onValueChange = { admissionNumber = it },
                                    label = { Text("Admission Number (manual override)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // Last issued hint for legacy students (only when editing and value is a prefix)
                                val year = remember(admissionNumber) {
                                    com.ecorvi.schmng.ui.utils.AdmissionUtils.getAdmissionYear(admissionNumber)
                                        ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                }
                                var lastIssued by remember { mutableStateOf<String?>(null) }
                                LaunchedEffect(isEditMode, admissionNumber, year) {
                                    if (isEditMode && admissionNumber.endsWith("-")) {
                                        lastIssued = FirestoreDatabase.getLastIssuedAdmissionNumber(year)
                                    } else {
                                        lastIssued = null
                                    }
                                }
                                if (lastIssued != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Last issued: ${lastIssued}", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = admissionDate,
                                onValueChange = { admissionDate = it },
                                label = { Text("Date of Admission (yyyy-MM-dd)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val cal = java.util.Calendar.getInstance()
                                        val prefill = runCatching {
                                            val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                            f.isLenient = false
                                            f.parse(admissionDate)
                                        }.getOrNull()
                                        if (prefill != null) cal.time = prefill

                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val picked = java.util.Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                                val outFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                                admissionDate = outFmt.format(picked.time)
                                            },
                                            cal.get(java.util.Calendar.YEAR),
                                            cal.get(java.util.Calendar.MONTH),
                                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                                    }
                                }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = academicYear,
                                onValueChange = { academicYear = it },
                                label = { Text("Academic Year (e.g., 2025-26)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = aadharNumber,
                                onValueChange = { value ->
                                    val digits = value.filter { it.isDigit() }.take(12)
                                    aadharNumber = digits
                                },
                                label = { Text("Aadhar Number (12 digits)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                )
                            )
                        }
                        if (aadharNumber.length in 1..11) {
                            item {
                                Text(
                                    text = "Aadhar must be 12 digits",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                )
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = aaparId,
                                onValueChange = { aaparId = it.trim() },
                                label = { Text("AAPAR ID") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Add parent information section when creating a student OR editing a student
                    if (personType == "student") {
                        item {
                            ParentInformationSection(
                                parentFirstName = parentFirstName,
                                onParentFirstNameChange = { parentFirstName = it },
                                parentLastName = parentLastName,
                                onParentLastNameChange = { parentLastName = it },
                                parentEmail = parentEmail,
                                onParentEmailChange = { parentEmail = it },
                                parentPassword = parentPassword,
                                onParentPasswordChange = { parentPassword = it },
                                parentPhone = parentPhone,
                                onParentPhoneChange = { parentPhone = it },
                                parentAddress = parentAddress,
                                onParentAddressChange = { parentAddress = it },
                                showParentPassword = showParentPassword,
                                onShowParentPasswordChange = { showParentPassword = it }
                            )
                        }
                    }

                    // Save Button
                    item {
                        Button(
                            onClick = {
                                if (isEditMode) {
                                    validateAndSavePerson(
                                        Person(
                                            id = personId ?: "",
                                            firstName = firstName.trim(),
                                            lastName = lastName.trim(),
                                            email = email.trim(),
                                            phone = phone.trim(),
                                            type = personTypeValue,
                                            className = formatClassForStorage(selectedClass),
                                            rollNumber = rollNumber.trim(),
                                            gender = gender,
                                            dateOfBirth = dateOfBirth.trim(),
                                            mobileNo = mobileNo.trim(),
                                            address = address.trim(),
                                            age = age.toIntOrNull() ?: 0,
                                            admissionNumber = admissionNumber,
                                            admissionDate = admissionDate,
                                            academicYear = academicYear,
                                            aadharNumber = aadharNumber,
                                            aaparId = aaparId
                                        ),
                                        personType,
                                        {
                                            isLoading = false
                                            isErrorDialog = false
                                            errorMessage = "${personType.capitalize()} updated successfully"
                                            showErrorDialog = true
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("${personType.capitalize()} updated successfully")
                                                navController.popBackStack()
                                            }
                                        },
                                        { e ->
                                            isLoading = false
                                            isErrorDialog = true
                                            errorMessage = "Error updating ${personType.capitalize()}: ${e.message}"
                                            showErrorDialog = true
                                        }
                                    )
                                } else {
                                    if (personType == "student") {
                                        savePerson()
                                    } else {
                                        // For teacher or staff, use savePersonDirectly
                                        val auth = FirebaseAuth.getInstance()
                                        val db = FirebaseFirestore.getInstance()
                                        val person = Person(
                                            id = "",
                                            firstName = firstName.trim(),
                                            lastName = lastName.trim(),
                                            email = email.trim(),
                                            password = password,
                                            phone = phone.trim(),
                                            type = personType, // enforce type
                                            className = formatClassForStorage(selectedClass),
                                            rollNumber = rollNumber.trim(),
                                            gender = gender,
                                            dateOfBirth = dateOfBirth.trim(),
                                            mobileNo = mobileNo.trim(),
                                            address = address.trim(),
                                            age = age.toIntOrNull() ?: 0,
                                            admissionNumber = if (personType == "student") admissionNumber else "",
                                            admissionDate = if (personType == "student") admissionDate else "",
                                            academicYear = if (personType == "student") academicYear else "",
                                            aadharNumber = if (personType == "student") aadharNumber else "",
                                            aaparId = if (personType == "student") aaparId else ""
                                        )
                                        savePersonDirectly(
                                            person = person,
                                            auth = auth,
                                            db = db,
                                            email = email.trim(),
                                            password = password,
                                            personType = personType,
                                            onSuccess = {
                                                isLoading = false
                                                isErrorDialog = false
                                                errorMessage = "${personType.capitalize()} account created successfully"
                                                showErrorDialog = true
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("${personType.capitalize()} account created successfully")
                                                    navController.popBackStack()
                                                }
                                            },
                                            onError = { e ->
                                                isLoading = false
                                                isErrorDialog = true
                                                errorMessage = "Error creating ${personType.capitalize()} account: ${e.message}"
                                                showErrorDialog = true
                                            }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (personType) {
                                    "student" -> StudentGreen
                                    "teacher" -> TeacherBlue
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        ) {
                            Text(
                                if (isEditMode) "Update ${personType.capitalize()}"
                                else "Add ${personType.capitalize()}",
                                color = Color.White
                            )
                        }
                    }
                }

                // Class Selection Dialog
                if (showClassDialog) {
                    AlertDialog(
                        onDismissRequest = { showClassDialog = false },
                        title = { 
                            Text(
                                "Select Class",
                                color = when (personType) {
                                    "student" -> StudentGreen
                                    "teacher" -> TeacherBlue
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            ) 
                        },
                        text = {
                            LazyColumn {
                                items(Constants.CLASS_OPTIONS) { classOption ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedClass = classOption
                                                showClassDialog = false
                                            }
                                            .padding(vertical = 12.dp, horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedClass == classOption,
                                            onClick = {
                                                selectedClass = classOption
                                                showClassDialog = false
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = when (personType) {
                                                    "student" -> StudentGreen
                                                    "teacher" -> TeacherBlue
                                                    else -> MaterialTheme.colorScheme.primary
                                                }
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(classOption)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { showClassDialog = false },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = when (personType) {
                                        "student" -> StudentGreen
                                        "teacher" -> TeacherBlue
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            ) {
                                Text("Close")
                            }
                        }
                    )
                }

                // Type Selection Dialog
                if (showTypeDialog) {
                    AlertDialog(
                        onDismissRequest = { showTypeDialog = false },
                        title = { 
                            Text(
                                "Select Type",
                                color = when (personType) {
                                    "student" -> StudentGreen
                                    "teacher" -> TeacherBlue
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            ) 
                        },
                        text = {
                            LazyColumn {
                                items(typeOptions) { option ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                personTypeValue = option
                                                showTypeDialog = false
                                            }
                                            .padding(vertical = 12.dp, horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = personTypeValue == option,
                                            onClick = {
                                                personTypeValue = option
                                                showTypeDialog = false
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = when (personType) {
                                                    "student" -> StudentGreen
                                                    "teacher" -> TeacherBlue
                                                    else -> MaterialTheme.colorScheme.primary
                                                }
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(option)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { showTypeDialog = false },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = when (personType) {
                                        "student" -> StudentGreen
                                        "teacher" -> TeacherBlue
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            ) {
                                Text("Close")
                            }
                        }
                    )
                }
            }
        }

        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = {
                    Text(
                        if (isErrorDialog) "Error" else "Success",
                        color = if (isErrorDialog) Color.Red else Color(0xFF4CAF50)
                    )
                },
                text = { Text(errorMessage ?: if (isErrorDialog) "An unknown error occurred" else "Operation successful") },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun PreviewAddPersonScreen() {
    MaterialTheme {
    val navController = rememberNavController()
    AddPersonScreen(navController, "student")
    }
}
