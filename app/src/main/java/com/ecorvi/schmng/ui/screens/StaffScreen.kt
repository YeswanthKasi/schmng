package com.ecorvi.schmng.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.firestore.ListenerRegistration

private val StaffPurple = Color(0xFF9C27B0) // Purple color for staff theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(navController: NavController) {
    var staffMembers by remember { mutableStateOf<List<Person>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var staffListener: ListenerRegistration? by remember { mutableStateOf(null) }
    var selectedDepartment by remember { mutableStateOf("All Departments") }
    var searchQuery by remember { mutableStateOf("") }
    
    // List of departments
    val departments = listOf(
        "All Departments",
        "Administration",
        "Maintenance",
        "Library",
        "IT Support",
        "Accounts",
        "Security",
        "Others"
    )

    // Effect to load staff data
    DisposableEffect(Unit) {
        staffListener = FirestoreDatabase.listenForStaffUpdates(
            onUpdate = { updatedStaff ->
                staffMembers = updatedStaff
                isLoading = false
            },
            onError = { error ->
                errorMessage = "Error loading staff: ${error.message}"
                isLoading = false
            }
        )

        onDispose {
            staffListener?.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Non-Teaching Staff",
                        color = StaffPurple,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = StaffPurple
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_person/staff") },
                containerColor = StaffPurple
            ) {
                Icon(Icons.Default.Add, "Add Staff Member", tint = Color.White)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = StaffPurple
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                val filteredStaff = staffMembers
                    .filter { staff ->
                        (selectedDepartment == "All Departments" || staff.department == selectedDepartment) &&
                        (searchQuery.isEmpty() || 
                         "${staff.firstName} ${staff.lastName}".contains(searchQuery, ignoreCase = true) ||
                         staff.department.contains(searchQuery, ignoreCase = true) ||
                         staff.designation.contains(searchQuery, ignoreCase = true))
                    }

                if (filteredStaff.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "No results",
                            modifier = Modifier.size(48.dp),
                            tint = StaffPurple.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No staff members found",
                            style = MaterialTheme.typography.titleMedium,
                            color = StaffPurple.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filteredStaff) { staff ->
                            StaffListItem(
                                staff = staff,
                                onItemClick = {
                                    navController.navigate("staff_profile/${staff.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffListItem(
    staff: Person,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onItemClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Staff Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(StaffPurple.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Staff",
                    tint = StaffPurple,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Staff Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "${staff.firstName} ${staff.lastName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = staff.designation ?: "Staff Member",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StaffPurple
                )
                Text(
                    text = staff.department ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Add navigation arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Profile",
                tint = StaffPurple.copy(alpha = 0.5f)
            )
        }
    }
} 