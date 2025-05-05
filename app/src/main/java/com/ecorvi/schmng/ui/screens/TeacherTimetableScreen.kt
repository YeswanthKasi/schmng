package com.ecorvi.schmng.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Timetable
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherTimetableScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedDay by remember { mutableStateOf(getCurrentDay()) }
    var selectedClass by remember { mutableStateOf("Class 1") } // Initial selection
    var selectedTeacher by remember { mutableStateOf("") }
    var timetables by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var teacherName by remember { mutableStateOf<String?>(null) }
    var teachersList by remember { mutableStateOf<List<Person>>(emptyList()) }
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Time slots configuration
    val timeSlots = remember {
        listOf(
            "9:00 - 9:45",
            "9:45 - 10:30",
            "10:30 - 11:15",
            "11:15 - 12:00",
            "12:00 - 12:45", // Lunch Break
            "12:45 - 1:30",
            "1:30 - 2:15",
            "2:15 - 3:00"
        )
    }

    // Available subjects
    val subjects = remember {
        listOf(
            "Mathematics",
            "Science",
            "English",
            "Social Studies",
            "Physical Education",
            "Art",
            "Music",
            "Computer Science",
            "Break",
            "Lunch"
        )
    }

    // Fetch teachers list
    LaunchedEffect(Unit) {
        FirestoreDatabase.listenForTeacherUpdates(
            onUpdate = { teachers ->
                teachersList = teachers
                if (teachers.isNotEmpty() && selectedTeacher.isEmpty()) {
                    selectedTeacher = "${teachers[0].firstName} ${teachers[0].lastName}"
                }
            },
            onError = { e ->
                Log.e("TeacherTimetable", "Error fetching teachers: ${e.message}")
            }
        )
    }

    // Fetch teacher information
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            FirestoreDatabase.getTeacherByUserId(
                userId = currentUser.uid,
                onComplete = { teacher ->
                    if (teacher != null) {
                        teacherName = "${teacher.firstName} ${teacher.lastName}"
                        // Fetch timetables for the teacher
                        FirestoreDatabase.fetchTimetablesForTeacher(
                            teacherName = teacherName ?: "",
                            onComplete = { fetchedTimetables ->
                                timetables = fetchedTimetables
                                isLoading = false
                            },
                            onFailure = { e ->
                                errorMessage = e.message
                                isLoading = false
                            }
                        )
                    } else {
                        Toast.makeText(context, "Teacher information not found", Toast.LENGTH_LONG).show()
                        navController.navigateUp()
                    }
                },
                onFailure = { e ->
                    Toast.makeText(context, "Error fetching teacher information: ${e.message}", Toast.LENGTH_LONG).show()
                    navController.navigateUp()
                }
            )
        } else {
            Toast.makeText(context, "Please log in to continue", Toast.LENGTH_LONG).show()
            navController.navigate("login")
        }
    }

    // Filter timetables by selected day, class, and teacher
    val filteredTimetables = remember(timetables, selectedDay, selectedClass, selectedTeacher) {
        timetables.filter { 
            it.dayOfWeek == selectedDay && 
            (selectedClass.isEmpty() || it.classGrade == selectedClass) &&
            (selectedTeacher.isEmpty() || it.teacher == selectedTeacher)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(teacherName ?: "Teacher Timetable") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Filter section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Class selection dropdown
                var expandedClass by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedClass,
                        onExpandedChange = { expandedClass = it }
                    ) {
                        OutlinedTextField(
                            value = selectedClass,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Class") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClass) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedClass,
                            onDismissRequest = { expandedClass = false }
                        ) {
                            listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5", 
                                  "Class 6", "Class 7", "Class 8", "Class 9", "Class 10").forEach { classOption ->
                                DropdownMenuItem(
                                    text = { Text(classOption) },
                                    onClick = {
                                        selectedClass = classOption
                                        expandedClass = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Teacher selection dropdown
                var expandedTeacher by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedTeacher,
                        onExpandedChange = { expandedTeacher = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTeacher,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Teacher") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTeacher) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTeacher,
                            onDismissRequest = { expandedTeacher = false }
                        ) {
                            teachersList.forEach { teacher ->
                                val fullName = "${teacher.firstName} ${teacher.lastName}"
                                DropdownMenuItem(
                                    text = { Text(fullName) },
                                    onClick = {
                                        selectedTeacher = fullName
                                        expandedTeacher = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Display selected filters
            if (selectedClass.isNotEmpty() || selectedTeacher.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    if (selectedClass.isNotEmpty()) {
                        Text(
                            text = "Class: $selectedClass",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (selectedTeacher.isNotEmpty()) {
                        Text(
                            text = "Teacher: $selectedTeacher",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Day selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday").forEach { day ->
                    FilterChip(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        label = { Text(day.take(3)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (filteredTimetables.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No classes scheduled for $selectedDay")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTimetables) { timetable ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = timetable.subject,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Class: ${timetable.classGrade}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Time: ${timetable.timeSlot}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Room: ${timetable.roomNumber}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getCurrentDay(): String {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
    return when (currentDay) {
        java.util.Calendar.MONDAY -> "Monday"
        java.util.Calendar.TUESDAY -> "Tuesday"
        java.util.Calendar.WEDNESDAY -> "Wednesday"
        java.util.Calendar.THURSDAY -> "Thursday"
        java.util.Calendar.FRIDAY -> "Friday"
        java.util.Calendar.SATURDAY -> "Saturday"
        else -> "Monday" // Default to Monday if it's Sunday
    }
} 