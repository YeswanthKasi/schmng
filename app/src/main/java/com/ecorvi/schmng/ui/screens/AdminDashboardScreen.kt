package com.ecorvi.schmng.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ecorvi.schmng.ui.components.*
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Fee
import com.ecorvi.schmng.ui.data.model.Schedule
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.layout.ContentScale
import com.ecorvi.schmng.R
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.filled.Update
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import java.text.SimpleDateFormat
import java.util.*
import com.ecorvi.schmng.ui.components.AnalyticsPieChart
import kotlinx.coroutines.async
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ecorvi.schmng.ui.navigation.AppNavigation
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import com.ecorvi.schmng.ui.navigation.BottomNav
import com.ecorvi.schmng.ui.utils.getCurrentDate
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random
import com.ecorvi.schmng.ui.theme.*
import com.ecorvi.schmng.models.AttendanceRecord
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.UserType
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.components.AttendancePieChart

// Define primary colors used throughout the dashboard
private val PrimaryBlue = Color(0xFF1F41BB)    // Main theme color
private val StudentGreen = Color(0xFF4CAF50)   // Color for student-related items
private val TeacherBlue = Color(0xFF2196F3)    // Color for teacher-related items
private val StaffPurple = Color(0xFF9C27B0)    // Color for staff-related items
private val ScheduleOrange = Color(0xFFFF9800)  // Color for schedule-related items
private val FeesRed = Color(0xFFE91E63)        // Color for fee-related items
private val BackgroundColor = Color.White.copy(alpha = 0.95f)  // Semi-transparent background

// Add these color definitions with the other colors
private val GraphBlue = Color(0xFF2196F3)
private val GraphGreen = Color(0xFF4CAF50)
private val GraphOrange = Color(0xFFFF9800)
private val GraphPurple = Color(0xFF9C27B0)
private val GraphRed = Color(0xFFE91E63)

// Add these color definitions with the other colors at the top
private val MaleColor = Color(0xFF2196F3)  // Blue for male
private val FemaleColor = Color(0xFFE91E63)  // Pink for female
private val OtherColor = Color(0xFF9C27B0)   // Purple for other
private val PresentColor = Color(0xFF4CAF50)  // Green for present
private val AbsentColor = Color(0xFFE57373)   // Red for absent
private val LeaveColor = Color(0xFFFFB74D)    // Orange for leave

