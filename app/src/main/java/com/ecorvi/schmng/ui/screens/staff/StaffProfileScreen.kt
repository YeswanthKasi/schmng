package com.ecorvi.schmng.ui.screens.staff

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
import com.ecorvi.schmng.ui.components.StaffBottomNavigation
import com.ecorvi.schmng.models.User
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.google.firebase.auth.FirebaseAuth
import com.ecorvi.schmng.ui.navigation.StaffBottomNavItem
import com.ecorvi.schmng.ui.components.ProfilePhotoComponent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffProfileScreen(
    navController: NavController,
    currentRoute: String?,
    staffId: String,
    isAdmin: Boolean = false
) {
    var staffData by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf("") }
    var showGenderDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load staff data
    LaunchedEffect(staffId) {
        isLoading = true
        FirestoreDatabase.getStaffById(
            staffId = staffId,
            onComplete = { staff ->
                staffData = staff
                selectedGender = staff?.gender ?: ""
                isLoading = false
            },
            onError = {
                isLoading = false
            }
        )
        profilePhotoUrl = FirestoreDatabase.getProfilePhotoUrl(staffId)
    }

    // Check if this screen is accessed from bottom navigation
    val isFromBottomNav = currentRoute?.startsWith("staff_profile/") == true && !isAdmin

    if (showGenderDialog) {
        AlertDialog(
            onDismissRequest = { showGenderDialog = false },
            title = { Text("Select Gender") },
            text = {
                Column {
                    RadioButton(
                        selected = selectedGender == "Male",
                        onClick = { selectedGender = "Male" }
                    )
                    Text("Male")
                    RadioButton(
                        selected = selectedGender == "Female",
                        onClick = { selectedGender = "Female" }
                    )
                    Text("Female")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGenderDialog = false
                        scope.launch {
                            FirestoreDatabase.updateStaffField(
                                staffId = staffId,
                                field = "gender",
                                value = selectedGender,
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Gender updated successfully")
                                    }
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to update gender: $error")
                                    }
                                }
                            )
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Staff Profile",
                        color = Color(0xFF1F41BB),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (!isFromBottomNav) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF1F41BB)
                            )
                        }
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(
                            onClick = { navController.navigate("add_person/staff?personId=$staffId") }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color(0xFF1F41BB)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            if (isFromBottomNav) {
                StaffBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1F41BB))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Profile Photo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProfilePhotoComponent(
                        userId = staffId,
                        photoUrl = profilePhotoUrl,
                        isEditable = isAdmin,
                        themeColor = Color(0xFF1F41BB),
                        onPhotoUpdated = { url ->
                            profilePhotoUrl = url
                        },
                        onError = { error ->
                            scope.launch {
                                snackbarHostState.showSnackbar(error)
                            }
                        }
                    )
                }

                // Profile Information
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        staffData?.let { staff ->
                            ProfileField("Name", "${staff.firstName} ${staff.lastName}")
                            ProfileField("Email", staff.email)
                            ProfileField("Phone", staff.phone)
                            ProfileField("Mobile", staff.mobileNo)
                            ProfileField("Designation", staff.designation)
                            ProfileField("Department", staff.department)
                            
                            // Gender field with edit button for admin
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Gender",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = selectedGender,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF1F41BB)
                                    )
                                }
                                if (isAdmin) {
                                    IconButton(onClick = { showGenderDialog = true }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Gender",
                                            tint = Color(0xFF1F41BB)
                                        )
                                    }
                                }
                            }
                            
                            ProfileField("Age", staff.age.toString())
                            ProfileField("Date of Birth", staff.dateOfBirth)
                            ProfileField("Address", staff.address)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF1F41BB)
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
} 