package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffProfileScreen(
    navController: NavController,
    staffId: String,
    isAdmin: Boolean = false
) {
    var staff by remember { mutableStateOf<Person?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isDeleting by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load staff data
    LaunchedEffect(staffId) {
        isLoading = true
        FirestoreDatabase.staffCollection.document(staffId).get()
            .addOnSuccessListener { document ->
                staff = document.toObject(Person::class.java)
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Staff Profile",
                        color = Color(0xFF9C27B0),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF9C27B0)
                        )
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(
                            onClick = { navController.navigate("add_person/staff?personId=$staffId") }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color(0xFF9C27B0)
                            )
                        }
                    }
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
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF9C27B0)
                )
            } else if (staff == null) {
                Text(
                    "Failed to load staff profile",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProfileField("First Name", staff?.firstName ?: "")
                            ProfileField("Last Name", staff?.lastName ?: "")
                            ProfileField("Email", staff?.email ?: "")
                            ProfileField("Department", staff?.department ?: "")
                            ProfileField("Designation", staff?.designation ?: "")
                            ProfileField("Gender", staff?.gender ?: "")
                            ProfileField("Date of Birth", staff?.dateOfBirth ?: "")
                            ProfileField("Mobile No", staff?.mobileNo ?: "")
                            ProfileField("Address", staff?.address ?: "")
                            ProfileField("Age", staff?.age?.toString() ?: "")
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (isAdmin) {
                        Button(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Staff")
                        }
                    }
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Staff") },
                    text = { Text("Are you sure you want to delete this staff member? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                isDeleting = true
                                FirestoreDatabase.deleteStaffMember(
                                    staffId = staffId,
                                    onSuccess = {
                                        isDeleting = false
                                        showDeleteDialog = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Staff member deleted successfully")
                                        }
                                        navController.popBackStack()
                                    },
                                    onFailure = { e ->
                                        isDeleting = false
                                        showDeleteDialog = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Failed to delete staff member: ${e.message}")
                                        }
                                    }
                                )
                            },
                            enabled = !isDeleting
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text("Delete")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
} 