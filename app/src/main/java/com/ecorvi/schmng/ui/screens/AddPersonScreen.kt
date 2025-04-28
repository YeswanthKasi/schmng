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
import androidx.compose.ui.platform.testTag
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
import android.app.DatePickerDialog
import java.util.*


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
    var showDatePicker by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isEditMode = personId != null
    val coroutineScope = rememberCoroutineScope()

    // Define person type options
    val typeOptions = if (personType == "student") {
        listOf("Regular", "Scholarship", "Transfer")
    } else {
        listOf("Regular", "Temporary", "Visiting")
    }

    // Helper to show error messages and force recomposition
    fun showMessage(message: String) {
        errorMessage = message
        // Optionally clear after a delay for UX, but not needed for test
        // LaunchedEffect(errorMessage) {
        //     delay(3000)
        //     errorMessage = null
        // }
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
        if (!isEditMode && password.length < 6) {
            showMessage("Password must be at least 6 characters")
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
        if (validateInputs()) {
            isLoading = true
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val userId = authTask.result?.user?.uid ?: return@addOnCompleteListener

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

                        val userData = mapOf(
                            "role" to personType,
                            "email" to email,
                            "name" to "$firstName $lastName",
                            "createdAt" to com.google.firebase.Timestamp.now(),
                            "userId" to userId,
                            "type" to personTypeValue,
                            "className" to selectedClass
                        )

                        FirestoreDatabase.createUserWithRole(
                            userId = userId,
                            userData = userData,
                            person = person,
                            onSuccess = {
                                isLoading = false
                                showMessage("${personType.capitalize()} added successfully")
                                navController.popBackStack()
                            },
                            onFailure = { e ->
                                isLoading = false
                                showMessage("Error saving ${personType}: ${e.message}")
                            }
                        )
                    } else {
                        isLoading = false
                        showMessage("Error creating user: ${authTask.exception?.message}")
                    }
                }
        }
    }

    // Add this function for development/testing only. Do NOT use in production.
    fun savePersonWithAdminCredentials(adminEmail: String, adminPassword: String) {
        if (validateInputs()) {
            isLoading = true
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        // Immediately sign back in as admin
                        auth.signInWithEmailAndPassword(adminEmail, adminPassword)
                            .addOnCompleteListener { adminSignInTask ->
                                if (adminSignInTask.isSuccessful) {
                                    val userId = authTask.result?.user?.uid ?: return@addOnCompleteListener

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

                                    val userData = mapOf(
                                        "role" to personType,
                                        "email" to email,
                                        "name" to "$firstName $lastName",
                                        "createdAt" to com.google.firebase.Timestamp.now(),
                                        "userId" to userId,
                                        "type" to personTypeValue,
                                        "className" to selectedClass
                                    )

                                    FirestoreDatabase.createUserWithRole(
                                        userId = userId,
                                        userData = userData,
                                        person = person,
                                        onSuccess = {
                                            isLoading = false
                                            showMessage("${personType.capitalize()} added successfully")
                                            navController.popBackStack()
                                        },
                                        onFailure = { e ->
                                            isLoading = false
                                            showMessage("Error saving ${personType}: ${e.message}")
                                        }
                                    )
                                } else {
                                    isLoading = false
                                    showMessage("Error re-authenticating as admin: ${adminSignInTask.exception?.message}")
                                }
                            }
                    } else {
                        isLoading = false
                        showMessage("Error creating user: ${authTask.exception?.message}")
                    }
                }
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
                        navController.navigateUp()
                    },
                    onFailure = { e ->
                        isLoading = false
                        showMessage("Error updating student: ${e.message}")
                    }
                )
            } else {
                FirestoreDatabase.updateTeacher(
                    person,
                    onSuccess = {
                        isLoading = false
                        showMessage("Teacher updated successfully")
                        navController.navigateUp()
                    },
                    onFailure = { e ->
                        isLoading = false
                        showMessage("Error updating teacher: ${e.message}")
                    }
                )
            }
        } else {
            savePerson()
        }
    }

    fun reauthenticateAndSave(adminEmail: String, adminPassword: String) {
        isAuthenticating = true
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(adminEmail, adminPassword)
            .addOnCompleteListener { authTask ->
                isAuthenticating = false
                if (authTask.isSuccessful) {
                    savePerson()
                } else {
                    val errorMsg = authTask.exception?.localizedMessage ?: "Incorrect password. Please try again."
                    showMessage(errorMsg)
                }
            }
    }

    LaunchedEffect(personId) {
        if (personId != null) {
            isLoading = true
            coroutineScope.launch {
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
                isLoading = false
            }
        }
    }

    // DatePickerDialog logic
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dateOfBirth = "${month + 1}/$dayOfMonth/$year"
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.setOnCancelListener { showDatePicker = false }
        datePickerDialog.show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit ${personType.capitalize()}" else "Add ${personType.capitalize()}") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        content = { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading || isAuthenticating) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
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
                                    .background(Color(0xFF1F41BB), shape = CircleShape),
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
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = gender == "Male",
                                            onClick = { gender = "Male" }
                                        )
                                        Text("Male")
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = gender == "Female",
                                            onClick = { gender = "Female" }
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
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = "Select Date",
                                        modifier = Modifier.clickable { showDatePicker = true }
                                    )
                                },
                                readOnly = true
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
                                        showPasswordDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F41BB))
                            ) {
                                Text(
                                    if (isEditMode) "Update ${personType.capitalize()}"
                                    else "Add ${personType.capitalize()}",
                                    color = Color.White
                                )
                            }
                        }

                        // Error Message
                        if (errorMessage != null) {
                            item {
                                Text(
                                    text = errorMessage!!,
                                    color = Color.Red,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .testTag("error_message")
                                )
                            }
                        }
                    }

                    // Class Selection Dialog
                    if (showClassDialog) {
                        AlertDialog(
                            onDismissRequest = { showClassDialog = false },
                            title = { Text("Select Class") },
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
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(classOption)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showClassDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }

                    // Type Selection Dialog
                    if (showTypeDialog) {
                        AlertDialog(
                            onDismissRequest = { showTypeDialog = false },
                            title = { Text("Select Type") },
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
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(option)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showTypeDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }

                    // Password Confirmation Dialog
                    if (showPasswordDialog) {
                        AlertDialog(
                            onDismissRequest = { showPasswordDialog = false },
                            title = { Text("Admin Password Required") },
                            text = {
                                OutlinedTextField(
                                    value = adminPasswordInput,
                                    onValueChange = { adminPasswordInput = it },
                                    label = { Text("Enter Admin Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val adminEmail = FirebaseAuth.getInstance().currentUser?.email
                                    if (adminEmail.isNullOrBlank()) {
                                        showMessage("Admin email not found. Please log in again.")
                                        showPasswordDialog = false
                                    } else if (adminPasswordInput.isBlank()) {
                                        showMessage("Please enter your password.")
                                    } else {
                                        showPasswordDialog = false
                                        reauthenticateAndSave(adminEmail, adminPasswordInput)
                                    }
                                }) { Text("Confirm") }
                            },
                            dismissButton = {
                                Button(onClick = { showPasswordDialog = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }
        }
    )
}

@Preview
@Composable
fun PreviewAddPersonScreen() {
    val navController = rememberNavController()
    AddPersonScreen(navController, "student")
}