// Main composable function for the Admin Dashboard Screen
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    currentRoute: String,
    onRouteSelected: (String) -> Unit
) {
    // Initialize Firebase Authentication instance
    val auth = FirebaseAuth.getInstance()
    // Get current context for Android operations
    val context = LocalContext.current
    
    // Define management items for the dashboard
    val manageItems = listOf(
        "STUDENTS" to Icons.Default.Person,
        "TEACHERS" to Icons.Default.School,
        "STAFF" to Icons.Default.Group,
        "TIMETABLE" to Icons.Default.Schedule
    )
    
    // State management using remember and mutableStateOf
    var selectedTab by remember { mutableIntStateOf(0) }  // Track selected bottom navigation tab
    var showDialog by remember { mutableStateOf(false) }  // Control dialog visibility
    var selectedCategory by remember { mutableStateOf("") }  // Track selected category
    var showAddInput by remember { mutableStateOf(false) }  // Control add input visibility
    var showViewList by remember { mutableStateOf(false) }  // Control list view visibility
    var inputText by remember { mutableStateOf("") }  // Store input text
    
    // State for data lists
    var schedules by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var pendingFees by remember { mutableStateOf<List<Fee>>(emptyList()) }
    
    // Loading and error states
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Snackbar host state for showing messages
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Statistics counters
    var studentCount by remember { mutableStateOf(0) }
    var teacherCount by remember { mutableStateOf(0) }
    var staffCount by remember { mutableStateOf(0) }
    var studentAttendance by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var teacherAttendance by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var staffAttendance by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    
    // UI state management
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showNavigationDrawer by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Initialize drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Effect to load initial data with improved loading state management
    LaunchedEffect(Unit) {
        isLoading = true
        
        try {
            withContext(Dispatchers.IO) {
                // Launch all data fetching operations concurrently
                val studentDeferred = async { 
                    suspendCoroutine<Int> { continuation ->
                        FirestoreDatabase.fetchStudentCount { count ->
                            continuation.resume(count)
                        }
                    }
                }
                
                val teacherDeferred = async {
                    suspendCoroutine<Int> { continuation ->
                        FirestoreDatabase.fetchTeacherCount { count ->
                            continuation.resume(count)
                        }
                    }
                }

                val staffDeferred = async {
                    suspendCoroutine<Int> { continuation ->
                        FirestoreDatabase.fetchStaffCount { count ->
                            continuation.resume(count)
                        }
                    }
                }

                // Get today's date in yyyy-MM-dd format
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())

                // Fetch today's attendance records for each user type
                val studentAttendanceDeferred = async {
                    suspendCoroutine<List<AttendanceRecord>> { continuation ->
                        FirestoreDatabase.fetchAttendanceByDate(
                            date = today,
                            userType = UserType.STUDENT,
                            onComplete = { records -> continuation.resume(records) }
                        )
                    }
                }

                val teacherAttendanceDeferred = async {
                    suspendCoroutine<List<AttendanceRecord>> { continuation ->
                        FirestoreDatabase.fetchAttendanceByDate(
                            date = today,
                            userType = UserType.TEACHER,
                            onComplete = { records -> continuation.resume(records) }
                        )
                    }
                }

                val staffAttendanceDeferred = async {
                    suspendCoroutine<List<AttendanceRecord>> { continuation ->
                        FirestoreDatabase.fetchAttendanceByDate(
                            date = today,
                            userType = UserType.STAFF,
                            onComplete = { records -> continuation.resume(records) }
                        )
                    }
                }

                try {
                    // Wait for all operations to complete
                    studentCount = studentDeferred.await()
                    teacherCount = teacherDeferred.await()
                    staffCount = staffDeferred.await()
                    
                    // Get attendance records
                    studentAttendance = studentAttendanceDeferred.await()
                    teacherAttendance = teacherAttendanceDeferred.await()
                    staffAttendance = staffAttendanceDeferred.await()
                    
                    withContext(Dispatchers.Main) {
                        // Add small delay for smooth transition
                        delay(200)
                        isLoading = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Failed to load data: ${e.message}"
                        isLoading = false
                    }
                }
            }
        } catch (e: Exception) {
            errorMessage = "Failed to initialize dashboard: ${e.message}"
            isLoading = false
        }
    }

    // Function to show temporary messages
    fun showMessage(msg: String) {
        errorMessage = msg
        coroutineScope.launch {
            delay(3000)
            errorMessage = null
        }
    }

    // Function to add new items (schedules or fees)
    fun addItem(category: String, item: String) {
        if (item.isBlank()) return
        
        when (category) {
            "SCHEDULE" -> {
                // Create new schedule object
                val schedule = Schedule(
                    title = item,
                    description = "", 
                    date = "", 
                    time = "", 
                    className = ""
                )
                // Add schedule to Firebase
                FirestoreDatabase.addSchedule(
                    schedule = schedule,
                    onSuccess = {
                        showMessage("Schedule added successfully")
                        // Refresh schedules list
                        FirestoreDatabase.fetchSchedules(
                            onComplete = { schedules = it },
                            onFailure = { e -> showMessage("Failed to refresh schedules: ${e.message}") }
                        )
                    },
                    onFailure = { e -> showMessage("Error adding schedule: ${e.message}") }
                )
            }
            "PENDING FEES" -> {
                // Create new fee object with required fields
                val fee = Fee(
                    id = "",  // Will be set by Firestore
                    studentName = item,
                    studentId = "",  // This should be set when selecting a specific student
                    amount = 0.0,
                    dueDate = getCurrentDate(),  // Use current date as default
                    className = "",  // This should be set when selecting a specific student
                    status = "Pending",
                    description = "New fee entry"
                )
                // Add fee to Firebase
                FirestoreDatabase.addFee(
                    fee = fee,
                    onSuccess = {
                        showMessage("Fee added successfully")
                        // Refresh fees list
                        FirestoreDatabase.fetchPendingFees(
                            onComplete = { pendingFees = it },
                            onFailure = { e -> showMessage("Failed to refresh fees: ${e.message}") }
                        )
                    },
                    onFailure = { e -> showMessage("Error adding fee: ${e.message}") }
                )
            }
        }
    }

    // Function to handle help option - opens email client
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

    // Function to handle user logout
    fun handleLogout() {
        try {
            // First clear all listeners and subscriptions
            FirestoreDatabase.cleanup()
            
            // Sign out from Firebase
            auth.signOut()
            
            // Clear cached user data
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()  // Clear all preferences
                .apply()

            // Navigate to login screen with proper cleanup
            navController.navigate("login") {
                popUpTo(navController.graph.id) {
                    inclusive = true  // This removes all screens from the back stack
                }
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Log.e("AdminDashboard", "Error during logout: ${e.message}")
            // Still try to navigate to login even if there's an error
            navController.navigate("login")
        }
    }

    // Function to check for app updates
    fun checkForUpdates() {
        try {
            // Try to open Play Store app
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // If Play Store app is not installed, open in browser
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to open Play Store", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dialog for displaying options
    if (showDialog) {
        // Get data list based on selected category
        val dataList = when (selectedCategory) {
            "SCHEDULE" -> schedules.map { it.title }
            "PENDING FEES" -> pendingFees.map { "${it.studentName} - ₹${it.amount}" }
            else -> emptyList()
        }
        
        // Show options dialog
        OptionsDialog(
            category = selectedCategory,
            onDismiss = {
                showDialog = false
                showAddInput = false
                showViewList = false
                inputText = ""
            },
            onAddClick = { showAddInput = true; showViewList = false },
            onViewClick = { showViewList = true; showAddInput = false },
            showAddInput = showAddInput,
            showViewList = showViewList,
            inputText = inputText,
            onInputChange = { inputText = it },
            onConfirmAdd = { newItem ->
                addItem(selectedCategory, newItem)
                showAddInput = false
                showDialog = false
                inputText = ""
            },
            onDelete = { item ->
                when (selectedCategory) {
                    "SCHEDULE" -> {
                        // Delete schedule from Firebase
                        val scheduleToDelete = schedules.find { it.title == item }
                        if (scheduleToDelete != null) {
                            FirestoreDatabase.deleteSchedule(
                                scheduleId = scheduleToDelete.id,
                                onSuccess = {
                                    showMessage("Schedule deleted successfully")
                                    FirestoreDatabase.fetchSchedules(
                                        onComplete = { schedules = it },
                                        onFailure = { e -> showMessage("Failed to refresh schedules: ${e.message}") }
                                    )
                                },
                                onFailure = { e -> showMessage("Error deleting schedule: ${e.message}") }
                            )
                        }
                    }
                    "PENDING FEES" -> {
                        // Delete fee from Firebase
                        val feeToDelete = pendingFees.find { "${it.studentName} - ₹${it.amount}" == item }
                        if (feeToDelete != null) {
                            FirestoreDatabase.deleteFee(
                                feeId = feeToDelete.id,
                                onSuccess = {
                                    showMessage("Fee deleted successfully")
                                    FirestoreDatabase.fetchPendingFees(
                                        onComplete = { pendingFees = it },
                                        onFailure = { e -> showMessage("Failed to refresh fees: ${e.message}") }
                                    )
                                },
                                onFailure = { e -> showMessage("Error deleting fee: ${e.message}") }
                            )
                        }
                    }
                }
            },
            dataList = dataList
        )
    }

    // Loading shimmer effect component with improved animations
    @Composable
    fun DashboardShimmer() {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.95f)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // AI Search bar shimmer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ShimmerLoading(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(56.dp),
                        cornerRadius = 28
                    )
                }
            }

            // Analytics Card shimmer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title
                        ShimmerText(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            width = 120
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Pie chart placeholder
                        ShimmerCircle(
                            size = 200
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Legend items
                        repeat(3) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ShimmerCircle(size = 8)
                                    ShimmerText(width = 80)
                                }
                                ShimmerText(width = 40)
                            }
                        }
                    }
                }
            }

            // Monthly Fee Analytics Card shimmer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        ShimmerText(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            width = 100
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Bar graph placeholder
                        ShimmerLoading(
                                    modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            cornerRadius = 8
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Month labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(6) {
                                ShimmerText(width = 30)
                            }
                        }
                    }
                }
            }

            // Attendance Overview Card shimmer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        ShimmerText(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            width = 160
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left side: Pie Chart
                            ShimmerCircle(
                                modifier = Modifier.weight(1f),
                                size = 120
                            )
                            
                            // Right side: Legend
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp)
                            ) {
                                repeat(3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            ShimmerCircle(size = 8)
                                            ShimmerText(width = 60)
                                        }
                                        ShimmerText(width = 80)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pending Fees Card shimmer
            item {
                ShimmerCard(
                    height = 80,
                    showContent = false
                        )
                    }
        }
    }

    // Main navigation drawer layout
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Drawer content layout
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = Color.White.copy(alpha = 0.95f)
            ) {
                // App logo and title section
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Check if in preview mode
                    val isPreview = LocalInspectionMode.current
                    if (!isPreview) {
                        Image(
                            painter = if (isPreview) 
                                painterResource(id = android.R.drawable.ic_menu_gallery) 
                            else 
                                painterResource(id = R.drawable.ecorvilogo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(80.dp)
                                .padding(8.dp)
                        )
                    }
                    // App title
                    Text(
                        text = "Ecorvi School Management",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Navigation menu items
                Spacer(modifier = Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 16.sp) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Students") },
                    label = { Text("Students", fontSize = 16.sp) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("students")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.School, contentDescription = "Teachers") },
                    label = { Text("Teachers", fontSize = 16.sp) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("teachers")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Group, contentDescription = "Staff") },
                    label = { Text("Non-Teaching Staff", fontSize = 16.sp) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("staff")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Announcement, contentDescription = "Notices") },
                    label = { Text("Notices", fontSize = 16.sp) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("admin_notices")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.EventNote, contentDescription = "Leave Requests") },
                    label = { Text("Leave Requests", fontSize = 16.sp) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("admin_leave_list")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Timetable") },
                    label = { Text("Timetable", fontSize = 16.sp) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("timetable_management")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Schedule") },
                    label = { Text("Schedule", fontSize = 16.sp) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("schedules")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Attendance") },
                    label = { Text("Attendance", fontSize = 16.sp) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("attendance_analytics")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.DirectionsBus, contentDescription = "Bus Tracking") },
                    label = {Text("Bus Tracking", fontSize = 16.sp)},
                    selected = false,
                    onClick = {
                        Toast.makeText(context, "Bus Tracking is coming soon!", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(modifier = Modifier.weight(1f))

                // Version and Update section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    Text(
                        text = "Version ${packageInfo.versionName}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color.Gray,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { checkForUpdates() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Update,
                            contentDescription = "Check for Updates",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Check for Updates",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = PrimaryBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

    ) {
        // Main content scaffold
        CommonBackground {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    "AdminHub",
                                    color = PrimaryBlue,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (drawerState.isClosed) drawerState.open()
                                        else drawerState.close()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = PrimaryBlue
                                )
                            }
                        },
                        actions = {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = "Profile Menu",
                                        tint = PrimaryBlue
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier
                                        .background(Color.White)
                                        .width(160.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Profile") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            navController.navigate("admin_profile")
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Privacy & Security") },
                                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/ecorvischoolmanagement/home"))
                                            context.startActivity(intent)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Help & Support") },
                                        leadingIcon = { Icon(Icons.Default.Help, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:info@ecorvi.com")
                                            }
                                            context.startActivity(intent)
                                        }
                                    )
                                    Divider()
                                    DropdownMenuItem(
                                        text = { Text("Logout", color = Color.Red) },
                                        leadingIcon = { 
                                                Icon(
                                                    Icons.Default.Logout,
                                                contentDescription = null,
                                                tint = Color.Red
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            handleLogout()
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White.copy(alpha = 0.95f)
                        )
                    )
                },
                bottomBar = {
                    BottomNav(
                        navController = navController,
                        currentRoute = currentRoute,
                        onItemSelected = { item -> onRouteSelected(item.route) }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                content = { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        if (isLoading) {
                            DashboardShimmer()
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
                                Button(
                                    onClick = {
                                        isLoading = true
                                        errorMessage = null
                                        // Retry loading data - Reverted to static calls
                                        FirestoreDatabase.fetchStudentCount { count ->
                                            studentCount = count
                                        }
                                        FirestoreDatabase.fetchTeacherCount { count ->
                                            teacherCount = count
                                        }
                                        isLoading = false
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                        } else {
                            // Dashboard content
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(
                                    top = 12.dp,
                                    bottom = 16.dp
                                )
                            ) {
                                item {
                                    AiSearchBar()
                                }
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = BackgroundColor,
                                        shadowElevation = 2.dp
                                    ) {

                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Text on top
                                            Text(
                                                "Total Count",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    textAlign = TextAlign.Center
                                                ),
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1F41BB),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            // Pie Chart below the text
                                            AnalyticsPieChart(
                                                studentCount = studentCount,
                                                teacherCount = teacherCount,
                                                staffCount = staffCount,
                                                onStudentClick = { navController.navigate("students") },
                                                onTeacherClick = { navController.navigate("teachers") },
                                                onStaffClick = { navController.navigate("staff") } // Assuming this navigation exists
                                            )
                                        }
                                    }
                                }
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FeeAnalyticsCard(navController)
                                }
                                
                                // Attendance Analytics Card
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .clickable { navController.navigate("attendance_analytics") },
                                        shape = RoundedCornerShape(12.dp),
                                        color = BackgroundColor,
                                        shadowElevation = 2.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Attendance Overview",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        textAlign = TextAlign.Center
                                                    ),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1F41BB),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(15.dp))
                                            
                                            // Attendance Overview with Pie Chart
                                            AttendanceOverviewCard(
                                                studentCount = studentCount,
                                                teacherCount = teacherCount,
                                                staffCount = staffCount,
                                                studentAttendance = studentAttendance,
                                                teacherAttendance = teacherAttendance,
                                                staffAttendance = staffAttendance
                                            )
                                        }
                                    }
                                }
                                
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AnimatedSummaryRow(
                                        leftTitle = "Manage Teacher Absences",
                                        rightTitle = "Pending Fees",
                                        leftIcon = Icons.Default.PersonOff,
                                        rightIcon = Icons.Default.CurrencyRupee,
                                        leftClick = { 
                                            try {
                                                navController.navigate("manage_teacher_absences")
                                            } catch (e: Exception) {
                                                showMessage("Navigation error: ${e.message}")
                                            }
                                        },
                                        rightClick = { 
                                            try {
                                                navController.navigate("pending_fees")
                                            } catch (e: Exception) {
                                                showMessage("Navigation error: ${e.message}")
                                            }
                                        },
                                        leftColor = Color(0xFFE57373), // Light red for absences
                                        rightColor = FeesRed
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

// AI Search Bar Component
@Composable
private fun AiSearchBar(modifier: Modifier = Modifier) {
    // State for search text and focus
    var searchText by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    
    // Create interaction source for the search bar
    val interactionSource = remember { MutableInteractionSource() }
    
    // Setup infinite transition for animated gradient
    val infiniteTransition = rememberInfiniteTransition(label = "ai-search-bar")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "gradientOffset"
    )
    val gradientColors = listOf(
        Color(0xFF14BD08), // Purple
        Color(0xFF9C27B0), // Blue
        Color(0xFFC2B436), // Pink
        Color(0xFF00E5FF), // Cyan
        Color(0xFF133BB2)  // Purple
    )
    val brush = Brush.linearGradient(
        colors = gradientColors,
        start = Offset(0f, 0f),
        end = Offset(400f * (0.5f + 0.5f * kotlin.math.sin(animatedOffset * 2 * Math.PI).toFloat()), 400f * (0.5f - 0.5f * kotlin.math.cos(animatedOffset * 2 * Math.PI).toFloat()))
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
                        text = "Ask AI anything...",
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
                        tint = Color(0xFF030307),
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Search",
                        tint = Color(0xFF000000),
                        modifier = Modifier.size(24.dp)
                    )
                },
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Color(0xFF1F41BB),
                    focusedLeadingIconColor = Color(0xFF1F41BB),
                    unfocusedLeadingIconColor = Color(0xFF1F41BB),
                    focusedTrailingIconColor = Color(0xFF1F41BB),
                    unfocusedTrailingIconColor = Color(0xFF1F41BB)
                ),
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchBar() {
    var searchText by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isFocused) 4.dp else 2.dp,
        color = Color.White.copy(alpha = 0.95f)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .onFocusChanged { state ->
                    isFocused = state.isFocused
                },
            placeholder = {
                Text(
                    text = "Search...",
                    fontSize = 14.sp,
                    color = if (isFocused) PrimaryBlue.copy(alpha = 0.6f) else Color.Gray
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isFocused) PrimaryBlue else Color.Gray,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(start = 4.dp)
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                cursorColor = PrimaryBlue,
                focusedLeadingIconColor = PrimaryBlue,
                unfocusedLeadingIconColor = Color.Gray.copy(alpha = 0.7f)
            ),
            singleLine = true,
            interactionSource = interactionSource,
            textStyle = TextStyle(fontSize = 14.sp)
        )
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Red,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun AnimatedSummaryRow(
    leftTitle: String,
    rightTitle: String,
    leftIcon: ImageVector,
    rightIcon: ImageVector,
    leftClick: () -> Unit,
    rightClick: () -> Unit,
    leftColor: Color,
    rightColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedSummaryCard(
            title = leftTitle,
            icon = leftIcon,
            onClick = leftClick,
            backgroundColor = leftColor,
            modifier = Modifier.weight(1f)
        )
        AnimatedSummaryCard(
            title = rightTitle,
            icon = rightIcon,
            onClick = rightClick,
            backgroundColor = rightColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AnimatedSummaryCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    // State for press animation
    var isPressed by remember { mutableStateOf(false) }
    
    // Scale animation for press effect
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )

    // Card surface with shadow and animation
    Surface(
        modifier = Modifier
            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
            .widthIn(max = 320.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(modifier),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (isPressed) 2.dp else 4.dp,
        color = backgroundColor
    ) {
        // Card content
        Row(
            modifier = Modifier
                .clickable {
                    isPressed = true
                    onClick()
                    isPressed = false
                }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card icon with animation
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        scaleX = if (isPressed) 0.9f else 1f
                        scaleY = if (isPressed) 0.9f else 1f
                    }
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Card title with animation
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = if (isPressed) 0.8f else 1f
                    }
                    .weight(1f)
            )
        }
    }
}

