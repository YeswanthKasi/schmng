package com.ecorvi.schmng.ui.screens.student

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
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ecorvi.schmng.models.ClassEvent
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.navigation.StudentBottomNavItem
import com.ecorvi.schmng.ui.screens.StudentBottomNavigation
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassEventsScreen(
    navController: NavController,
    currentRoute: String? = null,
    onRouteSelected: ((String) -> Unit)? = null
) {
    var events by remember { mutableStateOf<List<ClassEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val currentUser = FirebaseAuth.getInstance().currentUser
    var studentClass by remember { mutableStateOf<String?>(null) }

    // Fetch student's class
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            FirestoreDatabase.getStudentClass(
                studentId = userId,
                onComplete = { className: String? ->
                    // Standardize class format to "Class X"
                    studentClass = className?.let { originalClassName ->
                        if (!originalClassName.startsWith("Class ")) {
                            "Class ${originalClassName}".replace("st", "").replace("nd", "").replace("rd", "").replace("th", "")
                        } else {
                            originalClassName
                        }
                    }
                },
                onFailure = { e: Exception ->
                    errorMessage = e.message
                }
            )
        }
    }

    // Fetch class events when student class is available
    LaunchedEffect(studentClass) {
        studentClass?.let { className ->
            FirestoreDatabase.getClassEvents(
                className = className,
                onComplete = { fetchedEvents: List<ClassEvent> ->
                    events = fetchedEvents.sortedByDescending { event -> event.eventDate }
                    isLoading = false
                },
                onFailure = { e: Exception ->
                    errorMessage = e.message
                    isLoading = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Class Events") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            StudentBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
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
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "An error occurred",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (events.isEmpty()) {
                Text(
                    text = "No class events found",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events) { event ->
                        EventCard(event = event)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventCard(event: ClassEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (event.priority.lowercase()) {
                "urgent" -> Color(0xFFFFEBEE)  // Light red for urgent
                "important" -> Color(0xFFFFF3E0)  // Light orange for important
                else -> Color(0xFFF1F8E9)  // Light green for normal
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Priority: ${event.priority.capitalize()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = when (event.priority.lowercase()) {
                        "urgent" -> Color(0xFFD32F2F)  // Darker red for text
                        "important" -> Color(0xFFE65100)  // Darker orange for text
                        else -> Color(0xFF33691E)  // Darker green for text
                    }
                )
                Text(
                    text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(event.eventDate)),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
} 