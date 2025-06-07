package com.ecorvi.schmng.ui.screens.parent

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
import com.ecorvi.schmng.ui.theme.ParentBlue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentTimetableScreen(
    navController: NavController,
    childUid: String
) {
    var studentClass by remember { mutableStateOf<String?>(null) }
    var studentName by remember { mutableStateOf<String?>(null) }
    var timetables by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var timeSlots by remember { mutableStateOf<List<String>>(emptyList()) }
    val currentDay = getCurrentDay()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
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

    // Fetch student's class and timetable data
    LaunchedEffect(childUid) {
        try {
            // First get student data from Firestore directly
            val studentDoc = FirebaseFirestore.getInstance()
                .collection("students")
                .document(childUid)
                .get()
                .await()

            if (studentDoc.exists()) {
                val student = studentDoc.toObject(com.ecorvi.schmng.ui.data.model.Person::class.java)
                student?.let {
                    studentClass = it.className
                    studentName = "${it.firstName} ${it.lastName}"
                    Log.d("ParentTimetable", "Student class: ${it.className}")
                    
                    // Then fetch timetable data
                    FirestoreDatabase.fetchTimetablesForClass(
                        classGrade = it.className,
                        onComplete = { fetchedTimetables ->
                            timetables = fetchedTimetables.sortedBy { timetable -> 
                                parseTimeForSorting(timetable.timeSlot)
                            }
                            isLoading = false
                        },
                        onFailure = { e ->
                            Log.e("ParentTimetable", "Error fetching timetable: ${e.message}")
                            errorMessage = "Failed to load timetable: ${e.message}"
                            isLoading = false
                            Toast.makeText(context, "Error loading timetable: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                } ?: run {
                    errorMessage = "Student data not found"
                    isLoading = false
                    Toast.makeText(context, "Student data not found", Toast.LENGTH_SHORT).show()
                }
            } else {
                errorMessage = "Student not found"
                isLoading = false
                Toast.makeText(context, "Student not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Error fetching student data"
            isLoading = false
            Toast.makeText(context, "Error fetching student data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Fetch time slots from Firestore
    LaunchedEffect(Unit) {
        FirestoreDatabase.fetchTimeSlots(
            onComplete = { slots: List<String> ->
                timeSlots = slots.sortedBy { parseTimeForSorting(it) }
            },
            onFailure = { e: Exception ->
                Log.e("ParentTimetable", "Error fetching time slots: ${e.message}")
                Toast.makeText(context, "Error loading time slots", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Child's Timetable", color = ParentBlue) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = ParentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
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
                    CircularProgressIndicator(color = ParentBlue)
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
                // Student info and today's highlight
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8EEFF)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        studentName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ParentBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = "Class: $studentClass",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ParentBlue
                        )
                        Text(
                            text = "Today: ${daysOfWeek[currentDay] ?: currentDay}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ParentBlue
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
                                .background(Color(0xFFE8EEFF))
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
                                                ParentBlue.copy(alpha = 0.1f)
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
                                                ParentBlue.copy(alpha = 0.1f)
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
                                                color = ParentBlue
                                            )
                                            if (entry.teacher.isNotBlank()) {
                                                Text(
                                                    text = entry.teacher,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            if (entry.roomNumber.isNotBlank()) {
                                                Text(
                                                    text = entry.roomNumber,
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
            }
        }
    }
}

private fun getCurrentDay(): String {
    return SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
}

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
        
        "${outputFormat.format(startTime)}\n${outputFormat.format(endTime)}"
    } catch (e: Exception) {
        time
    }
}

private fun parseTimeForSorting(timeSlot: String): Int {
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