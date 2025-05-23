package com.ecorvi.schmng.ui.screens.teacher

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
fun TeacherNoticeDraftListScreen(
    navController: NavController,
    viewModel: NoticeViewModel = viewModel()
) {
    val notices by viewModel.notices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Filter drafts and pending notices
    val drafts = notices.filter { it.status == Notice.STATUS_DRAFT }
    val pendingNotices = notices.filter { it.status == Notice.STATUS_PENDING }
    
    LaunchedEffect(Unit) {
        viewModel.loadNotices()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Notices") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("teacher_notice_create") }) {
                        Icon(Icons.Default.Add, "Create Notice")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
        } else if (drafts.isEmpty() && pendingNotices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No notices yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (drafts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Drafts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(drafts) { notice ->
                        NoticeDraftCard(
                            notice = notice,
                            onEdit = { navController.navigate("teacher_notice_create/${notice.id}") },
                            onDelete = { viewModel.deleteNotice(notice.id) },
                            onSubmit = { viewModel.submitForApproval(notice.id) }
                        )
                    }
                }
                
                if (pendingNotices.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Approval",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(pendingNotices) { notice ->
                        NoticePendingCard(notice = notice)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticeDraftCard(
    notice: Notice,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
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
                    text = notice.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onSubmit) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Submit for Approval",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = notice.content,
                maxLines = 2,
                color = Color.Gray
            )
            
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
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(notice.createdAt)),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun NoticePendingCard(notice: Notice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = notice.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.Pending,
                    contentDescription = "Pending Approval",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = notice.content,
                maxLines = 2,
                color = Color.Gray
            )
            
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
        }
    }
} 