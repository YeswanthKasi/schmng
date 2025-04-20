package com.ecorvi.schmng.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.components.ScheduleListItem
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Schedule
import com.google.firebase.firestore.ListenerRegistration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(navController: NavController) {
    var schedules by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf<Schedule?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var listener: ListenerRegistration? by remember { mutableStateOf(null) }

    // Load schedules
    LaunchedEffect(Unit) {
        try {
            listener = FirestoreDatabase.listenForScheduleUpdates(
                onUpdate = { fetchedSchedules ->
                    schedules = fetchedSchedules
                    isLoading = false
                    errorMessage = null
                },
                onError = { exception ->
                    isLoading = false
                    errorMessage = exception.message
                    Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            isLoading = false
            errorMessage = e.message
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Cleanup listener
    DisposableEffect(Unit) {
        onDispose {
            listener?.remove()
        }
    }

    CommonBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Schedules",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { 
                        try {
                            navController.navigate("add_schedule")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    containerColor = Color(0xFF1F41BB)
                ) {
                    Icon(Icons.Default.Add, "Add Schedule", tint = Color.White)
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF1F41BB))
                        }
                    }
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Error: $errorMessage",
                                color = Color.Red,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    schedules.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No schedules found",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color.Gray
                                    )
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = schedules,
                                key = { it.id }
                            ) { schedule ->
                                ScheduleListItem(
                                    schedule = schedule,
                                    onDeleteClick = { showDeleteDialog = schedule }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        showDeleteDialog?.let { schedule ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { 
                    Text(
                        "Delete Schedule",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = { 
                    Text(
                        "Are you sure you want to delete '${schedule.title}'? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            FirestoreDatabase.deleteSchedule(
                                schedule.id,
                                onSuccess = {
                                    Toast.makeText(context, "Schedule deleted successfully", Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = null
                                },
                                onFailure = { e ->
                                    Toast.makeText(context, "Failed to delete schedule: ${e.message}", Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = null
                                }
                            )
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Red
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
