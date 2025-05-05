package com.ecorvi.schmng.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Schedule
import com.ecorvi.schmng.ui.utils.Constants
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleScreen(navController: NavController, scheduleId: String?) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(Constants.CLASS_OPTIONS.first()) }
    var selectedRecipientType by remember { mutableStateOf(Constants.RECIPIENT_TYPES.first()) }
    var status by remember { mutableStateOf("Active") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showClassDropdown by remember { mutableStateOf(false) }
    var showRecipientTypeDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }

    // Load schedule data if in edit mode
    LaunchedEffect(scheduleId) {
        if (scheduleId != null) {
            isLoading = true
            FirestoreDatabase.getScheduleById(
                scheduleId = scheduleId,
                onComplete = { schedule ->
                    if (schedule != null) {
                        title = schedule.title
                        description = schedule.description
                        date = schedule.date
                        time = schedule.time
                        selectedClass = schedule.className
                        selectedRecipientType = schedule.recipientType
                        status = schedule.status
                    }
                    isLoading = false
                },
                onFailure = { e ->
                    Toast.makeText(context, "Error loading schedule: ${e.message}", Toast.LENGTH_SHORT).show()
                    isLoading = false
                    navController.navigateUp()
                }
            )
        }
    }

    CommonBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            if (scheduleId != null) "Edit Schedule" else "Add Schedule",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
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
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (DD/MM/YYYY)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (HH:MM)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Class Dropdown
                ExposedDropdownMenuBox(
                    expanded = showClassDropdown,
                    onExpandedChange = { showClassDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedClass,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Class") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showClassDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showClassDropdown,
                        onDismissRequest = { showClassDropdown = false }
                    ) {
                        Constants.CLASS_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedClass = option
                                    showClassDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recipient Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = showRecipientTypeDropdown,
                    onExpandedChange = { showRecipientTypeDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedRecipientType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Recipient Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRecipientTypeDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showRecipientTypeDropdown,
                        onDismissRequest = { showRecipientTypeDropdown = false }
                    ) {
                        Constants.RECIPIENT_TYPES.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedRecipientType = option
                                    showRecipientTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = showStatusDropdown,
                    onExpandedChange = { showStatusDropdown = it }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showStatusDropdown,
                        onDismissRequest = { showStatusDropdown = false }
                    ) {
                        listOf("Active", "Pending", "Completed", "Cancelled").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    status = option
                                    showStatusDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (title.isBlank() || description.isBlank() || date.isBlank() || time.isBlank()) {
                            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true
                        val schedule = Schedule(
                            id = scheduleId ?: "",
                            title = title,
                            description = description,
                            date = date,
                            time = time,
                            className = selectedClass,
                            recipientType = selectedRecipientType,
                            status = status
                        )

                        if (scheduleId != null) {
                            // Update existing schedule
                            FirestoreDatabase.updateSchedule(
                                schedule = schedule,
                                onSuccess = {
                                    isLoading = false
                                    Toast.makeText(context, "Schedule updated successfully", Toast.LENGTH_SHORT).show()
                                    navController.navigateUp()
                                },
                                onFailure = { e ->
                                    isLoading = false
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            // Add new schedule
                            FirestoreDatabase.addSchedule(
                                schedule = schedule,
                                onSuccess = {
                                    isLoading = false
                                    Toast.makeText(context, "Schedule added successfully", Toast.LENGTH_SHORT).show()
                                    navController.navigateUp()
                                },
                                onFailure = { e ->
                                    isLoading = false
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(if (scheduleId != null) "Update Schedule" else "Add Schedule")
                    }
                }
            }
        }
    }
} 