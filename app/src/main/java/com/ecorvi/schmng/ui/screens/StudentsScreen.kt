package com.ecorvi.schmng.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.components.StudentListItem
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.utils.Constants
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import com.ecorvi.schmng.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("All Classes") }
    var showClassFilter by remember { mutableStateOf(false) }
    var studentsList by remember { mutableStateOf<List<Person>>(emptyList()) }
    var filteredStudents by remember { mutableStateOf<List<Person>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }
    var showAttendanceButton by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Person?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Update showAttendanceButton when class is selected
    LaunchedEffect(selectedClass) {
        showAttendanceButton = selectedClass != "All Classes"
    }

    // Firestore real-time listener
    LaunchedEffect(Unit) {
        try {
            listenerRegistration = FirestoreDatabase.listenForStudentUpdates(
                onUpdate = { fetchedStudents ->
                    studentsList = fetchedStudents
                    isLoading = false
                    Log.d("StudentsScreen", "Real-time student updates: $fetchedStudents")
                },
                onError = { exception ->
                    Log.e("Firestore", "Error fetching students in real-time", exception)
                    isLoading = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Error loading students: ${exception.message}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("Firestore", "Error setting up student listener", e)
            scope.launch {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            }
        }
    }

    // Update filtered students when search or class filter changes
    LaunchedEffect(searchQuery, selectedClass, studentsList) {
        filteredStudents = studentsList.filter { student ->
            val matchesSearch = student.firstName.contains(searchQuery, ignoreCase = true) ||
                    student.lastName.contains(searchQuery, ignoreCase = true)
            val matchesClass = selectedClass == "All Classes" || student.className == selectedClass
            matchesSearch && matchesClass
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Students") },
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
                    placeholder = { Text("Search students...") },
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
            Column {
                if (showAttendanceButton) {
                    FloatingActionButton(
                        onClick = { 
                            if (selectedClass != "All Classes") {
                                navController.navigate("student_attendance/$selectedClass")
                            } else {
                                Toast.makeText(context, "Please select a class first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.padding(bottom = 8.dp),
                        containerColor = Color(0xFF1F41BB)
                    ) {
                        Icon(Icons.Default.CalendarToday, "Take Attendance", tint = Color.White)
                    }
                }
                FloatingActionButton(
                    onClick = { navController.navigate("add_student") },
                    containerColor = Color(0xFF1F41BB)
                ) {
                    Icon(Icons.Default.Add, "Add Student", tint = Color.White)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { padding ->
            Image(
                painter = painterResource(id = R.drawable.bg_ui),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(
                    color = Color(0xFFFFFFFF).copy(alpha = 0.1f),
                    blendMode = BlendMode.SrcAtop
                )
            )

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
                } else if (filteredStudents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No students found", color = Color.Gray)
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
                            items = filteredStudents,
                            key = { it.id }
                        ) { student ->
                            StudentListItem(
                                student = student,
                                onItemClick = {
                                    navController.navigate("view_profile/student/${student.id}")
                                },
                                onDeleteClick = {
                                    showDeleteDialog = student
                                }
                            )
                        }
                    }
                }
            }
        }
    )

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
    showDeleteDialog?.let { student ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Confirm Delete") },
            text = {
                Text("Are you sure you want to delete ${student.firstName} ${student.lastName}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        FirestoreDatabase.deleteStudent(
                            student.id,
                            onSuccess = {
                                Toast.makeText(context, "Student deleted successfully", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = null
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Error deleting student: ${e.message}", Toast.LENGTH_SHORT).show()
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

@Preview
@Composable
fun PreviewStudentsScreen() {
    StudentsScreen(navController = NavController(LocalContext.current))
}
