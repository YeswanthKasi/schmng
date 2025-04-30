package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.ChatMessage
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

data class ChatPreview(
    val userId: String,
    val name: String,
    val lastMessage: String,
    val timestamp: Long,
    val unread: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val scope = rememberCoroutineScope()
    
    var chatPreviews by remember { mutableStateOf<List<ChatPreview>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Load chat list
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            try {
                FirestoreDatabase.listenForChatUpdates(
                    userId = uid,
                    onUpdate = { messages ->
                        scope.launch {
                            try {
                                // Process each chat and get the other participant's details
                                val newPreviews = mutableListOf<ChatPreview>()
                                
                                for (message in messages) {
                                    // Get the other participant's ID
                                    val otherUserId = message.participants.find { it != uid }
                                    if (otherUserId != null) {
                                        // Get user details
                                        val person = FirestoreDatabase.getStudent(otherUserId)
                                            ?: FirestoreDatabase.getTeacher(otherUserId)
                                            ?: FirestoreDatabase.getStaffMember(otherUserId)

                                        if (person != null) {
                                            val preview = ChatPreview(
                                                userId = otherUserId,
                                                name = "${person.firstName} ${person.lastName}",
                                                lastMessage = message.message,
                                                timestamp = message.timestamp,
                                                unread = !message.read && message.receiverId == uid
                                            )
                                            newPreviews.add(preview)
                                        }
                                    }
                                }
                                
                                // Update chat previews list
                                chatPreviews = newPreviews.distinctBy { it.userId }
                                isLoading = false
                            } catch (e: Exception) {
                                error = e.message
                                isLoading = false
                            }
                        }
                    },
                    onError = { e ->
                        error = e.message
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        } ?: run {
            error = "User not logged in"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Messages",
                        color = Color(0xFF1F41BB),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1F41BB)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("new_message") },
                containerColor = Color(0xFF1F41BB),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Edit, contentDescription = "New Message")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search messages...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF1F41BB)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1F41BB),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1F41BB))
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        error ?: "Unknown error occurred",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (chatPreviews.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No messages yet",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        chatPreviews.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.lastMessage.contains(searchQuery, ignoreCase = true)
                        }
                    ) { preview ->
                        ChatPreviewItem(
                            preview = preview,
                            onClick = {
                                navController.navigate("chat/${preview.userId}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatPreviewItem(
    preview: ChatPreview,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onClick,
        color = if (preview.unread) Color(0xFFF5F5FF) else Color.White,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = Color(0xFF1F41BB).copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = Color(0xFF1F41BB)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Message Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = preview.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (preview.unread) FontWeight.Bold else FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preview.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (preview.unread) Color.Black else Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Time and Status
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatTimestamp(preview.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (preview.unread) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = CircleShape,
                        color = Color(0xFF1F41BB)
                    ) { }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    return when {
        now.get(Calendar.DATE) == messageTime.get(Calendar.DATE) -> {
            // Today - show time
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        now.get(Calendar.DATE) - messageTime.get(Calendar.DATE) == 1 -> {
            // Yesterday
            "Yesterday"
        }
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            // Same year - show date without year
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
        else -> {
            // Different year - show date with year
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
} 