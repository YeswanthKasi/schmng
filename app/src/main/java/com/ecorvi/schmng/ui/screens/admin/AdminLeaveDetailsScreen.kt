@file:OptIn(ExperimentalMaterial3Api::class)
package com.ecorvi.schmng.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.viewmodels.AdminLeaveViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminLeaveDetailsScreen(
    navController: NavController,
    leaveId: String,
    viewModel: AdminLeaveViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val leave by viewModel.leaveDetails.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    var adminRemarks by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Load leave details on open
    LaunchedEffect(leaveId) {
        viewModel.loadLeaveDetails(leaveId)
    }
    // Set adminRemarks when leave loads
    LaunchedEffect(leave) {
        adminRemarks = leave?.adminRemarks ?: ""
    }
    // Show error
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave Request Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (loading || leave == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (loading) CircularProgressIndicator() else Text("Loading...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Teacher: ${leave!!.teacherName}")
                Text("From: ${dateFormat.format(Date(leave!!.fromDate))}")
                Text("To: ${dateFormat.format(Date(leave!!.toDate))}")
                Text("Reason: ${leave!!.reason}")
                Text("Status: ${leave!!.status}")
                OutlinedTextField(
                    value = adminRemarks,
                    onValueChange = { adminRemarks = it },
                    label = { Text("Admin Remarks") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.updateLeaveStatus(leaveId, "approved", adminRemarks)
                            navController.popBackStack()
                        },
                        enabled = !loading && leave!!.status == "pending"
                    ) { Text("Approve") }
                    Button(
                        onClick = {
                            viewModel.updateLeaveStatus(leaveId, "rejected", adminRemarks)
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        enabled = !loading && leave!!.status == "pending"
                    ) { Text("Reject") }
                }
            }
        }
    }
} 