package com.ecorvi.schmng.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.CommonBackground
import com.ecorvi.schmng.ui.components.FeeListItem
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Fee
import com.google.firebase.firestore.ListenerRegistration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingFeesScreen(navController: NavController) {
    var pendingFees by remember { mutableStateOf<List<Fee>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf<Fee?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    // Fix: Change the type to handle nullable ListenerRegistration
    var listener: ListenerRegistration? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        try {
            listener = FirestoreDatabase.listenForFeeUpdates(
                onUpdate = { fetchedFees ->
                    pendingFees = fetchedFees
                    isLoading = false
                    errorMessage = null
                },
                onError = { exception ->
                    isLoading = false
                    errorMessage = exception.message
                    Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            isLoading = false
            errorMessage = e.message
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Cleanup listener
    DisposableEffect(Unit) {
        onDispose {
            listener?.remove()
        }
    }

    CommonBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Pending Fees",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { 
                        try {
                            navController.navigate("add_fee")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    containerColor = Color(0xFF1F41BB)
                ) {
                    Icon(Icons.Default.Add, "Add Fee", tint = Color.White)
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF1F41BB))
                        }
                    }
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Error: $errorMessage",
                                color = Color.Red,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    pendingFees.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Money,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No pending fees",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color.Gray
                                    )
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = pendingFees,
                                key = { it.id }
                            ) { fee ->
                                FeeListItem(
                                    fee = fee,
                                    onDeleteClick = { showDeleteDialog = fee }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        showDeleteDialog?.let { fee ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { 
                    Text(
                        "Delete Fee Record",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = { 
                    Text(
                        "Are you sure you want to delete the fee record for ${fee.studentName}? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            FirestoreDatabase.deleteFee(
                                fee.id,
                                onSuccess = {
                                    Toast.makeText(context, "Fee record deleted successfully", Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = null
                                },
                                onFailure = { e ->
                                    Toast.makeText(context, "Failed to delete fee record: ${e.message}", Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = null
                                }
                            )
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Red
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
} 