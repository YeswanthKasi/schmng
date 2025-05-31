package com.ecorvi.schmng.ui.screens.teacher

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.data.model.Timetable
import com.ecorvi.schmng.viewmodels.TeacherDashboardViewModel
import com.ecorvi.schmng.models.ScheduleItem
import com.ecorvi.schmng.models.TeacherSchedule
import java.text.SimpleDateFormat
import java.util.*

private val PrimaryBlue = Color(0xFF1F41BB)

private val daysOfWeek = mapOf(
    "Monday" to "Mon",
    "Tuesday" to "Tue",
    "Wednesday" to "Wed",
    "Thursday" to "Thu",
    "Friday" to "Fri",
    "Saturday" to "Sat"
)

private fun formatTimeSlot(time: String): String {
    return try {
        val parts = time.split("-").map { it.trim() }
        if (parts.size != 2) return time
        
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        val outputFormat = SimpleDateFormat("h:mm a", Locale.US)
        timeFormat.isLenient = false
        
        val startTime = timeFormat.parse(parts[0])
        val endTime = timeFormat.parse(parts[1])
        
        if (startTime == null || endTime == null) return time
        
        "${outputFormat.format(startTime)} - ${outputFormat.format(endTime)}"
    } catch (e: Exception) {
        time
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    navController: NavController,
    viewModel: TeacherDashboardViewModel
) {
    val teacherData by viewModel.teacherData.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val timeSlots by viewModel.timeSlots.collectAsState()
    val timetables by viewModel.timetables.collectAsState()
    val context = LocalContext.current
    val currentDay = getCurrentDay()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Dashboard") },
                actions = {
                    IconButton(onClick = { 
                        viewModel.signOut()
                        // Clear only necessary preferences
                        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .remove("user_role")
                            .remove("stay_signed_in")
                            .remove("fcm_token")
                            .apply()
                        // Navigate to login
                        navController.navigate("login") {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        CommonBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryBlue
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "${teacherData?.firstName} ${teacherData?.lastName}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                }

                // Error message if any
                error?.let { errorMessage ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Today's Schedule Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(20.dp)
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
                                color = PrimaryBlue
                            )
                            IconButton(onClick = { navController.navigate("teacher_timetable") }) {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "View Full Timetable",
                                    tint = PrimaryBlue
                                )
                            }
                        }

                        if (loading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = PrimaryBlue)
                            }
                        } else if (timeSlots.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No classes scheduled",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // Grid header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
                            ) {
                                // Empty cell for time slot column
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .fillMaxHeight()
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Time",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                // Day headers
                                daysOfWeek.forEach { (fullDay, shortDay) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                            .background(
                                                if (fullDay == currentDay)
                                                    MaterialTheme.colorScheme.primaryContainer
                                                else
                                                    Color.Transparent
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = shortDay,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Time slot rows
                            timeSlots.forEach { timeSlot ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    // Time slot column
                                    Box(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .fillMaxHeight()
                                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = formatTimeSlot(timeSlot),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Day columns
                                    daysOfWeek.forEach { (fullDay, _) ->
                                        val entry = timetables.find {
                                            it.timeSlot.trim().equals(timeSlot.trim(), ignoreCase = true) &&
                                            it.dayOfWeek.trim().equals(fullDay.trim(), ignoreCase = true)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                                .background(
                                                    if (fullDay == currentDay)
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                                    else
                                                        Color.Transparent
                                                )
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (entry != null) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = entry.subject,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = entry.classGrade,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    if (entry.roomNumber.isNotBlank()) {
                                                        Text(
                                                            text = "Room ${entry.roomNumber}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            textAlign = TextAlign.Center
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

                // Quick Actions Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Schedule,
                        title = "Timetable",
                        onClick = { navController.navigate("teacher_timetable") }
                    )
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Group,
                        title = "Students",
                        onClick = { navController.navigate("teacher_students") }
                    )
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Event,
                        title = "Leave",
                        onClick = { navController.navigate("teacher_leave_list") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        title = "Attendance",
                        onClick = { navController.navigate("teacher_attendance") }
                    )
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Person,
                        title = "Profile",
                        onClick = { navController.navigate("teacher_profile") }
                    )
                }

                // Notice Board Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("teacher_notice_list") },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notice Board",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.Default.Announcement,
                                contentDescription = "Notices",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create and manage notices for students",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

private fun getCurrentDay(): String {
    val calendar = Calendar.getInstance()
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> "Sunday"
    }
} 