package com.ecorvi.schmng.ui.screens.teacher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.UserType
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.viewmodels.AttendanceViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TeacherAttendanceScreen(
    navController: NavController,
    viewModel: AttendanceViewModel
) {
    val users by viewModel.users.collectAsState()
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Calculate attendance statistics
    val totalStudents = users.size
    val presentCount = attendanceRecords.values.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = attendanceRecords.values.count { it.status == AttendanceStatus.ABSENT }
    val permissionCount = attendanceRecords.values.count { it.status == AttendanceStatus.PERMISSION }
    val attendanceRate = if (totalStudents > 0) (presentCount.toFloat() / totalStudents) * 100 else 0f

    // Always load student attendance
    LaunchedEffect(selectedDate) {
        viewModel.loadUsers(UserType.STUDENT)
        viewModel.loadAttendance(
            selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            UserType.STUDENT
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Student Attendance")
                        Text(
                            selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, "Select Date")
                    }
                    if (viewModel.hasUnsavedChanges.collectAsState().value) {
                        IconButton(
                            onClick = { viewModel.submitAttendance(UserType.STUDENT) }
                        ) {
                            Icon(Icons.Default.Save, "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        CommonBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Quick Actions Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionButton(
                                text = "All Present",
                                icon = Icons.Default.CheckCircle,
                                color = MaterialTheme.colorScheme.tertiary,
                                onClick = {
                                    users.forEach { student ->
                                        viewModel.updateAttendance(
                                            student.id,
                                            UserType.STUDENT,
                                            AttendanceStatus.PRESENT
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                text = "All Absent",
                                icon = Icons.Default.Cancel,
                                color = MaterialTheme.colorScheme.error,
                                onClick = {
                                    users.forEach { student ->
                                        viewModel.updateAttendance(
                                            student.id,
                                            UserType.STUDENT,
                                            AttendanceStatus.ABSENT
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Summary Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatChip(
                        label = "Present",
                        count = presentCount,
                        total = totalStudents,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    StatChip(
                        label = "Absent",
                        count = absentCount,
                        total = totalStudents,
                        color = MaterialTheme.colorScheme.error
                    )
                    StatChip(
                        label = "Leave",
                        count = permissionCount,
                        total = totalStudents,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Content
                AnimatedContent(
                    targetState = loading,
                    transitionSpec = {
                        fadeIn() with fadeOut()
                    }
                ) { isLoading ->
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        error != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(error ?: "")
                                    Button(onClick = { 
                                        viewModel.loadUsers(UserType.STUDENT)
                                        viewModel.loadAttendance(
                                            selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                            UserType.STUDENT
                                        )
                                    }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = users,
                                    key = { it.id }
                                ) { student ->
                                    val attendance = attendanceRecords[student.id]
                                    StudentAttendanceCard(
                                        studentName = "${student.firstName} ${student.lastName}",
                                        rollNumber = student.rollNumber ?: "",
                                        currentStatus = attendance?.status,
                                        onStatusChange = { newStatus ->
                                            viewModel.updateAttendance(
                                                student.id,
                                                UserType.STUDENT,
                                                newStatus
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = rememberDatePickerState(
                    initialSelectedDateMillis = selectedDate
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                ),
                showModeToggle = false
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
    }
}

@Composable
private fun StudentAttendanceCard(
    studentName: String,
    rollNumber: String,
    currentStatus: AttendanceStatus?,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (currentStatus) {
                AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                AttendanceStatus.PERMISSION -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                null -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = studentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Roll No: $rollNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusIconButton(
                    status = AttendanceStatus.PRESENT,
                    currentStatus = currentStatus,
                    onStatusChange = onStatusChange,
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatusIconButton(
                    status = AttendanceStatus.ABSENT,
                    currentStatus = currentStatus,
                    onStatusChange = onStatusChange,
                    color = MaterialTheme.colorScheme.error
                )
                StatusIconButton(
                    status = AttendanceStatus.PERMISSION,
                    currentStatus = currentStatus,
                    onStatusChange = onStatusChange,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun StatusIconButton(
    status: AttendanceStatus,
    currentStatus: AttendanceStatus?,
    onStatusChange: (AttendanceStatus) -> Unit,
    color: Color
) {
    val selected = status == currentStatus
    IconButton(
        onClick = { onStatusChange(status) }
    ) {
        Icon(
            imageVector = when (status) {
                AttendanceStatus.PRESENT -> Icons.Default.CheckCircle
                AttendanceStatus.ABSENT -> Icons.Default.Cancel
                AttendanceStatus.PERMISSION -> Icons.Default.Timer
            },
            contentDescription = status.name,
            tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
} 