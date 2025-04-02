package com.ecorvi.schmng.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.StudentListItem
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.firestore.ListenerRegistration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var studentsList by remember { mutableStateOf<List<Person>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }

    // Start listening for real-time updates when screen loads
    LaunchedEffect(Unit) {
        listenerRegistration = FirestoreDatabase.listenForStudentUpdates(
            onUpdate = { fetchedStudents ->
                studentsList = fetchedStudents
                isLoading = false
                Log.d("StudentsScreen", "Real-time student updates: $fetchedStudents")
            },
            onError = { exception ->
                Log.e("Firestore", "Error fetching students in real-time", exception)
                isLoading = false
            }
        )
    }

    // Stop listening when the composable is removed from the screen
    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
            Log.d("StudentsScreen", "Stopped listening for real-time student updates")
        }
    }

    val filteredStudents = studentsList.filter { student ->
        student.firstName.contains(searchQuery, ignoreCase = true) ||
                student.lastName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Students") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Future: Add Filters */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_student") },
                containerColor = Color(0xFF1F41BB)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Student", tint = Color.White)
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search students") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0xFF1F41BB),
                        unfocusedIndicatorColor = Color.Gray,
                        unfocusedContainerColor = Color(0xFFECE6F0),
                        focusedContainerColor = Color(0xFFECE6F0)
                    )
                )

                // Student List
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 50.dp),
                        color = Color(0xFF1F41BB)
                    )
                } else if (filteredStudents.isEmpty()) {
                    Text(
                        text = "No students found",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 50.dp),
                        color = Color.Gray
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredStudents) { student ->
                            StudentListItem(
                                student = student,
                                onViewClick = { navController.navigate("profile/${student.id}/student") },
                                onDeleteClick = {
                                    FirestoreDatabase.deleteStudent(
                                        student.id,
                                        onSuccess = {
                                            studentsList = studentsList.filter { it.id != student.id } // Update list
                                        },
                                        onFailure = { exception ->
                                            Log.e("Firestore", "Failed to delete student: ${exception.message}")
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}



@Preview
@Composable
fun PreviewStudentsScreen() {
    StudentsScreen(navController = NavController(LocalContext.current))
}