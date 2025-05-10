package com.ecorvi.schmng.ui.screens

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.roundToInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import com.ecorvi.schmng.ui.utils.getStartOfMonth
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.components.AttendancePieChart

// Color definitions
val colorPresent = Color(0xFF00C853)
val colorAbsent = Color(0xFFD32F2F)
val colorLeave = Color(0xFFFFA000)
val colorAccent = Color(0xFF003f87)
val colorSecondary = Color(0xFFa8c7e6)
val colorOrange = Color(0xFFFF7F00)
val colorDeepBlue = Color(0xFF00214c)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceAnalyticsScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    
    // State variables
    var selectedUserType by remember { mutableStateOf(UserType.STUDENT) }
    var classList by remember { mutableStateOf(listOf<String>()) }
    var selectedClass by remember { mutableStateOf<String?>(null) }
    var teacherCount by remember { mutableStateOf(0) }
    var staffCount by remember { mutableStateOf(0) }
    var studentCount by remember { mutableStateOf(0) }
    
    // Set default to today's date
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayEnd = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
    
    var dateRange by remember { mutableStateOf<Pair<Long, Long>>(Pair(today, todayEnd)) }
    var filterType by remember { mutableStateOf("Day") } // Default to "Day"
    var isDateDialogOpen by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var attendanceRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var userMap by remember { mutableStateOf<Map<String, Person>>(emptyMap()) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showErrorSnackbar by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    // Check user role and redirect if needed
    LaunchedEffect(currentUser?.uid) {
        try {
            if (currentUser?.uid != null) {
                val userDoc = db.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                val userRole = userDoc.getString("role")?.lowercase()
                if (userRole == "student") {
                    navController.navigate("student_attendance") {
                        popUpTo("attendance_analytics") { inclusive = true }
                    }
                }
            }
        } catch (e: Exception) {
            errorMessage = "Error checking user role: ${e.message}"
            showErrorSnackbar = true
        }
    }

    // Fetch classes for filter
    LaunchedEffect(selectedUserType) {
        try {
            isLoading = true
            val collection = when (selectedUserType) {
                UserType.STUDENT -> FirestoreDatabase.studentsCollection
                UserType.TEACHER -> FirestoreDatabase.teachersCollection
                UserType.STAFF -> FirestoreDatabase.staffCollection
            }
            val snapshot = collection.get().await()
            classList = snapshot.documents
                .mapNotNull { it.getString("className") }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            selectedClass = null
        } catch (e: Exception) {
            errorMessage = "Error loading classes: ${e.message}"
            showErrorSnackbar = true
        } finally {
            isLoading = false
        }
    }

    // Fetch teacher and staff counts
    LaunchedEffect(selectedUserType) {
        try {
            when (selectedUserType) {
                UserType.TEACHER -> {
                    FirestoreDatabase.fetchTeacherCount { count ->
                        teacherCount = count
                    }
                }
                UserType.STAFF -> {
                    FirestoreDatabase.fetchStaffCount { count ->
                        staffCount = count
                    }
                }
                else -> { /* No need to fetch counts for students */ }
            }
        } catch (e: Exception) {
            errorMessage = "Failed to fetch counts: ${e.message}"
            showErrorSnackbar = true
        }
    }

    // Fetch student count
    LaunchedEffect(selectedUserType) {
        try {
            when (selectedUserType) {
                UserType.STUDENT -> {
                    FirestoreDatabase.fetchStudentCount { count ->
                        studentCount = count
                    }
                }
                UserType.TEACHER, UserType.STAFF -> { /* No need to fetch student count for teachers or staff */ }
            }
        } catch (e: Exception) {
            errorMessage = "Failed to fetch student count: ${e.message}"
            showErrorSnackbar = true
        }
    }

    // Fetch attendance data
    LaunchedEffect(selectedUserType, dateRange, selectedClass) {
        try {
            isLoading = true
            error = null
            
            // Fetch attendance records
            val records = fetchAttendanceRecords(db, selectedUserType, dateRange)
            
            // Fetch user details
            val userIds = records.map { it.userId }.toSet()
            if (userIds.isNotEmpty()) {
                val collection = when (selectedUserType) {
                    UserType.STUDENT -> FirestoreDatabase.studentsCollection
                    UserType.TEACHER -> FirestoreDatabase.teachersCollection
                    UserType.STAFF -> FirestoreDatabase.staffCollection
                }
                
                val userDocs = collection
                    .whereIn("id", userIds.toList().take(10)) // Firestore limit
                    .get()
                    .await()
                
                val userMapFetched = userDocs.documents.associate { doc ->
                    doc.id to (doc.toObject(Person::class.java) ?: Person(id = doc.id))
                }
                userMap = userMapFetched
            }
            
            // Filter records by class
            attendanceRecords = records.filter { 
                selectedClass == null || userMap[it.userId]?.className == selectedClass 
            }
        } catch (e: Exception) {
            error = "Failed to load attendance: ${e.message}"
            errorMessage = error ?: "Unknown error occurred"
            showErrorSnackbar = true
        } finally {
            isLoading = false
        }
    }

    // --- Data Aggregation ---
    // Build a list of unique days in the range
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val daysInRange = mutableListOf<String>()
    run {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateRange.first
        while (cal.timeInMillis <= dateRange.second) {
            daysInRange.add(dayFormat.format(Date(cal.timeInMillis)))
            cal.add(Calendar.DATE, 1)
        }
    }
    // Map: day -> list of records for that day
    val recordsByDay = daysInRange.associateWith { day ->
        attendanceRecords.filter { dayFormat.format(Date(it.date)) == day }
    }
    // For summary: unique students in range
    val uniqueStudentIds = attendanceRecords.map { it.userId }.distinct()
    val uniqueStudentCount = uniqueStudentIds.size
    // For each day, count unique students per status
    val trendData = daysInRange.map { day ->
        val recs = recordsByDay[day] ?: emptyList()
        val present = recs.filter { it.status == AttendanceStatus.PRESENT }.map { it.userId }.distinct().count()
        val absent = recs.filter { it.status == AttendanceStatus.ABSENT }.map { it.userId }.distinct().count()
        val leave = recs.filter { it.status == AttendanceStatus.PERMISSION }.map { it.userId }.distinct().count()
        Triple(present, absent, leave)
    }
    // For summary: total present/absent/leave (unique students)
    val presentCount = attendanceRecords.filter { it.status == AttendanceStatus.PRESENT }.map { it.userId }.distinct().count()
    val absentCount = attendanceRecords.filter { it.status == AttendanceStatus.ABSENT }.map { it.userId }.distinct().count()
    val leaveCount = attendanceRecords.filter { it.status == AttendanceStatus.PERMISSION }.map { it.userId }.distinct().count()
    val totalCount = uniqueStudentCount
    val attendanceRate = if (totalCount > 0) presentCount.toFloat() / totalCount * 100 else 0f
    // Top absentees (by userId)
    val absenteeMap = attendanceRecords.filter { it.status == AttendanceStatus.ABSENT }
        .groupBy { it.userId }
        .mapValues { it.value.map { rec -> dayFormat.format(Date(rec.date)) }.distinct().count() }
        .toList().sortedByDescending { it.second }.take(5)

    val colorCard = Color(0xFFF5F8FF)

    // Date filtering options
    val filterOptions = listOf("Day", "Month")
    
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(
                            onClick = { data.dismiss() }
                        ) {
                            Text("Dismiss", color = Color.White)
                        }
                    }
                ) {
                    Text(data.visuals.message)
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Attendance Analytics") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showExportDialog = true },
                        enabled = !isLoading && attendanceRecords.isNotEmpty()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorAccent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("attendance/${selectedUserType.name}") },
                containerColor = colorAccent
            ) {
                Icon(Icons.Default.Add, contentDescription = "Take Attendance", tint = Color.White)
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorAccent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF5F8FF))
            ) {
                // Filters section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // User type selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            UserType.values().forEach { userType ->
                                FilterChip(
                                    selected = selectedUserType == userType,
                                    onClick = { selectedUserType = userType },
                                    label = { Text(userType.name, fontSize = 12.sp) },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                        
                        // Filters row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Class filter
                            if (classList.isNotEmpty()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    DropdownMenuBox(
                                        label = "Class",
                                        options = classList,
                                        selectedOption = selectedClass,
                                        onOptionSelected = { selectedClass = it }
                                    )
                                }
                            }
                            
                            // Filter type selector
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filterOptions.forEach { option ->
                                    FilterChip(
                                        selected = filterType == option,
                                        onClick = { 
                                            filterType = option
                                            // Reset date range based on filter type
                                            dateRange = if (option == "Day") {
                                                Pair(today, todayEnd)
                                            } else {
                                                val firstDayOfMonth = Calendar.getInstance().apply {
                                                    set(Calendar.DAY_OF_MONTH, 1)
                                                    set(Calendar.HOUR_OF_DAY, 0)
                                                    set(Calendar.MINUTE, 0)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }.timeInMillis
                                                val lastDayOfMonth = Calendar.getInstance().apply {
                                                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                                                    set(Calendar.HOUR_OF_DAY, 23)
                                                    set(Calendar.MINUTE, 59)
                                                    set(Calendar.SECOND, 59)
                                                    set(Calendar.MILLISECOND, 999)
                                                }.timeInMillis
                                                Pair(firstDayOfMonth, lastDayOfMonth)
                                            }
                                        },
                                        label = { Text(option, fontSize = 12.sp) }
                                    )
                                }
                            }
                            
                            // Date picker
                            OutlinedButton(
                                onClick = { isDateDialogOpen = true },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                val dateFormat = SimpleDateFormat(
                                    when (filterType) {
                                        "Day" -> "MMM dd"
                                        else -> "MMM yyyy"
                                    },
                                    Locale.getDefault()
                                )
                                Text(
                                    dateFormat.format(Date(dateRange.first)),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                if (attendanceRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No attendance records found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                } else {
                    // Content
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                    ) {
                        // Today's attendance
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        if (filterType == "Day") "Today's Attendance" else "Monthly Attendance",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colorAccent
                                    )
                                    
                                    // Get today's records and ensure each user is counted only once with their latest status
                                    val todayAttendanceRecords = attendanceRecords.filter { record -> 
                                        record.date >= dateRange.first && record.date <= dateRange.second 
                                    }
                                    
                                    val latestStatusByUser = todayAttendanceRecords
                                        .groupBy { it.userId }
                                        .mapValues { (_, records) -> 
                                            records.maxByOrNull { it.date }?.status ?: AttendanceStatus.ABSENT 
                                        }
                                    
                                    val todayPresent = latestStatusByUser.values.count { it == AttendanceStatus.PRESENT }
                                    val todayAbsent = latestStatusByUser.values.count { it == AttendanceStatus.ABSENT }
                                    val todayLeave = latestStatusByUser.values.count { it == AttendanceStatus.PERMISSION }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        AttendanceStatusCard("Present", todayPresent, colorPresent)
                                        AttendanceStatusCard("Absent", todayAbsent, colorAbsent)
                                        AttendanceStatusCard("Leave", todayLeave, colorLeave)
                                    }
                                }
                            }
                        }

                        // Distribution chart
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Attendance Distribution",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colorAccent
                                    )
                                    
                                    // Get total enrolled count for the selected type and class
                                    val totalEnrolled = when (selectedUserType) {
                                        UserType.STUDENT -> studentCount
                                        UserType.TEACHER -> teacherCount
                                        UserType.STAFF -> staffCount
                                    }
                                    
                                    // Filter records for today only
                                    val todayRecords = attendanceRecords.filter { record ->
                                        record.date >= dateRange.first && record.date <= dateRange.second
                                    }
                                    
                                    // Get unique users and their latest status for today
                                    val latestStatusByUser = todayRecords
                                        .groupBy { it.userId }
                                        .mapValues { (_, records) -> 
                                            records.maxByOrNull { it.date }?.status ?: AttendanceStatus.ABSENT 
                                        }
                                    
                                    // Count unique users for each status
                                    val uniquePresent = latestStatusByUser.values.count { it == AttendanceStatus.PRESENT }
                                    val uniqueAbsent = latestStatusByUser.values.count { it == AttendanceStatus.ABSENT }
                                    val uniqueLeave = latestStatusByUser.values.count { it == AttendanceStatus.PERMISSION }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            AttendancePieChart(
                                                present = uniquePresent,
                                                absent = uniqueAbsent,
                                                leave = uniqueLeave,
                                                total = totalEnrolled
                                            )
                                        }
                                        
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            PieChartLegendItem("Present", colorPresent, uniquePresent, totalEnrolled)
                                            PieChartLegendItem("Absent", colorAbsent, uniqueAbsent, totalEnrolled)
                                            PieChartLegendItem("Leave", colorLeave, uniqueLeave, totalEnrolled)
                                        }
                                    }
                                }
                            }
                        }

                        // Insights
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Insights",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colorAccent
                                    )
                                    
                                    val insights = generateInsights(
                                        records = attendanceRecords,
                                        filterType = filterType,
                                        userType = selectedUserType,
                                        classListSize = classList.size,
                                        teacherCount = teacherCount,
                                        staffCount = staffCount,
                                        studentCount = studentCount,
                                        dateRange = dateRange
                                    )
                                    
                                    insights.forEach { insight ->
                                        InsightItem(insight)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Show error snackbar
    LaunchedEffect(showErrorSnackbar) {
        if (showErrorSnackbar) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Long
            )
            showErrorSnackbar = false
        }
    }

    // Date picker dialog
    if (isDateDialogOpen) {
        DateFilterDialog(
            filterType = filterType,
            initialDateRange = dateRange,
            onDismiss = { isDateDialogOpen = false },
            onDateSelected = { newRange ->
                dateRange = newRange
                isDateDialogOpen = false
            }
        )
    }

    // Export dialog
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExportPDF = {
                try {
                    exportToPDF(context, attendanceRecords, userMap)
                } catch (e: Exception) {
                    errorMessage = "Failed to export PDF: ${e.message}"
                    showErrorSnackbar = true
                }
            },
            onExportExcel = {
                try {
                    exportToExcel(context, attendanceRecords, userMap)
                } catch (e: Exception) {
                    errorMessage = "Failed to export Excel: ${e.message}"
                    showErrorSnackbar = true
                }
            },
            onExportCSV = {
                try {
                    exportToCSV(context, attendanceRecords, userMap)
                } catch (e: Exception) {
                    errorMessage = "Failed to export CSV: ${e.message}"
                    showErrorSnackbar = true
                }
            }
        )
    }
}

