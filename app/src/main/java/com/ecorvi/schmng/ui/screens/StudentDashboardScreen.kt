package com.ecorvi.schmng.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Fee
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.data.model.Schedule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val PrimaryBlue = Color(0xFF1F41BB)
private val ScheduleOrange = Color(0xFFFF9800)
private val FeesRed = Color(0xFFE91E63)
private val BackgroundColor = Color.White.copy(alpha = 0.95f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var student by remember { mutableStateOf<Person?>(null) }
    var todaySchedule by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var pendingFees by remember { mutableStateOf<List<Fee>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Fetch student data
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            try {
                // First check if user exists in users collection
                FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        if (userDoc.exists() && userDoc.getString("role") == "student") {
                            // Then fetch student data
                            FirebaseFirestore.getInstance().collection("students")
                                .document(currentUser.uid)
                                .get()
                                .addOnSuccessListener { studentDoc ->
                                    if (studentDoc.exists()) {
                                        student = studentDoc.toObject(Person::class.java)
                                        
                                        // Fetch today's schedule for the student's class
                                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                        FirestoreDatabase.fetchSchedules(
                                            onComplete = { schedules ->
                                                todaySchedule = schedules.filter { 
                                                    it.date == today && 
                                                    (it.className == student?.className || it.className == "all")
                                                }
                                            },
                                            onFailure = { e -> errorMessage = e.message }
                                        )

                                        // Fetch pending fees for the student
                                        FirestoreDatabase.fetchPendingFees(
                                            onComplete = { fees ->
                                                pendingFees = fees.filter { 
                                                    it.studentId == currentUser.uid || 
                                                    (it.className == student?.className && it.studentId == "all")
                                                }
                                            },
                                            onFailure = { e -> errorMessage = e.message }
                                        )

                                        isLoading = false
                                    } else {
                                        errorMessage = "Student data not found. Please contact administrator."
                                        isLoading = false
                                    }
                                }
                                .addOnFailureListener { e ->
                                    errorMessage = "Failed to fetch student data: ${e.message}"
                                    isLoading = false
                                }
                        } else {
                            errorMessage = "Invalid user role or user not found. Please contact administrator."
                            isLoading = false
                        }
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Failed to verify user: ${e.message}"
                        isLoading = false
                    }
            } catch (e: Exception) {
                errorMessage = e.message
                isLoading = false
            }
        } else {
            errorMessage = "User not authenticated. Please log in again."
            isLoading = false
        }
    }

    CommonBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "STUDENT DASHBOARD",
                                color = PrimaryBlue,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            auth.signOut()
                            navController.navigate("login") {
                                popUpTo("student_dashboard") { inclusive = true }
                            }
                        }) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = PrimaryBlue
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    )
                )
            },
            content = { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (isLoading) {
                        // TODO: Add loading shimmer
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryBlue)
                        }
                    } else if (errorMessage != null) {
                        // Error state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Error loading dashboard",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Red
                            )
                            Text(
                                text = errorMessage ?: "Unknown error occurred",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else {
                        // Dashboard content
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            // Today's Schedule
                            item {
                                DashboardCard(
                                    title = "Today's Classes",
                                    icon = Icons.Default.Schedule,
                                    color = ScheduleOrange,
                                    onClick = { navController.navigate("schedules") }
                                ) {
                                    if (todaySchedule.isEmpty()) {
                                        Text("No classes scheduled for today")
                                    } else {
                                        Column {
                                            todaySchedule.forEach { schedule ->
                                                Text(
                                                    text = "${schedule.time} - ${schedule.title}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Pending Fees
                            item {
                                DashboardCard(
                                    title = "Fee Due",
                                    icon = Icons.Default.CurrencyRupee,
                                    color = FeesRed,
                                    onClick = { navController.navigate("pending_fees") }
                                ) {
                                    if (pendingFees.isEmpty()) {
                                        Text("No pending fees")
                                    } else {
                                        Column {
                                            pendingFees.forEach { fee ->
                                                Text(
                                                    text = "₹${fee.amount} - Due: ${fee.dueDate}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // TODO: Attendance Summary
                            item {
                                DashboardCard(
                                    title = "My Attendance",
                                    icon = Icons.Default.Person,
                                    color = PrimaryBlue,
                                    onClick = { /* TODO: Navigate to attendance screen */ }
                                ) {
                                    Text("Attendance feature coming soon")
                                }
                            }

                            // TODO: My Subjects
                            item {
                                DashboardCard(
                                    title = "My Subjects",
                                    icon = Icons.Default.Book,
                                    color = PrimaryBlue,
                                    onClick = { /* TODO: Navigate to subjects screen */ }
                                ) {
                                    Text("Subjects feature coming soon")
                                }
                            }

                            // TODO: Exam Results
                            item {
                                DashboardCard(
                                    title = "Latest Results",
                                    icon = Icons.Default.Assignment,
                                    color = PrimaryBlue,
                                    onClick = { /* TODO: Navigate to results screen */ }
                                ) {
                                    Text("Results feature coming soon")
                                }
                            }

                            // TODO: Announcements
                            item {
                                DashboardCard(
                                    title = "Notice Board",
                                    icon = Icons.Default.Announcement,
                                    color = PrimaryBlue,
                                    onClick = { /* TODO: Navigate to announcements screen */ }
                                ) {
                                    Text("Announcements feature coming soon")
                                }
                            }

                            // TODO: Upcoming Events
                            item {
                                DashboardCard(
                                    title = "Events",
                                    icon = Icons.Default.Event,
                                    color = PrimaryBlue,
                                    onClick = { /* TODO: Navigate to events screen */ }
                                ) {
                                    Text("Events feature coming soon")
                                }
                            }

                            // My Profile
                            item {
                                DashboardCard(
                                    title = "My Profile",
                                    icon = Icons.Default.Person,
                                    color = PrimaryBlue,
                                    onClick = { 
                                        student?.id?.let { 
                                            navController.navigate("profile/$it/student") 
                                        }
                                    }
                                ) {
                                    student?.let {
                                        Column {
                                            Text(
                                                text = "${it.firstName} ${it.lastName}",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "Class: ${it.className}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Roll No: ${it.rollNumber}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }

                            // TODO: Class Teacher Info
                            item {
                                DashboardCard(
                                    title = "Class Teacher",
                                    icon = Icons.Default.School,
                                    color = PrimaryBlue,
                                    onClick = { /* TODO: Navigate to teacher details */ }
                                ) {
                                    Text("Class teacher info coming soon")
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
} 