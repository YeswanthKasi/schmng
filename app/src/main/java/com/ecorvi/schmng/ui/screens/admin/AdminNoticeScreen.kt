package com.ecorvi.schmng.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ecorvi.schmng.models.Notice
import com.ecorvi.schmng.viewmodels.NoticeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNoticeScreen(
    navController: NavController,
    viewModel: NoticeViewModel = viewModel()
) {
    var selectedFilter by remember { mutableStateOf(Notice.STATUS_PENDING) }
    val notices by viewModel.notices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedNotices by remember { mutableStateOf(setOf<String>()) }
    var showRejectDialog by remember { mutableStateOf<String?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    
    val filterOptions = listOf(
        Notice.STATUS_PENDING to "Pending Approval",
        Notice.STATUS_APPROVED to "Approved",
        Notice.STATUS_REJECTED to "Rejected"
    )
    
    LaunchedEffect(selectedFilter) {
        viewModel.loadNotices(selectedFilter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notice Management") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (selectedNotices.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                selectedNotices.forEach { noticeId ->
                                    viewModel.deleteNotice(noticeId)
                                }
                                selectedNotices = emptySet()
                            }
                        ) {
                            Icon(Icons.Default.Delete, "Delete Selected")
                        }
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
            // Filter chips
            LazyRow(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { (value, label) ->
                    FilterChip(
                        selected = selectedFilter == value,
                        onClick = { selectedFilter = value },
                        label = { Text(label) }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (notices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (selectedFilter) {
                            Notice.STATUS_PENDING -> "No notices pending approval"
                            Notice.STATUS_APPROVED -> "No approved notices"
                            Notice.STATUS_REJECTED -> "No rejected notices"
                            else -> "No notices found"
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notices) { notice ->
                        NoticeCard(
                            notice = notice,
                            isSelected = notice.id in selectedNotices,
                            onSelect = { selected ->
                                selectedNotices = if (selected) {
                                    selectedNotices + notice.id
                                } else {
                                    selectedNotices - notice.id
                                }
                            },
                            onApprove = if (notice.status == Notice.STATUS_PENDING) {
                                { viewModel.approveNotice(notice.id) }
                            } else null,
                            onReject = if (notice.status == Notice.STATUS_PENDING) {
                                { showRejectDialog = notice.id }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Rejection dialog
    if (showRejectDialog != null) {
        AlertDialog(
            onDismissRequest = { 
                showRejectDialog = null
                rejectionReason = ""
            },
            title = { Text("Reject Notice") },
            text = {
                OutlinedTextField(
                    value = rejectionReason,
                    onValueChange = { rejectionReason = it },
                    label = { Text("Reason for rejection") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRejectDialog?.let { noticeId ->
                            viewModel.rejectNotice(noticeId, rejectionReason)
                        }
                        showRejectDialog = null
                        rejectionReason = ""
                    },
                    enabled = rejectionReason.isNotBlank()
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showRejectDialog = null
                        rejectionReason = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticeCard(
    notice: Notice,
    isSelected: Boolean,
    onSelect: (Boolean) -> Unit,
    onApprove: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surface
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = onSelect
                    )
                    Text(
                        text = notice.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (notice.status == Notice.STATUS_PENDING) {
                    Row {
                        IconButton(
                            onClick = { onApprove?.invoke() }
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                "Approve",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { onReject?.invoke() }
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                "Reject",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = notice.content,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "By: ${notice.authorName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Target: ${notice.targetClass}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(notice.createdAt)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
} 