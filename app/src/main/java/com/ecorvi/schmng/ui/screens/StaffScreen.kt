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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.firestore.ListenerRegistration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(navController: NavController) {
    var staffMembers by remember { mutableStateOf<List<Person>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var staffListener: ListenerRegistration? by remember { mutableStateOf(null) }
    var selectedDepartment by remember { mutableStateOf("All Departments") }
    
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
                title = { Text("Non-Teaching Staff") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Department filter dropdown
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.FilterList, "Filter by department")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            departments.forEach { department ->
                                DropdownMenuItem(
                                    text = { Text(department) },
                                    onClick = {
                                        selectedDepartment = department
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_staff") },
                containerColor = Color(0xFF9C27B0)
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
                    modifier = Modifier.align(Alignment.Center)
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
                val filteredStaff = if (selectedDepartment == "All Departments") {
                    staffMembers
                } else {
                    staffMembers.filter { it.department == selectedDepartment }
                }

                if (filteredStaff.isEmpty()) {
                    Text(
                        text = "No staff members found",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filteredStaff) { staff ->
                            StaffListItem(
                                staff = staff,
                                onItemClick = {
                                    navController.navigate("staff_details/${staff.id}")
                                },
                                onDeleteClick = {
                                    FirestoreDatabase.deleteStaffMember(
                                        staffId = staff.id,
                                        onSuccess = {
                                            Toast.makeText(
                                                context,
                                                "Staff member deleted successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onFailure = { e ->
                                            Toast.makeText(
                                                context,
                                                "Error deleting staff member: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
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
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Staff Icon
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color(0xFF9C27B0).copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Staff",
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Staff Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "${staff.firstName} ${staff.lastName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = staff.designation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = staff.department,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete Button
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
} 