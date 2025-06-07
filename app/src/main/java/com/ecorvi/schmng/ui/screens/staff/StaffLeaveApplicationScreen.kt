package com.ecorvi.schmng.ui.screens.staff

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.StaffBottomNavigation
import com.ecorvi.schmng.viewmodels.StaffLeaveViewModel
import com.google.firebase.auth.FirebaseAuth
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffLeaveApplicationScreen(
    navController: NavController,
    viewModel: StaffLeaveViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val staffId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var fromDate by remember { mutableStateOf(LocalDate.now()) }
    var toDate by remember { mutableStateOf(LocalDate.now()) }
    var leaveType by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }
    var showLeaveTypeDialog by remember { mutableStateOf(false) }
    var staffName by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var isLoadingStaff by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val loading by viewModel.loading.collectAsState()
    val submitSuccess by viewModel.submitSuccess.collectAsState()
    val error by viewModel.error.collectAsState()

    val leaveTypes = listOf(
        "Sick Leave",
        "Casual Leave",
        "Emergency Leave",
        "Other"
    )

    // Load staff information
    LaunchedEffect(staffId) {
        FirestoreDatabase.staffCollection
            .document(staffId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    staffName = "${document.getString("firstName")} ${document.getString("lastName")}"
                    department = document.getString("department") ?: "Administration"
                }
                isLoadingStaff = false
            }
            .addOnFailureListener {
                isLoadingStaff = false
            }
    }

    // Handle error and success states
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetError()
        }
    }

    LaunchedEffect(submitSuccess) {
        if (submitSuccess) {
            snackbarHostState.showSnackbar("Leave application submitted successfully!")
            viewModel.resetSubmitSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Apply Leave",
                        color = Color(0xFF1F41BB),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1F41BB)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            StaffBottomNavigation(
                currentRoute = null,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Leave Type Selection
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showLeaveTypeDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Leave Type",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = leaveType.ifEmpty { "Select Leave Type" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (leaveType.isEmpty()) Color.Gray else Color.Black
                        )
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color(0xFF1F41BB)
                    )
                }
            }

            // Date Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // From Date
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    onClick = { showFromDatePicker = true }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "From Date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = fromDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // To Date
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    onClick = { showToDatePicker = true }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "To Date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = toDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Reason Input
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("Reason for Leave") },
                placeholder = { Text("Enter your reason here") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1F41BB),
                    focusedLabelColor = Color(0xFF1F41BB)
                )
            )

            // Submit Button
            Button(
                onClick = {
                    if (staffName.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Error: Staff information not available")
                        }
                        return@Button
                    }
                    if (leaveType.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please select a leave type")
                        }
                        return@Button
                    }
                    if (reason.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter a reason for leave")
                        }
                        return@Button
                    }
                    viewModel.submitLeaveApplication(
                        fromDate = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        toDate = toDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        reason = reason,
                        leaveType = leaveType,
                        department = department
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && !isLoadingStaff,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F41BB),
                    disabledContainerColor = Color(0xFF1F41BB).copy(alpha = 0.5f)
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("Submit Application")
                }
            }
        }
    }

    // Leave Type Dialog
    if (showLeaveTypeDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveTypeDialog = false },
            title = {
                Text(
                    "Select Leave Type",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF1F41BB)
                )
            },
            text = {
                Column {
                    leaveTypes.forEach { type ->
                        TextButton(
                            onClick = {
                                leaveType = type
                                showLeaveTypeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = type,
                                color = if (type == leaveType) Color(0xFF1F41BB) else Color.Black
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLeaveTypeDialog = false }) {
                    Text("Cancel", color = Color(0xFF1F41BB))
                }
            }
        )
    }

    // Date Pickers
    if (showFromDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showFromDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showFromDatePicker = false }) {
                    Text("OK", color = Color(0xFF1F41BB))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFromDatePicker = false }) {
                    Text("Cancel", color = Color(0xFF1F41BB))
                }
            }
        ) {
            DatePicker(
                state = rememberDatePickerState(
                    initialSelectedDateMillis = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                ),
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = Color(0xFF1F41BB),
                    todayDateBorderColor = Color(0xFF1F41BB),
                    selectedYearContainerColor = Color(0xFF1F41BB)
                )
            )
        }
    }

    if (showToDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showToDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showToDatePicker = false }) {
                    Text("OK", color = Color(0xFF1F41BB))
                }
            },
            dismissButton = {
                TextButton(onClick = { showToDatePicker = false }) {
                    Text("Cancel", color = Color(0xFF1F41BB))
                }
            }
        ) {
            DatePicker(
                state = rememberDatePickerState(
                    initialSelectedDateMillis = toDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                ),
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = Color(0xFF1F41BB),
                    todayDateBorderColor = Color(0xFF1F41BB),
                    selectedYearContainerColor = Color(0xFF1F41BB)
                )
            )
        }
    }
} 