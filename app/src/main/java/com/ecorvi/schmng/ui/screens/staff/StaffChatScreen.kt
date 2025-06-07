package com.ecorvi.schmng.ui.screens.staff

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Send
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
import com.ecorvi.schmng.ui.data.store.StaffMessageStore
import com.ecorvi.schmng.ui.data.store.TempChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffChatScreen(
    navController: NavController,
    chatId: String
) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(StaffMessageStore.getMessages(chatId)) }
    val otherUserName = StaffMessageStore.getUserName(chatId)
    val lastSeen = StaffMessageStore.getUserLastSeen(chatId)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = otherUserName,
                            color = Color(0xFF1F41BB),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = lastSeen,
                            fontSize = 12.sp,
                            color = Color.Gray
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
                items(messages) { message ->
                    MessageBubble(message = message)
                }
            }

            // Message input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1F41BB),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            val newMessage = TempChatMessage(
                                id = (messages.size + 1).toString(),
                                message = messageText,
                                timestamp = "Just now",
                                isCurrentUser = true
                            )
                            StaffMessageStore.addMessage(chatId, newMessage)
                            messages = StaffMessageStore.getMessages(chatId)
                            messageText = ""

                            coroutineScope.launch {
                                listState.animateScrollToItem(messages.size - 1)
                                delay(1500) // Simulate receiving a reply
                                val replyMessage = TempChatMessage(
                                    id = (messages.size + 2).toString(),
                                    message = "Thanks for your message! We'll get back to you shortly.",
                                    timestamp = "Just now",
                                    isCurrentUser = false,
                                    status = MessageStatus.READ
                                )
                                StaffMessageStore.addMessage(chatId, replyMessage)
                                messages = StaffMessageStore.getMessages(chatId)
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    containerColor = Color(0xFF1F41BB),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: TempChatMessage) {
    val horizontalAlignment = if (message.isCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isCurrentUser) Color(0xFF1F41BB) else Color(0xFFF0F4FF)
    val textColor = if (message.isCurrentUser) Color.White else Color.Black
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (message.isCurrentUser) 16.dp else 4.dp,
        bottomEnd = if (message.isCurrentUser) 4.dp else 16.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = bubbleShape
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.message,
                    color = textColor,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = message.timestamp,
                        color = if (message.isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                        fontSize = 12.sp
                    )
                    if (message.isCurrentUser) {
                        Icon(
                            imageVector = when (message.status) {
                                MessageStatus.SENT -> Icons.Default.Check
                                MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                MessageStatus.READ -> Icons.Default.DoneAll // Use blue check for read
                            },
                            contentDescription = message.status.name,
                            modifier = Modifier.size(16.dp),
                            tint = if (message.status == MessageStatus.READ) Color(0xFF4FC3F7) else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
} 