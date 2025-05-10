package com.ecorvi.schmng.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecorvi.schmng.models.AttendanceRecord
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.User
import com.ecorvi.schmng.models.UserType
import com.ecorvi.schmng.viewmodels.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onNavigateBack: () -> Unit,
    initialUserType: UserType = UserType.STUDENT,
    viewModel: AttendanceViewModel = viewModel()
) {
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedUserType by remember { mutableStateOf(initialUserType) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<() -> Unit>({}) }
    var confirmMessage by remember { mutableStateOf("") }
    
    val users by viewModel.users.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.attendanceRecords.collectAsStateWithLifecycle()
    val pendingChanges by viewModel.pendingChanges.collectAsStateWithLifecycle()
    val isLoading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val presentCount by viewModel.presentCount.collectAsStateWithLifecycle()
    val absentCount by viewModel.absentCount.collectAsStateWithLifecycle()
    val permissionCount by viewModel.permissionCount.collectAsStateWithLifecycle()

    val todayMillis = remember { System.currentTimeMillis() }
    val isFutureDate = selectedDate > todayMillis + 24 * 60 * 60 * 1000L // allow up to end of today
    val dateObj = Date(selectedDate)
    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val dateString = dateFormat.format(dateObj)
    val dayString = dayFormat.format(dateObj)

    LaunchedEffect(selectedDate, selectedUserType) {
        viewModel.loadUsers(selectedUserType)
        viewModel.loadAttendance(selectedDate, selectedUserType)
    }

    fun showConfirmationDialog(message: String, action: () -> Unit) {
        confirmMessage = message
        confirmAction = action
        showConfirmDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Attendance", modifier = Modifier.weight(1f))
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, "Select Date")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = dateString,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = dayString,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (hasUnsavedChanges && !isFutureDate) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Unsaved changes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.resetChanges() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    showConfirmationDialog(
                                        "Save attendance records?"
                                    ) {
                                        viewModel.submitAttendance(selectedUserType)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                enabled = !isLoading,
                                modifier = Modifier.widthIn(min = 120.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(
                                        "Save Attendance",
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // User type selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                UserType.values().forEach { userType ->
                    FilterChip(
                        selected = selectedUserType == userType,
                        onClick = { selectedUserType = userType },
                        label = { Text(userType.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Quick action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        showConfirmationDialog(
                            "Mark all as Present?"
                        ) {
                            viewModel.markAllAttendance(
                                userType = selectedUserType,
                                status = AttendanceStatus.PRESENT
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF00C853)
                    ),
                    enabled = !isLoading && !isFutureDate
                ) {
                    Text("Mark All Present")
                }
                OutlinedButton(
                    onClick = {
                        showConfirmationDialog(
                            "Mark all as Absent?"
                        ) {
                            viewModel.markAllAttendance(
                                userType = selectedUserType,
                                status = AttendanceStatus.ABSENT
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    ),
                    enabled = !isLoading && !isFutureDate
                ) {
                    Text("Mark All Absent")
                }
            }

            // --- Compact summary card ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Today's Summary",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryItem("Total", totalCount.toString(), fontSize = 16.sp)
                        SummaryItem("Present", presentCount.toString(), Color(0xFF00C853), fontSize = 16.sp)
                        SummaryItem("Absent", absentCount.toString(), Color(0xFFD32F2F), fontSize = 16.sp)
                        SummaryItem("Leave", permissionCount.toString(), Color(0xFFFFA000), fontSize = 16.sp)
                    }
                    LinearProgressIndicator(
                        progress = if (totalCount > 0)
                            presentCount.toFloat() / totalCount
                        else 0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(6.dp),
                        color = Color(0xFF00C853)
                    )
                    Text(
                        text = "Attendance Rate: ${
                            if (totalCount > 0)
                                "%.1f%%".format(presentCount.toFloat() / totalCount * 100)
                            else "0%"
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // List of users with attendance marking
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(users) { user ->
                    UserAttendanceItem(
                        user = user,
                        attendanceStatus = pendingChanges[user.id]?.let { status ->
                            status // Use pending change if exists
                        } ?: attendanceRecords[user.id]?.status, // Otherwise use saved record
                        onStatusChange = { status ->
                            if (!isFutureDate) {
                                viewModel.updateAttendance(
                                    userId = user.id,
                                    userType = selectedUserType,
                                    status = status
                                )
                            }
                        },
                        enabled = !isFutureDate
                    )
                }
            }

            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Text(errorMessage)
                }
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                selectedDate = it
                                viewModel.resetChanges()
                                viewModel.loadAttendance(it, selectedUserType)
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                },
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnClickOutside = true
                )
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirm Action") },
                text = { Text(confirmMessage) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmAction()
                            showConfirmDialog = false
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun UserAttendanceItem(
    user: User,
    attendanceStatus: AttendanceStatus?,
    onStatusChange: (AttendanceStatus) -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // User info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when (user.type.lowercase()) {
                            "student" -> "Roll: ${user.rollNumber}"
                            else -> user.mobileNo
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Attendance options
            Row(
                modifier = Modifier.padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttendanceStatus.values().forEach { status ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(40.dp)
                    ) {
                        RadioButton(
                            selected = attendanceStatus == status,
                            onClick = { if (enabled) onStatusChange(status) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = when (status) {
                                    AttendanceStatus.PRESENT -> Color(0xFF00C853)
                                    AttendanceStatus.ABSENT -> Color(0xFFD32F2F)
                                    AttendanceStatus.PERMISSION -> Color(0xFFFFA000)
                                },
                                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.size(20.dp),
                            enabled = enabled
                        )
                        Text(
                            text = when (status) {
                                AttendanceStatus.PRESENT -> "Present"
                                AttendanceStatus.ABSENT -> "Absent"
                                AttendanceStatus.PERMISSION -> "Leave"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (status) {
                                AttendanceStatus.PRESENT -> Color(0xFF00C853)
                                AttendanceStatus.ABSENT -> Color(0xFFD32F2F)
                                AttendanceStatus.PERMISSION -> Color(0xFFFFA000)
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    fontSize: TextUnit = MaterialTheme.typography.titleLarge.fontSize
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = fontSize),
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
} 