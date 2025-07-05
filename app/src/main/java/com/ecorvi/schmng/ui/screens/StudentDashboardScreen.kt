package com.ecorvi.schmng.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerDefaults
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import com.ecorvi.schmng.ui.data.store.TempMessageStore
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.ecorvi.schmng.ui.components.TeacherAttendanceCard
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecorvi.schmng.viewmodels.AttendanceViewModel
import com.ecorvi.schmng.ui.navigation.StudentBottomNavItem
import com.ecorvi.schmng.services.RemoteConfigService
import com.airbnb.lottie.compose.*
import com.ecorvi.schmng.R

private val PrimaryBlue = Color(0xFF1F41BB)
private val ScheduleOrange = Color(0xFFFF9800)
private val MessageBlue = Color(0xFF2196F3)
private val NoticeBlue = Color(0xFF3F51B5)
private val AttendanceGreen = Color(0xFF4CAF50)
private val BusOrange = Color(0xFFFF5722)
private val FeesRed = Color(0xFFE91E63)
private val BackgroundColor = Color.White.copy(alpha = 0.95f)
private val CardBackgroundColor = Color.White.copy(alpha = 0.98f)
private val ShadowColor = Color.Black.copy(alpha = 0.1f)

// Add UI constants
private val CARD_CORNER_RADIUS = 24.dp
private val CARD_ELEVATION = 0.dp  // Removing default elevation
private val CARD_PRESSED_ELEVATION = 2.dp
private val STANDARD_CARD_HEIGHT = 160.dp
private val QUICK_ACCESS_CARD_HEIGHT = 140.dp // Smaller height for quick access cards
private val CARD_BACKGROUND_ALPHA = 0.98f
private val ICON_BOX_SIZE = 52.dp
private val STANDARD_PADDING = 16.dp
private val HALF_PADDING = 8.dp

// Add animation specs
private val cardPressAnimationSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

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

// Function for getting current day
private fun getCurrentDayOfWeek(): String {
    val calendar = Calendar.getInstance()
    val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    return days[calendar.get(Calendar.DAY_OF_WEEK) - 1]
}

// Function to normalize day names to handle different formats
private fun normalizeDayName(day: String): String {
    // Convert to lowercase and trim for comparison
    val normalizedDay = day.trim().lowercase()
    
    // Map of possible variations to standard names
    return when (normalizedDay) {
        "mon", "monday" -> "Monday"
        "tue", "tuesday" -> "Tuesday"
        "wed", "wednesday" -> "Wednesday"
        "thu", "thursday" -> "Thursday"
        "fri", "friday" -> "Friday"
        "sat", "saturday" -> "Saturday"
        "sun", "sunday" -> "Sunday"
        else -> day // Return original if no match
    }
}

// Add this function at the top level of the file, after the imports
private fun parseTimeSlot(timeSlot: String): Int {
    return try {
        val startTime = timeSlot.split("-").firstOrNull()?.trim() ?: return 0
        val (hour, minute) = startTime.split(":").map { it.trim().toInt() }
        hour * 60 + minute
    } catch (e: Exception) {
        0
    }
}

@Composable
fun StudentAiSearchBar(modifier: Modifier = Modifier) {
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
        ), 
        label = "gradientOffset"
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
            .padding(top = HALF_PADDING, bottom = STANDARD_PADDING)
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)
                .graphicsLayer {
                    clip = true
                    shape = RoundedCornerShape(28.dp)
                    shadowElevation = CARD_ELEVATION.toPx()
                    alpha = 0.99f
                }
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
                    .background(CardBackgroundColor, RoundedCornerShape(26.dp))
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

@Composable
fun StudentBottomNavigation(
    currentRoute: String?,
    onNavigate: (StudentBottomNavItem) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.95f)),
        containerColor = Color.White.copy(alpha = 0.95f),
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            StudentBottomNavItem.Home,
            StudentBottomNavItem.Schedule,
            StudentBottomNavItem.Attendance,
            StudentBottomNavItem.Notices,
            StudentBottomNavItem.Profile
        )

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item) }
            )
        }
    }
}

