package com.ecorvi.schmng.ui.screens.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.theme.ParentBlue
import com.ecorvi.schmng.ui.components.ParentBottomNavigation
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.ecorvi.schmng.models.ClassEvent
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.navigation.ParentBottomNavItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentEventsScreen(
    navController: NavController,
    childUid: String,
    showBackButton: Boolean = true,
    currentRoute: String? = null,
    onRouteSelected: ((String) -> Unit)? = null
) {
    var events by remember { mutableStateOf<List<ClassEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var studentClass by remember { mutableStateOf("") }

    // Fetch events
    LaunchedEffect(childUid) {
        try {
            val db = FirebaseFirestore.getInstance()
            
            // First get student's class
            val studentDoc = db.collection("students")
                .document(childUid)
                .get()
                .await()
                
            if (studentDoc.exists()) {
                studentClass = studentDoc.getString("className") ?: ""
                
                // Standardize class format to "Class X"
                val standardizedClass = if (!studentClass.startsWith("Class ")) {
                    "Class $studentClass".replace("st", "").replace("nd", "").replace("rd", "").replace("th", "")
                } else {
                    studentClass
                }
                
                Log.d("ParentEvents", "Fetching events for class: $standardizedClass")
                
                // Use FirestoreDatabase helper method to fetch events
                FirestoreDatabase.getClassEvents(
                    className = standardizedClass,
                    onComplete = { fetchedEvents ->
                        events = fetchedEvents.sortedBy { it.eventDate }
                        isLoading = false
                        Log.d("ParentEvents", "Successfully fetched ${events.size} events")
                    },
                    onFailure = { e ->
                        error = "Failed to load events: ${e.message}"
                        Log.e("ParentEvents", "Error loading events: ${e.message}")
                        isLoading = false
                    }
                )
            } else {
                error = "Student profile not found"
                Log.e("ParentEvents", "Student profile not found for ID: $childUid")
                isLoading = false
            }
        } catch (e: Exception) {
            error = "Failed to load events: ${e.message}"
            Log.e("ParentEvents", "Error loading events: ${e.message}")
            e.printStackTrace()
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Class Events", color = ParentBlue) },
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
                            text = error ?: "An error occurred",
                            color = ParentBlue,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                events.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null,
                            tint = ParentBlue,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No upcoming events",
                            color = ParentBlue,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(events) { event ->
                            EventCard(event)
                        }
                    }
                }
            }
        }
    }
}

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
        ),
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
                        if (event.priority.lowercase() == "urgent") Icons.Default.PriorityHigh 
                        else Icons.Default.Event,
                        contentDescription = null,
                        tint = when (event.priority.lowercase()) {
                            "urgent" -> Color.Red
                            "important" -> Color(0xFFE65100)
                            else -> ParentBlue
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = ParentBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (event.targetClass != "all") {
                    Text(
                        text = "Class: ${event.targetClass}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ParentBlue
                    )
                }
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .format(Date(event.eventDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
} 