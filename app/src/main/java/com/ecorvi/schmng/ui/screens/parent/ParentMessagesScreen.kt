package com.ecorvi.schmng.ui.screens.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.ChatMessage
import com.ecorvi.schmng.ui.theme.ParentBlue
import com.ecorvi.schmng.ui.components.ParentBottomNavigation
import com.ecorvi.schmng.ui.navigation.ParentBottomNavItem
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentMessagesScreen(
    navController: NavController,
    childUid: String,
    showBackButton: Boolean = true,
    currentRoute: String? = null,
    onRouteSelected: ((String) -> Unit)? = null
) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            try {
                FirestoreDatabase.getParentMessages(currentUserId)?.let {
                    messages = it.sortedByDescending { msg -> msg.timestamp }
                    isLoading = false
                } ?: run {
                    error = "Could not fetch messages"
                    isLoading = false
                }
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        } else {
            error = "User not authenticated"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", color = ParentBlue) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ParentBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            ParentBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = { item ->
                    when (item) {
                        ParentBottomNavItem.Home -> {
                            navController.navigate("parent_dashboard") {
                                popUpTo("parent_dashboard") {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                        else -> {
                            navController.navigate("${item.route}/$childUid") {
                                popUpTo("parent_dashboard") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        MessagesContent(
            padding = padding,
            isLoading = isLoading,
            error = error,
            messages = messages
        )
    }
}

@Composable
private fun MessagesContent(
    padding: PaddingValues,
    isLoading: Boolean,
    error: String?,
    messages: List<ChatMessage>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ParentBlue
                )
            }
            error != null -> {
                Text(
                    text = error,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            messages.isEmpty() -> {
                Text(
                    text = "No messages",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages) { message ->
                        MessageCard(message)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(message: ChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF1F41BB),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message.senderName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F41BB)
                    )
                }
                Text(
                    text = formatDateTime(message.timestamp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message.message,
                fontSize = 14.sp,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                when (message.status.uppercase()) {
                    "SENT" -> Icon(
                        Icons.Default.Check,
                        contentDescription = "Sent",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    "DELIVERED" -> Icon(
                        Icons.Default.DoneAll,
                        contentDescription = "Delivered",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    "READ" -> Icon(
                        Icons.Default.DoneAll,
                        contentDescription = "Read",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    else -> Icon(
                        Icons.Default.Check,
                        contentDescription = "Unknown Status",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(status: String) {
    when (status.uppercase()) {
        "SENT" -> Icon(Icons.Default.Check, "Sent")
        "DELIVERED" -> Icon(Icons.Default.DoneAll, "Delivered")
        "READ" -> Icon(Icons.Default.DoneAll, "Read", tint = Color.Blue)
        else -> Icon(Icons.Default.Check, "Unknown Status")
    }
}

private fun formatDateTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    return when {
        now.get(Calendar.DATE) == messageTime.get(Calendar.DATE) -> {
            // Today - show time
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
        }
        now.get(Calendar.DATE) - messageTime.get(Calendar.DATE) == 1 -> {
            // Yesterday
            "Yesterday"
        }
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            // Same year - show date without year
            SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
        else -> {
            // Different year - show full date
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
} 