package com.ecorvi.schmng.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Timetable
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.utils.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableManagementScreen(navController: NavController) {
    var selectedClass by remember { mutableStateOf("Class 1") }
    var selectedDay by remember { mutableStateOf(getCurrentDay()) }
    var selectedTeacher by remember { mutableStateOf<String?>(null) }
    var timetables by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var teachers by remember { mutableStateOf<List<Person>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var timetableToDelete by remember { mutableStateOf<Timetable?>(null) }
    var showClassDropdown by remember { mutableStateOf(false) }
    var showDayDropdown by remember { mutableStateOf(false) }
    var showTeacherDropdown by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    var isAdmin by remember { mutableStateOf(false) }

    val daysOfWeek = remember {
        mapOf(
            "Monday" to "Mon",
            "Tuesday" to "Tue",
            "Wednesday" to "Wed",
            "Thursday" to "Thu",
            "Friday" to "Fri",
            "Saturday" to "Sat"
        )
    }

    // Check if user is admin
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            FirestoreDatabase.getUserRole(
                userId = currentUser.uid,
                onComplete = { role ->
                    isAdmin = role?.lowercase() == "admin"
                    if (!isAdmin) {
                        Toast.makeText(context, "Only administrators can access timetable management", Toast.LENGTH_LONG).show()
                        navController.navigateUp()
                    }
                },
                onFailure = { e ->
                    Toast.makeText(context, "Error verifying permissions: ${e.message}", Toast.LENGTH_LONG).show()
                    navController.navigateUp()
                }
            )
        } else {
            Toast.makeText(context, "Please log in to continue", Toast.LENGTH_LONG).show()
            navController.navigate("login")
        }
    }

    // Fetch teachers list
    LaunchedEffect(Unit) {
        FirestoreDatabase.listenForTeacherUpdates(
            onUpdate = { fetchedTeachers ->
                teachers = fetchedTeachers
            },
            onError = { e ->
                Log.e("TimetableManagement", "Error fetching teachers: ${e.message}")
                Toast.makeText(context, "Error loading teachers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Fetch timetables when class, day, or teacher changes
    LaunchedEffect(selectedClass, selectedDay, selectedTeacher) {
        isLoading = true
        errorMessage = null
        Log.d("TimetableManagement", "Fetching timetables - Class: $selectedClass, Day: $selectedDay, Teacher: $selectedTeacher")
        
        FirestoreDatabase.fetchTimetablesForClass(
            classGrade = selectedClass,
            onComplete = { fetchedTimetables ->
                Log.d("TimetableManagement", "Fetched ${fetchedTimetables.size} timetables")
                timetables = fetchedTimetables
                    .filter { it.dayOfWeek == selectedDay }
                    .filter { selectedTeacher == null || it.teacher == selectedTeacher }
                    .sortedBy { it.timeSlot }
                Log.d("TimetableManagement", "Filtered ${timetables.size} timetables")
                isLoading = false
            },
            onFailure = { e ->
                Log.e("TimetableManagement", "Error fetching timetables: ${e.message}")
                errorMessage = e.message
                isLoading = false
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && timetableToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this timetable entry?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        timetableToDelete?.let { entry ->
                            FirestoreDatabase.deleteTimetable(
                                timetableId = entry.id,
                                onSuccess = {
                                    Toast.makeText(context, "Timetable deleted successfully", Toast.LENGTH_SHORT).show()
                                    // Remove the deleted item from the list
                                    timetables = timetables.filter { it.id != entry.id }
                                    showDeleteDialog = false
                                    timetableToDelete = null
                                },
                                onFailure = { e ->
                                    Toast.makeText(context, "Error deleting timetable: ${e.message}", Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = false
                                    timetableToDelete = null
                                }
                            )
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timetable Management") },
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
            // Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Class Selection Dropdown
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { showClassDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Class: $selectedClass",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .padding(end = 4.dp),
                                maxLines = 2,
                                softWrap = true,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "Select Class",
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(start = 4.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showClassDropdown,
                        onDismissRequest = { showClassDropdown = false }
                    ) {
                        Constants.CLASS_OPTIONS.forEach { classOption ->
                            DropdownMenuItem(
                                text = { Text(classOption) },
                                onClick = {
                                    selectedClass = classOption
                                    showClassDropdown = false
                                }
                            )
                        }
                    }
                }

                // Day Selection Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDayDropdown = true }
                            .padding(8.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Day: ${daysOfWeek[selectedDay] ?: selectedDay}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .padding(end = 4.dp),
                                maxLines = 2,
                                softWrap = true,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "Select Day",
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(start = 4.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showDayDropdown,
                        onDismissRequest = { showDayDropdown = false }
                    ) {
                        daysOfWeek.forEach { (fullDay, shortDay) ->
                            DropdownMenuItem(
                                text = { Text("$shortDay ($fullDay)") },
                                onClick = {
                                    selectedDay = fullDay
                                    showDayDropdown = false
                                }
                            )
                        }
                    }
                }

                // Teacher Selection Dropdown
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { showTeacherDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedTeacher ?: "Teacher: Select",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .padding(end = 4.dp),
                                maxLines = 2,
                                softWrap = true,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "Select Teacher",
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(start = 4.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showTeacherDropdown,
                        onDismissRequest = { showTeacherDropdown = false }
                    ) {
                        // Add a "Clear Selection" option
                        DropdownMenuItem(
                            text = { Text("Clear Selection") },
                            onClick = {
                                selectedTeacher = null
                                showTeacherDropdown = false
                            }
                        )
                        teachers.sortedBy { it.firstName }.forEach { teacher ->
                            val fullName = "${teacher.firstName} ${teacher.lastName}".trim()
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(teacher.firstName, style = MaterialTheme.typography.bodyMedium)
                                        Text(teacher.lastName, style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                onClick = {
                                    selectedTeacher = fullName
                                    showTeacherDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add New Timetable Button
            Button(
                onClick = { navController.navigate("add_timetable") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Timetable Entry")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timetable List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (timetables.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No timetable entries found for ${selectedClass} - ${selectedDay}")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(timetables) { timetable ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = timetable.subject,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Time: ${timetable.timeSlot}")
                                    Text("Teacher: ${timetable.teacher}")
                                    Text("Room: ${timetable.roomNumber}")
                                }
                                
                                // Admin Controls
                                IconButton(
                                    onClick = { 
                                        navController.navigate("edit_timetable/${timetable.id}")
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        timetableToDelete = timetable
                                        showDeleteDialog = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getCurrentDay(): String {
    val calendar = Calendar.getInstance()
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> "Monday" // Default to Monday for Sunday
    }
} 