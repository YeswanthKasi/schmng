package com.ecorvi.schmng.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStaffScreen(navController: NavController, personId: String? = null) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var mobileNo by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Effect to load existing staff data if editing
    LaunchedEffect(personId) {
        if (personId != null) {
            isLoading = true
            FirestoreDatabase.staffCollection.document(personId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val staff = document.toObject(Person::class.java)
                        staff?.let {
                            firstName = it.firstName
                            lastName = it.lastName
                            email = it.email
                            phone = it.phone
                            mobileNo = it.mobileNo
                            designation = it.designation
                            department = it.department
                            age = it.age.toString()
                            gender = it.gender
                            address = it.address
                            className = it.className
                            rollNumber = it.rollNumber
                            dateOfBirth = it.dateOfBirth
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "Error loading staff data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    isLoading = false
                }
        }
    }

    // List of departments
    val departments = listOf(
        "Administration",
        "Maintenance",
        "Library",
        "IT Support",
        "Accounts",
        "Security",
        "Others"
    )

    // Department dropdown state
    var expandedDepartment by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (personId == null) "Add Staff Member" else "Edit Staff Member") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Personal Information Section
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Personal Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "Hide password" else "Show password"
                                    )
                                }
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = mobileNo,
                            onValueChange = { mobileNo = it },
                            label = { Text("Mobile Number") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Age") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )

                        // Gender selection
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Gender",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = gender == "Male",
                                    onClick = { gender = "Male" }
                                )
                                Text(
                                    text = "Male",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                Spacer(modifier = Modifier.width(32.dp))
                                RadioButton(
                                    selected = gender == "Female",
                                    onClick = { gender = "Female" }
                                )
                                Text(
                                    text = "Female",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }

                // Professional Information Section
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Professional Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = designation,
                            onValueChange = { designation = it },
                            label = { Text("Designation") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Department Dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedDepartment,
                            onExpandedChange = { expandedDepartment = it }
                        ) {
                            OutlinedTextField(
                                value = department,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Department") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDepartment)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expandedDepartment,
                                onDismissRequest = { expandedDepartment = false }
                            ) {
                                departments.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept) },
                                        onClick = {
                                            department = dept
                                            expandedDepartment = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (validateInputs()) {
                            isLoading = true
                            val staffData = Person(
                                id = personId ?: UUID.randomUUID().toString(),
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                phone = phone,
                                mobileNo = mobileNo,
                                type = "staff",
                                className = className,
                                rollNumber = rollNumber,
                                gender = gender,
                                dateOfBirth = dateOfBirth,
                                address = address,
                                age = age.toIntOrNull() ?: 0,
                                designation = designation,
                                department = department,
                                password = password
                            )

                            if (personId == null) {
                                // Create Firebase Auth user first
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnSuccessListener { authResult ->
                                        val userId = authResult.user?.uid ?: return@addOnSuccessListener
                                        
                                        // Start a batch write
                                        val batch = db.batch()
                                        
                                        // Add to staff collection
                                        val staffRef = db.collection("non_teaching_staff").document(userId)
                                        val staffDataWithId = staffData.copy(id = userId)
                                        batch.set(staffRef, staffDataWithId)
                                        
                                        // Add to users collection
                                        val userRef = db.collection("users").document(userId)
                                        val userData = mapOf(
                                            "role" to "staff",
                                            "email" to email,
                                            "name" to "$firstName $lastName",
                                            "createdAt" to com.google.firebase.Timestamp.now(),
                                            "userId" to userId,
                                            "type" to "staff",
                                            "department" to department
                                        )
                                        batch.set(userRef, userData)
                                        
                                        // Commit the batch
                                        batch.commit()
                                            .addOnSuccessListener {
                                                isLoading = false
                                                Toast.makeText(
                                                    context,
                                                    "Staff member added successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                navController.navigateUp()
                                            }
                                            .addOnFailureListener { e ->
                                                // If Firestore fails, delete the auth user
                                                authResult.user?.delete()
                                                isLoading = false
                                                Toast.makeText(
                                                    context,
                                                    "Error adding staff member: ${e.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(
                                            context,
                                            "Error creating user: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            } else {
                                // Updating existing staff member
                                db.collection("non_teaching_staff").document(personId).set(staffData)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        Toast.makeText(
                                            context,
                                            "Staff member updated successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navController.navigateUp()
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(
                                            context,
                                            "Error updating staff member: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Please fill all required fields",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (personId == null) "Add Staff Member" else "Update Staff Member")
                    }
                }
            }
        }
    }
}

private fun validateInputs(): Boolean {
    // Add your validation logic here
    return true
} 