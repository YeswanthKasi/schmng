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
import com.ecorvi.schmng.ui.components.ProfilePhotoComponent
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.AdminProfile
import com.ecorvi.schmng.ui.data.model.SchoolProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var adminProfile by remember { mutableStateOf<AdminProfile?>(null) }
    var schoolProfile by remember { mutableStateOf<SchoolProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    
    // Editable states for admin profile
    var adminName by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("") }
    var adminPhone by remember { mutableStateOf("") }
    var adminDesignation by remember { mutableStateOf("") }
    
    // Editable states for school profile
    var schoolName by remember { mutableStateOf("") }
    var schoolAddress by remember { mutableStateOf("") }
    var schoolPhone by remember { mutableStateOf("") }
    var schoolEmail by remember { mutableStateOf("") }
    var schoolWebsite by remember { mutableStateOf("") }
    var schoolDescription by remember { mutableStateOf("") }

    // Load profiles and photo URL
    LaunchedEffect(currentUser?.uid) {
        try {
            currentUser?.uid?.let { uid ->
                // Load profile photo
                profilePhotoUrl = FirestoreDatabase.getProfilePhotoUrl(uid)
                
                // Load admin profile
                val admin = FirestoreDatabase.getAdminProfile(uid)
                adminProfile = admin
                admin?.let {
                    adminName = it.name
                    adminEmail = it.email
                    adminPhone = it.phone
                    adminDesignation = it.designation
                }

                // Load school profile
                val school = FirestoreDatabase.getSchoolProfile()
                schoolProfile = school
                school?.let {
                    schoolName = it.name
                    schoolAddress = it.address
                    schoolPhone = it.phone
                    schoolEmail = it.email
                    schoolWebsite = it.website
                    schoolDescription = it.description
                }
                isLoading = false
            }
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Profile") },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, "Edit Profile")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
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
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(error ?: "Unknown error occurred")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo
                currentUser?.uid?.let { uid ->
                    ProfilePhotoComponent(
                        userId = uid,
                        photoUrl = profilePhotoUrl,
                        isEditable = isEditing,
                        onPhotoUpdated = { url ->
                            profilePhotoUrl = url
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Admin Profile Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Admin Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F41BB)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isEditing) {
                            OutlinedTextField(
                                value = adminName,
                                onValueChange = { adminName = it },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = adminEmail,
                                onValueChange = { adminEmail = it },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = adminPhone,
                                onValueChange = { adminPhone = it },
                                label = { Text("Phone") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = adminDesignation,
                                onValueChange = { adminDesignation = it },
                                label = { Text("Designation") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            ProfileField("Name", adminName)
                            ProfileField("Email", adminEmail)
                            ProfileField("Phone", adminPhone)
                            ProfileField("Designation", adminDesignation)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // School Profile Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "School Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F41BB)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isEditing) {
                            OutlinedTextField(
                                value = schoolName,
                                onValueChange = { schoolName = it },
                                label = { Text("School Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = schoolAddress,
                                onValueChange = { schoolAddress = it },
                                label = { Text("Address") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = schoolPhone,
                                onValueChange = { schoolPhone = it },
                                label = { Text("Phone") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = schoolEmail,
                                onValueChange = { schoolEmail = it },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = schoolWebsite,
                                onValueChange = { schoolWebsite = it },
                                label = { Text("Website") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = schoolDescription,
                                onValueChange = { schoolDescription = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        } else {
                            ProfileField("School Name", schoolName)
                            ProfileField("Address", schoolAddress)
                            ProfileField("Phone", schoolPhone)
                            ProfileField("Email", schoolEmail)
                            ProfileField("Website", schoolWebsite)
                            ProfileField("Description", schoolDescription)
                        }
                    }
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        // Update admin profile
                                        val updatedAdminProfile = AdminProfile(
                                            name = adminName,
                                            email = adminEmail,
                                            phone = adminPhone,
                                            designation = adminDesignation
                                        )
                                        currentUser?.uid?.let { uid ->
                                            FirestoreDatabase.updateAdminProfile(uid, updatedAdminProfile)
                                        }

                                        // Update school profile
                                        val updatedSchoolProfile = SchoolProfile(
                                            name = schoolName,
                                            address = schoolAddress,
                                            phone = schoolPhone,
                                            email = schoolEmail,
                                            website = schoolWebsite,
                                            description = schoolDescription
                                        )
                                        FirestoreDatabase.updateSchoolProfile(updatedSchoolProfile)

                                        isEditing = false
                                    } catch (e: Exception) {
                                        error = e.message
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1F41BB)
                            )
                        ) {
                            Text("Save Changes")
                        }
                        OutlinedButton(
                            onClick = {
                                isEditing = false
                                // Reset to original values
                                adminProfile?.let {
                                    adminName = it.name
                                    adminEmail = it.email
                                    adminPhone = it.phone
                                    adminDesignation = it.designation
                                }
                                schoolProfile?.let {
                                    schoolName = it.name
                                    schoolAddress = it.address
                                    schoolPhone = it.phone
                                    schoolEmail = it.email
                                    schoolWebsite = it.website
                                    schoolDescription = it.description
                                }
                            }
                        ) {
                            Text("Cancel")
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
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
} 