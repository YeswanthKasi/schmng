package com.ecorvi.schmng.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
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
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.ecorvi.schmng.ui.data.model.Timetable
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import com.ecorvi.schmng.ui.data.store.TempMessageStore

private val PrimaryBlue = Color(0xFF1F41BB)
private val ScheduleOrange = Color(0xFFFF9800)
private val FeesRed = Color(0xFFE91E63)
private val BackgroundColor = Color.White.copy(alpha = 0.95f)
private val StudentGradientColors = listOf(
    Color(0xFF1F41BB), // Primary Blue
    Color(0xFF4CAF50), // Student Green
    Color(0xFF2196F3), // Light Blue
    Color(0xFF1F41BB)  // Primary Blue
)

sealed class StudentDashboardUiState {
    object Loading : StudentDashboardUiState()
    data class Success(val student: Person) : StudentDashboardUiState()
    data class Error(val message: String) : StudentDashboardUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var student by remember { mutableStateOf<Person?>(null) }
    var todayTimetable by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var todaySchedule by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var pendingFees by remember { mutableStateOf<List<Fee>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current

    fun handleHelpClick() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:info@ecorvi.com")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "No email app found. Please email us at info@ecorvi.com",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun handleLogout() {
        auth.signOut()
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("user_role")
            .remove("stay_signed_in")
            .apply()
        navController.navigate("login") {
            popUpTo(0) { inclusive = true }
        }
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            try {
                isLoading = true
                // First update the FCM token
                try {
                    val token = FirebaseMessaging.getInstance().token.await()
                    Log.d("FCM", "Retrieved token: $token")
                    
                    val userRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
                    val tokenData = hashMapOf(
                        "fcmToken" to token,
                        "lastUpdated" to FieldValue.serverTimestamp()
                    )
                    
                    userRef.update(tokenData as Map<String, Any>).await()
                    Log.d("FCM", "Token successfully updated for user: ${currentUser.uid}")
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to update FCM token", e)
                    errorMessage = "Failed to update notification token: ${e.message}"
                    isLoading = false
                    return@LaunchedEffect
                }

                // Then fetch user data
                FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        if (userDoc.exists() && userDoc.getString("role") == "student") {
                            FirebaseFirestore.getInstance().collection("students")
                                .document(currentUser.uid)
                                .get()
                                .addOnSuccessListener { studentDoc ->
                                    if (studentDoc.exists()) {
                                        student = studentDoc.toObject(Person::class.java)
                                        
                                        // Get today's day of week
                                        val calendar = Calendar.getInstance()
                                        val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                                        val today = days[calendar.get(Calendar.DAY_OF_WEEK) - 1]
                                        
                                        // Fetch today's timetable
                                        FirebaseFirestore.getInstance()
                                            .collection("timetables")
                                            .whereEqualTo("classGrade", student?.className)
                                            .whereEqualTo("dayOfWeek", today)
                                            .whereEqualTo("isActive", true)
                                            .get()
                                            .addOnSuccessListener { timetablesDocs ->
                                                todayTimetable = timetablesDocs.mapNotNull { doc ->
                                                    doc.toObject(Timetable::class.java)
                                                }.sortedBy { it.timeSlot }
                                                
                                                // Fetch today's special schedules
                                                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                                FirebaseFirestore.getInstance()
                                                    .collection("schedules")
                                                    .whereEqualTo("date", todayDate)
                                                    .whereIn("className", listOf(student?.className, "all"))
                                                    .whereEqualTo("status", "Active")
                                                    .get()
                                                    .addOnSuccessListener { schedulesDocs ->
                                                        todaySchedule = schedulesDocs.mapNotNull { doc ->
                                                            doc.toObject(Schedule::class.java)
                                                        }.sortedBy { it.time }
                                                        isLoading = false
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("StudentDashboard", "Error fetching schedules: ${e.message}")
                                                        isLoading = false
                                                    }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("StudentDashboard", "Error fetching timetable: ${e.message}")
                                                isLoading = false
                                            }
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("Menu", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                Divider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("My Profile") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        student?.id?.let { 
                            navController.navigate("view_profile/student/$it") 
                        }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Schedule") },
                    label = { Text("My Schedule") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("student_schedule")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.CheckCircleOutline, contentDescription = "Attendance") },
                    label = { Text("My Attendance") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        Toast.makeText(context, "Attendance feature coming soon", Toast.LENGTH_SHORT).show()
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Announcement, contentDescription = "Announcements") },
                    label = { Text("Announcements") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("student_announcements")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.School, contentDescription = "Class Teacher") },
                    label = { Text("Class Teacher") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("student_teacher_info")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.DirectionsBus, contentDescription = "School Bus") },
                    label = { Text("School Bus") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        Toast.makeText(context, "School Bus tracking coming soon", Toast.LENGTH_SHORT).show()
                    }
                )
                Divider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Help, contentDescription = "Help") },
                    label = { Text("Help") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        handleHelpClick()
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = "Logout") },
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        handleLogout()
                    }
                )
            }
        }
    ) {
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
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menu",
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
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                StudentAiSearchBar()
                            }

                            item {
                                if (isLoading) {
                                    ShimmerCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                    )
                                } else {
                                    DashboardCard(
                                        title = "My Profile",
                                        icon = Icons.Default.Person,
                                        color = PrimaryBlue,
                                        onClick = { 
                                            try {
                                                student?.id?.let { studentId -> 
                                                    navController.navigate("student_profile/$studentId")
                                                } ?: run {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Error: Student profile not loaded")
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                                }
                                            }
                                        }
                                    ) {
                                        student?.let { studentData ->
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "${studentData.firstName} ${studentData.lastName}",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Class: ${studentData.className}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = "Roll No: ${studentData.rollNumber}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.Gray
                                                )
                                            }
                                        } ?: Text("Loading profile data...")
                                    }
                                }
                            }

                            item {
                                if (isLoading) {
                                    ShimmerCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                    )
                                } else {
                                    DashboardCard(
                                        title = "Today's Classes",
                                        icon = Icons.Default.Schedule,
                                        color = ScheduleOrange,
                                        onClick = { navController.navigate("student_schedule") }
                                    ) {
                                        if (todaySchedule.isEmpty()) {
                                            Text("No classes scheduled for today")
                                        } else {
                                            Column {
                                                todaySchedule.take(3).forEach { schedule ->
                                                    Text(
                                                        text = "${schedule.time} - ${schedule.title}",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                                if (todaySchedule.size > 3) {
                                                    Text("...", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                if (isLoading) {
                                    ShimmerCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                    )
                                } else {
                                    DashboardCard(
                                        title = "My Timetable",
                                        icon = Icons.Default.Schedule,
                                        color = PrimaryBlue,
                                        onClick = { navController.navigate("student_timetable") }
                                    ) {
                                        Text("View your class schedule")
                                    }
                                }
                            }

                            item {
                                if (isLoading) {
                                    ShimmerCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                    )
                                } else {
                                    DashboardCard(
                                        title = "My Attendance",
                                        icon = Icons.Default.CheckCircleOutline,
                                        color = PrimaryBlue,
                                        onClick = {
                                            Toast.makeText(context, "Attendance feature coming soon", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text("View your attendance summary")
                                    }
                                }
                            }

                            item {
                                if (isLoading) {
                                    ShimmerCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                    )
                                } else {
                                    DashboardCard(
                                        title = "Notice Board",
                                        icon = Icons.Default.Announcement,
                                        color = PrimaryBlue,
                                        onClick = { navController.navigate("student_announcements") }
                                    ) {
                                        Text("View latest announcements")
                                    }
                                }
                            }

                            item {
                                if (isLoading) {
                                    ShimmerCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                    )
                                } else {
                                    DashboardCard(
                                        title = "Messages",
                                        icon = Icons.Default.Message,
                                        color = PrimaryBlue,
                                        onClick = { navController.navigate("student_messages") }
                                    ) {
                                        Text("View and send messages")
                                    }
                                }
                            }

                            item {
                                if (isLoading) {
                                    ShimmerCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                    )
                                } else {
                                    DashboardCard(
                                        title = "School Bus",
                                        icon = Icons.Default.DirectionsBus,
                                        color = ScheduleOrange,
                                        onClick = {
                                            Toast.makeText(context, "School Bus tracking coming soon", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text("Track your school bus")
                                    }
                                }
                            }
                        }

                        if (errorMessage != null) {
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
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ShimmerCard(
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 1000, translateAnim.value - 1000),
        end = Offset(translateAnim.value, translateAnim.value)
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = brush)
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

@Composable
private fun StudentAiSearchBar(modifier: Modifier = Modifier) {
    var searchText by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val infiniteTransition = rememberInfiniteTransition(label = "ai-search-bar")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "gradientOffset"
    )
    
    val brush = Brush.linearGradient(
        colors = StudentGradientColors,
        start = Offset(0f, 0f),
        end = Offset(400f * (0.5f + 0.5f * kotlin.math.sin(animatedOffset * 2 * Math.PI).toFloat()), 
                     400f * (0.5f - 0.5f * kotlin.math.cos(animatedOffset * 2 * Math.PI).toFloat()))
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)
                .background(
                    brush = brush,
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(2.dp)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Color.White, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp)),
                placeholder = {
                    Text(
                        text = "Ask AI anything about your studies...",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "AI Search",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Search",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                },
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = PrimaryBlue,
                    focusedLeadingIconColor = PrimaryBlue,
                    unfocusedLeadingIconColor = PrimaryBlue,
                    focusedTrailingIconColor = PrimaryBlue,
                    unfocusedTrailingIconColor = PrimaryBlue
                ),
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudentDashboardScreenPreview() {
    val navController = rememberNavController()
    StudentDashboardScreen(navController = navController)
} 