@Composable
fun DashboardQuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackgroundColor.copy(alpha = CARD_BACKGROUND_ALPHA)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = CARD_ELEVATION,
            pressedElevation = CARD_PRESSED_ELEVATION
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryBlue,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    navController: NavController,
    currentRoute: String,
    onRouteSelected: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var student by remember { mutableStateOf<Person?>(null) }
    var todayTimetable by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var weeklyTimetable by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var todaySchedule by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var timeSlots by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingFees by remember { mutableStateOf<List<Fee>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current
    
    // Get app version
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    
    // Current day
    val currentDay = getCurrentDayOfWeek()
    var selectedDay by remember { mutableStateOf(currentDay) }

    var scale by remember { mutableFloatStateOf(1f) }
    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
    }

    // Fetch time slots from Firestore
    LaunchedEffect(Unit) {
        FirestoreDatabase.fetchTimeSlots(
            onComplete = { slots ->
                timeSlots = slots.sortedBy { parseTimeSlot(it) }
            },
            onFailure = { e ->
                Log.e("StudentDashboard", "Error fetching time slots: ${e.message}")
            }
        )
    }

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
                // First get student data
                val studentDoc = FirebaseFirestore.getInstance()
                    .collection("students")
                    .document(currentUser.uid)
                    .get()
                    .await()

                if (studentDoc.exists()) {
                    student = studentDoc.toObject(Person::class.java)
                    
                    // Then fetch timetable data using FirestoreDatabase
                    FirestoreDatabase.fetchTimetablesForClass(
                        classGrade = student?.className ?: "",
                        onComplete = { fetchedTimetables ->
                            weeklyTimetable = fetchedTimetables.sortedBy { timetable -> 
                                parseTimeSlot(timetable.timeSlot)
                            }

                            // Filter for today's timetable
                            todayTimetable = weeklyTimetable.filter {
                                normalizeDayName(it.dayOfWeek) == normalizeDayName(currentDay)
                            }

                            // Update today's schedule display
                            todaySchedule = todayTimetable.map { timetable ->
                                Schedule(
                                    time = timetable.timeSlot,
                                    title = "${timetable.subject} (${timetable.teacher})"
                                )
                            }

                            isLoading = false
                        },
                        onFailure = { e ->
                            Log.e("StudentDashboard", "Error fetching timetable: ${e.message}")
                            errorMessage = "Failed to load timetable: ${e.message}"
                            isLoading = false
                        }
                    )
                } else {
                    errorMessage = "Student data not found"
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("StudentDashboard", "Error loading data: ${e.message}")
                errorMessage = "Error loading data: ${e.message}"
                isLoading = false
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp),
                drawerContainerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    // Top section with menu items
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Menu",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Divider()
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("My Profile") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(StudentBottomNavItem.Profile.route)
                            }
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Schedule, contentDescription = "Schedule") },
                            label = { Text("My Schedule") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(StudentBottomNavItem.Schedule.route)
                            }
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.CheckCircleOutline, contentDescription = "Attendance") },
                            label = { Text("My Attendance") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(StudentBottomNavItem.Attendance.route)
                            }
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Announcement, contentDescription = "Notices") },
                            label = { Text("Notifications") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(StudentBottomNavItem.Notices.route)
                            }
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Event, contentDescription = "Class Events") },
                            label = { Text("Class Events") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("student_class_events")
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
                            icon = { Icon(Icons.Default.Message, contentDescription = "Messages") },
                            label = { Text("Messages") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("student_messages")
                            }
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.DirectionsBus, contentDescription = "School Bus") },
                            label = { Text("School Bus") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                Toast.makeText(
                                    context,
                                    "School Bus tracking coming soon",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
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

                    // Bottom section with version and updates
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Version $versionName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        TextButton(
                            onClick = {
                                scope.launch { drawerState.close() }
                                Toast.makeText(
                                    context,
                                    "You are using the latest version",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Check for Updates",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth()) {}
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
                    actions = {
                        var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                currentTime = System.currentTimeMillis()
                                kotlinx.coroutines.delay(1000)
                            }
                        }
                        val dateFormat = remember { java.text.SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
                        val timeFormat = remember { java.text.SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
                        val date = remember(currentTime) { dateFormat.format(Date(currentTime)) }
                        val time = remember(currentTime) { timeFormat.format(Date(currentTime)) }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 12.dp)) {
                            Text(date, color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(time, color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    )
                )
            },
            bottomBar = {
                StudentBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { item ->
                        onRouteSelected(item.route)
                    }
                )
            }
        ) { padding ->
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFF5F7FA), Color(0xFFE3ECF7))
                        )
                    )
                    .padding(padding)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(vertical = STANDARD_PADDING),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            // Section header with icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                            ) {

                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = STANDARD_PADDING)
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFFE0E0E0),
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.98f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "School Schedule",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = PrimaryBlue
                                        )
                                        IconButton(
                                            onClick = { navController.navigate("student_timetable") }
                                        ) {
                                            Icon(
                                                Icons.Default.OpenInNew,
                                                contentDescription = "View Full Timetable",
                                                tint = PrimaryBlue
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    // Day selector row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                                        days.forEach { day ->
                                            val isSelected = day == selectedDay
                                            val shortDay = day.take(3)
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable { selectedDay = day }
                                                    .background(
                                                        if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surface
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.outline,
                                                        shape = CircleShape
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = shortDay,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                    // Timetable content
                                    val isSunday = selectedDay == "Sunday"
                                    var showTable by remember(isSunday) { mutableStateOf(false) }
                                    if (isSunday) {
                                        if (!showTable) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                // Lottie animation for happy Sunday
                                                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.enjoy))
                                                val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
                                                LottieAnimation(
                                                    composition = composition,
                                                    progress = { progress },
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.95f)
                                                        .height(200.dp)
                                                        .padding(bottom = 8.dp)
                                                )
                                                Text(
                                                    text = "It's Sunday!",
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = PrimaryBlue,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                                Text(
                                                    text = "Relax, recharge, and get ready for a new week!",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = Color.Gray,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(bottom = 16.dp)
                                                )
                                                Text(
                                                    text = "\"The beautiful thing about learning is that no one can take it away from you.\"\n– B.B. King",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color(0xFF4CAF50),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(bottom = 24.dp)
                                                )
                                                Button(onClick = { showTable = true }) {
                                                    Text("View Timetable")
                                                }
                                            }
                                        } else {
                                            // Show timetable as usual for Sunday if requested
                                            TimetableTable(
                                                timeSlots = timeSlots,
                                                weeklyTimetable = weeklyTimetable,
                                                selectedDay = selectedDay
                                            )
                                        }
                                    } else {
                                        // Always show timetable for other days
                                        TimetableTable(
                                            timeSlots = timeSlots,
                                            weeklyTimetable = weeklyTimetable,
                                            selectedDay = selectedDay
                                        )
                                    }
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
        }
    }
}

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier
) {
    com.ecorvi.schmng.ui.components.ShimmerCard(modifier = modifier)
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryBlue,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .padding(horizontal = STANDARD_PADDING, vertical = STANDARD_PADDING)
    )
}

