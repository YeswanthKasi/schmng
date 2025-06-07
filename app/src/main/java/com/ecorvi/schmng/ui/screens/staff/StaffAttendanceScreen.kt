package com.ecorvi.schmng.ui.screens.staff

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.StaffBottomNavigation
import com.ecorvi.schmng.ui.navigation.StaffBottomNavItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.UserType
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAttendanceScreen(
    navController: NavController
) {
    var selectedPeriod by remember { mutableStateOf("Last 3 Months") }
    var attendanceData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalDays by remember { mutableStateOf(0) }
    var presentDays by remember { mutableStateOf(0) }
    var absentDays by remember { mutableStateOf(0) }
    var leaveDays by remember { mutableStateOf(0) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    
    val periodOptions = listOf(
        "Last Week",
        "Last Month",
        "Last 3 Months",
        "Last 6 Months",
        "Last Year"
    )

    // Load attendance data
    LaunchedEffect(selectedPeriod) {
        isLoading = true
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val startDate = when (selectedPeriod) {
                "Last Week" -> System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                "Last Month" -> System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                "Last 3 Months" -> System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
                "Last 6 Months" -> System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)
                "Last Year" -> System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
                else -> System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
            }

            // Format today's date for the collection path
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())

            // Fetch attendance records from Firestore using the correct path structure
            FirebaseFirestore.getInstance()
                .collection("attendance")
                .document(today)
                .collection("staff")
                .document(currentUser.uid)
                .collection("records")
                .whereGreaterThanOrEqualTo("date", startDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    val records = mutableMapOf<String, String>()
                    snapshot.documents.forEach { doc ->
                        val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            .format(Date(doc.getLong("date") ?: 0))
                        val status = doc.getString("status") ?: AttendanceStatus.ABSENT.name
                        records[date] = status
                    }
                    attendanceData = records
                    presentDays = records.count { it.value == AttendanceStatus.PRESENT.name }
                    absentDays = records.count { it.value == AttendanceStatus.ABSENT.name }
                    leaveDays = records.count { it.value == AttendanceStatus.PERMISSION.name }
                    totalDays = records.size
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    println("Error fetching attendance: ${e.message}")
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Attendance",
                        color = Color(0xFF1F41BB),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Button(
                        onClick = { navController.navigate("staff_leave_application") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F41BB)
                        ),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Apply Leave",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply Leave")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            StaffBottomNavigation(
                currentRoute = null,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1F41BB))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Staff Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF0F4FF)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Period Selector
                        ExposedDropdownMenuBox(
                            expanded = isDropdownExpanded,
                            onExpandedChange = { isDropdownExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = selectedPeriod,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Period") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                periodOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            selectedPeriod = option
                                            isDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Attendance Summary
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Attendance Summary",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF1F41BB),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AttendanceStatItem("Total", totalDays.toString(), Color.Gray)
                            AttendanceStatItem("Present", presentDays.toString(), Color(0xFF4CAF50))
                            AttendanceStatItem("Absent", absentDays.toString(), Color(0xFFE53935))
                            AttendanceStatItem("Leave", leaveDays.toString(), Color(0xFFFFA000))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = if (totalDays > 0) presentDays.toFloat() / totalDays else 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Color(0xFF4CAF50),
                            trackColor = Color(0xFFE0E0E0)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Attendance Rate: ${if (totalDays > 0) "%.1f".format(presentDays.toFloat() / totalDays * 100) else "0"}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1F41BB),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Attendance History
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Attendance History",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF1F41BB),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn {
                            items(attendanceData.toList().sortedByDescending { it.first }) { (date, status) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = date,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = LocalDate.parse(date.replace(" ", "-"), DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
                                                .dayOfWeek.toString().lowercase()
                                                .replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = when (status) {
                                            AttendanceStatus.PRESENT.name -> Color(0xFF4CAF50)
                                            AttendanceStatus.ABSENT.name -> Color(0xFFE53935)
                                            AttendanceStatus.PERMISSION.name -> Color(0xFFFFA000)
                                            else -> Color(0xFFFFA000)
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (attendanceData.toList().last() != (date to status)) {
                                    Divider(color = Color(0xFFE0E0E0))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
} 