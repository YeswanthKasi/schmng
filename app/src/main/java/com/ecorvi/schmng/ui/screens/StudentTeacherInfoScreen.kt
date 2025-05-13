package com.ecorvi.schmng.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTeacherInfoScreen(navController: NavController) {
    var classTeacher by remember { mutableStateOf<Person?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Fetch student's class and then find their class teacher
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            try {
                // First get the student's class using coroutine
                Log.d("StudentTeacherInfo", "Fetching student data for uid: ${currentUser.uid}")
                val student = FirestoreDatabase.getStudent(currentUser.uid)
                if (student != null) {
                    Log.d("StudentTeacherInfo", "Found student with class: ${student.className}")
                    // Then find the teacher assigned to this class
                    val teacherQuery = FirestoreDatabase.teachersCollection
                        .whereEqualTo("className", student.className)
                        .get()
                        .await()

                    Log.d("StudentTeacherInfo", "Teacher query completed. Empty? ${teacherQuery.isEmpty}")
                    if (!teacherQuery.isEmpty) {
                        // Get the first matching teacher
                        val teacherDoc = teacherQuery.documents[0]
                        Log.d("StudentTeacherInfo", "Found teacher document with ID: ${teacherDoc.id}")
                        val teacher = teacherDoc.toObject(Person::class.java)
                        teacher?.id = teacherDoc.id
                        classTeacher = teacher
                        Log.d("StudentTeacherInfo", "Successfully set class teacher data")
                    } else {
                        Log.e("StudentTeacherInfo", "No teacher found for class: ${student.className}")
                        errorMessage = "No class teacher assigned for ${student.className}"
                    }
                } else {
                    Log.e("StudentTeacherInfo", "Student not found for uid: ${currentUser.uid}")
                    errorMessage = "Student information not found"
                }
            } catch (e: Exception) {
                Log.e("StudentTeacherInfo", "Error fetching data: ${e.message}", e)
                Log.e("StudentTeacherInfo", "Stack trace: ${e.stackTraceToString()}")
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Class Teacher") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                classTeacher != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Teacher",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "${classTeacher?.firstName} ${classTeacher?.lastName}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Class Teacher of ${classTeacher?.className}",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Teacher Details
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DetailRow("Email", classTeacher?.email ?: "")
                                    DetailRow("Phone", classTeacher?.phone ?: "")
                                    if (!classTeacher?.gender.isNullOrBlank()) {
                                        DetailRow("Gender", classTeacher?.gender ?: "")
                                    }
                                    if (!classTeacher?.dateOfBirth.isNullOrBlank()) {
                                        DetailRow("Date of Birth", classTeacher?.dateOfBirth ?: "")
                                    }
                                    if (!classTeacher?.address.isNullOrBlank()) {
                                        DetailRow("Address", classTeacher?.address ?: "")
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        text = "No class teacher information available",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
} 