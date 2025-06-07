package com.ecorvi.schmng.ui.screens.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.ecorvi.schmng.ui.theme.ParentBlue
import com.ecorvi.schmng.ui.components.ParentBottomNavigation
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.ecorvi.schmng.ui.navigation.ParentBottomNavItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentNoticesScreen(
    navController: NavController,
    childUid: String,
    showBackButton: Boolean = true,
    currentRoute: String? = null,
    onRouteSelected: ((String) -> Unit)? = null
) {
    var notices by remember { mutableStateOf<List<Notice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var studentClass by remember { mutableStateOf("") }

    // Fetch notices
    LaunchedEffect(childUid) {
        isLoading = true
        error = null
        
        try {
            // First get student's class
            val db = FirebaseFirestore.getInstance()
            val studentDoc = db.collection("students")
                .document(childUid)
                .get()
                .await()
                
            if (studentDoc.exists()) {
                studentClass = studentDoc.getString("className") ?: ""
                Log.d("Notices", "Student class: $studentClass")
                
                // Now fetch notices
                val noticesSnapshot = db.collection("notices")
                    .whereEqualTo("status", "approved")
                    .whereEqualTo("targetClass", studentClass)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                notices = noticesSnapshot.documents.mapNotNull { doc ->
                    try {
                        Notice(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            content = doc.getString("content") ?: "",
                            date = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            author = doc.getString("authorName") ?: "",
                            status = doc.getString("status") ?: "approved",
                            targetClass = doc.getString("targetClass") ?: "all",
                            priority = doc.getString("priority") ?: "normal"
                        )
                    } catch (e: Exception) {
                        Log.e("Notices", "Error converting notice document: ${e.message}")
                        null
                    }
                }
                
                // Also fetch notices for all classes
                val allNoticesSnapshot = db.collection("notices")
                    .whereEqualTo("status", "approved")
                    .whereEqualTo("targetClass", "all")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val allNotices = allNoticesSnapshot.documents.mapNotNull { doc ->
                    try {
                        Notice(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            content = doc.getString("content") ?: "",
                            date = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            author = doc.getString("authorName") ?: "",
                            status = doc.getString("status") ?: "approved",
                            targetClass = doc.getString("targetClass") ?: "all",
                            priority = doc.getString("priority") ?: "normal"
                        )
                    } catch (e: Exception) {
                        Log.e("Notices", "Error converting notice document: ${e.message}")
                        null
                    }
                }
                
                // Combine and sort all notices
                notices = (notices + allNotices).sortedByDescending { it.date }
                
            } else {
                error = "Student profile not found"
            }
        } catch (e: Exception) {
            error = "Failed to load notices: ${e.message}"
            Log.e("Notices", "Error loading notices: ${e.message}")
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notices", color = ParentBlue) },
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
                    val route = when (item) {
                        ParentBottomNavItem.Home -> "parent_dashboard"
                        else -> "${item.route}/$childUid"
                    }
                    navController.navigate(route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        popUpTo("parent_dashboard") {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        NoticesContent(
            padding = padding,
            isLoading = isLoading,
            error = error,
            notices = notices,
            onBackClick = { navController.popBackStack() }
        )
    }
}

@Composable
private fun NoticesContent(
    padding: PaddingValues,
    isLoading: Boolean,
    error: String?,
    notices: List<Notice>,
    onBackClick: () -> Unit
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = ParentBlue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error ?: "Unknown error",
                        color = ParentBlue,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ParentBlue
                        )
                    ) {
                        Text("Go Back")
                    }
                }
            }
            notices.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = ParentBlue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notices available",
                        color = ParentBlue,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ParentBlue
                        )
                    ) {
                        Text("Go Back")
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(notices) { notice ->
                        NoticeCard(notice)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(notice: Notice) {
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
                        if (notice.priority == "high") Icons.Default.PriorityHigh else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (notice.priority == "high") Color.Red else Color(0xFF1F41BB),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = notice.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1F41BB),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(notice.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = notice.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Posted by: ${notice.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (notice.targetClass != "all") {
                    Text(
                        text = "For: ${notice.targetClass}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1F41BB)
                    )
                }
            }
        }
    }
}

data class Notice(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val date: Long = System.currentTimeMillis(),
    val author: String = "",
    val status: String = "approved",
    val targetClass: String = "all",
    val priority: String = "normal"
) 