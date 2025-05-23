package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.ecorvi.schmng.ui.navigation.StudentBottomNavItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeListScreen(
    navController: NavController,
    currentRoute: String,
    onRouteSelected: (String) -> Unit,
    viewModel: NoticeViewModel = viewModel()
) {
    val notices by viewModel.notices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotices(Notice.STATUS_APPROVED)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notices") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                listOf(
                    StudentBottomNavItem.Home,
                    StudentBottomNavItem.Schedule,
                    StudentBottomNavItem.Attendance,
                    StudentBottomNavItem.Notices,
                    StudentBottomNavItem.Profile
                ).forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = { onRouteSelected(item.route) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (notices.isEmpty()) {
                Text(
                    text = "No notices available",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notices) { notice ->
                        NoticeCard(notice = notice)
                    }
                }
            }

            error?.let {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter)
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
    notice: Notice
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.Announcement,
                    contentDescription = "Notice",
                    tint = MaterialTheme.colorScheme.primary
                )
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
                    text = "Target: ${notice.targetClass}",
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