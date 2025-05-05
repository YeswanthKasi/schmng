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

private val StudentGreen = Color(0xFF4CAF50) // Green color for student theme

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    title = { 
                        Text(
                            "Students",
                            color = StudentGreen,
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                "Back",
                                tint = StudentGreen
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showClassFilter = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                "Filter",
                                tint = StudentGreen
                            )
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
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Search,
                            "Search",
                            tint = StudentGreen
                        ) 
                    }
                )

                // Selected class chip
                if (selectedClass != "All Classes") {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                        color = StudentGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedClass,
                                color = StudentGreen,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { selectedClass = "All Classes" },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear filter",
                                    tint = StudentGreen
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_person/student") },
                containerColor = StudentGreen
            ) {
                Icon(Icons.Default.Add, "Add Student", tint = Color.White)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (filteredStudents.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "No results",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No students found",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
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
                                    navController.navigate("student_profile/${student.id}")
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
            title = { Text("Select Class", color = StudentGreen) },
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
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = StudentGreen
                                )
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
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = StudentGreen
                                    )
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showClassFilter = false }) {
                    Text("Close", color = StudentGreen)
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
