package com.ecorvi.schmng.ui.screens.parent

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.firebase.auth.FirebaseAuth
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.data.model.Timetable
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.navigation.ParentBottomNavItem
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ecorvi.schmng.R
import com.ecorvi.schmng.ui.theme.ParentBlue
import com.ecorvi.schmng.ui.components.ParentBottomNavigation
import java.text.SimpleDateFormat
import java.util.*

private val CardBackgroundColor = Color.White

private val ParentGradientColors = listOf(
    Color(0xFF1F41BB), // Primary Blue
    Color(0xFF4CAF50), // Green
    Color(0xFF2196F3), // Light Blue
    Color(0xFF1F41BB)  // Primary Blue
)

// Create a companion object to store cached data
private object ParentDashboardCache {
    var childInfo: Person? = null
}

@Composable
fun ParentAiSearchBar(modifier: Modifier = Modifier) {
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
        colors = ParentGradientColors,
        start = Offset(0f, 0f),
        end = Offset(800f * (0.5f + 0.5f * kotlin.math.sin(animatedOffset * 2 * Math.PI).toFloat()), 
                     800f * (0.5f - 0.5f * kotlin.math.cos(animatedOffset * 2 * Math.PI).toFloat()))
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp)
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
                    shadowElevation = 0.dp.toPx()
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
                        text = "Ask AI anything about your child's education...",
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
                        tint = ParentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Search",
                        tint = ParentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                },
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = ParentBlue,
                    focusedLeadingIconColor = ParentBlue,
                    unfocusedLeadingIconColor = ParentBlue,
                    focusedTrailingIconColor = ParentBlue,
                    unfocusedTrailingIconColor = ParentBlue
                ),
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    navController: NavController,
    currentRoute: String,
    onRouteSelected: (String) -> Unit
) {
    var childInfo by remember { mutableStateOf(ParentDashboardCache.childInfo) }
    var error by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var weeklyTimetable by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var todayTimetable by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var todaySchedule by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDay by remember { mutableStateOf(getCurrentDay()) }
    var timeSlots by remember { mutableStateOf<List<String>>(emptyList()) }
    val currentDay = getCurrentDay()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Function to fetch child information and timetable
    fun fetchChildInfo() {
        if (currentUserId != null) {
            scope.launch {
                try {
                    val student = FirestoreDatabase.getStudentForParent(currentUserId)
                    if (student != null) {
                        childInfo = student
                        ParentDashboardCache.childInfo = student
                        error = null

                        // Fetch timetable data
                        FirestoreDatabase.fetchTimetablesForClass(
                            classGrade = student.className,
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
                                Log.e("ParentDashboard", "Error fetching timetable: ${e.message}")
                                error = "Failed to load timetable: ${e.message}"
                                isLoading = false
                            }
                        )

                        // Fetch time slots
                        FirestoreDatabase.fetchTimeSlots(
                            onComplete = { slots ->
                                timeSlots = slots.sortedBy { parseTimeSlot(it) }
                            },
                            onFailure = { e ->
                                Log.e("ParentDashboard", "Error fetching time slots: ${e.message}")
                            }
                        )
                    }
                } catch (e: Exception) {
                    if (childInfo == null) {
                        error = e.message
                    }
                }
            }
        } else {
            if (childInfo == null) {
                error = "User not authenticated"
            }
        }
    }

    // Fetch data on first launch and when returning to screen
    LaunchedEffect(Unit) {
        fetchChildInfo()
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        ParentDashboardCache.childInfo = null // Clear cache on logout
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Yes", color = ParentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
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
                        // App logo and title section
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val isPreview = LocalInspectionMode.current
                            if (!isPreview) {
                                Image(
                                    painter = painterResource(id = R.drawable.ecorvilogo),
                                    contentDescription = "App Logo",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .padding(8.dp)
                                )
                            }
                            Text(
                                text = "Ecorvi School Management",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                color = ParentBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider()

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Person, contentDescription = "Student Info") },
                            label = { Text("Student Information") },
                            selected = false,
                            onClick = {
                                scope.launch { 
                                    drawerState.close()
                                    childInfo?.id?.let { 
                                        navController.navigate("parent_student_info/$it")
                                    }
                                }
                            }
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.CheckCircleOutline, contentDescription = "Attendance") },
                            label = { Text("Attendance") },
                            selected = false,
                            onClick = {
                                scope.launch { 
                                    drawerState.close()
                                    childInfo?.id?.let { 
                                        navController.navigate("parent_attendance/$it")
                                    }
                                }
                            }
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.Announcement, contentDescription = "Notices") },
                            label = { Text("Notices") },
                            selected = false,
                            onClick = {
                                scope.launch { 
                                    drawerState.close()
                                    childInfo?.id?.let { 
                                        navController.navigate("parent_notices/$it")
                                    }
                                }
                            }
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Event, contentDescription = "Events") },
                            label = { Text("Events") },
                            selected = false,
                            onClick = {
                                scope.launch { 
                                    drawerState.close()
                                    childInfo?.id?.let { 
                                        navController.navigate("parent_events/$it")
                                    }
                                }
                            }
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Messages") },
                            label = { Text("Messages") },
                            selected = false,
                            onClick = {
                                scope.launch { 
                                    drawerState.close()
                                    childInfo?.id?.let { 
                                        navController.navigate("parent_messages/$it")
                                    }
                                }
                            }
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.DirectionsBus, contentDescription = "School Bus") },
                            label = { Text("School Bus") },
                            selected = false,
                            onClick = {
                                scope.launch { 
                                    drawerState.close()
                                    Toast.makeText(
                                        context,
                                        "School Bus tracking coming soon",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Today, contentDescription = "Timetable") },
                            label = { Text("Timetable") },
                            selected = false,
                            onClick = {
                                scope.launch { 
                                    drawerState.close()
                                    childInfo?.id?.let { 
                                        navController.navigate("parent_timetable/$it")
                                    }
                                }
                            }
                        )

                        HorizontalDivider()

                        NavigationDrawerItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help") },
                            label = { Text("Help") },
                            selected = false,
                            onClick = {
                                scope.launch { 
                                    drawerState.close()
                                    Toast.makeText(
                                        context,
                                        "Help center coming soon",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout") },
                            label = { Text("Logout") },
                            selected = false,
                            onClick = {
                                scope.launch { 
                                    drawerState.close()
                                    showLogoutDialog = true
                                }
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
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        TextButton(
                            onClick = {
                                scope.launch { 
                                    drawerState.close()
                                    Toast.makeText(
                                        context,
                                        "You are using the latest version",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Check for Updates",
                                style = MaterialTheme.typography.bodySmall,
                                color = ParentBlue
                            )
                        }
                    }
                }
            }
        }
    ) {
        CommonBackground {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Column {
                        TopAppBar(
                            title = { Text("Parent Dashboard", color = ParentBlue) },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "Menu",
                                        tint = ParentBlue
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.White
                            )
                        )
                        
                        // AI Search Bar
                        ParentAiSearchBar()
                    }
                },
                bottomBar = {
                    ParentBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigate = { item ->
                            val route = when (item) {
                                ParentBottomNavItem.Home -> item.route
                                else -> "${item.route}/${childInfo?.id ?: ""}"
                            }
                            if (childInfo?.id != null || item == ParentBottomNavItem.Home) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please wait while loading student information...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Student info card at top
                        childInfo?.let { student ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFE8EEFF)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "${student.firstName} ${student.lastName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ParentBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Class: ${student.className}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ParentBlue
                                    )
                                }
                            }
                        }

                        // Timetable Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
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
                                        text = "Today's Schedule",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ParentBlue
                                    )
                                    IconButton(
                                        onClick = { 
                                            childInfo?.id?.let {
                                                navController.navigate("parent_timetable/$it")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.OpenInNew,
                                            contentDescription = "View Full Timetable",
                                            tint = ParentBlue
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                
                                // Day selector row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEachIndexed { index, shortDay ->
                                        val fullDay = when(index) {
                                            0 -> "Monday"
                                            1 -> "Tuesday"
                                            2 -> "Wednesday"
                                            3 -> "Thursday"
                                            4 -> "Friday"
                                            else -> "Saturday"
                                        }
                                        val isSelected = normalizeDayName(selectedDay) == normalizeDayName(fullDay)
                                        Surface(
                                            modifier = Modifier.height(32.dp),
                                            shape = MaterialTheme.shapes.small,
                                            color = if (isSelected) ParentBlue else Color.Transparent,
                                            border = BorderStroke(1.dp, if (isSelected) ParentBlue else Color.Gray)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clickable { selectedDay = fullDay }
                                                    .padding(horizontal = 12.dp),
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
                                }
                                
                                // Timetable content
                                if (timeSlots.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = ParentBlue)
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        // Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "Time",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = ParentBlue,
                                                modifier = Modifier.weight(1.2f)
                                            )
                                            Text(
                                                text = "Subject",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = ParentBlue,
                                                modifier = Modifier.weight(1.1f)
                                            )
                                            Text(
                                                text = "Teacher",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = ParentBlue,
                                                modifier = Modifier.weight(1.1f)
                                            )
                                            Text(
                                                text = "Room",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = ParentBlue,
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
                                            val timeText = if (idx == 0 && timeSlot.contains("-")) {
                                                val parts = timeSlot.split("-")
                                                if (parts.size == 2) parts[0].trim() + "-\n" + parts[1].trim() else timeSlot
                                            } else timeSlot
                                            
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .background(
                                                        if (isFilled) ParentBlue.copy(alpha = 0.1f)
                                                        else Color.Transparent,
                                                        MaterialTheme.shapes.small
                                                    )
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = timeText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isFilled) ParentBlue else Color.Gray,
                                                    modifier = Modifier.weight(1.2f)
                                                )
                                                Text(
                                                    text = classForThisSlot?.subject ?: "-",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isFilled) ParentBlue else Color.Gray,
                                                    modifier = Modifier.weight(1.1f)
                                                )
                                                Text(
                                                    text = classForThisSlot?.teacher ?: "-",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isFilled) ParentBlue else Color.Gray,
                                                    modifier = Modifier.weight(1.1f)
                                                )
                                                Text(
                                                    text = classForThisSlot?.roomNumber ?: "-",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isFilled) ParentBlue else Color.Gray,
                                                    modifier = Modifier.weight(0.7f),
                                                    textAlign = TextAlign.End
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getCurrentDay(): String {
    return SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
}

private fun normalizeDayName(day: String): String {
    return day.trim().lowercase()
}

private fun parseTimeSlot(timeSlot: String): Int {
    return try {
        val startTime = timeSlot.split("-")[0].trim()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        timeFormat.isLenient = false
        val date = timeFormat.parse(startTime) ?: return 0
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    } catch (e: Exception) {
        0
    }
}

data class Schedule(
    val time: String,
    val title: String
) 