package com.ecorvi.schmng.ui.screens

import android.os.Bundle
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.models.ClassEvent
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class Announcement(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val date: String = "",
    val targetClass: String = "all",
    val priority: String = "normal"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    navController: NavController,
    currentRoute: String? = null,
    onRouteSelected: ((String) -> Unit)? = null
) {
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var classEvents by remember { mutableStateOf<List<ClassEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Fetch announcements and class events
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            try {
                // Get student's class
                val student = FirestoreDatabase.getStudent(currentUser.uid)
                if (student != null) {
                    // Fetch announcements
                    FirebaseFirestore.getInstance()
                        .collection("announcements")
                        .whereIn("targetClass", listOf("all", student.className))
                        .get()
                        .addOnSuccessListener { documents ->
                            announcements = documents.mapNotNull { doc ->
                                try {
                                    Announcement(
                                        id = doc.id,
                                        title = doc.getString("title") ?: "",
                                        content = doc.getString("content") ?: "",
                                        date = doc.getString("date") ?: "",
                                        targetClass = doc.getString("targetClass") ?: "all",
                                        priority = doc.getString("priority") ?: "normal"
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }

                    // Fetch class events
                    FirebaseFirestore.getInstance()
                        .collection("class_events")
                        .whereEqualTo("targetClass", student.className)
                        .whereEqualTo("status", "active")
                        .get()
                        .addOnSuccessListener { documents ->
                            classEvents = documents.mapNotNull { doc ->
                                doc.toObject(ClassEvent::class.java)
                            }
                            isLoading = false
                        }
                        .addOnFailureListener { e ->
                            errorMessage = e.message
                            isLoading = false
                        }
                }
            } catch (e: Exception) {
                errorMessage = e.message
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Announcements & Events") }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show class events first
                if (classEvents.isNotEmpty()) {
                    item {
                        Text(
                            text = "Upcoming Class Events",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(classEvents.sortedBy { it.eventDate }) { event ->
                        EventCard(event = event)
                    }

                    item {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }
                }

                // Then show announcements
                if (announcements.isNotEmpty()) {
                    item {
                        Text(
                            text = "General Announcements",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(announcements) { announcement ->
                        AnnouncementCard(announcement = announcement)
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
            containerColor = when (event.priority) {
                "urgent" -> Color(0xFFFFEBEE)
                "important" -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colorScheme.surface
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .format(Date(event.eventDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = event.type.capitalize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnnouncementCard(announcement: Announcement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (announcement.priority) {
                "urgent" -> Color(0xFFFFEBEE)
                "important" -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = announcement.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}