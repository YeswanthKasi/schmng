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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
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
fun AnnouncementsScreen(navController: NavController) {
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Log screen view for in-app messaging
    LaunchedEffect(Unit) {
        Firebase.analytics.logEvent("view_announcements", Bundle().apply {
            putString("user_id", currentUser?.uid ?: "")
        })
    }

    // Fetch announcements for the student's class
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            FirestoreDatabase.getStudent(currentUser.uid)?.let { student ->
                FirebaseFirestore.getInstance()
                    .collection("announcements")
                    .get()
                    .addOnSuccessListener { documents ->
                        announcements = documents.mapNotNull { doc ->
                            doc.toObject(Announcement::class.java)
                        }.filter {
                            it.targetClass == "all" || it.targetClass == student.className
                        }.sortedByDescending { it.date }
                        isLoading = false
                    }
                    .addOnFailureListener { e ->
                        errorMessage = e.message
                        isLoading = false
                    }
            }
        }
    }

    CommonBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Announcements") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    )
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
                        text = errorMessage ?: "Unknown error",
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else if (announcements.isEmpty()) {
                    Text(
                        text = "No announcements",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(announcements) { announcement ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (announcement.priority) {
                                        "high" -> Color(0xFFFFEBEE)
                                        "medium" -> Color(0xFFFFF3E0)
                                        else -> Color.White
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
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = announcement.date,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = announcement.content,
                                        color = Color.DarkGray
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

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
fun AnnouncementsScreenPreview() {
    val sampleAnnouncements = listOf(
        Announcement(
            title = "Important Meeting",
            content = "There will be a mandatory meeting for all students.",
            date = "2023-12-01",
            targetClass = "all",
            priority = "high"
        ),
        Announcement(
            title = "Homework Reminder",
            content = "Don't forget to submit your homework by Friday.",
            date = "2023-11-28",
            targetClass = "Class A",
            priority = "medium"
        ),
        Announcement(
            title = "School Trip",
            content = "Details about the upcoming school trip.",
            date = "2023-11-25",
            targetClass = "Class B",
            priority = "normal"
        ),
        Announcement(
            title = "Test Alert",
            content = "Reminder about the math test on December 5th.",
            date = "2023-11-20",
            targetClass = "all",
            priority = "high"
        )
    )

    // Simulate a simplified version of the screen
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(sampleAnnouncements) { announcement ->
           Text(text = announcement.title, modifier = Modifier.padding(8.dp))
        }
    }
}