package com.ecorvi.schmng.ui.screens

import android.net.http.SslCertificate.saveState
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.AdminProfile
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.data.model.SchoolProfile
import com.ecorvi.schmng.ui.navigation.BottomNav
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    currentRoute: String,
    onRouteSelected: (String) -> Unit,
    id: String = "",
    type: String = ""
) {
    var isEditing by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingSchool by remember { mutableStateOf(false) }
    var adminProfile by remember { mutableStateOf<AdminProfile?>(null) }
    var schoolProfile by remember { mutableStateOf<SchoolProfile?>(null) }
    var editedAdminProfile by remember { mutableStateOf<AdminProfile?>(null) }
    var editedSchoolProfile by remember { mutableStateOf<SchoolProfile?>(null) }
    var personProfile by remember { mutableStateOf<Person?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load profiles
    LaunchedEffect(Unit) {
        try {
            if (id.isNotEmpty() && type.isNotEmpty()) {
                // Load student or teacher profile
                personProfile = FirestoreDatabase.getPersonById(id, type)
            } else {
                // Load admin and school profiles
                FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    adminProfile = FirestoreDatabase.getAdminProfile(uid)
                    schoolProfile = FirestoreDatabase.getSchoolProfile()
                }
            }
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to load profile: ${e.message}")
            }
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
                actions = {
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
        bottomBar = {
            BottomNav(
                navController = navController,
                currentRoute = currentRoute,
                onItemSelected = { item -> onRouteSelected(item.route) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            if (id.isNotEmpty() && type.isNotEmpty()) {
                // Display student or teacher profile
                personProfile?.let { person ->
                    ProfileSection(
                        title = "${type.capitalize()} Profile",
                        icon = Icons.Default.Person,
                        items = listOf(
                            "Name" to "${person.firstName} ${person.lastName}",
                            "Email" to (person.email ?: ""),
                            "Phone" to (person.phone ?: ""),
                            "Class" to (person.className ?: "")
                        ),
                        isEditing = false
                    )
                }
            } else {
                // Display admin and school profiles
                schoolProfile?.let { school ->
                    ProfileSection(
                        title = "School Profile",
                        icon = Icons.Default.School,
                        items = listOf(
                            "Name" to (school.name ?: ""),
                            "Principal" to (school.principalName ?: ""),
                            "Board" to (school.boardType ?: ""),
                            "Email" to (school.email ?: ""),
                            "Phone" to (school.phone ?: ""),
                            "Website" to (school.website ?: ""),
                            "Address" to (school.address ?: "")
                        ),
                        isEditing = isEditing,
                        onEditClick = {
                            editedSchoolProfile = school
                            editingSchool = true
                            showEditDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                adminProfile?.let { admin ->
                    ProfileSection(
                        title = "Admin Profile",
                        icon = Icons.Default.Person,
                        items = listOf(
                            "Name" to (admin.name ?: ""),
                            "Designation" to (admin.designation ?: ""),
                            "Email" to (admin.email ?: ""),
                            "Phone" to (admin.phone ?: "")
                        ),
                        isEditing = isEditing,
                        onEditClick = {
                            editedAdminProfile = admin
                            editingSchool = false
                            showEditDialog = true
                        }
                    )
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
