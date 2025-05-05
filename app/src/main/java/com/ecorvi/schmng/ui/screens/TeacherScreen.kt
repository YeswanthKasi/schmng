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
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Firestore real-time listener
    LaunchedEffect(Unit) {
        listenerRegistration = FirestoreDatabase.listenForTeacherUpdates(
            onUpdate = { fetchedTeachers ->
                teachersList = fetchedTeachers
                isLoading = false
            },
            onError = { exception ->
                Log.e("TeacherScreen", "Error fetching teachers", exception)
                isLoading = false
                scope.launch {
                    snackbarHostState.showSnackbar("Error loading teachers: ${exception.message}")
                }
            }
        )
    }

    // Cleanup listener when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
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
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                        color = Color(0xFF1F41BB).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedClass,
                                color = Color(0xFF1F41BB),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { selectedClass = "All Classes" },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear filter",
                                    tint = Color(0xFF1F41BB)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_person/teacher") },
                containerColor = Color(0xFF1F41BB)
            ) {
                Icon(Icons.Default.Add, "Add Teacher", tint = Color.White)
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
                } else if (filteredTeachers.isEmpty()) {
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
                            text = "No teachers found",
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
                            items = filteredTeachers,
                            key = { it.id }
                        ) { teacher ->
                            TeacherListItem(
                                teacher = teacher,
                                onItemClick = {
                                    navController.navigate("teacher_profile/${teacher.id}")
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
fun TeachersScreenPreview() {
    TeachersScreen(NavController(LocalContext.current))
}