@Composable
fun AttendanceStatusCard(label: String, count: Int, color: Color) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
fun PieChartLegendItem(label: String, color: Color, value: Int, total: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "$label ($value out of $total)",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun InsightItem(insight: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = colorAccent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(insight, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun DateFilterDialog(
    filterType: String,
    initialDateRange: Pair<Long, Long>,
    onDismiss: () -> Unit,
    onDateSelected: (Pair<Long, Long>) -> Unit
) {
    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(initialDateRange.first) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (filterType == "Day") "Select Date" else "Select Month") },
        text = {
            Column {
                // Year and Month selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedMonth == 0) {
                            selectedYear--
                            selectedMonth = 11
                        } else {
                            selectedMonth--
                        }
                        // Update selectedDate when month changes
                        calendar.set(selectedYear, selectedMonth, 1)
                        selectedDate = calendar.timeInMillis
                    }) {
                        Icon(Icons.Default.KeyboardArrowLeft, null)
                    }
                    Text(
                        "${getMonthName(selectedMonth)} $selectedYear",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = {
                        if (selectedMonth == 11) {
                            selectedYear++
                            selectedMonth = 0
                        } else {
                            selectedMonth++
                        }
                        // Update selectedDate when month changes
                        calendar.set(selectedYear, selectedMonth, 1)
                        selectedDate = calendar.timeInMillis
                    }) {
                        Icon(Icons.Default.KeyboardArrowRight, null)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (filterType == "Day") {
                    // Calendar grid
                    calendar.set(selectedYear, selectedMonth, 1)
                    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    
                    // Weekday headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar days
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.height(200.dp)
                    ) {
                        // Empty cells for days before the first of the month
                        items(firstDayOfWeek - 1) {
                            Box(modifier = Modifier.aspectRatio(1f))
                        }

                        // Days of the month
                        items(daysInMonth) { day ->
                            val currentCalendar = Calendar.getInstance().apply {
                                set(selectedYear, selectedMonth, day + 1, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val currentDate = currentCalendar.timeInMillis
                            val isSelected = Calendar.getInstance().apply {
                                timeInMillis = selectedDate
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis == currentDate

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        selectedDate = currentDate
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${day + 1}",
                                    color = if (isSelected) Color.White else Color.Unspecified,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    // Month selection grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(12) { month ->
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedMonth == month)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            Color.Transparent
                                    )
                                    .clickable {
                                        selectedMonth = month
                                        calendar.set(selectedYear, month, 1)
                                        selectedDate = calendar.timeInMillis
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getMonthName(month).substring(0, 3),
                                    color = if (selectedMonth == month) Color.White else Color.Unspecified
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startDate = Calendar.getInstance().apply {
                        timeInMillis = selectedDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val endDate = if (filterType == "Day") {
                        Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                    } else {
                        Calendar.getInstance().apply {
                            set(selectedYear, selectedMonth, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                    }
                    onDateSelected(Pair(startDate, endDate))
                    onDismiss()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExportPDF: () -> Unit,
    onExportExcel: () -> Unit,
    onExportCSV: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Attendance") },
        text = {
            Column {
                TextButton(
                    onClick = {
                        onExportPDF()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export as PDF")
                }
                TextButton(
                    onClick = {
                        onExportExcel()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export as Excel")
                }
                TextButton(
                    onClick = {
                        onExportCSV()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export as CSV")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions for data processing
fun groupRecordsByDay(records: List<AttendanceRecord>, dateRange: Pair<Long, Long>): List<Triple<Int, Int, Int>> {
    val calendar = Calendar.getInstance()
    val days = mutableListOf<Triple<Int, Int, Int>>()
    
    calendar.timeInMillis = dateRange.first
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    
    val endCalendar = Calendar.getInstance().apply { 
        timeInMillis = dateRange.second
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    
    while (!calendar.after(endCalendar)) {
        val dayStart = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val dayEnd = calendar.timeInMillis - 1
        
        val dayRecords = records.filter { 
            it.date in dayStart..dayEnd 
        }
        
        // Count unique users for each status
        val uniquePresent = dayRecords.filter { it.status == AttendanceStatus.PRESENT }.map { it.userId }.distinct().size
        val uniqueAbsent = dayRecords.filter { it.status == AttendanceStatus.ABSENT }.map { it.userId }.distinct().size
        val uniqueLeave = dayRecords.filter { it.status == AttendanceStatus.PERMISSION }.map { it.userId }.distinct().size
        
        days.add(Triple(uniquePresent, uniqueAbsent, uniqueLeave))
        
    }
    
    return days
}

fun groupRecordsByHour(records: List<AttendanceRecord>): List<Triple<Int, Int, Int>> {
    val calendar = Calendar.getInstance()
    return (0..23).map { hour ->
        val hourRecords = records.filter {
            calendar.timeInMillis = it.date
            calendar.get(Calendar.HOUR_OF_DAY) == hour
        }
        Triple(
            hourRecords.count { it.status == AttendanceStatus.PRESENT },
            hourRecords.count { it.status == AttendanceStatus.ABSENT },
            hourRecords.count { it.status == AttendanceStatus.PERMISSION }
        )
    }
}

fun generateInsights(
    records: List<AttendanceRecord>,
    filterType: String,
    userType: UserType,
    classListSize: Int,
    teacherCount: Int,
    staffCount: Int,
    studentCount: Int,
    dateRange: Pair<Long, Long>
): List<String> {
    val insights = mutableListOf<String>()
    
    val totalUsers = when (userType) {
        UserType.STUDENT -> studentCount
        UserType.TEACHER -> teacherCount
        UserType.STAFF -> staffCount
    }
    
    insights.add("Total ${userType.name.lowercase()}s: $totalUsers")
    
    // Get unique users with PRESENT status for the date range
    val presentCount = records
        .filter { record -> 
            record.date >= dateRange.first && 
            record.date <= dateRange.second
        }
        .groupBy { it.userId }
        .mapValues { (_, records) -> 
            records.maxByOrNull { it.date }?.status == AttendanceStatus.PRESENT 
        }
        .count { it.value }
    
    val attendanceRate = if (totalUsers > 0) {
        (presentCount.toFloat() / totalUsers * 100).roundToInt()
    } else 0
    
    insights.add("Average attendance: $attendanceRate%")
    
    return insights
}

fun exportToPDF(context: Context, records: List<AttendanceRecord>, userMap: Map<String, Person>) {
    // Implement PDF export
}

fun exportToExcel(context: Context, records: List<AttendanceRecord>, userMap: Map<String, Person>) {
    // Implement Excel export
}

fun exportToCSV(context: Context, records: List<AttendanceRecord>, userMap: Map<String, Person>) {
    // Implement CSV export
}

@Composable
fun DropdownMenuBox(label: String, options: List<String>, selectedOption: String?, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(50)) {
            Text(selectedOption ?: label)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

suspend fun fetchAttendanceRecords(
    db: FirebaseFirestore,
    userType: UserType,
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
            .collection(userType.name.lowercase())
            .get()
            .await()
        for (doc in snapshot.documents) {
            doc.toObject(AttendanceRecord::class.java)?.let { records.add(it) }
        }
        calendar.add(Calendar.DATE, 1)
    }
    return records
}

fun getMonthName(month: Int): String {
    return Calendar.getInstance().apply {
        set(Calendar.MONTH, month)
    }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
} 