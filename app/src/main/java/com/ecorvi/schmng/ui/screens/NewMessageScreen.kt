package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var students by remember { mutableStateOf<List<Person>>(emptyList()) }
    var teachers by remember { mutableStateOf<List<Person>>(emptyList()) }
    var staff by remember { mutableStateOf<List<Person>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load users
    LaunchedEffect(Unit) {
        FirestoreDatabase.listenForStudentUpdates(
            onUpdate = { studentList ->
                students = studentList
                isLoading = false
            },
            onError = { e ->
                error = e.message
                isLoading = false
            }
        )

        FirestoreDatabase.listenForTeacherUpdates(
            onUpdate = { teacherList ->
                teachers = teacherList
                isLoading = false
            },
            onError = { e ->
                error = e.message
                isLoading = false
            }
        )

        FirestoreDatabase.listenForStaffUpdates(
            onUpdate = { staffList ->
                staff = staffList
                isLoading = false
            },
            onError = { e ->
                error = e.message
                isLoading = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "New Message",
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
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search users...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF1F41BB)
                    )
                },
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1F41BB),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

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
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Filter and combine all users
                    val allUsers = (students + teachers + staff).filter { person ->
                        val searchTerm = searchQuery.lowercase()
                        person.firstName.lowercase().contains(searchTerm) ||
                        person.lastName.lowercase().contains(searchTerm) ||
                        person.email.lowercase().contains(searchTerm)
                    }

                    items(allUsers) { person ->
                        UserListItem(
                            person = person,
                            onClick = {
                                navController.navigate("chat/${person.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserListItem(
    person: Person,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = Color(0xFF1F41BB).copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = Color(0xFF1F41BB)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User Info
            Column {
                Text(
                    text = "${person.firstName} ${person.lastName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (person.type) {
                        "student" -> "Student - ${person.className}"
                        "teacher" -> "Teacher"
                        "staff" -> "Staff"
                        else -> person.type.capitalize()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
} 