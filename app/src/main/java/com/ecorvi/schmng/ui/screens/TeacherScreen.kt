package com.ecorvi.schmng.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.components.TeacherListItem
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.utils.Constants
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("All Classes") }
    var showClassFilter by remember { mutableStateOf(false) }
    var teachersList by remember { mutableStateOf<List<Person>>(emptyList()) }
    var filteredTeachers by remember { mutableStateOf<List<Person>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Person?>(null) }
    val context = LocalContext.current

    // Firestore real-time listener
    LaunchedEffect(Unit) {
        listenerRegistration = FirestoreDatabase.listenForTeacherUpdates(
            onUpdate = { fetchedTeachers ->
                teachersList = fetchedTeachers
                isLoading = false
                Log.d("TeachersScreen", "Real-time teacher updates: $fetchedTeachers")
            },
            onError = { exception ->
                Log.e("Firestore", "Error fetching teachers in real-time", exception)
                isLoading = false
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
            Log.d("TeachersScreen", "Stopped listening for real-time teacher updates")
        }
    }

    // Update filtered teachers when search or class filter changes
    LaunchedEffect(searchQuery, selectedClass, teachersList) {
        filteredTeachers = teachersList.filter { teacher ->
            val matchesSearch = teacher.firstName.contains(searchQuery, ignoreCase = true) ||
                    teacher.lastName.contains(searchQuery, ignoreCase = true)
            val matchesClass = selectedClass == "All Classes" || teacher.className == selectedClass
            matchesSearch && matchesClass
        }
    }

    CommonBackground {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("Teachers") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showClassFilter = true }) {
                                Icon(Icons.Default.FilterList, "Filter")
                            }
                        }
                    )
                    
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search teachers...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        leadingIcon = { Icon(Icons.Default.Search, "Search") }
                    )

                    // Selected class chip
                    if (selectedClass != "All Classes") {
                        Surface(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedClass)
                                IconButton(
                                    onClick = { selectedClass = "All Classes" }
                                ) {
                                    Icon(Icons.Default.Close, "Clear filter")
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("add_teacher") },
                    containerColor = Color(0xFF1F41BB)
                ) {
                    Icon(Icons.Default.Add, "Add Teacher", tint = Color.White)
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1F41BB))
                    }
                } else if (filteredTeachers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No teachers found", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredTeachers,
                            key = { it.id }
                        ) { teacher ->
                            TeacherListItem(
                                teacher = teacher,
                                onItemClick = {
                                    navController.navigate("profile/${teacher.id}/teacher")
                                },
                                onDeleteClick = {
                                    showDeleteDialog = teacher
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Class filter dialog
    if (showClassFilter) {
        AlertDialog(
            onDismissRequest = { showClassFilter = false },
            title = { Text("Select Class") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    ListItem(
                        headlineContent = { Text("All Classes") },
                        leadingContent = {
                            RadioButton(
                                selected = selectedClass == "All Classes",
                                onClick = {
                                    selectedClass = "All Classes"
                                    showClassFilter = false
                                }
                            )
                        }
                    )
                    Constants.CLASS_OPTIONS.forEach { classOption ->
                        ListItem(
                            headlineContent = { Text(classOption) },
                            leadingContent = {
                                RadioButton(
                                    selected = selectedClass == classOption,
                                    onClick = {
                                        selectedClass = classOption
                                        showClassFilter = false
                                    }
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showClassFilter = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { teacher ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Confirm Delete") },
            text = {
                Text("Are you sure you want to delete ${teacher.firstName} ${teacher.lastName}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        FirestoreDatabase.deleteTeacher(
                            teacher.id,
                            onSuccess = {
                                Toast.makeText(context, "Teacher deleted successfully", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = null
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Error deleting teacher: ${e.message}", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = null
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FilterOption(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF1F41BB)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Composable
fun TeachersScreenPreview() {
    val context = LocalContext.current
    val navController = NavController(context)
    TeachersScreen(navController)
}
