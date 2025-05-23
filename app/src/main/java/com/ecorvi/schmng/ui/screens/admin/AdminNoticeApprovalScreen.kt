package com.ecorvi.schmng.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ecorvi.schmng.models.Notice
import com.ecorvi.schmng.viewmodels.NoticeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNoticeApprovalScreen(
    navController: NavController,
    viewModel: NoticeViewModel = viewModel()
) {
    val notices by viewModel.notices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var showRejectDialog by remember { mutableStateOf(false) }
    var selectedNoticeId by remember { mutableStateOf<String?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    
    val pendingNotices = notices.filter { it.status == Notice.STATUS_PENDING }
    
    LaunchedEffect(Unit) {
        viewModel.loadNotices(Notice.STATUS_PENDING)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notice Approvals") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (pendingNotices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No notices pending approval")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pendingNotices) { notice ->
                    NoticeApprovalCard(
                        notice = notice,
                        onApprove = { viewModel.approveNotice(notice.id) },
                        onReject = {
                            selectedNoticeId = notice.id
                            showRejectDialog = true
                        }
                    )
                }
            }
        }
    }
    
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = {
                showRejectDialog = false
                selectedNoticeId = null
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
                        selectedNoticeId?.let { id ->
                            viewModel.rejectNotice(id, rejectionReason)
                        }
                        showRejectDialog = false
                        selectedNoticeId = null
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
                        showRejectDialog = false
                        selectedNoticeId = null
                        rejectionReason = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun NoticeApprovalCard(
    notice: Notice,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                Column {
                    Text(
                        text = notice.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "By ${notice.authorName}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Row {
                    IconButton(onClick = onApprove) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Approve",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onReject) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Reject",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = notice.content,
                color = Color.DarkGray
            )
            
            if (notice.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Attachments",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = "${notice.attachments.size} attachment(s)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Target: ${notice.targetClass}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Submitted: ${
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(notice.updatedAt))
                    }",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            if (notice.priority == Notice.PRIORITY_HIGH) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.PriorityHigh,
                        contentDescription = "High Priority",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "High Priority",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
} 