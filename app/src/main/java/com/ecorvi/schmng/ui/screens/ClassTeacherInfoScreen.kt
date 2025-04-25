package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassTeacherInfoScreen(navController: NavController) {
    var teacher by remember { mutableStateOf<Person?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Fetch class teacher information
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            FirestoreDatabase.getStudent(currentUser.uid)?.let { student ->
                FirebaseFirestore.getInstance()
                    .collection("teachers")
                    .whereEqualTo("assignedClass", student.className)
                    .get()
                    .addOnSuccessListener { documents ->
                        teacher = documents.firstOrNull()?.toObject(Person::class.java)
                        isLoading = false
                    }
                    .addOnFailureListener { e ->
                        errorMessage = e.message
                        isLoading = false
                    }
            }
        }
    }

    CommonBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Class Teacher") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    )
                )
            }
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
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else if (teacher == null) {
                    Text(
                        text = "Class teacher information not available",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Teacher Profile Picture
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F41BB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Teacher Profile",
                                tint = Color.White,
                                modifier = Modifier.size(80.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Teacher Name
                        Text(
                            text = "${teacher?.firstName} ${teacher?.lastName}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Teacher Details
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
                                InfoRow(
                                    icon = Icons.Default.Email,
                                    label = "Email",
                                    value = teacher?.email ?: ""
                                )
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                InfoRow(
                                    icon = Icons.Default.Phone,
                                    label = "Phone",
                                    value = teacher?.phone ?: ""
                                )
                                if (!teacher?.mobileNo.isNullOrBlank()) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    InfoRow(
                                        icon = Icons.Default.Phone,
                                        label = "Mobile",
                                        value = teacher?.mobileNo ?: ""
                                    )
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                InfoRow(
                                    icon = Icons.Default.Class,
                                    label = "Class",
                                    value = teacher?.className ?: ""
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF1F41BB)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 16.sp
            )
        }
    }
} 