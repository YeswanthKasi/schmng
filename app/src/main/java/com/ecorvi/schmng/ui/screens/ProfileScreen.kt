package com.ecorvi.schmng.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.AdminProfile
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.data.model.SchoolProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    id: String = "",
    type: String = ""
) {
    var adminProfile by remember { mutableStateOf<AdminProfile?>(null) }
    var schoolProfile by remember { mutableStateOf<SchoolProfile?>(null) }
    var person by remember { mutableStateOf<Person?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingSchool by remember { mutableStateOf(false) }
    
    // State for edited values
    var editedAdminProfile by remember { mutableStateOf<AdminProfile?>(null) }
    var editedSchoolProfile by remember { mutableStateOf<SchoolProfile?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Load profiles
    LaunchedEffect(id, type) {
        isLoading = true
        error = null
        
        try {
            when {
                // Load specific profile based on type
                id.isNotEmpty() && type.isNotEmpty() -> {
                    person = when (type) {
                        "student" -> FirestoreDatabase.getStudent(id)
                        "teacher" -> FirestoreDatabase.getTeacher(id)
                        "staff" -> FirestoreDatabase.getStaffMember(id)
                        else -> null
                    }
                    isLoading = false
                }
                // Load admin and school profile for general profile view
                else -> {
                    FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                        try {
                            val adminData = FirestoreDatabase.getAdminProfile(uid)
                            val schoolData = FirestoreDatabase.getSchoolProfile()
                            
                            adminProfile = adminData
                            schoolProfile = schoolData
                            editedAdminProfile = adminData?.copy()
                            editedSchoolProfile = schoolData?.copy()
                            
                            isLoading = false
                        } catch (e: Exception) {
                            error = e.message
                            isLoading = false
                        }
                    } ?: run {
                        error = "User not authenticated"
                        isLoading = false
                    }
                }
            }
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        color = Color(0xFF1F41BB),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1F41BB)
                        )
                    }
                },
                actions = {
                    // Only show edit button for admin viewing their own profile
                    if (id.isEmpty() && type.isEmpty()) {
                        IconButton(onClick = { isEditing = !isEditing }) {
                            Icon(
                                if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                                contentDescription = if (isEditing) "Cancel Edit" else "Edit Profile",
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1F41BB))
            }
        } else if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(error ?: "Unknown error occurred", color = Color.Red)
            }
        } else {
            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                when {
                    person != null -> {
                        // Show person profile
                        item {
                            ProfileSection(
                                title = when(type) {
                                    "student" -> "Student Profile"
                                    "teacher" -> "Teacher Profile"
                                    "staff" -> "Staff Profile"
                                    else -> "Profile"
                                },
                                icon = when(type) {
                                    "student" -> Icons.Default.School
                                    "teacher" -> Icons.Default.Class
                                    "staff" -> Icons.Default.Badge
                                    else -> Icons.Default.Person
                                },
                                items = listOf(
                                    "Name" to "${person?.firstName} ${person?.lastName}",
                                    "Email" to (person?.email ?: ""),
                                    "Phone" to (person?.phone ?: ""),
                                    "Type" to (person?.type?.capitalize() ?: ""),
                                    "Class" to (person?.className ?: ""),
                                    if (type == "student") "Roll Number" to (person?.rollNumber ?: "") else null,
                                    "Gender" to (person?.gender ?: ""),
                                    "Date of Birth" to (person?.dateOfBirth ?: ""),
                                    "Address" to (person?.address ?: ""),
                                    if (type != "student") "Designation" to (person?.designation ?: "") else null,
                                    if (type == "staff") "Department" to (person?.department ?: "") else null
                                ).filterNotNull()
                            )
                        }
                    }
                    else -> {
                        // Show school profile
                        item {
                            schoolProfile?.let { school ->
                                ProfileSection(
                                    title = "School Profile",
                                    icon = Icons.Default.School,
                                    items = listOf(
                                        "Name" to (school.name),
                                        "Principal" to (school.principalName),
                                        "Board" to (school.boardType),
                                        "Established" to (school.establishedYear),
                                        "Email" to (school.email),
                                        "Phone" to (school.phone),
                                        "Website" to (school.website),
                                        "Address" to (school.address),
                                        "Description" to (school.description)
                                    ),
                                    isEditing = isEditing,
                                    onEditClick = {
                                        editingSchool = true
                                        showEditDialog = true
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Show admin profile
                        item {
                            adminProfile?.let { admin ->
                                ProfileSection(
                                    title = "Admin Profile",
                                    icon = Icons.Default.AdminPanelSettings,
                                    items = listOf(
                                        "Name" to (admin.name),
                                        "Designation" to (admin.designation),
                                        "Email" to (admin.email),
                                        "Phone" to (admin.phone),
                                        "Join Date" to (admin.joinDate),
                                        "Last Login" to (admin.lastLogin)
                                    ),
                                    isEditing = isEditing,
                                    onEditClick = {
                                        editingSchool = false
                                        showEditDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (editingSchool) "Edit School Profile" else "Edit Admin Profile") },
            text = {
                    Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editingSchool) {
                        editedSchoolProfile?.let { school ->
                            OutlinedTextField(
                                value = school.name ?: "",
                                onValueChange = { editedSchoolProfile = school.copy(name = it) },
                                label = { Text("School Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = school.principalName ?: "",
                                onValueChange = { editedSchoolProfile = school.copy(principalName = it) },
                                label = { Text("Principal Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = school.boardType ?: "",
                                onValueChange = { editedSchoolProfile = school.copy(boardType = it) },
                                label = { Text("Board Type") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = school.email ?: "",
                                onValueChange = { editedSchoolProfile = school.copy(email = it) },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = school.phone ?: "",
                                onValueChange = { editedSchoolProfile = school.copy(phone = it) },
                                label = { Text("Phone") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = school.website ?: "",
                                onValueChange = { editedSchoolProfile = school.copy(website = it) },
                                label = { Text("Website") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = school.address ?: "",
                                onValueChange = { editedSchoolProfile = school.copy(address = it) },
                                label = { Text("Address") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        editedAdminProfile?.let { admin ->
                            OutlinedTextField(
                                value = admin.name ?: "",
                                onValueChange = { editedAdminProfile = admin.copy(name = it) },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = admin.designation ?: "",
                                onValueChange = { editedAdminProfile = admin.copy(designation = it) },
                                label = { Text("Designation") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = admin.email ?: "",
                                onValueChange = { editedAdminProfile = admin.copy(email = it) },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = admin.phone ?: "",
                                onValueChange = { editedAdminProfile = admin.copy(phone = it) },
                                label = { Text("Phone") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
                confirmButton = {
                Button(
                        onClick = {
                                        scope.launch {
                            try {
                                if (editingSchool) {
                                    editedSchoolProfile?.let { school ->
                                        FirestoreDatabase.updateSchoolProfile(school)
                                        schoolProfile = school
                                    }
                            } else {
                                    editedAdminProfile?.let { admin ->
                                        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                                            FirestoreDatabase.updateAdminProfile(uid, admin)
                                            adminProfile = admin
                                        }
                                    }
                                }
                                showEditDialog = false
                                snackbarHostState.showSnackbar("Profile updated successfully")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed to update profile: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Save")
                    }
                },
                dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    icon: ImageVector,
    items: List<Pair<String, String>>,
    isEditing: Boolean = false,
    onEditClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
        color = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF1F41BB),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (isEditing && onEditClick != null) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF1F41BB)
                    )
                }
            }
        }

            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                    .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                    )
                    Text(
                        text = value,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun ProfileOption(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF1F41BB)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color.Gray
            )
        }
    }
}