@Composable
fun OptionsDialog(
    category: String,
    onDismiss: () -> Unit,
    onAddClick: () -> Unit,
    onViewClick: () -> Unit,
    showAddInput: Boolean,
    showViewList: Boolean,
    inputText: String,
    onInputChange: (String) -> Unit,
    onConfirmAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
    dataList: List<String>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(category) },
        text = {
            Column {
                // Show main options if no sub-view is active
                if (!showAddInput && !showViewList) {
                    Button(
                        onClick = onAddClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add")
                    }
                    Button(
                        onClick = onViewClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View")
                    }
                }

                // Show input field for adding new item
                if (showAddInput) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        label = { Text("Enter ${category.lowercase()}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Show list of existing items
                if (showViewList) {
                    LazyColumn {
                        items(dataList) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDelete(item) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showAddInput) {
                TextButton(onClick = { onConfirmAdd(inputText) }) {
                    Text("Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Enhanced expandable item component
@Composable
fun EnhancedExpandableItem(
    title: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onItemClick: () -> Unit
) {
    // State for press animation
    var isPressed by remember { mutableStateOf(false) }

    // Item surface with shadow
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isPressed) 1.dp else 2.dp,
        color = Color.White.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    isPressed = true
                    onItemClick()
                    isPressed = false
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = PrimaryBlue,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = onExpandClick,
                modifier = Modifier
                    .size(32.dp)
                    .scale(if (isPressed) 0.9f else 1f)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAdminDashboardScreen() {
    AdminDashboardScreen(rememberNavController(), "admin_dashboard", {})
}

@Composable
fun CommonBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_ui),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        content()
    }
}

@Composable
private fun AttendanceCountRow(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
fun FeeAnalyticsCard(navController: NavController) {
    var selectedMonth by remember { mutableStateOf<Int?>(null) }
    
    // Fixed monthly data for first 6 months
    val monthlyFees = remember {
        listOf(
            120000, // Jan
            135000, // Feb
            95000,  // Mar
            110000, // Apr
            125000, // May
            140000  // Jun
        )
    }

    // Handle navigation when a month is selected
    LaunchedEffect(selectedMonth) {
        if (selectedMonth != null) {
            navController.navigate("fee_analytics/${selectedMonth}")
            selectedMonth = null
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(220.dp),
        shape = RoundedCornerShape(12.dp),
        color = BackgroundColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                "Monthly Fee",
                style = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center
                ),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F41BB),
                modifier = Modifier.fillMaxWidth()
            )
            
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val barWidth = (size.width - 40f) / 6
                                    val clickedIndex = ((offset.x - 40f) / barWidth).toInt()
                                    if (clickedIndex in monthlyFees.indices) {
                                        selectedMonth = clickedIndex
                                    }
                                }
                            }
                    ) {
                        val barWidth = (size.width - 40f) / 6
                        val maxFee = monthlyFees.maxOrNull()?.toFloat() ?: 0f
                        
                        // Draw axes
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.5f),
                            start = Offset(40f, 0f),
                            end = Offset(40f, size.height - 20f),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.5f),
                            start = Offset(40f, size.height - 20f),
                            end = Offset(size.width, size.height - 20f),
                            strokeWidth = 1f
                        )
                        
                        monthlyFees.forEachIndexed { index, fee ->
                            val barHeight = (fee / maxFee) * (size.height - 40f)
                            val performance = fee / maxFee
                            val barColor = when {
                                performance > 0.8f -> Color(0xFF4CAF50)
                                performance > 0.6f -> Color(0xFF8BC34A)
                                performance > 0.4f -> Color(0xFFFFC107)
                                performance > 0.2f -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            }
                            
                            val isSelected = selectedMonth == index
                            val barAlpha = if (isSelected) 1f else 0.7f
                            
                            // Calculate bar position
                            val barX = 40f + (index * barWidth) + 15f
                            
                            drawRect(
                                color = barColor.copy(alpha = barAlpha),
                                topLeft = Offset(
                                    barX,
                                    size.height - 20f - barHeight
                                ),
                                size = Size(barWidth - 30f, barHeight)
                            )
                            
                            if (isSelected) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    "₹${fee/1000}K",
                                    barX,
                                    size.height - 20f - barHeight - 10f,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.BLACK
                                        textSize = 30f
                                        textAlign = android.graphics.Paint.Align.LEFT
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Month labels
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
                        val barWidth = (LocalConfiguration.current.screenWidthDp - 64) / 6f
                        
                        months.forEachIndexed { index, month ->
                            Box(
                                modifier = Modifier
                                    .width(barWidth.dp)
                                    .offset(x = (-8).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun AttendanceStatItem(label: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
        }
        Text(
            text = "0",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun AttendanceOverviewCard(
    studentCount: Int,
    teacherCount: Int,
    staffCount: Int,
    studentAttendance: List<AttendanceRecord>,
    teacherAttendance: List<AttendanceRecord>,
    staffAttendance: List<AttendanceRecord>
) {
    var selectedCategory by remember { mutableStateOf("Students") }
    val categories = listOf("Students", "Teachers", "Staff")
    
    // State for gender statistics
    var genderStats by remember { mutableStateOf<Map<String, Int>>(mapOf("male" to 0, "female" to 0)) }
    var isLoadingGenderStats by remember { mutableStateOf(true) }
    var genderError by remember { mutableStateOf<String?>(null) }

    // Load gender statistics when category changes
    LaunchedEffect(selectedCategory) {
        isLoadingGenderStats = true
        try {
            FirestoreDatabase.fetchGenderStatistics(
                userType = selectedCategory,
                onSuccess = { stats ->
                    genderStats = stats
                    isLoadingGenderStats = false
                },
                onError = { e ->
                    genderError = e.message
                    isLoadingGenderStats = false
                }
            )
        } catch (e: Exception) {
            genderError = e.message
            isLoadingGenderStats = false
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = BackgroundColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Attendance & Gender Analysis",
                style = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center
                ),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F41BB),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Category Selection Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCategory = category },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedCategory == category) 
                            PrimaryBlue.copy(alpha = 0.1f) 
                        else 
                            Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (selectedCategory == category) PrimaryBlue else Color.Gray.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                    Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedCategory == category) 
                                    PrimaryBlue 
                                else 
                                    Color.Gray
                    )
                        }
                    }
                }
            }

            // Analysis Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left side: Attendance Pie Chart
                    Box(
                        modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    val (attendance, total) = when (selectedCategory) {
                        "Students" -> studentAttendance to studentCount
                        "Teachers" -> teacherAttendance to teacherCount
                        else -> staffAttendance to staffCount
                    }
                    
                    val present = attendance.count { it.status == AttendanceStatus.PRESENT }
                    val absent = attendance.count { it.status == AttendanceStatus.ABSENT }
                    val leave = attendance.count { it.status == AttendanceStatus.PERMISSION }
                    
                    AttendancePieChart(
                        present = present,
                        absent = absent,
                        leave = leave,
                        total = total
                    )
                }

                // Right side: Gender Distribution
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingGenderStats) {
                        CircularProgressIndicator(
                            color = PrimaryBlue,
                            modifier = Modifier.size(48.dp)
                        )
                    } else if (genderError != null) {
                    Text(
                            text = "Error loading gender data",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        val total = (genderStats["male"] ?: 0) + (genderStats["female"] ?: 0)
                        val malePercentage = if (total > 0) (genderStats["male"] ?: 0).toFloat() / total else 0f
                        val femalePercentage = if (total > 0) (genderStats["female"] ?: 0).toFloat() / total else 0f
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            GenderDistributionChart(
                                malePercentage = malePercentage,
                                femalePercentage = femalePercentage,
                                otherPercentage = 0f
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Show actual counts
                            Text(
                                text = "Male: ${genderStats["male"] ?: 0}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaleColor
                            )
                            Text(
                                text = "Female: ${genderStats["female"] ?: 0}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FemaleColor
                            )
                        }
                    }
                }
            }

            // Legend
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                // Attendance Legend
                Text(
                    "Attendance Status",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem("Present", PresentColor)
                    LegendItem("Absent", AbsentColor)
                    LegendItem("Leave", LeaveColor)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Gender Legend - modified to only show male and female
                Text(
                    "Gender Distribution",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem("Male", MaleColor)
                    LegendItem("Female", FemaleColor)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
                    Box(
                        modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
                    )
                    Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.DarkGray
        )
    }
}

