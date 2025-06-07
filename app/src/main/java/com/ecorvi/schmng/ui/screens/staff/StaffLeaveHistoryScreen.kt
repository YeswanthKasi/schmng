package com.ecorvi.schmng.ui.screens.staff

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.StaffBottomNavigation
import com.ecorvi.schmng.models.LeaveApplication
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffLeaveHistoryScreen(
    navController: NavController,
    currentRoute: String?
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Dummy data for demonstration
    val leaveApplications = remember {
        listOf(
            LeaveApplication(
                id = "1",
                userId = "S001",
                userName = "John Doe",
                userType = LeaveApplication.TYPE_STAFF,
                fromDate = System.currentTimeMillis(),
                toDate = System.currentTimeMillis() + 86400000,
                reason = "Medical appointment",
                status = LeaveApplication.STATUS_APPROVED,
                appliedAt = System.currentTimeMillis() - 172800000,
                reviewedBy = "Admin",
                reviewedAt = System.currentTimeMillis() - 86400000,
                adminRemarks = "Approved",
                leaveType = "Sick Leave",
                department = "Administration"
            ),
            LeaveApplication(
                id = "2",
                userId = "S001",
                userName = "John Doe",
                userType = LeaveApplication.TYPE_STAFF,
                fromDate = System.currentTimeMillis() + 604800000,
                toDate = System.currentTimeMillis() + 691200000,
                reason = "Family function",
                status = LeaveApplication.STATUS_PENDING,
                appliedAt = System.currentTimeMillis(),
                leaveType = "Casual Leave",
                department = "Administration"
            ),
            LeaveApplication(
                id = "3",
                userId = "S001",
                userName = "John Doe",
                userType = LeaveApplication.TYPE_STAFF,
                fromDate = System.currentTimeMillis() - 604800000,
                toDate = System.currentTimeMillis() - 518400000,
                reason = "Personal work",
                status = LeaveApplication.STATUS_REJECTED,
                appliedAt = System.currentTimeMillis() - 691200000,
                reviewedBy = "Admin",
                reviewedAt = System.currentTimeMillis() - 604800000,
                adminRemarks = "Insufficient staff",
                leaveType = "Emergency Leave",
                department = "Administration"
            )
        )
    }

    val filters = listOf("All", "Pending", "Approved", "Rejected")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Leave History",
                        color = Color(0xFF1F41BB),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1F41BB)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color(0xFF1F41BB)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            StaffBottomNavigation(
                currentRoute = currentRoute,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Chip
            if (selectedFilter != "All") {
                FilterChip(
                    selected = true,
                    onClick = { selectedFilter = "All" },
                    label = { Text(selectedFilter) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear filter",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            // Leave Applications List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val filteredApplications = when (selectedFilter) {
                    "Pending" -> leaveApplications.filter { it.status == LeaveApplication.STATUS_PENDING }
                    "Approved" -> leaveApplications.filter { it.status == LeaveApplication.STATUS_APPROVED }
                    "Rejected" -> leaveApplications.filter { it.status == LeaveApplication.STATUS_REJECTED }
                    else -> leaveApplications
                }

                items(filteredApplications) { application ->
                    LeaveApplicationCard(application = application)
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = {
                Text(
                    "Filter by Status",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF1F41BB)
                )
            },
            text = {
                Column {
                    filters.forEach { filter ->
                        TextButton(
                            onClick = {
                                selectedFilter = filter
                                showFilterDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = filter,
                                color = if (filter == selectedFilter) Color(0xFF1F41BB) else Color.Black
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Cancel", color = Color(0xFF1F41BB))
                }
            }
        )
    }
}

@Composable
private fun LeaveApplicationCard(
    application: LeaveApplication
) {
    val statusColor = when (application.status) {
        LeaveApplication.STATUS_PENDING -> Color(0xFFFFA000)
        LeaveApplication.STATUS_APPROVED -> Color(0xFF4CAF50)
        LeaveApplication.STATUS_REJECTED -> Color(0xFFE53935)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Leave Type and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = application.leaveType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F41BB)
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = application.status.capitalize(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date Range
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatDate(application.fromDate) + " - " + formatDate(application.toDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reason
            Text(
                text = application.reason,
                style = MaterialTheme.typography.bodyLarge
            )

            if (application.status != LeaveApplication.STATUS_PENDING && application.adminRemarks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Admin Remarks
                Text(
                    text = "Remarks: ${application.adminRemarks}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Applied Date
            Text(
                text = "Applied on " + formatDate(application.appliedAt),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    )
    return dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
} 