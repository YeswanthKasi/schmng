package com.ecorvi.schmng.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ecorvi.schmng.ui.data.model.MessageStatus
import com.ecorvi.schmng.ui.data.store.TempMessageStore
import com.ecorvi.schmng.ui.data.store.TempChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    chatId: String
) {
    var messageText by remember { mutableStateOf("") }
    
    // Get messages from TempMessageStore
    var messages by remember { mutableStateOf(TempMessageStore.getMessages(chatId)) }
    
    // Get user info from TempMessageStore
    val otherUserName = TempMessageStore.getUserName(chatId)
    val lastSeen = TempMessageStore.getUserLastSeen(chatId)
    
    // For auto-scrolling to the latest message
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Handle back navigation
    BackHandler {
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chat",
                        color = Color(0xFF1F41BB),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                .padding(16.dp)
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
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
                            if (messageText.isNotBlank()) {
                                val newMessage = TempChatMessage(
                                    id = System.currentTimeMillis().toString(),
                                    message = messageText,
                                    timestamp = "Just now",
                                    isCurrentUser = true,
                                    status = MessageStatus.SENT
                                )
                                
                                // Add message to store
                                TempMessageStore.addMessage(chatId, newMessage)
                                // Update local messages
                                messages = TempMessageStore.getMessages(chatId)
                                messageText = ""
                                
                                // Auto-scroll to the latest message
                                coroutineScope.launch {
                                    listState.animateScrollToItem(messages.size - 1)
                                    
                                    // Simulate message status updates
                                    delay(1000)
                                    TempMessageStore.updateMessageStatus(chatId, newMessage.id, MessageStatus.DELIVERED)
                                    messages = TempMessageStore.getMessages(chatId)
                                    
                                    delay(2000)
                                    TempMessageStore.updateMessageStatus(chatId, newMessage.id, MessageStatus.READ)
                                    messages = TempMessageStore.getMessages(chatId)
                                }
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

@Composable
private fun MessageBubble(message: TempChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isCurrentUser) Color(0xFF1F41BB) else Color.LightGray,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (message.isCurrentUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.message,
                    color = if (message.isCurrentUser) Color.White else Color.Black
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = message.timestamp,
                        color = if (message.isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                        fontSize = 12.sp
                    )
                    if (message.isCurrentUser) {
                        Spacer(modifier = Modifier.width(2.dp))
                        when (message.status) {
                            MessageStatus.SENT -> {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Sent",
                                    modifier = Modifier.size(12.dp),
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            MessageStatus.DELIVERED -> {
                                Box(modifier = Modifier.width(16.dp)) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Delivered",
                                        modifier = Modifier.size(12.dp).offset(x = (-2).dp),
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Delivered",
                                        modifier = Modifier.size(12.dp).offset(x = 2.dp),
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            MessageStatus.READ -> {
                                Box(modifier = Modifier.width(16.dp)) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Read",
                                        modifier = Modifier.size(12.dp).offset(x = (-2).dp),
                                        tint = Color.White
                                    )
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Read",
                                        modifier = Modifier.size(12.dp).offset(x = 2.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 