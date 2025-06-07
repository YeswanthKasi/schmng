@file:OptIn(ExperimentalMaterial3Api::class)
package com.ecorvi.schmng.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.models.LeaveApplication
import com.ecorvi.schmng.viewmodels.AdminLeaveViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminLeaveListScreen(
    navController: NavController,
    viewModel: AdminLeaveViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val leaveList by viewModel.leaveList.collectAsState()
    val loading by viewModel.loading.collectAsState()
    var selectedStatus by remember { mutableStateOf("pending") }
    var selectedUserType by remember { mutableStateOf("all") }
    val statusOptions = listOf("pending", "approved", "rejected", "all")
    val userTypeOptions = listOf("all", LeaveApplication.TYPE_TEACHER, LeaveApplication.TYPE_STAFF)
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Load leaves when status changes
    LaunchedEffect(selectedStatus) {
        viewModel.loadLeaves(selectedStatus)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave Requests") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1F41BB)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status Filter
            Text(
                "Filter by Status",
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF1F41BB)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusOptions.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status.replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1F41BB),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // User Type Filter
            Text(
                "Filter by User Type",
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF1F41BB)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                userTypeOptions.forEach { type ->
                    FilterChip(
                        selected = selectedUserType == type,
                        onClick = { selectedUserType = type },
                        label = {
                            Text(
                                when (type) {
                                    "all" -> "All"
                                    LeaveApplication.TYPE_TEACHER -> "Teachers"
                                    LeaveApplication.TYPE_STAFF -> "Staff"
                                    else -> type.replaceFirstChar { it.uppercase() }
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1F41BB),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1F41BB))
                }
            } else if (leaveList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No leave requests found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val filteredList = leaveList.filter {
                        (selectedUserType == "all" || it.userType == selectedUserType)
                    }

                    items(filteredList) { leave ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("admin_leave_details/${leave.id}") },
                            colors = CardDefaults.cardColors(
                                containerColor = when (leave.userType) {
                                    LeaveApplication.TYPE_TEACHER -> Color(0xFFF3E5F5)
                                    LeaveApplication.TYPE_STAFF -> Color(0xFFE3F2FD)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = leave.userName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color(0xFF1F41BB)
                                        )
                                        Text(
                                            text = "${leave.userType.capitalize()} - ${leave.department}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                    }
                                    StatusChip(leave.status)
                                }

                                Text(
                                    text = "From: ${dateFormat.format(Date(leave.fromDate))}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "To: ${dateFormat.format(Date(leave.toDate))}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Reason: ${leave.reason}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (!leave.adminRemarks.isNullOrBlank()) {
                                    Text(
                                        text = "Remarks: ${leave.adminRemarks}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF1F41BB)
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

@Composable
private fun StatusChip(status: String) {
    val (color, label) = when (status) {
        LeaveApplication.STATUS_PENDING -> Color(0xFFFFB300) to "Pending"
        LeaveApplication.STATUS_APPROVED -> Color(0xFF43A047) to "Approved"
        LeaveApplication.STATUS_REJECTED -> Color(0xFFE53935) to "Rejected"
        else -> Color.Gray to status.capitalize()
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
} 