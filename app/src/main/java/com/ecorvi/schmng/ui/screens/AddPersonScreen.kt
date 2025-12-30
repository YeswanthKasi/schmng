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
    
    // New fields for students
    var caste by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var subCaste by remember { mutableStateOf("") }
    var modeOfTransport by remember { mutableStateOf("") }
    var feeStructure by remember { mutableStateOf("") }
    var feePaid by remember { mutableStateOf("") }
    var showFeeConfirmationDialog by remember { mutableStateOf(false) }
    var pendingFeeStructure by remember { mutableStateOf<String?>(null) }
    var pendingFeePaid by remember { mutableStateOf<String?>(null) }

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

    // Transport Options
    val transportOptions = listOf("School Bus", "Own Transport", "Public Transport", "Walking")
    var showTransportDialog by remember { mutableStateOf(false) }

    // Category Options
    val categoryOptions = listOf("General", "OBC", "SC", "ST", "EWS", "Others")
    var showCategoryDialog by remember { mutableStateOf(false) }

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
                    caste = person.caste
                    category = person.category
                    subCaste = person.subCaste
                    modeOfTransport = person.modeOfTransport
                    feeStructure = if (person.feeStructure > 0) person.feeStructure.toString() else ""
                    feePaid = if (person.feePaid > 0) person.feePaid.toString() else ""

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
                            
                            // Calculate fee remaining
                            val feeStruct = feeStructure.toDoubleOrNull() ?: 0.0
                            val paid = feePaid.toDoubleOrNull() ?: 0.0
                            val remaining = (feeStruct - paid).coerceAtLeast(0.0)
                            
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
                                aaparId = aaparId,
                                caste = caste.trim(),
                                category = category.trim(),
                                subCaste = subCaste.trim(),
                                modeOfTransport = modeOfTransport,
                                feeStructure = feeStruct,
                                feePaid = paid,
                                feeRemaining = remaining
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
                    // Calculate fee remaining for students
                    val feeStruct = if (personType == "student") feeStructure.toDoubleOrNull() ?: 0.0 else 0.0
                    val paid = if (personType == "student") feePaid.toDoubleOrNull() ?: 0.0 else 0.0
                    val remaining = (feeStruct - paid).coerceAtLeast(0.0)
                    
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
                        age = age.toIntOrNull() ?: 0,
                        admissionNumber = if (personType == "student") admissionNumber else "",
                        admissionDate = if (personType == "student") admissionDate else "",
                        academicYear = if (personType == "student") academicYear else "",
                        aadharNumber = if (personType == "student") aadharNumber else "",
                        aaparId = if (personType == "student") aaparId else "",
                        caste = if (personType == "student") caste.trim() else "",
                        category = if (personType == "student") category.trim() else "",
                        subCaste = if (personType == "student") subCaste.trim() else "",
                        modeOfTransport = if (personType == "student") modeOfTransport else "",
                        feeStructure = feeStruct,
                        feePaid = paid,
                        feeRemaining = remaining
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
        // Calculate fee remaining for students
        val feeStruct = if (personType == "student") feeStructure.toDoubleOrNull() ?: 0.0 else 0.0
        val paid = if (personType == "student") feePaid.toDoubleOrNull() ?: 0.0 else 0.0
        val remaining = (feeStruct - paid).coerceAtLeast(0.0)
        
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
            aaparId = aaparId,
            caste = if (personType == "student") caste.trim() else "",
            category = if (personType == "student") category.trim() else "",
            subCaste = if (personType == "student") subCaste.trim() else "",
            modeOfTransport = if (personType == "student") modeOfTransport else "",
            feeStructure = feeStruct,
            feePaid = paid,
            feeRemaining = remaining
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


                    item {
                        // 1. Personal Information Section
                        Text(
                            text = "Personal Information",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (personType == "student") StudentGreen else TeacherBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Admission Number (Grouped with Name for Students)
                        if (personType == "student") {
                            OutlinedTextField(
                                value = admissionNumber,
                                onValueChange = { admissionNumber = it },
                                label = { Text("Admission Number") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                singleLine = true,
                                enabled = !isEditMode, // Usually auto-generated or fixed
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color.Black,
                                    disabledLabelColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (!isEditMode) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                singleLine = true,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // 2. Academic Details Section
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                        Text(
                            text = "Academic Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (personType == "student") StudentGreen else TeacherBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        OutlinedTextField(
                            value = selectedClass,
                            onValueChange = { },
                            label = { Text("Class") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { showClassDialog = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Select Class") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (personType == "student") {
                            OutlinedTextField(
                                value = rollNumber,
                                onValueChange = { rollNumber = it },
                                label = { Text("Roll Number") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = admissionDate,
                                onValueChange = { admissionDate = it },
                                label = { Text("Admission Date (YYYY-MM-DD)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        // Simple date picker logic could go here
                                        val cal = java.util.Calendar.getInstance()
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                                val c = java.util.Calendar.getInstance()
                                                c.set(year, month, dayOfMonth)
                                                admissionDate = fmt.format(c.time)
                                            },
                                            cal.get(java.util.Calendar.YEAR),
                                            cal.get(java.util.Calendar.MONTH),
                                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }) {
                                        Icon(Icons.Default.DateRange, "Select Date")
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // 3. Additional Details (Category, Transport, etc.)
                        if (personType == "student") {
                            Divider(modifier = Modifier.padding(vertical = 16.dp))
                            Text(
                                text = "Additional Details",
                                style = MaterialTheme.typography.titleMedium,
                                color = StudentGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Category Dropdown
                            Box {
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = { },
                                    label = { Text("Category") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showCategoryDialog = true },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Select Category") }
                                )
                                DropdownMenu(
                                    expanded = showCategoryDialog,
                                    onDismissRequest = { showCategoryDialog = false }
                                ) {
                                    categoryOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                category = option
                                                showCategoryDialog = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = caste,
                                onValueChange = { caste = it },
                                label = { Text("Caste") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = subCaste,
                                onValueChange = { subCaste = it },
                                label = { Text("Sub Caste") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Transport Dropdown
                            Box {
                                OutlinedTextField(
                                    value = modeOfTransport,
                                    onValueChange = { },
                                    label = { Text("Mode of Transport") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clickable { showTransportDialog = true },
                                    trailingIcon = {
                                        Icon(Icons.Default.ArrowDropDown, "Select Transport")
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = if (modeOfTransport.isEmpty()) Color.Gray else LocalContentColor.current,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    enabled = false
                                )
                            }
                            if (showTransportDialog) {
                                AlertDialog(
                                    onDismissRequest = { showTransportDialog = false },
                                    title = { 
                                        Text(
                                            "Select Mode of Transport",
                                            color = StudentGreen
                                        ) 
                                    },
                                    text = {
                                        LazyColumn {
                                            items(transportOptions) { option ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            modeOfTransport = option
                                                            showTransportDialog = false
                                                        }
                                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = modeOfTransport == option,
                                                        onClick = {
                                                            modeOfTransport = option
                                                            showTransportDialog = false
                                                        },
                                                        colors = RadioButtonDefaults.colors(
                                                            selectedColor = StudentGreen
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
                                            onClick = { showTransportDialog = false },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = StudentGreen
                                            )
                                        ) {
                                            Text("Close")
                                        }
                                    }
                                )
                            }
                            }
                        }
                        
                        // Fee Structure Section
                        item {
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                color = Color.Gray.copy(alpha = 0.3f)
                            )
                        }
                        item {
                            Text(
                                text = "Fee Structure",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StudentGreen,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = feeStructure,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() || it == '.' }.take(15)
                                    feeStructure = digits
                                },
                                label = { Text("Total Fee Structure (₹)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                leadingIcon = {
                                    Text("₹", color = StudentGreen, style = MaterialTheme.typography.bodyLarge)
                                },
                                supportingText = {
                                    Text("Enter the total fee amount set during admission")
                                }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = feePaid,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() || it == '.' }.take(15)
                                    feePaid = digits
                                },
                                label = { Text("Amount Paid (₹)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                leadingIcon = {
                                    Text("₹", color = StudentGreen, style = MaterialTheme.typography.bodyLarge)
                                },
                                supportingText = {
                                    val feeStruct = feeStructure.toDoubleOrNull() ?: 0.0
                                    val paid = feePaid.toDoubleOrNull() ?: 0.0
                                    val remaining = feeStruct - paid
                                    Text(
                                        if (feeStruct > 0) {
                                            if (remaining >= 0) {
                                                "Remaining: ₹${String.format("%.2f", remaining)}"
                                            } else {
                                                "Overpaid: ₹${String.format("%.2f", -remaining)}"
                                            }
                                        } else {
                                            "Enter fee structure first"
                                        },
                                        color = if (feeStruct > 0 && remaining >= 0) Color.Gray else Color.Red
                                    )
                                }
                            )
                        }
                        // Display fee summary
                        if (feeStructure.isNotBlank() && feeStructure.toDoubleOrNull() != null) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = StudentGreen.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        val feeStruct = feeStructure.toDoubleOrNull() ?: 0.0
                                        val paid = feePaid.toDoubleOrNull() ?: 0.0
                                        val remaining = feeStruct - paid
                                        Text(
                                            text = "Fee Summary",
                                            fontWeight = FontWeight.Bold,
                                            color = StudentGreen
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Total Fee: ₹${String.format("%.2f", feeStruct)}")
                                        Text("Paid: ₹${String.format("%.2f", paid)}")
                                        Text(
                                            "Remaining: ₹${String.format("%.2f", remaining.coerceAtLeast(0.0))}",
                                            color = if (remaining > 0) Color.Red else StudentGreen
                                        )
                                    }
                                 } }
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
                                    // Validate fee fields for students
                                    if (personType == "student") {
                                        val feeStruct = feeStructure.toDoubleOrNull() ?: 0.0
                                        val paid = feePaid.toDoubleOrNull() ?: 0.0
                                        if (feeStruct > 0 && paid > feeStruct) {
                                            errorMessage = "Amount paid cannot exceed total fee structure"
                                            showErrorDialog = true
                                            return@Button
                                        }
                                    }
                                    
                                    // Calculate fee remaining
                                    val feeStruct = feeStructure.toDoubleOrNull() ?: 0.0
                                    val paid = feePaid.toDoubleOrNull() ?: 0.0
                                    val remaining = (feeStruct - paid).coerceAtLeast(0.0)
                                    
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
                                            aaparId = aaparId,
                                            caste = caste.trim(),
                                            category = category.trim(),
                                            subCaste = subCaste.trim(),
                                            modeOfTransport = modeOfTransport,
                                            feeStructure = feeStruct,
                                            feePaid = paid,
                                            feeRemaining = remaining
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
                                        // Calculate fee remaining for students
                                        val feeStruct = if (personType == "student") feeStructure.toDoubleOrNull() ?: 0.0 else 0.0
                                        val paid = if (personType == "student") feePaid.toDoubleOrNull() ?: 0.0 else 0.0
                                        val remaining = (feeStruct - paid).coerceAtLeast(0.0)
                                        
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
                                            aaparId = if (personType == "student") aaparId else "",
                                            caste = if (personType == "student") caste.trim() else "",
                                            category = if (personType == "student") category.trim() else "",
                                            subCaste = if (personType == "student") subCaste.trim() else "",
                                            modeOfTransport = if (personType == "student") modeOfTransport else "",
                                            feeStructure = feeStruct,
                                            feePaid = paid,
                                            feeRemaining = remaining
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
