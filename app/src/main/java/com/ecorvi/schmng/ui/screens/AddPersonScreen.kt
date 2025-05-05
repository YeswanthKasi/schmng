package com.ecorvi.schmng.ui.screens

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
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val StudentGreen = Color(0xFF4CAF50) // Green color for student theme
private val TeacherBlue = Color(0xFF1F41BB) // Blue color for teacher theme

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
    var showClassDialog by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(personId != null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    val adminEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    var adminPassword by remember { mutableStateOf("") }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isEditMode = personId != null
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Define person type options
    val typeOptions = if (personType == "student") {
        listOf("Regular", "Scholarship", "Transfer")
    } else {
        listOf("Regular", "Temporary", "Visiting")
    }

    fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun validateInputs(): Boolean {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || (!isEditMode && password.isBlank())) {
            showMessage("Please fill in all required fields")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("Please enter a valid email address")
            return false
        }
        if (phone.isNotBlank() && !android.util.Patterns.PHONE.matcher(phone).matches()) {
            showMessage("Please enter a valid phone number")
            return false
        }
        if (age.isNotBlank() && age.toIntOrNull() == null) {
            showMessage("Please enter a valid age")
            return false
        }
        return true
    }

    fun savePerson() {
        if (!validateInputs()) {
            return
        }
        
        isLoading = true
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        // Start a Firestore batch to ensure atomicity
        val batch = db.batch()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                // 1. Create user document first
                val userDocRef = db.collection("users").document(userId)
                val userData = mapOf(
                    "role" to personType,
                    "email" to email,
                    "name" to "$firstName $lastName",
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "userId" to userId,
                    "type" to personTypeValue,
                    "className" to selectedClass
                )
                batch.set(userDocRef, userData)

                // 2. Create person document
                val personDocRef = when (personType) {
                    "student" -> db.collection("students").document(userId)
                    "teacher" -> db.collection("teachers").document(userId)
                    else -> db.collection("non_teaching_staff").document(userId)
                }

                val person = Person(
                    id = userId,
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    email = email.trim(),
                    password = password,
                    phone = phone.trim(),
                    type = personTypeValue,
                    className = selectedClass,
                    rollNumber = rollNumber.trim(),
                    gender = gender,
                    dateOfBirth = dateOfBirth.trim(),
                    mobileNo = mobileNo.trim(),
                    address = address.trim(),
                    age = age.toIntOrNull() ?: 0
                )
                batch.set(personDocRef, person)

                // 3. Commit the batch
                batch.commit()
                    .addOnSuccessListener {
                        isLoading = false
                        showMessage("${personType.capitalize()} added successfully")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("${personType.capitalize()} added successfully")
                            navController.popBackStack()
                        }
                    }
                    .addOnFailureListener { e ->
                        // If Firestore fails, delete the auth user to maintain consistency
                        authResult.user?.delete()?.addOnCompleteListener {
                            isLoading = false
                            errorMessage = "Error saving ${personType}: ${e.message}"
                            showErrorDialog = true
                        }
                    }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = "Error creating user: ${e.message}"
                showErrorDialog = true
            }
    }

    fun validateAndSavePerson() {
        if (!validateInputs()) {
            return
        }

        val ageInt = age.toIntOrNull() ?: 0
        
        val person = Person(
            id = personId ?: "",
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = email.trim(),
            phone = phone.trim(),
            type = personTypeValue,
            className = selectedClass,
            rollNumber = rollNumber.trim(),
            gender = gender,
            dateOfBirth = dateOfBirth.trim(),
            mobileNo = mobileNo.trim(),
            address = address.trim(),
            age = ageInt
        )

        isLoading = true

        if (isEditMode) {
            if (personType == "student") {
                FirestoreDatabase.updateStudent(
                    person,
                    onSuccess = {
                        isLoading = false
                        showMessage("Student updated successfully")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Student updated successfully")
                            navController.popBackStack()
                        }
                    },
                    onFailure = { e ->
                        isLoading = false
                        errorMessage = "Error updating student: ${e.message}"
                        showErrorDialog = true
                    }
                )
            } else {
                FirestoreDatabase.updateTeacher(
                    person,
                    onSuccess = {
                        isLoading = false
                        showMessage("Teacher updated successfully")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Teacher updated successfully")
                            navController.popBackStack()
                        }
                    },
                    onFailure = { e ->
                        isLoading = false
                        errorMessage = "Error updating teacher: ${e.message}"
                        showErrorDialog = true
                    }
                )
            }
        } else {
            savePerson()
        }
    }

    fun savePersonWithAdmin(adminPassword: String) {
        if (!validateInputs()) return

        isLoading = true
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        // 1. Create the new user (student/teacher/staff)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                // 2. Immediately sign back in as admin
                auth.signInWithEmailAndPassword(adminEmail, adminPassword)
                    .addOnSuccessListener {
                        // 3. Now do the Firestore writes as admin
                        val userDocRef = db.collection("users").document(userId)
                        val userData = mapOf(
                            "role" to personType,
                            "email" to email,
                            "name" to "$firstName $lastName",
                            "createdAt" to com.google.firebase.Timestamp.now(),
                            "userId" to userId,
                            "type" to personTypeValue,
                            "className" to selectedClass
                        )
                        batch.set(userDocRef, userData)

                        val personDocRef = when (personType) {
                            "student" -> db.collection("students").document(userId)
                            "teacher" -> db.collection("teachers").document(userId)
                            else -> db.collection("non_teaching_staff").document(userId)
                        }
                        val person = Person(
                            id = userId,
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            email = email.trim(),
                            password = password,
                            phone = phone.trim(),
                            type = personTypeValue,
                            className = selectedClass,
                            rollNumber = rollNumber.trim(),
                            gender = gender,
                            dateOfBirth = dateOfBirth.trim(),
                            mobileNo = mobileNo.trim(),
                            address = address.trim(),
                            age = age.toIntOrNull() ?: 0
                        )
                        batch.set(personDocRef, person)

                        batch.commit()
                            .addOnSuccessListener {
                                isLoading = false
                                showMessage("${personType.capitalize()} added successfully")
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("${personType.capitalize()} added successfully")
                                    navController.popBackStack()
                                }
                            }
                            .addOnFailureListener { e ->
                                authResult.user?.delete()?.addOnCompleteListener {
                                    isLoading = false
                                    errorMessage = "Error saving ${personType}: ${e.message}"
                                    showErrorDialog = true
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        // Could not sign back in as admin
                        isLoading = false
                        errorMessage = "Failed to sign back in as admin: ${e.message}"
                        showErrorDialog = true
                    }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = "Error creating user: ${e.message}"
                showErrorDialog = true
            }
    }

    fun savePersonDirectly() {
        if (!validateInputs()) return

        isLoading = true
        val db = FirebaseFirestore.getInstance()
        val person = Person(
            id = db.collection(personType + "s").document().id, // generate a new ID
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = email.trim(),
            password = password, // optional, not used for login
            phone = phone.trim(),
            type = personTypeValue,
            className = selectedClass,
            rollNumber = rollNumber.trim(),
            gender = gender,
            dateOfBirth = dateOfBirth.trim(),
            mobileNo = mobileNo.trim(),
            address = address.trim(),
            age = age.toIntOrNull() ?: 0
        )
        val collection = when (personType) {
            "student" -> db.collection("students")
            "teacher" -> db.collection("teachers")
            else -> db.collection("non_teaching_staff")
        }
        collection.document(person.id)
            .set(person)
            .addOnSuccessListener {
                isLoading = false
                showMessage("${personType.capitalize()} added successfully")
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("${personType.capitalize()} added successfully")
                    navController.popBackStack()
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = "Error saving ${personType}: ${e.message}"
                showErrorDialog = true
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
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load data: ${e.message}"
                showErrorDialog = true
            }
            isLoading = false
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
                    // Profile Picture Placeholder
                    item {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(
                                    when (personType) {
                                        "student" -> StudentGreen
                                        "teacher" -> TeacherBlue
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Picture",
                                tint = Color.White,
                                modifier = Modifier.size(60.dp)
                            )
                        }
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
                            onValueChange = { dateOfBirth = it },
                            label = { Text("Date of Birth (mm/dd/yyyy)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            trailingIcon = {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
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

                    // Age
                    item {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { newValue ->
                                // Only allow numeric input
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    age = newValue
                                }
                            },
                            label = { Text("Age") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            )
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

                    // Save Button
                    item {
                        Button(
                            onClick = {
                                if (isEditMode) {
                                    validateAndSavePerson()
                                } else {
                                    savePersonDirectly()
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

                // Show dialog to get admin password only
                if (showAdminLoginDialog) {
                    AlertDialog(
                        onDismissRequest = { showAdminLoginDialog = false },
                        title = { Text("Admin Authentication Required") },
                        text = {
                            Column {
                                Text("Please enter your admin password to confirm this action.")
                                OutlinedTextField(
                                    value = adminPassword,
                                    onValueChange = { adminPassword = it },
                                    label = { Text("Admin Password") },
                                    visualTransformation = PasswordVisualTransformation()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showAdminLoginDialog = false
                                savePersonWithAdmin(adminPassword)
                            }) { Text("Continue") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAdminLoginDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }

        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Error") },
                text = { Text(errorMessage ?: "An unknown error occurred") },
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
    val navController = rememberNavController()
    AddPersonScreen(navController, "student")
}
