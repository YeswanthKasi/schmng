package com.ecorvi.schmng.ui.screens.teacher

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecorvi.schmng.models.ClassEvent
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.viewmodel.ClassEventViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassEventManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: ClassEventViewModel = viewModel()
) {
    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var showAddEventDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<ClassEvent?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<ClassEvent?>(null) }
    var showEditConfirmation by remember { mutableStateOf<ClassEvent?>(null) }
    var pendingEditEvent by remember { mutableStateOf<ClassEvent?>(null) }
    
    val currentUser = FirebaseAuth.getInstance().currentUser
    
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { teacherId ->
            viewModel.loadTeacherEvents(teacherId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Class Events") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddEventDialog = true }) {
                        Icon(Icons.Default.Add, "Add Event")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events) { event ->
                        EventCard(
                            event = event,
                            onEdit = { selectedEvent = it },
                            onDelete = { showDeleteConfirmation = it }
                        )
                    }
                }
            }

            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(errorMessage)
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteConfirmation?.let { event ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete the event '${event.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEvent(event)
                        showDeleteConfirmation = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddEventDialog) {
        EventDialog(
            event = null,
            onDismiss = { showAddEventDialog = false },
            onSave = { title, description, date, targetClass, priority, type ->
                currentUser?.uid?.let { teacherId ->
                    viewModel.createEvent(
                        title = title,
                        description = description,
                        eventDate = date,
                        targetClass = targetClass,
                        teacherId = teacherId,
                        priority = priority,
                        type = type
                    )
                }
                showAddEventDialog = false
            }
        )
    }

    selectedEvent?.let { event ->
        EventDialog(
            event = event,
            onDismiss = { selectedEvent = null },
            onSave = { title, description, date, targetClass, priority, type ->
                pendingEditEvent = event.copy(
                    title = title,
                    description = description,
                    eventDate = date,
                    targetClass = targetClass,
                    priority = priority,
                    type = type,
                    updatedAt = System.currentTimeMillis()
                )
                showEditConfirmation = event
                selectedEvent = null
            }
        )
    }

    // Edit Confirmation Dialog
    showEditConfirmation?.let { event ->
        AlertDialog(
            onDismissRequest = { 
                showEditConfirmation = null
                pendingEditEvent = null
            },
            title = { Text("Confirm Edit") },
            text = { Text("Are you sure you want to save changes to '${event.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingEditEvent?.let { viewModel.updateEvent(it) }
                        showEditConfirmation = null
                        pendingEditEvent = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showEditConfirmation = null
                        pendingEditEvent = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EventCard(
    event: ClassEvent,
    onEdit: (ClassEvent) -> Unit,
    onDelete: (ClassEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (event.priority) {
                "urgent" -> Color(0xFFFFEBEE)
                "important" -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = { onEdit(event) }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = { onDelete(event) }) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = event.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Class: ${event.targetClass}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .format(Date(event.eventDate)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDialog(
    event: ClassEvent?,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    var description by remember { mutableStateOf(event?.description ?: "") }
    var priority by remember { mutableStateOf(event?.priority ?: "normal") }
    var type by remember { mutableStateOf(event?.type ?: "general") }
    var eventDate by remember { mutableStateOf(event?.eventDate ?: System.currentTimeMillis()) }
    
    val currentUser = FirebaseAuth.getInstance().currentUser
    var teacherClass by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch teacher's assigned class
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            teacherClass = FirestoreDatabase.getTeacherAssignedClass(currentUser.uid)?.let { className ->
                // Standardize class format to "Class X"
                if (!className.startsWith("Class ")) {
                    "Class $className".replace("st", "").replace("nd", "").replace("rd", "").replace("th", "")
                } else {
                    className
                }
            }
            isLoading = false
        }
    }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = eventDate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (event == null) "Add Event" else "Edit Event") },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Display teacher's assigned class
                    Text(
                        text = "Target Class: ${teacherClass ?: "No class assigned"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, day)
                                        eventDate = calendar.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) {
                            Text("Set Date")
                        }
                        
                        Button(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                        calendar.set(Calendar.MINUTE, minute)
                                        eventDate = calendar.timeInMillis
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            }
                        ) {
                            Text("Set Time")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Selected: ${
                            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                .format(Date(eventDate))
                        }",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Priority")
                            RadioButton(
                                selected = priority == "normal",
                                onClick = { priority = "normal" }
                            )
                            Text("Normal")
                            RadioButton(
                                selected = priority == "important",
                                onClick = { priority = "important" }
                            )
                            Text("Important")
                            RadioButton(
                                selected = priority == "urgent",
                                onClick = { priority = "urgent" }
                            )
                            Text("Urgent")
                        }
                        
                        Column {
                            Text("Type")
                            RadioButton(
                                selected = type == "general",
                                onClick = { type = "general" }
                            )
                            Text("General")
                            RadioButton(
                                selected = type == "exam",
                                onClick = { type = "exam" }
                            )
                            Text("Exam")
                            RadioButton(
                                selected = type == "activity",
                                onClick = { type = "activity" }
                            )
                            Text("Activity")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(title, description, eventDate, teacherClass ?: return@TextButton, priority, type)
                },
                enabled = !isLoading && teacherClass != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 