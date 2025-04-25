package com.ecorvi.schmng.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Timetable
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableViewScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    var selectedClass by remember { mutableStateOf("1") }
    var timetables by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Time slots from 8 AM to 5 PM (24-hour format)
    val timeSlots = remember {
        (8..17).map { hour ->
            String.format("%02d:00", hour)
        }
    }

    // Days of the week (Strings)
    val daysOfWeek = remember {
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    }

    LaunchedEffect(selectedClass) {
        isLoading = true
        FirestoreDatabase.fetchTimetablesForClass(
            classGrade = selectedClass, 
            onComplete = { result ->
                timetables = result.sortedBy { it.timeSlot }
                isLoading = false
            },
            onFailure = { e ->
                Log.e("TimetableView", "Error fetching timetables: ${e.message}")
                isLoading = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Class $selectedClass Timetable") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("timetable_management") }) {
                        Icon(Icons.Default.Edit, "Edit Timetable")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Class selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Class:", style = MaterialTheme.typography.titleMedium)
                DropdownMenu(
                    expanded = false,
                    onDismissRequest = { },
                    modifier = Modifier.width(120.dp)
                ) {
                    (1..10).forEach { classNum ->
                        DropdownMenuItem(
                            text = { Text("Class $classNum") },
                            onClick = { selectedClass = classNum.toString() }
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Timetable table
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header row with days
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(8.dp)
                        ) {
                            // Time column header
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Time",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            // Day columns
                            daysOfWeek.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        day.substring(0, 3),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }

                    // Time slots rows
                    items(timeSlots) { timeSlot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Gray)
                                .padding(4.dp)
                        ) {
                            // Time column
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    timeSlot,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Day columns
                            daysOfWeek.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val timetableEntry = timetables.find {
                                        it.dayOfWeek == day &&
                                        it.timeSlot.startsWith(timeSlot)
                                    }
                                    
                                    if (timetableEntry != null) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                timetableEntry.subject,
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "Room ${timetableEntry.roomNumber}",
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