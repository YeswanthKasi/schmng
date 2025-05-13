package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Schedule
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

private val PrimaryBlue = Color(0xFF1F41BB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScheduleScreen(
    navController: NavController,
    currentRoute: String,
    onRouteSelected: (String) -> Unit
) {
    var schedules by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        try {
            // Fetch schedules from Firestore
            FirestoreDatabase.fetchSchedules(
                onComplete = { fetchedSchedules ->
                    schedules = fetchedSchedules
                    isLoading = false
                },
                onFailure = { e ->
                    errorMessage = e.message
                    isLoading = false
                }
            )
        } catch (e: Exception) {
            errorMessage = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "My Schedule",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            StudentBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = { item ->
                    onRouteSelected(item.route)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                        text = errorMessage ?: "An error occurred",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(schedules) { schedule ->
                        ScheduleCard(schedule = schedule)
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleCard(schedule: Schedule) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = schedule.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Time: ${schedule.time}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            if (schedule.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = schedule.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
} 