@Composable
private fun GenderDistributionChart(
    malePercentage: Float,
    femalePercentage: Float,
    otherPercentage: Float // We'll keep this parameter to avoid breaking changes but won't use it
) {
    Canvas(
        modifier = Modifier
            .size(160.dp)
            .padding(8.dp)
    ) {
        val width = size.width
        val height = size.height
        val radius = minOf(width, height) / 2
        val centerX = width / 2
        val centerY = height / 2
        val strokeWidth = radius * 0.2f
        
        // Draw background circle
        drawCircle(
            color = Color.Gray.copy(alpha = 0.1f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(strokeWidth)
        )
        
        // Calculate angles - normalize to only male and female
        val totalPercentage = malePercentage + femalePercentage
        val normalizedMalePercentage = malePercentage / totalPercentage
        val normalizedFemalePercentage = femalePercentage / totalPercentage
        
        val maleAngle = normalizedMalePercentage * 360f
        val femaleAngle = normalizedFemalePercentage * 360f
        
        // Draw arcs for male and female
        val path = Path()
        path.addArc(
            Rect(
                left = centerX - radius,
                top = centerY - radius,
                right = centerX + radius,
                bottom = centerY + radius
            ),
            0f,
            maleAngle
        )
        drawPath(
            path = path,
            color = MaleColor,
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
        
        path.reset()
        path.addArc(
            Rect(
                left = centerX - radius,
                top = centerY - radius,
                right = centerX + radius,
                bottom = centerY + radius
            ),
            maleAngle,
            femaleAngle
        )
        drawPath(
            path = path,
            color = FemaleColor,
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
        
        // Draw percentage text in center
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                textSize = radius * 0.3f
                textAlign = android.graphics.Paint.Align.CENTER
                color = android.graphics.Color.BLACK
            }
            drawText(
                "${(normalizedMalePercentage * 100).toInt()}%",
                centerX,
                centerY,
                paint
            )
        }
    }
}

