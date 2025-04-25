package com.ecorvi.schmng.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Timetable
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTimetableScreen(navController: NavController) {
    var studentClass by remember { mutableStateOf<String?>(null) }
    var selectedDay by remember { mutableStateOf(getCurrentDay()) }
    var timetables by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Get student's class when screen loads
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            FirestoreDatabase.getStudent(currentUser.uid)?.let { student ->
                studentClass = student.className
                if (studentClass.isNullOrEmpty()) {
                    Toast.makeText(context, "Class information not found", Toast.LENGTH_LONG).show()
                    navController.navigateUp()
                }
            } ?: run {
                Toast.makeText(context, "Student information not found", Toast.LENGTH_LONG).show()
                navController.navigateUp()
            }
        } else {
            Toast.makeText(context, "Please log in to continue", Toast.LENGTH_LONG).show()
            navController.navigate("login")
        }
    }

    // Fetch timetables when student's class is loaded or day changes
    LaunchedEffect(studentClass, selectedDay) {
        if (!studentClass.isNullOrEmpty()) {
            isLoading = true
            errorMessage = null
            
            FirestoreDatabase.fetchTimetablesForClass(
                classGrade = studentClass!!,
                onComplete = { fetchedTimetables ->
                    timetables = fetchedTimetables
                        .filter { it.dayOfWeek == selectedDay }
                        .sortedBy { it.timeSlot }
                    isLoading = false
                },
                onFailure = { e ->
                    errorMessage = e.message
                    isLoading = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Class Schedule") },
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
            // Day Selection
            Text(
                "Select Day",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")) { day ->
                    FilterChip(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        label = { Text(day) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Class Information
            studentClass?.let {
                Text(
                    "Class: $it",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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
                    Text("No classes scheduled for $selectedDay")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(timetables) { timetable ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
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