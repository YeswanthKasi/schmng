@file:OptIn(ExperimentalMaterial3Api::class)
package com.ecorvi.schmng.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.viewmodels.TeacherLeaveViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeacherLeaveDetailsScreen(
    navController: NavController,
    leaveId: String,
    viewModel: TeacherLeaveViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val leave by viewModel.leaveDetails.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Load leave details when screen opens
    LaunchedEffect(leaveId) {
        viewModel.loadLeaveDetails(leaveId)
    }

    // Show error if any
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (leave == null) {
                Text(
                    text = "Leave application not found",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Leave Period",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("From: ${dateFormat.format(Date(leave!!.fromDate))}")
                            Text("To: ${dateFormat.format(Date(leave!!.toDate))}")
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Leave Details",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("Reason: ${leave!!.reason}")
                            Text("Status: ${leave!!.status.capitalize()}")
                            if (!leave!!.adminRemarks.isNullOrBlank()) {
                                Text("Admin Remarks: ${leave!!.adminRemarks}")
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Application Info",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("Applied on: ${dateFormat.format(Date(leave!!.appliedAt))}")
                            if (leave!!.reviewedAt > 0) {
                                Text("Reviewed on: ${dateFormat.format(Date(leave!!.reviewedAt))}")
                            }
                        }
                    }
                }
            }
        }
    }
} 