package com.ecorvi.schmng.ui.screens.teacher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.models.AttendanceRecord
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.UserType
import com.ecorvi.schmng.ui.components.AttendancePieChart
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.viewmodels.AttendanceViewModel
import com.ecorvi.schmng.viewmodels.TeacherDashboardViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// Color definitions
private val colorPresent = Color(0xFF4CAF50)
private val colorAbsent = Color(0xFFF44336)
private val colorLeave = Color(0xFFFFA000)
private val colorAccent = Color(0xFF1F41BB)
private val colorBackground = Color(0xFFF5F5F5)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TeacherAttendanceAnalyticsScreen(
    navController: NavController,
    attendanceViewModel: AttendanceViewModel,
    teacherViewModel: TeacherDashboardViewModel
) {
    val teacherData by teacherViewModel.teacherData.collectAsState()
    val timetables by teacherViewModel.timetables.collectAsState()
    val loading by teacherViewModel.loading.collectAsState()
    val attendanceLoading by teacherViewModel.attendanceLoading.collectAsState()
    val error by teacherViewModel.error.collectAsState()
    val attendanceStats by teacherViewModel.attendanceStats.collectAsState()
    val attendanceRecords by teacherViewModel.attendanceRecords.collectAsState()
    val selectedMonth by teacherViewModel.selectedMonth.collectAsState()
    val selectedYear by teacherViewModel.selectedYear.collectAsState()
    
    // Get teacher's classes from timetables
    val teacherClasses = timetables.map { it.classGrade }.distinct()

    // Add filter state
    var selectedFilter by remember { mutableStateOf("This Month") }
    
    // Add loading state for filtering
    var isFiltering by remember { mutableStateOf(false) }
    
    // Update LaunchedEffect to handle loading state
    LaunchedEffect(selectedFilter) {
        isFiltering = true
        val calendar = Calendar.getInstance()
        if (selectedFilter == "This Month") {
            teacherViewModel.fetchMonthlyAttendance(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH)
            )
        } else {
            teacherViewModel.fetchWeeklyAttendance()
        }
        delay(500) // Minimum loading time for smooth UX
        isFiltering = false
    }

    LaunchedEffect(selectedMonth, selectedYear) {
        if (selectedFilter == "This Month") {
            isFiltering = true
            teacherViewModel.fetchMonthlyAttendance(selectedYear, selectedMonth)
            delay(500) // Minimum loading time for smooth UX
            isFiltering = false
        }
    }
    
    // Calculate attendance statistics
    val totalRecords = attendanceRecords.size
    val presentCount = attendanceStats.presentCount
    val absentCount = attendanceStats.absentCount
    val leaveCount = attendanceStats.leaveCount
    val attendanceRate = attendanceStats.attendanceRate
    
    // Filter by class state
    var selectedClass by remember { mutableStateOf<String?>(null) }
    
    // Filter records by selected class if needed
    val filteredRecords = if (selectedClass != null && selectedClass != "All Classes") {
        attendanceRecords.filter { record ->
            record.classId == selectedClass || record.className == selectedClass
        }
    } else {
        attendanceRecords
    }
    
    // Group records by day for trend analysis
    val recordsByDay = filteredRecords
        .groupBy { 
            val recordDate = Date(it.date)
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(recordDate)
        }
        .mapValues { (_, records) ->
            Triple(
                records.count { it.status == AttendanceStatus.PRESENT },
                records.count { it.status == AttendanceStatus.ABSENT },
                records.count { it.status == AttendanceStatus.PERMISSION }
            )
        }
    
    // Group attendance by class
    val attendanceByClass = filteredRecords
        .groupBy { record -> 
            record.className ?: "Unknown Class"
        }
        .mapValues { (_, records) ->
            Triple(
                records.count { it.status == AttendanceStatus.PRESENT },
                records.count { it.status == AttendanceStatus.ABSENT },
                records.count { it.status == AttendanceStatus.PERMISSION }
            )
        }
    
    val scrollState = rememberScrollState()
    var isRefreshing by remember { mutableStateOf(false) }
    
    val transition = updateTransition(targetState = loading || attendanceLoading, label = "loading")
    val alpha by transition.animateFloat(
        label = "alpha",
        transitionSpec = { tween(durationMillis = 300) }
    ) { if (it) 0.6f else 1f }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Text("Attendance Analytics") 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorAccent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { paddingValues ->
        CommonBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .alpha(if (isFiltering) 0.3f else 1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Month Selector with Loading Indicator
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = colorAccent)
                        ) {
                            Column {
                                MonthSelector(
                                    selectedMonth = selectedMonth,
                                    selectedYear = selectedYear,
                                    onPreviousMonth = { 
                                        teacherViewModel.selectPreviousMonth()
                                    },
                                    onNextMonth = { 
                                        teacherViewModel.selectNextMonth()
                                    }
                                )
                                
                                // Subtle loading indicator
                                AnimatedVisibility(
                                    visible = isFiltering,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp),
                                        color = Color.White.copy(alpha = 0.7f),
                                        trackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }

                    // Filter Chips with Animation
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedFilter == "This Month",
                                        onClick = { 
                                            selectedFilter = "This Month"
                                        },
                                        label = { Text("This Month") },
                                        leadingIcon = if (selectedFilter == "This Month") {
                                            {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colorAccent.copy(alpha = 0.15f),
                                            selectedLabelColor = colorAccent
                                        )
                                    )
                                    
                                    FilterChip(
                                        selected = selectedFilter == "This Week",
                                        onClick = { 
                                            selectedFilter = "This Week"
                                        },
                                        label = { Text("This Week") },
                                        leadingIcon = if (selectedFilter == "This Week") {
                                            {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colorAccent.copy(alpha = 0.15f),
                                            selectedLabelColor = colorAccent
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Summary Card with Animations
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                colorAccent,
                                                colorAccent.copy(alpha = 0.8f)
                                            )
                                        )
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Monthly Attendance Analytics",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    AnimatedAttendanceCircle(
                                        attendanceRate = attendanceStats.attendanceRate,
                                        modifier = Modifier.size(160.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        ModernStatItem(
                                            count = attendanceStats.presentCount,
                                            label = "Present",
                                            color = colorPresent
                                        )
                                        
                                        ModernStatItem(
                                            count = attendanceStats.absentCount,
                                            label = "Absent",
                                            color = colorAbsent
                                        )
                                        
                                        ModernStatItem(
                                            count = attendanceStats.leaveCount,
                                            label = "Leave",
                                            color = colorLeave
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Class-wise Attendance with modern design
                    item {
                        AnimatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.School,
                                        contentDescription = null,
                                        tint = colorAccent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Class-wise Attendance",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorAccent
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (attendanceByClass.isEmpty()) {
                                    Text(
                                        text = "No class data available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                } else {
                                    attendanceByClass.forEach { (className, counts) ->
                                        val (present, absent, leave) = counts
                                        val total = present + absent + leave
                                        val rate = if (total > 0) (present.toFloat() / total) else 0f
                                        
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = className,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                
                                                Text(
                                                    text = "${(rate * 100).roundToInt()}%",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = when {
                                                        rate >= 0.9f -> colorPresent
                                                        rate >= 0.75f -> colorLeave
                                                        else -> colorAbsent
                                                    },
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            AnimatedProgressBar(
                                                progress = rate,
                                                color = when {
                                                    rate >= 0.9f -> colorPresent
                                                    rate >= 0.75f -> colorLeave
                                                    else -> colorAbsent
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                AttendanceDetailItem(
                                                    count = present,
                                                    label = "Present",
                                                    color = colorPresent
                                                )
                                                
                                                AttendanceDetailItem(
                                                    count = absent,
                                                    label = "Absent",
                                                    color = colorAbsent
                                                )
                                                
                                                AttendanceDetailItem(
                                                    count = leave,
                                                    label = "Leave",
                                                    color = colorLeave
                                                )
                                            }
                                        }
                                        
                                        if (attendanceByClass.entries.last().key != className) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                color = Color.LightGray.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Daily Trend with modern design
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = colorAccent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Daily Attendance Trend",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorAccent
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (recordsByDay.isEmpty()) {
                                    Text(
                                        text = "No trend data available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                } else {
                                    // Display trend data
                                    recordsByDay.entries.sortedBy { it.key }.forEach { (date, counts) ->
                                        val (present, absent, leave) = counts
                                        val total = present + absent + leave
                                        val rate = if (total > 0) (present.toFloat() / total) * 100 else 0f
                                        
                                        val displayDate = try {
                                            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
                                                ?: throw Exception("Failed to parse date")
                                            SimpleDateFormat("EEE, MMM d", Locale.US).format(parsedDate)
                                        } catch (e: Exception) {
                                            date
                                        }
                                        
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = displayDate,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                
                                                Text(
                                                    text = "${rate.roundToInt()}%",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = when {
                                                        rate >= 90 -> colorPresent
                                                        rate >= 75 -> colorLeave
                                                        else -> colorAbsent
                                                    },
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // Modern progress indicator with glow effect
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                            ) {
                                                // Background
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(8.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFEEEEEE))
                                                )
                                                
                                                // Progress
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(rate / 100f)
                                                        .height(8.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            brush = Brush.horizontalGradient(
                                                                colors = when {
                                                                    rate >= 90 -> listOf(colorPresent, colorPresent.copy(alpha = 0.7f))
                                                                    rate >= 75 -> listOf(colorLeave, colorLeave.copy(alpha = 0.7f))
                                                                    else -> listOf(colorAbsent, colorAbsent.copy(alpha = 0.7f))
                                                                }
                                                            )
                                                        )
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // Attendance details
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                AttendanceDetailItem(
                                                    count = present,
                                                    label = "Present",
                                                    color = colorPresent
                                                )
                                                
                                                AttendanceDetailItem(
                                                    count = absent,
                                                    label = "Absent",
                                                    color = colorAbsent
                                                )
                                                
                                                AttendanceDetailItem(
                                                    count = leave,
                                                    label = "Leave",
                                                    color = colorLeave
                                                )
                                            }
                                        }
                                        
                                        if (recordsByDay.entries.last().key != date) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                color = Color.LightGray.copy(alpha = 0.5f)
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

@Composable
private fun StatItem(
    label: String,
    value: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
private fun ModernStatItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}

@Composable
private fun AttendanceDetailItem(
    count: Int,
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.bodySmall,
            color = Color.DarkGray
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MonthSelector(
    selectedMonth: Int,
    selectedYear: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier
                .scale(animateFloatAsState(1f).value)
        ) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = "Previous Month",
                tint = Color.White
            )
        }

        AnimatedContent(
            targetState = Pair(selectedMonth, selectedYear),
            transitionSpec = {
                slideInHorizontally { height -> height } + fadeIn() with
                slideOutHorizontally { height -> -height } + fadeOut()
            }
        ) { (month, year) ->
            val calendar = remember { Calendar.getInstance() }
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            
            Text(
                text = "${calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())} $year",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Only show next month button if not current month
        val currentCalendar = Calendar.getInstance()
        val calendar = remember { Calendar.getInstance() }.apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
        }
        
        if (calendar.get(Calendar.YEAR) < currentCalendar.get(Calendar.YEAR) ||
            (calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
            calendar.get(Calendar.MONTH) < currentCalendar.get(Calendar.MONTH))) {
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = Color.White
                )
            }
        } else {
            // Placeholder to maintain layout
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun ShimmerEffect() {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val shimmerColorShades = listOf(
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.5f),
        Color.White.copy(alpha = 0.3f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColorShades,
                    start = Offset(translateAnim - 1000f, 0f),
                    end = Offset(translateAnim, 0f)
                )
            )
    )
}

@Composable
private fun AnimatedProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progressAnimation by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )
    
    Box(
        modifier = modifier
            .height(8.dp)
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFEEEEEE))
        )
        
        // Progress
        Box(
            modifier = Modifier
                .fillMaxWidth(progressAnimation)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(color, color.copy(alpha = 0.7f))
                    )
                )
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            initialAlpha = 0.3f
        ) + expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(durationMillis = 300)
        ),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            content()
        }
    }
}

@Composable
private fun AnimatedAttendanceCircle(
    attendanceRate: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = attendanceRate / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing)
    )
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            
            // Background circle
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )
            
            // Progress arc
            drawArc(
                color = when {
                    attendanceRate >= 90f -> colorPresent
                    attendanceRate >= 75f -> colorLeave
                    else -> colorAbsent
                },
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${attendanceRate.roundToInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Attendance",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
} 