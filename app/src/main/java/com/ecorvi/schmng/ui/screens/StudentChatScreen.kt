package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.ecorvi.schmng.ui.data.store.TempChatMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentChatScreen(
    navController: NavController,
    chatId: String
) {
    var messageText by remember { mutableStateOf("") }
    
    // Dummy messages
    val dummyMessages = remember {
        listOf(
            TempChatMessage(
                id = "1",
                message = "Hello Mrs. Smith",
                timestamp = "10:30 AM",
                isCurrentUser = true
            ),
            TempChatMessage(
                id = "2",
                message = "Hi there! How can I help you?",
                timestamp = "10:31 AM",
                isCurrentUser = false
            ),
            TempChatMessage(
                id = "3",
                message = "When is the assignment due?",
                timestamp = "10:32 AM",
                isCurrentUser = true
            ),
            TempChatMessage(
                id = "4",
                message = "The assignment is due next Friday",
                timestamp = "10:33 AM",
                isCurrentUser = false
            )
        )
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            when (chatId) {
                                "1" -> "Mrs. Smith"
                                "2" -> "Mr. Johnson"
                                "3" -> "Ms. Williams"
                                else -> "Chat"
                            },
                            color = Color(0xFF1F41BB),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Online",
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dummyMessages) { message ->
                    ChatMessageBubble(message = message)
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
                            messageText = ""
                            coroutineScope.launch {
                                listState.animateScrollToItem(dummyMessages.size - 1)
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
private fun ChatMessageBubble(message: TempChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isCurrentUser) Color(0xFF1F41BB) else Color.LightGray,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.message,
                    color = if (message.isCurrentUser) Color.White else Color.Black
                )
                Text(
                    text = message.timestamp,
                    color = if (message.isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
} 