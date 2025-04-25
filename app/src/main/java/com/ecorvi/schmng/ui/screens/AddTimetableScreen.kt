package com.ecorvi.schmng.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Timetable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.*
import android.util.Log
import com.ecorvi.schmng.ui.utils.Constants
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimetableScreen(navController: NavController, timetableId: String? = null) {
    val isEditMode = timetableId != null
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(isEditMode) }
    val coroutineScope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var isAdmin by remember { mutableStateOf(false) }

    // Form state
    var classGrade by remember { mutableStateOf("1st") }
    var dayOfWeek by remember { mutableStateOf("Monday") }
    var timeSlot by remember { mutableStateOf("08:00") }
    var subject by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var roomNumber by remember { mutableStateOf("") }
    
    // Dropdown states
    var showClassDropdown by remember { mutableStateOf(false) }
    var showDayDropdown by remember { mutableStateOf(false) }
    var showTimeDropdown by remember { mutableStateOf(false) }

    // Check if user is admin
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            FirestoreDatabase.getUserRole(
                userId = currentUser.uid,
                onComplete = { role ->
                    isAdmin = role?.lowercase() == "admin"
                    if (!isAdmin) {
                        Toast.makeText(context, "Only administrators can manage timetables", Toast.LENGTH_LONG).show()
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

    // Constants
    val classes = listOf("1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th")
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val timeSlots = (8..17).map { hour -> String.format("%02d:00", hour) }

    // Load existing timetable data if in edit mode
    LaunchedEffect(timetableId) {
        if (isEditMode && timetableId != null) {
            FirestoreDatabase.getTimetableById(
                timetableId = timetableId,
                onComplete = { timetable ->
                    if (timetable != null) {
                        classGrade = timetable.classGrade
                        dayOfWeek = timetable.dayOfWeek
                        timeSlot = timetable.timeSlot
                        subject = timetable.subject
                        teacher = timetable.teacher
                        roomNumber = timetable.roomNumber
                    }
                    isLoading = false
                },
                onFailure = { e ->
                    Toast.makeText(context, "Failed to load data: ${e.message}", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
            )
        }
    }

    fun validateForm(): Boolean {
        if (!isAdmin) {
            Toast.makeText(context, "Only administrators can perform this action", Toast.LENGTH_LONG).show()
            return false
        }
        return subject.isNotBlank() && teacher.isNotBlank() && roomNumber.isNotBlank()
    }

    fun handleSubmit() {
        if (!validateForm()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val timetable = Timetable(
            id = timetableId ?: "",
            classGrade = classGrade,
            dayOfWeek = dayOfWeek,
            timeSlot = timeSlot,
            subject = subject.trim(),
            teacher = teacher.trim(),
            roomNumber = roomNumber.trim(),
            isActive = true
        )

        isLoading = true
        if (isEditMode) {
            FirestoreDatabase.updateTimetable(
                timetable = timetable,
                onSuccess = {
                    Toast.makeText(context, "Timetable updated successfully", Toast.LENGTH_SHORT).show()
                    navController.navigateUp()
                },
                onFailure = { e ->
                    Toast.makeText(context, "Error updating timetable: ${e.message}", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
            )
        } else {
            FirestoreDatabase.addTimetable(
                timetable = timetable,
                onSuccess = {
                    Toast.makeText(context, "Timetable added successfully", Toast.LENGTH_SHORT).show()
                    navController.navigateUp()
                },
                onFailure = { e ->
                    Toast.makeText(context, "Error adding timetable: ${e.message}", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
            )
        }
    }

    CommonBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isEditMode) "Edit Timetable Entry" else "Add Timetable Entry") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { padding ->
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Class Selection
                    ExposedDropdownMenuBox(
                        expanded = showClassDropdown,
                        onExpandedChange = { showClassDropdown = !showClassDropdown }
                    ) {
                        OutlinedTextField(
                            value = classGrade,
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
                            classes.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        classGrade = option
                                        showClassDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Day Selection
                    ExposedDropdownMenuBox(
                        expanded = showDayDropdown,
                        onExpandedChange = { showDayDropdown = !showDayDropdown }
                    ) {
                        OutlinedTextField(
                            value = dayOfWeek,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Day") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDayDropdown)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showDayDropdown,
                            onDismissRequest = { showDayDropdown = false }
                        ) {
                            days.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        dayOfWeek = option
                                        showDayDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Time Selection
                    ExposedDropdownMenuBox(
                        expanded = showTimeDropdown,
                        onExpandedChange = { showTimeDropdown = !showTimeDropdown }
                    ) {
                        OutlinedTextField(
                            value = timeSlot,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Time") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTimeDropdown)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showTimeDropdown,
                            onDismissRequest = { showTimeDropdown = false }
                        ) {
                            timeSlots.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        timeSlot = option
                                        showTimeDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Subject Input
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Teacher Input
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Teacher") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Room Number Input
                    OutlinedTextField(
                        value = roomNumber,
                        onValueChange = { roomNumber = it },
                        label = { Text("Room Number") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Submit Button
                    Button(
                        onClick = { handleSubmit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(if (isEditMode) "Update Timetable" else "Add Timetable")
                    }
                }
            }
        }
    }
} 