package com.ecorvi.schmng.ui.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.models.AttendanceRecord
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.UserType
import com.ecorvi.schmng.ui.theme.ParentBlue
import com.ecorvi.schmng.ui.components.ParentBottomNavigation
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.ecorvi.schmng.ui.navigation.ParentBottomNavItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentAttendanceScreen(
    navController: NavController,
    childUid: String,
    showBackButton: Boolean = true,
    currentRoute: String? = null,
    onRouteSelected: ((String) -> Unit)? = null
) {
    val db = FirebaseFirestore.getInstance()
    
    // States
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var attendanceRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var dateRange by remember { mutableStateOf(Pair(getStartOfMonth(), System.currentTimeMillis())) }
    var studentName by remember { mutableStateOf("") }
    var studentClass by remember { mutableStateOf("") }

    // Fetch student info and attendance data
    LaunchedEffect(childUid, dateRange) {
        isLoading = true
        error = null
        
        try {
            // First, get student info
            val studentDoc = db.collection("students")
                .document(childUid)
                .get()
                .await()
                
            if (studentDoc.exists()) {
                studentName = "${studentDoc.getString("firstName") ?: ""} ${studentDoc.getString("lastName") ?: ""}"
                studentClass = studentDoc.getString("className") ?: ""
                
                // Now fetch attendance records
                val records = fetchStudentAttendanceRecords(db, childUid, dateRange)
                attendanceRecords = records
            } else {
                error = "Student profile not found"
            }
        } catch (e: Exception) {
            error = "Failed to load attendance: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Calculate statistics
    val totalDays = attendanceRecords.size
    val presentCount = attendanceRecords.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = attendanceRecords.count { it.status == AttendanceStatus.ABSENT }
    val leaveCount = attendanceRecords.count { it.status == AttendanceStatus.PERMISSION }
    val attendanceRate = if (totalDays > 0) presentCount.toFloat() / totalDays * 100 else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Summary", color = ParentBlue) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ParentBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            ParentBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = { item ->
                    val route = when (item) {
                        ParentBottomNavItem.Home -> "parent_dashboard"
                        else -> "${item.route}/$childUid"
                    }
                    navController.navigate(route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        popUpTo("parent_dashboard") {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        AttendanceContent(
            padding = padding,
            isLoading = isLoading,
            error = error,
            studentName = studentName,
            studentClass = studentClass,
            totalDays = totalDays,
            presentCount = presentCount,
            absentCount = absentCount,
            leaveCount = leaveCount,
            attendanceRate = attendanceRate,
            attendanceRecords = attendanceRecords,
            onDateRangeSelected = { newRange -> dateRange = newRange }
        )
    }
}

@Composable
private fun AttendanceContent(
    padding: PaddingValues,
    isLoading: Boolean,
    error: String?,
    studentName: String,
    studentClass: String,
    totalDays: Int,
    presentCount: Int,
    absentCount: Int,
    leaveCount: Int,
    attendanceRate: Float,
    attendanceRecords: List<AttendanceRecord>,
    onDateRangeSelected: (Pair<Long, Long>) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ParentBlue
                )
            }
            error != null -> {
                Text(
                    text = error,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Student info card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8EEFF)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = studentName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ParentBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Class: $studentClass",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ParentBlue
                            )
                            // Date Range Selector
                            DateRangeSelector(onDateRangeSelected = onDateRangeSelected)
                        }
                    }
                    // Attendance summary card
                    AttendanceSummaryCard(
                        totalDays = totalDays,
                        presentCount = presentCount,
                        absentCount = absentCount,
                        leaveCount = leaveCount,
                        attendanceRate = attendanceRate
                    )
                    // Detailed attendance records
                    AttendanceHistorySection(
                        attendanceRecords = attendanceRecords
                    )
                }
            }
        }
    }
}

