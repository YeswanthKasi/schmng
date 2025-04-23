package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    personId: String,
    personType: String
) {
    var person by remember { mutableStateOf<Person?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Fetch person data
    LaunchedEffect(personId) {
        isLoading = true
        error = null
        try {
            val fetchedPerson = if (personType == "student") {
                FirestoreDatabase.getStudent(personId)
            } else {
                FirestoreDatabase.getTeacher(personId)
            }
            person = fetchedPerson
            isLoading = false
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (personType == "student") "Student Profile" else "Teacher Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Edit button
                    IconButton(
                        onClick = { 
                            navController.navigate("add_${personType.lowercase()}/${personId}") {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(Icons.Default.Edit, "Edit Profile")
                    }
                    // Delete button
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete")
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
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading profile",
                            color = Color.Red,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = error!!,
                            color = Color.Gray
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    error = null
                                    try {
                                        val fetchedPerson = if (personType == "student") {
                                            FirestoreDatabase.getStudent(personId)
                                        } else {
                                            FirestoreDatabase.getTeacher(personId)
                                        }
                                        person = fetchedPerson
                                        isLoading = false
                                    } catch (e: Exception) {
                                        error = e.message
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }
                person != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Basic Information Section
                        ProfileSection(
                            title = "Basic Information",
                            items = listOf(
                                "Full Name" to "${person!!.firstName} ${person!!.lastName}",
                                "Age" to "${person!!.age} years",
                                "Gender" to (person!!.gender.ifEmpty { "Not specified" }),
                                "Date of Birth" to (person!!.dateOfBirth.ifEmpty { "Not specified" })
                            )
                        )

                        // Role-specific Information Section
                        if (personType == "student") {
                            ProfileSection(
                                title = "Academic Information",
                                items = listOf(
                                    "Class" to (person!!.className.ifEmpty { "Not assigned" }),
                                    "Roll Number" to (person!!.rollNumber.ifEmpty { "Not assigned" }),
                                    "Student Type" to person!!.type
                                )
                            )
                        } else {
                            ProfileSection(
                                title = "Professional Information",
                                items = listOf(
                                    "Assigned Class" to (person!!.className.ifEmpty { "Not assigned" }),
                                    "Teacher Type" to person!!.type
                                )
                            )
                        }

                        // Contact Information Section
                        ProfileSection(
                            title = "Contact Information",
                            items = listOf(
                                "Email" to (person!!.email.ifEmpty { "Not provided" }),
                                "Mobile Number" to (person!!.mobileNo.ifEmpty { "Not provided" }),
                                "Phone" to (person!!.phone.ifEmpty { "Not provided" }),
                                "Address" to (person!!.address.ifEmpty { "Not provided" })
                            )
                        )
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Profile") },
                text = { Text("Are you sure you want to delete this profile? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (personType == "student") {
                                FirestoreDatabase.deleteStudent(
                                    personId,
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Profile deleted successfully")
                                            navController.navigateUp()
                                        }
                                    },
                                    onFailure = { e ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Failed to delete profile: ${e.message}")
                                        }
                                    }
                                )
                            } else {
                                FirestoreDatabase.deleteTeacher(
                                    personId,
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Profile deleted successfully")
                                            navController.navigateUp()
                                        }
                                    },
                                    onFailure = { e ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Failed to delete profile: ${e.message}")
                                        }
                                    }
                                )
                            }
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    items: List<Pair<String, String>>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F41BB)
            )
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = value,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
