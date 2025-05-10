package com.ecorvi.schmng.ui.screens

import android.content.Context
import android.os.Environment
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.models.AttendanceRecord
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.UserType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ecorvi.schmng.ui.utils.getStartOfMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAttendanceScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // States
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var attendanceRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var dateRange by remember { mutableStateOf<Pair<Long, Long>>(Pair(getStartOfMonth(), System.currentTimeMillis())) }
    var studentName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var studentClass by remember { mutableStateOf("") }

    // Fetch student info and attendance data
    LaunchedEffect(currentUser?.uid, dateRange) {
        isLoading = true
        error = null
        
        if (currentUser?.uid == null) {
            error = "User not authenticated"
            isLoading = false
            return@LaunchedEffect
        }

        try {
            // First, get student info
            val studentDoc = db.collection("students")
                .document(currentUser.uid)
                .get()
                .await()
                
            if (studentDoc.exists()) {
                studentId = studentDoc.id
                studentName = "${studentDoc.getString("firstName") ?: ""} ${studentDoc.getString("lastName") ?: ""}"
                studentClass = studentDoc.getString("className") ?: ""
                
                // Now fetch attendance records for this student
                val records = fetchStudentAttendanceRecords(db, studentId, dateRange)
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

    // Month names for better display
    val monthNames = remember { 
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec") 
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Attendance") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Student info card
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
                        text = studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Class: $studentClass",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Date Range Selector - Simple version
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
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            // Simple dropdown for time period
                            var expanded by remember { mutableStateOf(false) }
                            val options = listOf("This Month", "Last Month", "Last 3 Months", "This Year")
                            var selectedOption by remember { mutableStateOf(options[0]) }
                            
                            Box {
                                TextButton(
                                    onClick = { expanded = true }
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
                                                
                                                // Update date range based on selection
                                                val calendar = Calendar.getInstance()
                                                val endDate = calendar.timeInMillis
                                                
                                                dateRange = when(option) {
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
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Attendance summary card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Attendance Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
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
                        progress = if (totalDays > 0) presentCount.toFloat() / totalDays else 0f,
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
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Detailed attendance records
            Text(
                text = "Attendance History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(error!!, color = Color.Red, modifier = Modifier.padding(16.dp))
            } else if (attendanceRecords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No attendance records found for the selected period")
                }
            } else {
                // Column headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Status", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
fun AttendanceHistoryItem(record: AttendanceRecord) {
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
    
    Divider(color = Color.LightGray.copy(alpha = 0.5f))
}

@Composable
fun AttendanceStatItem(label: String, value: Int, color: Color) {
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

// Helper to fetch student attendance records
suspend fun fetchStudentAttendanceRecords(
    db: FirebaseFirestore,
    studentId: String,
    dateRange: Pair<Long, Long>
): List<AttendanceRecord> {
    val records = mutableListOf<AttendanceRecord>()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateRange.first
    
    while (calendar.timeInMillis <= dateRange.second) {
        val dateStr = dateFormat.format(Date(calendar.timeInMillis))
        val snapshot = db.collection("attendance")
            .document(dateStr)
            .collection("student")
            .document(studentId)
            .get()
            .await()
        
        if (snapshot.exists()) {
            snapshot.toObject(AttendanceRecord::class.java)?.let {
                records.add(it)
            }
        } else {
            // Add a placeholder record for this date with ABSENT status (to show all days)
            val record = AttendanceRecord(
                id = studentId,
                userId = studentId,
                userType = UserType.STUDENT,
                date = calendar.timeInMillis,
                status = AttendanceStatus.ABSENT
            )
            records.add(record)
        }
        
        // Move to next day
        calendar.add(Calendar.DATE, 1)
    }
    
    return records
} 