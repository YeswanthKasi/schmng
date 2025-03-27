package com.ecorvi.schmng.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.TeacherListItem
import com.ecorvi.schmng.ui.data.InMemoryDatabase.teachersList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersScreen(navController: NavController) {
    // State for search query and selected class
    var searchQuery by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("All Classes") }
    val classOptions = listOf("All Classes", "Class 1", "Class 2", "Class 3", "Class 4", "Class 5")

    // Filter the teachers list based on search query and selected class
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
                    IconButton(onClick = { /* Handle filter or other actions */ }) {
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
                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = { /* Handle dropdown expansion if needed */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedClass,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Class") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = false,
                            onDismissRequest = {}
                        ) {
                            classOptions.forEach { className ->
                                DropdownMenuItem(
                                    text = { Text(className) },
                                    onClick = {
                                        selectedClass = className
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
                        onViewClick = {
                            navController.navigate("profile/${teacher.id}/teacher")
                        },
                        onDeleteClick = {
                            teachersList.remove(teacher)
                        }
                    )
                }
            }
        }
    )
}