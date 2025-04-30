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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ecorvi.schmng.ui.navigation.BottomNav
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    currentRoute: String,
    onRouteSelected: (String) -> Unit
) {
    val notifications = remember { 
        mutableStateListOf(
            NotificationItem(
                "New Schedule Added",
                "A new class schedule has been added for Class X",
                "2 hours ago",
                NotificationType.SCHEDULE
            ),
            NotificationItem(
                "Fee Payment Due",
                "Reminder: Fee payment due for March 2024",
                "5 hours ago",
                NotificationType.PAYMENT
            ),
            NotificationItem(
                "System Update",
                "The system will undergo maintenance tonight",
                "1 day ago",
                NotificationType.SYSTEM
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
                        color = Color(0xFF1F41BB),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* Clear all notifications */ }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear All",
                            tint = Color(0xFF1F41BB)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            BottomNav(
                navController = navController,
                currentRoute = currentRoute,
                onItemSelected = { item -> onRouteSelected(item.route) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(notifications) { notification ->
                NotificationCard(
                    notification = notification,
                    onDismiss = { notifications.remove(notification) }
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationItem,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Notification Icon
            Surface(
                color = Color(0xFF1F41BB).copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        NotificationType.SCHEDULE -> Icons.Default.Schedule
                        NotificationType.PAYMENT -> Icons.Default.Payment
                        NotificationType.SYSTEM -> Icons.Default.Info
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp),
                    tint = Color(0xFF1F41BB)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Notification Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Dismiss Button
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.Gray
                )
            }
        }
    }
}

data class NotificationItem(
    val title: String,
    val message: String,
    val time: String,
    val type: NotificationType
)

enum class NotificationType {
    SCHEDULE, PAYMENT, SYSTEM
} 