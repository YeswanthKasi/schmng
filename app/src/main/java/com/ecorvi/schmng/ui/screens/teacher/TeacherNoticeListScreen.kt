package com.ecorvi.schmng.ui.screens.teacher

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
fun TeacherNoticeListScreen(
    navController: NavController,
    viewModel: NoticeViewModel = viewModel()
) {
    var selectedFilter by remember { mutableStateOf("all") }
    val notices by viewModel.notices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedNotices by remember { mutableStateOf(setOf<String>()) }
    
    val filterOptions = listOf(
        "all" to "All Notices",
        Notice.STATUS_DRAFT to "My Drafts",
        Notice.STATUS_PENDING to "Pending Approval",
        Notice.STATUS_APPROVED to "Approved",
        Notice.STATUS_REJECTED to "Rejected"
    )
    
    LaunchedEffect(selectedFilter) {
        viewModel.loadNotices(if (selectedFilter == "all") null else selectedFilter)
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
                    } else {
                        IconButton(onClick = { navController.navigate("teacher_notice_create") }) {
                            Icon(Icons.Default.Add, "Create Notice")
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
                            Notice.STATUS_DRAFT -> "No draft notices"
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
                            onEdit = if (notice.status == Notice.STATUS_DRAFT) {
                                { navController.navigate("teacher_notice_create/${notice.id}") }
                            } else null,
                            onSubmit = if (notice.status == Notice.STATUS_DRAFT) {
                                { viewModel.submitForApproval(notice.id) }
                            } else null,
                            onDelete = if (notice.status == Notice.STATUS_DRAFT) {
                                { viewModel.deleteNotice(notice.id) }
                            } else null
                        )
                    }
                }
            }

            error?.let {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(it)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticeCard(
    notice: Notice,
    isSelected: Boolean,
    onSelect: (Boolean) -> Unit,
    onEdit: (() -> Unit)? = null,
    onSubmit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
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
                
                Row {
                    if (onSubmit != null) {
                        IconButton(onClick = onSubmit) {
                            Icon(Icons.Default.Send, "Submit for Approval")
                        }
                    }
                    if (onEdit != null) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete")
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
                Text(
                    text = "Status: ${notice.status.capitalize()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(notice.createdAt)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
} 