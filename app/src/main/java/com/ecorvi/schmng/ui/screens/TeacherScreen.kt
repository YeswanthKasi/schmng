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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.TeacherListItem
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.firestore.ListenerRegistration


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("All Classes") }
    val classOptions = listOf("All Classes", "Class 1", "Class 2", "Class 3", "Class 4", "Class 5")

    var teachersList by remember { mutableStateOf<List<Person>>(emptyList()) }
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }

    LaunchedEffect(Unit) {
        listenerRegistration = FirestoreDatabase.listenForTeacherUpdates(
            onUpdate = { teachersList = it },
            onError = { Log.e("TeachersScreen", "Error fetching teachers: ${it.message}") }
        )
    }



    val filteredTeachers = teachersList.filter { teacher ->
        (selectedClass == "All Classes" || teacher.className == selectedClass) &&
                (teacher.firstName.contains(searchQuery, ignoreCase = true) ||
                        teacher.lastName.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teachers") },
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
                onClick = { navController.navigate("add_teacher") },
                containerColor = Color(0xFF1F41BB)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Teacher", tint = Color.White)
            }
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search teachers") },
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
                }

                // Class Dropdown
                item {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedClass,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Class") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            classOptions.forEach { className ->
                                DropdownMenuItem(
                                    text = { Text(className) },
                                    onClick = {
                                        selectedClass = className
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Teachers List
                items(filteredTeachers) { teacher ->
                    TeacherListItem(
                        teacher = teacher,
                        onViewClick = { navController.navigate("profile/${teacher.id}/teacher") },
                        onDeleteClick = {
                            FirestoreDatabase.deleteTeacher(
                                teacher.id,
                                onSuccess = {
                                    teachersList = teachersList.filter { it.id != teacher.id } // Update list
                                },
                                onFailure = { exception ->
                                    Log.e("Firestore", "Failed to delete teacher: ${exception.message}")
                                }
                            )
                        }
                    )
                }
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Composable
fun TeachersScreenPreview() {
    val context = LocalContext.current
    val navController = NavController(context)
    TeachersScreen(navController)
}


