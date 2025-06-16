package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.ProfilePhotoComponent
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import kotlinx.coroutines.launch

private val StudentGreen = Color(0xFF4CAF50) // Green color for student theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    navController: NavController,
    studentId: String,
    isAdmin: Boolean = false,
    currentRoute: String? = null,
    onRouteSelected: ((String) -> Unit)? = null
) {
    var student by remember { mutableStateOf<Person?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isDeleting by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(studentId) {
        isLoading = true
        student = FirestoreDatabase.getStudent(studentId)
        profilePhotoUrl = FirestoreDatabase.getProfilePhotoUrl(studentId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Student Profile",
                        color = StudentGreen,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (currentRoute == null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = StudentGreen
                            )
                        }
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(
                            onClick = { navController.navigate("add_person/student?personId=$studentId") }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = StudentGreen
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (currentRoute != null && onRouteSelected != null) {
                StudentBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { item ->
                        onRouteSelected(item.route)
                    }
                )
            }
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
                    color = StudentGreen
                )
            } else if (student == null) {
                Text(
                    "Failed to load student profile",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Photo
                    ProfilePhotoComponent(
                        userId = studentId,
                        photoUrl = profilePhotoUrl,
                        isEditable = isAdmin,
                        themeColor = StudentGreen,
                        onPhotoUpdated = { url ->
                            profilePhotoUrl = url
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProfileField("First Name", student?.firstName ?: "")
                            ProfileField("Last Name", student?.lastName ?: "")
                            ProfileField("Email", student?.email ?: "")
                            ProfileField("Class", student?.className ?: "")
                            ProfileField("Gender", student?.gender ?: "")
                            ProfileField("Date of Birth", student?.dateOfBirth ?: "")
                            ProfileField("Mobile No", student?.mobileNo ?: "")
                            ProfileField("Address", student?.address ?: "")
                            ProfileField("Age", student?.age?.toString() ?: "")
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (isAdmin) {
                        Button(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Student")
                        }
                    }
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Student") },
                    text = { Text("Are you sure you want to delete this student? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                isDeleting = true
                                FirestoreDatabase.deleteStudent(
                                    studentId = studentId,
                                    onSuccess = {
                                        isDeleting = false
                                        showDeleteDialog = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Student deleted successfully")
                                        }
                                        navController.popBackStack()
                                    },
                                    onFailure = { e ->
                                        isDeleting = false
                                        showDeleteDialog = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Failed to delete student: ${e.message}")
                                        }
                                    }
                                )
                            },
                            enabled = !isDeleting
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text("Delete")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
} 