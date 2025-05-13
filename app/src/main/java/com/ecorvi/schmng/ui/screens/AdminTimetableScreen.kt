package com.ecorvi.schmng.ui.screens

import android.app.TimePickerDialog
import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Timetable
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import java.text.SimpleDateFormat
import kotlin.math.min

private fun formatTimeSlot(time: String): String {
    return try {
        val parts = time.split(" - ").map { it.trim() }
        if (parts.size != 2) return time
        
        // Return the original string if it's already in the correct format
        if (parts[0].contains("AM") || parts[0].contains("PM")) {
            return time
        }
        
        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        timeFormat.isLenient = false
        
        try {
            val startTime = timeFormat.parse(parts[0])
            val endTime = timeFormat.parse(parts[1])
            
            if (startTime == null || endTime == null) return time
            
            "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
        } catch (e: Exception) {
            time
        }
    } catch (e: Exception) {
        time
    }
}

private fun parseTimeForSorting(timeSlot: String): Int {
    return try {
        val startTime = timeSlot.split(" - ")[0].trim()
        
        // Handle both formats: "9:00 AM" and "9:00"
        val timeFormat = if (startTime.contains("AM") || startTime.contains("PM")) {
            SimpleDateFormat("h:mm a", Locale.US)
        } else {
            SimpleDateFormat("H:mm", Locale.US)
        }
        
        timeFormat.isLenient = false
        val date = timeFormat.parse(startTime) ?: return 0
        
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        // Convert to minutes since midnight for accurate sorting
        calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    } catch (e: Exception) {
        Log.e("AdminTimetable", "Error parsing time for sorting: $timeSlot", e)
        0
    }
}

private fun isTimeWithinRange(timeStr: String): Boolean {
    return try {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        timeFormat.isLenient = false
        
        val time = timeFormat.parse(timeStr) ?: return false
        val calendar = Calendar.getInstance()
        calendar.time = time

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        hour in 9..16  // 9 AM to 5 PM (16:59)
    } catch (e: Exception) {
        Log.e("AdminTimetable", "Error checking time range: $timeStr", e)
        false
    }
}