@Composable
private fun DateRangeSelector(
    onDateRangeSelected: (Pair<Long, Long>) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Period: ",
                style = MaterialTheme.typography.bodyMedium,
                color = ParentBlue
            )
            
            var expanded by remember { mutableStateOf(false) }
            val options = listOf("This Month", "Last Month", "Last 3 Months", "This Year")
            var selectedOption by remember { mutableStateOf(options[0]) }
            
            Box {
                TextButton(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ParentBlue
                    )
                ) {
                    Text(selectedOption)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Period")
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { 
                                selectedOption = option
                                expanded = false
                                
                                val calendar = Calendar.getInstance()
                                val endDate = calendar.timeInMillis
                                
                                val newRange = when(option) {
                                    "This Month" -> {
                                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                                        Pair(calendar.timeInMillis, endDate)
                                    }
                                    "Last Month" -> {
                                        calendar.add(Calendar.MONTH, -1)
                                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                                        val startDate = calendar.timeInMillis
                                        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                                        Pair(startDate, calendar.timeInMillis)
                                    }
                                    "Last 3 Months" -> {
                                        calendar.add(Calendar.MONTH, -3)
                                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                                        Pair(calendar.timeInMillis, endDate)
                                    }
                                    "This Year" -> {
                                        calendar.set(Calendar.DAY_OF_YEAR, 1)
                                        Pair(calendar.timeInMillis, endDate)
                                    }
                                    else -> Pair(getStartOfMonth(), endDate)
                                }
                                onDateRangeSelected(newRange)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceSummaryCard(
    totalDays: Int,
    presentCount: Int,
    absentCount: Int,
    leaveCount: Int,
    attendanceRate: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Attendance Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ParentBlue,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttendanceStatItem("Total", totalDays, Color.Gray)
                AttendanceStatItem("Present", presentCount, Color(0xFF00C853))
                AttendanceStatItem("Absent", absentCount, Color(0xFFD32F2F))
                AttendanceStatItem("Leave", leaveCount, Color(0xFFFFA000))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress indicator
            LinearProgressIndicator(
                progress = { if (totalDays > 0) presentCount.toFloat() / totalDays else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(8.dp),
                color = Color(0xFF00C853)
            )
            
            Text(
                text = "Attendance Rate: %.1f%%".format(attendanceRate),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
                fontWeight = FontWeight.Bold,
                color = ParentBlue
            )
        }
    }
}

@Composable
private fun AttendanceHistorySection(
    attendanceRecords: List<AttendanceRecord>
) {
    Column {
        Text(
            text = "Attendance History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ParentBlue,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (attendanceRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No attendance records found for the selected period")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Column headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8EEFF))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Date",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        color = ParentBlue
                    )
                    Text(
                        text = "Status",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        color = ParentBlue
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(attendanceRecords.sortedByDescending { it.date }) { record ->
                        AttendanceHistoryItem(record)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceHistoryItem(record: AttendanceRecord) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    
    val date = Date(record.date)
    val dateStr = dateFormat.format(date)
    val dayStr = dayFormat.format(date)
    
    val statusColor = when (record.status) {
        AttendanceStatus.PRESENT -> Color(0xFF00C853)
        AttendanceStatus.ABSENT -> Color(0xFFD32F2F)
        AttendanceStatus.PERMISSION -> Color(0xFFFFA000)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = dayStr,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        
        Text(
            text = record.status.name,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
    
    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
}

@Composable
private fun AttendanceStatItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

// Helper function to get start of current month
private fun getStartOfMonth(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

// Helper to fetch student attendance records
private suspend fun fetchStudentAttendanceRecords(
    db: FirebaseFirestore,
    studentId: String,
    dateRange: Pair<Long, Long>
): List<AttendanceRecord> {
    val records = mutableListOf<AttendanceRecord>()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance()

    try {
        // Validate inputs
        if (studentId.isBlank()) {
            Log.e("Attendance", "Invalid student ID provided")
            return emptyList()
        }

        if (dateRange.first > dateRange.second) {
            Log.e("Attendance", "Invalid date range: start date after end date")
            return emptyList()
        }

        // Limit date range to reasonable bounds (e.g., max 1 year)
        val maxRange = 365L * 24 * 60 * 60 * 1000 // 1 year in milliseconds
        val endDate = minOf(dateRange.second, dateRange.first + maxRange)
        
        calendar.timeInMillis = dateRange.first
        val endCalendar = Calendar.getInstance().apply { timeInMillis = endDate }

        Log.d("Attendance", "Fetching records from ${dateFormat.format(Date(dateRange.first))} to ${dateFormat.format(Date(endDate))}")

        while (!calendar.after(endCalendar)) {
            val currentDate = calendar.timeInMillis
            val dateStr = dateFormat.format(Date(currentDate))
            
            try {
                Log.d("Attendance", "Fetching record for date: $dateStr")
                val snapshot = db.collection("attendance")
                    .document(dateStr)
                    .collection("student")
                    .document(studentId)
                    .get()
                    .await()
                
                if (snapshot.exists()) {
                    try {
                        val record = snapshot.toObject(AttendanceRecord::class.java)
                        if (record != null && record.id.isNotBlank() && record.userId.isNotBlank()) {
                            // Validate and fix record if needed
                            if (record.date <= 0L) {
                                record.date = currentDate
                            }
                            if (record.status !in AttendanceStatus.values()) {
                                record.status = AttendanceStatus.ABSENT
                            }
                            records.add(record)
                            Log.d("Attendance", "Added valid record for date $dateStr")
                        } else {
                            Log.w("Attendance", "Invalid record found for date $dateStr")
                            records.add(createPlaceholderRecord(studentId, currentDate))
                        }
                    } catch (e: Exception) {
                        Log.e("Attendance", "Error deserializing record for date $dateStr: ${e.message}")
                        e.printStackTrace()
                        records.add(createPlaceholderRecord(studentId, currentDate))
                    }
                } else {
                    Log.d("Attendance", "No record found for date $dateStr, using placeholder")
                    records.add(createPlaceholderRecord(studentId, currentDate))
                }
            } catch (e: Exception) {
                Log.e("Attendance", "Error fetching record for date $dateStr: ${e.message}")
                e.printStackTrace()
                records.add(createPlaceholderRecord(studentId, currentDate))
            }
            
            calendar.add(Calendar.DATE, 1)
        }

        Log.d("Attendance", "Successfully fetched ${records.size} records")
    } catch (e: Exception) {
        Log.e("Attendance", "Critical error in fetchStudentAttendanceRecords: ${e.message}")
        e.printStackTrace()
        // If we have no records at all, create at least one placeholder
        if (records.isEmpty()) {
            records.add(createPlaceholderRecord(studentId, dateRange.first))
        }
    }
    
    return records.sortedByDescending { it.date }
}

private fun createPlaceholderRecord(studentId: String, timestamp: Long): AttendanceRecord {
    return AttendanceRecord(
        id = studentId,
        userId = studentId,
        userType = UserType.STUDENT,
        date = timestamp,
        status = AttendanceStatus.ABSENT,
        markedBy = "",
        remarks = "Auto-generated placeholder record",
        lastModified = System.currentTimeMillis()
    )
} 