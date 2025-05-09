package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.ecorvi.schmng.ui.data.model.MessageStatus
import com.ecorvi.schmng.ui.data.store.ChatPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMessagesScreen(
    navController: NavController
) {
    var searchQuery by remember { mutableStateOf("") }

    // Dummy chat previews
    val dummyChats = listOf(
        ChatPreview(
            id = "1",
            name = "Mrs. Smith",
            lastMessage = "When is the assignment due?",
            timestamp = "10:30 AM",
            status = MessageStatus.READ,
            unread = false,
            lastSeen = "Online"
        ),
        ChatPreview(
            id = "2",
            name = "Mr. Johnson",
            lastMessage = "The test will be on Monday",
            timestamp = "Yesterday",
            status = MessageStatus.DELIVERED,
            unread = true,
            lastSeen = "2h ago"
        ),
        ChatPreview(
            id = "3",
            name = "Ms. Williams",
            lastMessage = "Great work on your project!",
            timestamp = "2d ago",
            status = MessageStatus.READ,
            unread = false,
            lastSeen = "1d ago"
        )
    )

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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("student_new_message") },
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
            // Search TextField
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
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1F41BB),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                )
            )

            val filteredChats = dummyChats.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.lastMessage.contains(searchQuery, ignoreCase = true)
            }

            if (filteredChats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (searchQuery.isBlank()) {
                            Icon(
                                Icons.Default.Message,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Text(
                                "No messages yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                            Text(
                                "Start a conversation with your teachers",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        } else {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Text(
                                "No results found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                            Text(
                                "Try different search terms",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredChats) { preview ->
                        MessageItem(
                            preview = preview,
                            onClick = { navController.navigate("student_chat/${preview.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageItem(preview: ChatPreview, onClick: () -> Unit) {
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

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = preview.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (preview.unread) FontWeight.Bold else FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    when (preview.status) {
                        MessageStatus.SENT -> {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Sent",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                        }
                        MessageStatus.DELIVERED -> {
                            Box(modifier = Modifier.width(20.dp)) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Delivered",
                                    modifier = Modifier.size(16.dp).offset(x = (-2).dp),
                                    tint = Color.Gray
                                )
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Delivered",
                                    modifier = Modifier.size(16.dp).offset(x = 2.dp),
                                    tint = Color.Gray
                                )
                            }
                        }
                        MessageStatus.READ -> {
                            Box(modifier = Modifier.width(20.dp)) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Read",
                                    modifier = Modifier.size(16.dp).offset(x = (-2).dp),
                                    tint = Color(0xFF1F41BB)
                                )
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Read",
                                    modifier = Modifier.size(16.dp).offset(x = 2.dp),
                                    tint = Color(0xFF1F41BB)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = preview.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (preview.unread) Color.Black else Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = preview.timestamp,
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