private fun validateTimeSlot(startTime: String, endTime: String): Boolean {
    return try {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        timeFormat.isLenient = false
        
        val start = timeFormat.parse(startTime) ?: return false
        val end = timeFormat.parse(endTime) ?: return false
        
        val startCal = Calendar.getInstance()
        startCal.time = start
        
        val endCal = Calendar.getInstance()
        endCal.time = end
        
        // Check if times are within 9 AM to 5 PM
        val startHour = startCal.get(Calendar.HOUR_OF_DAY)
        val endHour = endCal.get(Calendar.HOUR_OF_DAY)
        
        if (startHour < 9 || startHour >= 17 || endHour < 9 || endHour > 17) {
            return false
        }
        
        // Ensure end time is after start time
        end.after(start)
    } catch (e: Exception) {
        Log.e("AdminTimetable", "Error validating time slot: $startTime - $endTime", e)
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTimetableScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedClass by remember { mutableStateOf("Class 1") }
    var selectedTeacher by remember { mutableStateOf("") }
    var timetables by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var teachersList by remember { mutableStateOf<List<Person>>(emptyList()) }
    var timeSlots by remember { mutableStateOf<List<String>>(emptyList()) }
    var subjects by remember { mutableStateOf<List<String>>(emptyList()) }
    var isEditingTimeSlot by remember { mutableStateOf(false) }
    var isEditingSubject by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var selectedStartTime by remember { mutableStateOf("") }
    var selectedEndTime by remember { mutableStateOf("") }
    var selectedTimeSlotToEdit by remember { mutableStateOf<String?>(null) }
    var selectedSubjectToEdit by remember { mutableStateOf<String?>(null) }
    var newTimeSlot by remember { mutableStateOf("") }
    var newSubject by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("class") }
    var showTimetableDialog by remember { mutableStateOf(false) }
    var dialogTimeSlot by remember { mutableStateOf("") }
    var dialogDay by remember { mutableStateOf("") }
    var dialogSubject by remember { mutableStateOf("") }
    var dialogTeacher by remember { mutableStateOf("") }
    var dialogRoom by remember { mutableStateOf("") }
    var editingTimetableId by remember { mutableStateOf<String?>(null) }
    var showTimeSlotListDialog by remember { mutableStateOf(false) }
    var showSubjectListDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var timeSlotToDelete by remember { mutableStateOf<String?>(null) }
    var showClassFilterForTeacher by remember { mutableStateOf(false) }

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

    // Default time slots from 9 AM to 5 PM
    val defaultTimeSlots = listOf(
        "9:00 AM - 9:40 AM",
        "9:45 AM - 10:25 AM",
        "10:30 AM - 11:10 AM",
        "11:15 AM - 11:55 AM",
        "12:00 PM - 12:40 PM",
        "12:45 PM - 1:25 PM",
        "1:30 PM - 2:10 PM",
        "2:15 PM - 2:55 PM",
        "3:00 PM - 3:40 PM",
        "3:45 PM - 4:25 PM",
        "4:30 PM - 5:00 PM"
    )

    // Fetch time slots and subjects from Firestore
    LaunchedEffect(Unit) {
        FirestoreDatabase.fetchTimeSlots(
            onComplete = { slots ->
                timeSlots = slots.sortedBy { parseTimeForSorting(it) }
            },
            onFailure = { e ->
                Log.e("AdminTimetable", "Error fetching time slots: ${e.message}")
                Toast.makeText(context, "Error loading time slots", Toast.LENGTH_SHORT).show()
            }
        )

        FirestoreDatabase.fetchSubjects(
            onComplete = { fetchedSubjects ->
                subjects = fetchedSubjects
            },
            onFailure = { e ->
                Log.e("AdminTimetable", "Error fetching subjects: ${e.message}")
            }
        )

        FirestoreDatabase.listenForTeacherUpdates(
            onUpdate = { teachers ->
                teachersList = teachers
                if (teachers.isNotEmpty() && selectedTeacher.isEmpty()) {
                    selectedTeacher = "${teachers[0].firstName} ${teachers[0].lastName}"
                }
            },
            onError = { e ->
                Log.e("AdminTimetable", "Error fetching teachers: ${e.message}")
            }
        )
    }

    // Add a listener for time slot updates
    LaunchedEffect(Unit) {
        FirestoreDatabase.listenForTimeSlotUpdates(
            onUpdate = { updatedSlots ->
                timeSlots = updatedSlots.sortedBy { parseTimeForSorting(it) }
            },
            onError = { e ->
                Log.e("AdminTimetable", "Error listening for time slot updates: ${e.message}")
                Toast.makeText(context, "Error updating time slots", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Fetch timetables based on filter type
    LaunchedEffect(filterType, selectedClass, selectedTeacher, showClassFilterForTeacher) {
        isLoading = true
        when (filterType) {
            "class" -> {
                FirestoreDatabase.fetchTimetablesForClass(
                    classGrade = selectedClass,
                    onComplete = { fetchedTimetables ->
                        timetables = fetchedTimetables
                        isLoading = false
                    },
                    onFailure = { e ->
                        errorMessage = e.message
                        isLoading = false
                    }
                )
            }
            "teacher" -> {
                if (selectedTeacher.isNotEmpty()) {
                    if (showClassFilterForTeacher) {
                        // Fetch timetables for specific teacher and class
                        FirestoreDatabase.fetchTimetablesForTeacherAndClass(
                            teacherName = selectedTeacher,
                            classGrade = selectedClass,
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
                        // Fetch all timetables for teacher
                        FirestoreDatabase.fetchTimetablesForTeacher(
                            teacherName = selectedTeacher,
                            onComplete = { fetchedTimetables ->
                                timetables = fetchedTimetables
                                isLoading = false
                            },
                            onFailure = { e ->
                                errorMessage = e.message
                                isLoading = false
                            }
                        )
                    }
                }
            }
        }
    }

    // Time picker dialogs
    if (showStartTimePicker) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                val newStartTime = SimpleDateFormat("h:mm a", Locale.US).format(calendar.time)
                
                if (isTimeWithinRange(newStartTime)) {
                    selectedStartTime = newStartTime
                    showStartTimePicker = false
                    showEndTimePicker = true
                } else {
                    Toast.makeText(context, "Please select a time between 9 AM and 5 PM", Toast.LENGTH_SHORT).show()
                }
            },
            9, // Default to 9 AM
            0,
            DateFormat.is24HourFormat(context)
        ).show()
    }

    if (showEndTimePicker) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                val newEndTime = SimpleDateFormat("h:mm a", Locale.US).format(calendar.time)
                
                if (validateTimeSlot(selectedStartTime, newEndTime)) {
                    selectedEndTime = newEndTime
                    showEndTimePicker = false
                    
                    // Create and save the time slot
                    val timeSlot = "$selectedStartTime - $selectedEndTime"
                    if (selectedTimeSlotToEdit != null) {
                        FirestoreDatabase.addTimeSlot(
                            timeSlot,
                            onSuccess = {
                                Toast.makeText(context, "Time slot updated", Toast.LENGTH_SHORT).show()
                                isEditingTimeSlot = false
                                selectedTimeSlotToEdit = null
                            },
                            onFailure = { exception ->
                                Toast.makeText(context, "Error: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        FirestoreDatabase.addTimeSlot(
                            timeSlot,
                            onSuccess = {
                                Toast.makeText(context, "Time slot added", Toast.LENGTH_SHORT).show()
                                isEditingTimeSlot = false
                            },
                            onFailure = { exception ->
                                Toast.makeText(context, "Error: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    selectedStartTime = ""
                    selectedEndTime = ""
                } else {
                    Toast.makeText(context, "Invalid time slot. End time must be after start time and between 9 AM and 5 PM", Toast.LENGTH_SHORT).show()
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            DateFormat.is24HourFormat(context)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Timetable") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showTimeSlotListDialog = true }) {
                        Icon(Icons.Default.Schedule, "Manage Time Slots")
                    }
                    IconButton(onClick = { showSubjectListDialog = true }) {
                        Icon(Icons.Default.Book, "Manage Subjects")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp)
        ) {
            // Filter type selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = filterType == "class",
                    onClick = { 
                        filterType = "class"
                        showClassFilterForTeacher = false 
                    },
                    label = { Text("By Class") }
                )
                FilterChip(
                    selected = filterType == "teacher",
                    onClick = { 
                        filterType = "teacher"
                        selectedTeacher = ""
                        showClassFilterForTeacher = false
                    },
                    label = { Text("By Teacher") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter selection
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filterType == "class") {
                    var expandedClass by remember { mutableStateOf(false) }
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
                } else {
                    // Teacher selection
                    var expandedTeacher by remember { mutableStateOf(false) }
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
                                        showClassFilterForTeacher = true
                                    }
                                )
                            }
                        }
                    }

                    // Show class filter after teacher is selected
                    if (showClassFilterForTeacher) {
                        var expandedClassForTeacher by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedClassForTeacher,
                            onExpandedChange = { expandedClassForTeacher = it }
                        ) {
                            OutlinedTextField(
                                value = selectedClass,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Class for Teacher") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClassForTeacher) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedClassForTeacher,
                                onDismissRequest = { expandedClassForTeacher = false }
                            ) {
                                listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5", 
                                      "Class 6", "Class 7", "Class 8", "Class 9", "Class 10").forEach { classOption ->
                                    DropdownMenuItem(
                                        text = { Text(classOption) },
                                        onClick = {
                                            selectedClass = classOption
                                            expandedClassForTeacher = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timetable view
            if (timeSlots.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No time slots found. Please add time slots to begin.",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(onClick = { isEditingTimeSlot = true }) {
                        Icon(Icons.Default.Schedule, contentDescription = "Add Time Slot")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Time Slot")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        // Header row with days
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(2.dp)
                        ) {
                            // Time column header
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Time",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            // Day columns headers with shortened names
                            daysOfWeek.forEach { (_, shortDay) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        shortDay,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    // Time slot rows - now sorted
                    items(timeSlots.filter { it.isNotBlank() }.sortedBy { parseTimeForSorting(it) }) { timeSlot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .border(0.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            // Time slot column
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .fillMaxHeight()
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val formattedTimeSlot = try {
                                    formatTimeSlot(timeSlot)
                                } catch (e: Exception) {
                                    timeSlot
                                }
                                Text(
                                    text = formattedTimeSlot,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                            // Day columns
                            daysOfWeek.forEach { (fullDay, _) ->
                                val entry = timetables.find { 
                                    it.timeSlot == timeSlot && it.dayOfWeek == fullDay 
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                        .clickable {
                                            dialogTimeSlot = timeSlot
                                            dialogDay = fullDay
                                            dialogSubject = entry?.subject ?: ""
                                            dialogTeacher = entry?.teacher ?: ""
                                            dialogRoom = entry?.roomNumber ?: ""
                                            editingTimetableId = entry?.id
                                            showTimetableDialog = true
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (entry != null) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = entry.subject,
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = entry.teacher,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            if (entry.roomNumber.isNotBlank()) {
                                                Text(
                                                    text = entry.roomNumber,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.secondary
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
        }
    }

    // Time slots list dialog
    if (showTimeSlotListDialog) {
        AlertDialog(
            onDismissRequest = { 
                showTimeSlotListDialog = false
                // Reset time picker states when closing dialog
                showStartTimePicker = false
                showEndTimePicker = false
            },
            title = { Text("Manage Time Slots") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    if (timeSlots.isEmpty()) {
                        Text(
                            "No time slots added yet. Add time slots or use default slots.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = {
                                defaultTimeSlots.forEach { slot ->
                                    FirestoreDatabase.addTimeSlot(
                                        slot,
                                        onSuccess = {
                                            // Do nothing for individual success
                                        },
                                        onFailure = { exception ->
                                            Log.e("AdminTimetable", "Error adding time slot: $slot", exception)
                                        }
                                    )
                                }
                                Toast.makeText(context, "Adding default time slots...", Toast.LENGTH_SHORT).show()
                                showTimeSlotListDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Default Time Slots (9 AM - 5 PM)")
                        }
                    } else {
                        LazyColumn {
                            items(timeSlots.sortedBy { parseTimeForSorting(it) }) { slot ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(formatTimeSlot(slot))
                                    Row {
                                        IconButton(
                                            onClick = {
                                                selectedTimeSlotToEdit = slot
                                                showTimeSlotListDialog = false
                                                // Show time picker after dialog is closed
                                                showStartTimePicker = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Edit, "Edit")
                                        }
                                        IconButton(
                                            onClick = {
                                                timeSlotToDelete = slot
                                                showDeleteConfirmation = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, "Delete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimeSlotListDialog = false
                        // Show time picker after dialog is closed
                        showStartTimePicker = true
                    }
                ) {
                    Text("Add New")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showTimeSlotListDialog = false
                        // Reset time picker states when closing dialog
                        showStartTimePicker = false
                        showEndTimePicker = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation && timeSlotToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmation = false
                timeSlotToDelete = null
            },
            title = { Text("Delete Time Slot") },
            text = { 
                Text("Are you sure you want to delete the time slot: ${formatTimeSlot(timeSlotToDelete!!)}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        FirestoreDatabase.deleteTimeSlot(
                            timeSlotToDelete!!,
                            onSuccess = {
                                Toast.makeText(context, "Time slot deleted successfully", Toast.LENGTH_SHORT).show()
                                // The listener will update the list automatically
                                showDeleteConfirmation = false
                                timeSlotToDelete = null
                            },
                            onFailure = { exception ->
                                Toast.makeText(context, "Error deleting time slot: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                                Log.e("AdminTimetable", "Error deleting time slot", exception)
                                showDeleteConfirmation = false
                                timeSlotToDelete = null
                            }
                        )
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteConfirmation = false
                        timeSlotToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Subjects list dialog
    if (showSubjectListDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSubjectListDialog = false
                // Reset time picker states when closing dialog
                showStartTimePicker = false
                showEndTimePicker = false
            },
            title = { Text("Manage Subjects") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // List of existing subjects
                    subjects.sorted().forEach { subject ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(subject)
                            Row {
                                IconButton(
                                    onClick = {
                                        selectedSubjectToEdit = subject
                                        newSubject = subject
                                        isEditingSubject = true
                                        showSubjectListDialog = false
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, "Edit")
                                }
                                IconButton(
                                    onClick = {
                                        FirestoreDatabase.deleteSubject(
                                            subject,
                                            onSuccess = {
                                                Toast.makeText(context, "Subject deleted", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = { exception ->
                                                Toast.makeText(context, "Error: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, "Delete")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isEditingSubject = true
                        showSubjectListDialog = false
                    }
                ) {
                    Text("Add New")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showSubjectListDialog = false
                        // Reset time picker states when closing dialog
                        showStartTimePicker = false
                        showEndTimePicker = false
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Timetable entry dialog
    if (showTimetableDialog) {
        var showSubjectDropdown by remember { mutableStateOf(false) }
        var showTeacherDropdown by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showTimetableDialog = false },
            title = { Text(if (editingTimetableId == null) "Add Timetable Entry" else "Edit Timetable Entry") },
            text = {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Subject dropdown
                    ExposedDropdownMenuBox(
                        expanded = showSubjectDropdown,
                        onExpandedChange = { showSubjectDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = dialogSubject,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subject") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubjectDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showSubjectDropdown,
                            onDismissRequest = { showSubjectDropdown = false }
                        ) {
                            subjects.forEach { subject ->
                                DropdownMenuItem(
                                    text = { Text(subject) },
                                    onClick = {
                                        dialogSubject = subject
                                        showSubjectDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Teacher dropdown
                    ExposedDropdownMenuBox(
                        expanded = showTeacherDropdown,
                        onExpandedChange = { showTeacherDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = dialogTeacher,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Teacher") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTeacherDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showTeacherDropdown,
                            onDismissRequest = { showTeacherDropdown = false }
                        ) {
                            teachersList.forEach { teacher ->
                                val fullName = "${teacher.firstName} ${teacher.lastName}"
                                DropdownMenuItem(
                                    text = { Text(fullName) },
                                    onClick = {
                                        dialogTeacher = fullName
                                        showTeacherDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Room number
                    OutlinedTextField(
                        value = dialogRoom,
                        onValueChange = { dialogRoom = it },
                        label = { Text("Room Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (dialogSubject.isNotBlank() && dialogTeacher.isNotBlank()) {
                            val timetable = Timetable(
                                id = editingTimetableId ?: "",
                                classGrade = selectedClass,
                                dayOfWeek = dialogDay,
                                timeSlot = dialogTimeSlot,
                                subject = dialogSubject,
                                teacher = dialogTeacher,
                                roomNumber = dialogRoom,
                                isActive = true
                            )
                            
                            if (editingTimetableId == null) {
                                FirestoreDatabase.addTimetable(
                                    timetable,
                                    onSuccess = {
                                        Toast.makeText(context, "Timetable entry added successfully", Toast.LENGTH_SHORT).show()
                                        showTimetableDialog = false
                                    },
                                    onFailure = { e ->
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                FirestoreDatabase.updateTimetable(
                                    timetable,
                                    onSuccess = {
                                        Toast.makeText(context, "Timetable entry updated successfully", Toast.LENGTH_SHORT).show()
                                        showTimetableDialog = false
                                    },
                                    onFailure = { e ->
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                ) {
                    Text(if (editingTimetableId == null) "Add" else "Update")
                }
            },
            dismissButton = {
                Row {
                    if (editingTimetableId != null) {
                        TextButton(
                            onClick = {
                                FirestoreDatabase.deleteTimetable(
                                    editingTimetableId!!,
                                    onSuccess = {
                                        Toast.makeText(context, "Timetable entry deleted successfully", Toast.LENGTH_SHORT).show()
                                        showTimetableDialog = false
                                    },
                                    onFailure = { e ->
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        ) {
                            Text("Delete")
                        }
                    }
                    TextButton(onClick = { showTimetableDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

private fun getCurrentDay(): String {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return when (currentDay) {
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> "Monday" // Default to Monday if it's Sunday
    }
} 