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

    // Load profiles
    LaunchedEffect(currentUser?.uid) {
        try {
            currentUser?.uid?.let { uid ->
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
            }
            isLoading = false
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
                        "Admin Profile",
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
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
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
                    Text(
                        error ?: "Unknown error occurred",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                // Admin Profile Section
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
                        Text(
                            "Admin Details",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F41BB)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isEditing) {
                            OutlinedTextField(
                                value = adminName,
                                onValueChange = { adminName = it },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1F41BB),
                                    focusedLabelColor = Color(0xFF1F41BB)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = adminEmail,
                                onValueChange = { adminEmail = it },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1F41BB),
                                    focusedLabelColor = Color(0xFF1F41BB)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = adminPhone,
                                onValueChange = { adminPhone = it },
                                label = { Text("Phone") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1F41BB),
                                    focusedLabelColor = Color(0xFF1F41BB)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = adminDesignation,
                                onValueChange = { adminDesignation = it },
                                label = { Text("Designation") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1F41BB),
                                    focusedLabelColor = Color(0xFF1F41BB)
                                )
                            )
                        } else {
                            ProfileField("Name", adminName)
                            ProfileField("Email", adminEmail)
                            ProfileField("Phone", adminPhone)
                            ProfileField("Designation", adminDesignation)
                        }
                    }
                }

                // School Profile Section
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
                        Text(
                            "School Details",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F41BB)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isEditing) {
                            OutlinedTextField(
                                value = schoolName,
                                onValueChange = { schoolName = it },
                                label = { Text("School Name") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1F41BB),
                                    focusedLabelColor = Color(0xFF1F41BB)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = schoolAddress,
                                onValueChange = { schoolAddress = it },
                                label = { Text("Address") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1F41BB),
                                    focusedLabelColor = Color(0xFF1F41BB)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = schoolPhone,
                                onValueChange = { schoolPhone = it },
                                label = { Text("Phone") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1F41BB),
                                    focusedLabelColor = Color(0xFF1F41BB)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = schoolEmail,
                                onValueChange = { schoolEmail = it },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1F41BB),
                                    focusedLabelColor = Color(0xFF1F41BB)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = schoolWebsite,
                                onValueChange = { schoolWebsite = it },
                                label = { Text("Website") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1F41BB),
                                    focusedLabelColor = Color(0xFF1F41BB)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = schoolDescription,
                                onValueChange = { schoolDescription = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1F41BB),
                                    focusedLabelColor = Color(0xFF1F41BB)
                                )
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

                // Save/Cancel Buttons when editing
                if (isEditing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { isEditing = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray
                            ),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Text("Cancel")
                        }
                        
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
                            ),
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        ) {
                            Text("Save")
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
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = value.ifEmpty { "Not set" },
            fontSize = 16.sp,
            color = if (value.isEmpty()) Color.Gray else Color.Black
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
} 