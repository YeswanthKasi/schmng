package com.ecorvi.schmng.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Today
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
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat

private val PrimaryBlue = Color(0xFF1F41BB)

private fun formatTimeSlot(time: String): String {
    return try {
        val parts = time.split("-").map { it.trim() }
        if (parts.size != 2) return time
        
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        val outputFormat = SimpleDateFormat("h:mm a", Locale.US)
        timeFormat.isLenient = false
        
        val startTime = timeFormat.parse(parts[0])
        val endTime = timeFormat.parse(parts[1])
        
        if (startTime == null || endTime == null) return time
        
        "${outputFormat.format(startTime)} - ${outputFormat.format(endTime)}"
    } catch (e: Exception) {
        time
    }
}

private fun parseTimeSlot(timeSlot: String): Int {
    return try {
        val startTime = timeSlot.split("-")[0].trim()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
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
fun TeacherTimetableScreen(navController: NavController) {
    val context = LocalContext.current
    var timetables by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var teacherName by remember { mutableStateOf<String?>(null) }
    var timeSlots by remember { mutableStateOf<List<String>>(emptyList()) }
    val currentDay = getCurrentDay()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser

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

    // Fetch time slots from Firestore
    LaunchedEffect(Unit) {
        FirestoreDatabase.fetchTimeSlots(
            onComplete = { slots ->
                timeSlots = slots.sortedBy { parseTimeSlot(it) }
            },
            onFailure = { e ->
                Log.e("TeacherTimetable", "Error fetching time slots: ${e.message}")
                errorMessage = "Failed to load time slots: ${e.message}"
            }
        )
    }

    // Fetch teacher information and timetables
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
                                // Standardize class format to "Class X"
                                timetables = fetchedTimetables.map { timetable ->
                                    if (!timetable.classGrade.startsWith("Class ")) {
                                        timetable.copy(
                                            classGrade = "Class ${timetable.classGrade.replace("st", "").replace("nd", "").replace("rd", "").replace("th", "")}"
                                        )
                                    } else {
                                        timetable
                                    }
                                }.sortedBy { parseTimeSlot(it.timeSlot) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Timetable") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Jump to today button
                    IconButton(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Today, "Jump to Today")
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
            if (isLoading || timeSlots.isEmpty()) {
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
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // Teacher info and current day
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = teacherName ?: "",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Today: ${daysOfWeek[currentDay] ?: currentDay}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Grid layout with time slots as rows and days as columns
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        // Header row with days
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer)
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
                            daysOfWeek.forEach { (fullDay, shortDay) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                        .background(
                                            if (fullDay == currentDay)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                Color.Transparent
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shortDay,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Time slot rows
                    items(timeSlots.size) { index ->
                        val timeSlot = timeSlots[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
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
                                Text(
                                    text = formatTimeSlot(timeSlot),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Day columns
                            daysOfWeek.forEach { (fullDay, _) ->
                                val entry = timetables.find {
                                    it.timeSlot.trim().equals(timeSlot.trim(), ignoreCase = true) &&
                                    it.dayOfWeek.trim().equals(fullDay.trim(), ignoreCase = true)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                        .background(
                                            if (fullDay == currentDay)
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                            else
                                                Color.Transparent
                                        )
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (entry != null) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                Text(
                                                text = entry.subject,
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                                text = entry.classGrade,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                textAlign = TextAlign.Center
                                            )
                                            if (entry.roomNumber.isNotBlank()) {
                                                Text(
                                                    text = "Room ${entry.roomNumber}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.secondary,
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

                // Quick Tips Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Quick Tips",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Current day's schedule is highlighted",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Tap the calendar icon to jump to today's schedule",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Room numbers are shown for each class",
                            style = MaterialTheme.typography.bodySmall
                        )
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