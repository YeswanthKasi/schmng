package com.ecorvi.schmng.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Timetable
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import java.text.SimpleDateFormat

private fun formatTimeSlot(time: String): String {
    return try {
        val parts = time.split(" - ").map { it.trim() }
        if (parts.size != 2) return time
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
        val timeFormat = if (startTime.contains("AM") || startTime.contains("PM")) {
            SimpleDateFormat("h:mm a", Locale.US)
        } else {
            SimpleDateFormat("H:mm", Locale.US)
        }
        timeFormat.isLenient = false
        val date = timeFormat.parse(startTime) ?: return 0
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    } catch (e: Exception) {
        0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTimetableScreen(navController: NavController) {
    var studentClass by remember { mutableStateOf<String?>(null) }
    var timetables by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var timeSlots by remember { mutableStateOf<List<String>>(emptyList()) }
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Fetch student's class and timetable data
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            try {
                val student = FirestoreDatabase.getStudent(user.uid)
                student?.let {
                    studentClass = it.className
                } ?: run {
                    errorMessage = "Student not found"
                    Toast.makeText(context, "Student not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error fetching student data"
                Toast.makeText(context, "Error fetching student data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fetch time slots from Firestore
    LaunchedEffect(Unit) {
        FirestoreDatabase.fetchTimeSlots(
            onComplete = { slots: List<String> ->
                timeSlots = slots.sortedBy { parseTimeForSorting(it) }
            },
            onFailure = { e: Exception ->
                Log.e("StudentTimetable", "Error fetching time slots: ${e.message}")
                Toast.makeText(context, "Error loading time slots", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Fetch timetables when student class is available
    LaunchedEffect(studentClass) {
        studentClass?.let { classGrade ->
            isLoading = true
            errorMessage = null
            FirestoreDatabase.fetchTimetablesForClass(
                classGrade = classGrade,
                onComplete = { fetchedTimetables: List<Timetable> ->
                    timetables = fetchedTimetables
                    isLoading = false
                },
                onFailure = { e: Exception ->
                    errorMessage = e.message
                    isLoading = false
                    Toast.makeText(context, "Error loading timetable: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Timetable") },
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
                .padding(8.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "An error occurred",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (studentClass == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No class assigned. Please contact your administrator.")
                }
            } else {
                Text(
                    text = "Class: $studentClass",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Grid layout with time slots as rows and days as columns
                LazyColumn {
                    item {
                        // Header row with days
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .border(0.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            // Empty cell for time slot column
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .fillMaxHeight()
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Time",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Day headers
                            daysOfWeek.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Time slot rows
                    items(timeSlots.filter { it.isNotBlank() }.size) { index ->
                        val timeSlot = timeSlots[index]
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
                            daysOfWeek.forEach { day ->
                                val entry = timetables.find {
                                    it.timeSlot.trim().equals(timeSlot.trim(), ignoreCase = true) &&
                                    it.dayOfWeek.trim().equals(day.trim(), ignoreCase = true)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (entry != null) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = entry.subject,
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = entry.teacher,
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = TextAlign.Center
                                            )
                                            if (entry.roomNumber.isNotBlank()) {
                                                Text(
                                                    text = entry.roomNumber,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    textAlign = TextAlign.Center
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
} 