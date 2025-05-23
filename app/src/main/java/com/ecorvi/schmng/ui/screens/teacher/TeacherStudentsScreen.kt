package com.ecorvi.schmng.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.models.StudentInfo
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.viewmodels.TeacherStudentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStudentsScreen(
    navController: NavController,
    viewModel: TeacherStudentsViewModel
) {
    val studentsState by viewModel.studentsState.collectAsState()
    var selectedClass by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Students") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Class filter dropdown
                    var isExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = isExpanded,
                        onExpandedChange = { isExpanded = it }
                    ) {
                        TextField(
                            value = selectedClass ?: "All Classes",
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier.menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Classes") },
                                onClick = {
                                    selectedClass = null
                                    isExpanded = false
                                    viewModel.filterByClass(null)
                                }
                            )
                            studentsState.classes.forEach { className ->
                                DropdownMenuItem(
                                    text = { Text(className) },
                                    onClick = {
                                        selectedClass = className
                                        isExpanded = false
                                        viewModel.filterByClass(className)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        CommonBackground {
            if (studentsState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (studentsState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(studentsState.error ?: "An unknown error occurred")
                        Button(onClick = { viewModel.refreshStudents() }) {
                            Text("Retry")
                        }
                    }
                }
            } else if (studentsState.students.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedClass != null) 
                            "No students found in $selectedClass" 
                        else 
                            "No students found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(studentsState.students) { student ->
                        StudentCard(
                            student = student,
                            onViewDetails = {
                                navController.navigate("student_details/${student.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentCard(
    student: StudentInfo,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onViewDetails
    ) {
        ListItem(
            headlineContent = {
                Text("${student.firstName} ${student.lastName}")
            },
            supportingContent = {
                Text("Class: ${student.className}")
            },
            leadingContent = {
                Icon(Icons.Default.Person, contentDescription = null)
            },
            trailingContent = {
                IconButton(onClick = onViewDetails) {
                    Icon(Icons.Default.ChevronRight, "View Details")
                }
            }
        )
    }
} 