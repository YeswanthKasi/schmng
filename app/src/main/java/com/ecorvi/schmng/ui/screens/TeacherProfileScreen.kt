package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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
fun TeacherProfileScreen(
    navController: NavController,
    teacherId: String,
    isAdmin: Boolean = false
) {
    var teacher by remember { mutableStateOf<Person?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isDeleting by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(teacherId) {
        isLoading = true
        teacher = FirestoreDatabase.getTeacher(teacherId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Teacher Profile",
                        color = Color(0xFF1F41BB),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1F41BB)
                        )
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(
                            onClick = { navController.navigate("add_person/teacher?personId=$teacherId") }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color(0xFF1F41BB)
                            )
                        }
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
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                teacher?.let { teacher ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Personal Information Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                ProfileRow("First Name", teacher.firstName)
                                ProfileRow("Last Name", teacher.lastName)
                                ProfileRow("Email", teacher.email)
                                ProfileRow("Phone", teacher.phone)
                                ProfileRow("Class", teacher.className)
                                ProfileRow("Gender", teacher.gender)
                                ProfileRow("Date of Birth", teacher.dateOfBirth)
                                ProfileRow("Mobile No", teacher.mobileNo)
                                ProfileRow("Address", teacher.address)
                                ProfileRow("Age", teacher.age.toString())
                                ProfileRow("Designation", teacher.designation)
                                ProfileRow("Department", teacher.department)
                            }
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                ),
                                enabled = !isDeleting
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete Teacher")
                            }
                        }
                    }
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Teacher not found")
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Confirm Delete") },
                text = { 
                    Text("Are you sure you want to delete ${teacher?.firstName} ${teacher?.lastName}? This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (!isDeleting) {
                                isDeleting = true
                                FirestoreDatabase.deleteTeacher(
                                    teacherId = teacherId,
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Teacher deleted successfully")
                                            navController.popBackStack()
                                        }
                                        isDeleting = false
                                        showDeleteDialog = false
                                    },
                                    onFailure = { exception ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Failed to delete teacher: ${exception.message}")
                                        }
                                        isDeleting = false
                                        showDeleteDialog = false
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isDeleting) "Deleting..." else "Delete")
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

@Composable
private fun ProfileRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
        }
    }
} 