@Composable
private fun TimetableTable(
    timeSlots: List<String>,
    weeklyTimetable: List<Timetable>,
    selectedDay: String
) {
    if (timeSlots.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryBlue.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "Subject",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = "Teacher",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = "Room",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue,
                    modifier = Modifier.weight(0.7f),
                    textAlign = TextAlign.End
                )
            }
            Spacer(Modifier.height(4.dp))
            // Time slots
            timeSlots.forEachIndexed { idx, timeSlot ->
                val classForThisSlot = weeklyTimetable.find {
                    it.timeSlot == timeSlot &&
                    normalizeDayName(it.dayOfWeek) == normalizeDayName(selectedDay)
                }
                val isFilled = classForThisSlot != null
                // Always split the time slot into two lines
                val timeText = if (timeSlot.contains("-")) {
                    val parts = timeSlot.split("-")
                    if (parts.size == 2) parts[0].trim() + "-\n" + parts[1].trim() else timeSlot
                } else timeSlot
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFilled) PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = if (isFilled) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.weight(1.2f),
                        maxLines = 2
                    )
                    if (isFilled) {
                        Text(
                            text = classForThisSlot!!.subject,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color.Black,
                            modifier = Modifier.weight(1.1f)
                        )
                        Text(
                            text = classForThisSlot.teacher,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1.1f)
                        )
                        Text(
                            text = classForThisSlot.roomNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.85f),
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.End
                        )
                    } else {
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.weight(1.1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.weight(1.1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
