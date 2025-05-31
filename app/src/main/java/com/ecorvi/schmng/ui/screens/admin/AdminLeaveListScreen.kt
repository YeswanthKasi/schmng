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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.models.LeaveApplication
import com.ecorvi.schmng.viewmodels.AdminLeaveViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val statusOptions = listOf("pending", "approved", "rejected", "all")
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
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusOptions.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (leaveList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No leave requests.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(leaveList) { leave ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { navController.navigate("admin_leave_details/${leave.id}") },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Teacher: ${leave.teacherName}")
                                Text("From: ${if (leave.fromDate > 0) dateFormat.format(Date(leave.fromDate)) else "-"}")
                                Text("To: ${if (leave.toDate > 0) dateFormat.format(Date(leave.toDate)) else "-"}")
                                Text("Reason: ${leave.reason}")
                                Text("Status: ${leave.status}")
                                if (!leave.adminRemarks.isNullOrBlank()) {
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