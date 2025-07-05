package com.ecorvi.schmng.ui.screens

import DeleteConfirmationDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ecorvi.schmng.ui.components.ProfilePhotoComponent
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import kotlinx.coroutines.launch

private val TeacherBlue = Color(0xFF1F41BB) // Blue color for teacher theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherProfileScreen(
    navController: NavController,
    teacherId: String,
    isAdmin: Boolean = false
) {
    var teacher by remember { mutableStateOf<Person?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load teacher data
    LaunchedEffect(teacherId) {
        try {
            teacher = FirestoreDatabase.getTeacher(teacherId)
            try {
                profilePhotoUrl = FirestoreDatabase.getProfilePhotoUrl(teacherId)
            } catch (e: Exception) {
                // Handle photo loading error silently
            }
            isLoading = false
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to load teacher profile: ${e.message}")
            }
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Teacher Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin) {
                        // Edit button
                        IconButton(
                            onClick = { 
                                navController.navigate("add_person/teacher?personId=$teacherId") 
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color.White
                            )
                        }
                        // Delete button
                        IconButton(
                            onClick = { showDeleteDialog = true }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TeacherBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TeacherBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo (no extra blue background, fully visible)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProfilePhotoComponent(
                        userId = teacherId,
                        photoUrl = profilePhotoUrl,
                        isEditable = isAdmin,
                        themeColor = Color.LightGray, // or Color.Transparent for no border
                        onPhotoUpdated = { url ->
                            profilePhotoUrl = url
                        }
                    )
                }
                
                // Profile Details
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Personal Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TeacherBlue
                        )
                        
                        teacher?.let { teacher ->
                            ProfileField("Name", "${teacher.firstName} ${teacher.lastName}")
                            ProfileField("Email", teacher.email)
                            ProfileField("Designation", teacher.designation ?: "")
                            ProfileField("Department", teacher.department ?: "")
                            ProfileField("Class", teacher.className ?: "")
                            ProfileField("Gender", teacher.gender ?: "")
                            ProfileField("Date of Birth", teacher.dateOfBirth ?: "")
                            ProfileField("Age", teacher.age?.toString() ?: "")
                            ProfileField("Mobile No", teacher.mobileNo ?: "")
                            ProfileField("Phone", teacher.phone ?: "")
                            ProfileField("Address", teacher.address ?: "")
                        } ?: run {
                            Text(
                                text = "No teacher data available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                onConfirm = {
                    isDeleting = true
                    FirestoreDatabase.deleteTeacher(
                        teacherId = teacherId,
                        onSuccess = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Teacher deleted successfully")
                                navController.navigateUp()
                            }
                        },
                        onFailure = { e ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to delete teacher: ${e.message}")
                                isDeleting = false
                                showDeleteDialog = false
                            }
                        }
                    )
                },
                onDismiss = { showDeleteDialog = false },
                itemType = "teacher"
            )
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value.ifEmpty { "Not provided" },
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
} 