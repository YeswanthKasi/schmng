@file:OptIn(ExperimentalMaterial3Api::class)
package com.ecorvi.schmng.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.models.LeaveApplication
import com.ecorvi.schmng.viewmodels.TeacherLeaveViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeacherLeaveListScreen(navController: NavController, viewModel: TeacherLeaveViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val leaveList by viewModel.leaveList.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Load leaves on screen open
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadMyLeaves()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Leave Applications") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("teacher_leave_apply") },
                containerColor = Color(0xFFEADCF7)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Apply for Leave")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (leaveList.isEmpty()) {
                Text(
                    text = "No leave applications yet.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(leaveList) { leave ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${dateFormat.format(Date(leave.fromDate))} to ${dateFormat.format(Date(leave.toDate))}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    StatusChip(leave.status)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Reason: ${leave.reason}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                // Show admin remark if present and leave is not pending
                                if (leave.status != LeaveApplication.STATUS_PENDING && !leave.adminRemarks.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Admin Remark: ${leave.adminRemarks}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
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
        else -> Color.Gray to status.capitalize(Locale.getDefault())
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