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
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load student data
    LaunchedEffect(studentId) {
        try {
            student = FirestoreDatabase.getStudent(studentId)
            try {
                profilePhotoUrl = FirestoreDatabase.getProfilePhotoUrl(studentId)
            } catch (e: Exception) {
                // Handle photo loading error silently
            }
            isLoading = false
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to load student profile: ${e.message}")
            }
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    if (isAdmin || currentRoute == null || onRouteSelected == null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isAdmin) {
                        // Edit button
                        IconButton(
                            onClick = { 
                                navController.navigate("add_person/student?personId=$studentId") 
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
                    containerColor = StudentGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (!isAdmin && currentRoute != null && onRouteSelected != null) {
                StudentBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { item -> onRouteSelected(item.route) }
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StudentGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo Holder (fully visible below the top bar)
                Box(
                    modifier = Modifier
                        .offset(y = 24.dp)
                        .size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProfilePhotoComponent(
                        userId = studentId,
                        photoUrl = profilePhotoUrl,
                        isEditable = false, // Always view-only for students
                        themeColor = StudentGreen
                    )
                }
                Text(
                    text = "Profile Photo",
                    color = StudentGreen,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .offset(y = 28.dp)
                        .padding(bottom = 0.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
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
                            color = StudentGreen
                        )
                        
                        student?.let { student ->
                            ProfileField("Name", "${student.firstName} ${student.lastName}")
                            ProfileField("Email", student.email)
                            ProfileField("Class", student.className ?: "")
                            ProfileField("Gender", student.gender ?: "")
                            ProfileField("Date of Birth", student.dateOfBirth ?: "")
                            ProfileField("Age", student.age?.toString() ?: "")
                            ProfileField("Mobile No", student.mobileNo ?: "")
                            ProfileField("Phone", student.phone ?: "")
                            ProfileField("Address", student.address ?: "")
                            
                            // Show parent information if available
                            student.parentInfo?.let { parent ->
                                Text(
                                    text = "Parent Information",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = StudentGreen,
                                    modifier = Modifier.padding(top = 24.dp)
                                )
                                ProfileField("Parent Name", parent.name)
                                ProfileField("Parent Email", parent.email)
                                ProfileField("Parent Phone", parent.phone)
                            }
                        } ?: run {
                            Text(
                                text = "No student data available",
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
                    FirestoreDatabase.deleteStudent(
                        studentId = studentId,
                        onSuccess = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Student deleted successfully")
                                navController.navigateUp()
                            }
                        },
                        onFailure = { e ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to delete student: ${e.message}")
                                isDeleting = false
                                showDeleteDialog = false
                            }
                        }
                    )
                },
                onDismiss = { showDeleteDialog = false },
                itemType = "student"
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