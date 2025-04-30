package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, otherUserId: String) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var otherUserName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Load other user's name
    LaunchedEffect(otherUserId) {
        try {
            val person = FirestoreDatabase.getStudent(otherUserId)
                ?: FirestoreDatabase.getTeacher(otherUserId)
                ?: FirestoreDatabase.getStaffMember(otherUserId)
            
            otherUserName = if (person != null) "${person.firstName} ${person.lastName}" else ""
            isLoading = false
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    // Load messages
    LaunchedEffect(currentUser?.uid, otherUserId) {
        currentUser?.uid?.let { uid ->
            try {
                FirestoreDatabase.getMessages(
                    userId1 = uid,
                    userId2 = otherUserId,
                    onNewMessage = { message ->
                        messages = (messages + message)
                            .distinctBy { it.id }
                            .sortedBy { it.timestamp }
                        
                        // Mark received messages as read
                        if (message.receiverId == uid && !message.read) {
                            FirestoreDatabase.markMessageAsRead(
                                messageId = message.id,
                                onSuccess = {},
                                onFailure = {}
                            )
                        }
                        
                        // Scroll to bottom when new message arrives
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                        
                        isLoading = false
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
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        otherUserName.ifEmpty { "Chat" },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                        text = error ?: "Unknown error occurred",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                // Messages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        val isCurrentUser = message.senderId == currentUser?.uid
                        MessageBubble(
                            message = message,
                            isCurrentUser = isCurrentUser
                        )
                    }
                }

                // Message Input
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            placeholder = { Text("Type a message...") },
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1F41BB),
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )
                        
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank() && currentUser != null) {
                                    val newMessage = ChatMessage(
                                        senderId = currentUser.uid,
                                        receiverId = otherUserId,
                                        message = messageText.trim(),
                                        senderName = currentUser.displayName ?: "",
                                        receiverName = otherUserName,
                                        participants = listOf(currentUser.uid, otherUserId)
                                    )
                                    
                                    FirestoreDatabase.sendMessage(
                                        message = newMessage,
                                        onSuccess = {
                                            messageText = ""
                                        },
                                        onFailure = { e ->
                                            error = e.message
                                        }
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color(0xFF1F41BB)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isCurrentUser) Color(0xFF1F41BB) else Color.LightGray,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.message,
                    color = if (isCurrentUser) Color.White else Color.Black
                )
                Text(
                    text = formatMessageTime(message.timestamp),
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
} 