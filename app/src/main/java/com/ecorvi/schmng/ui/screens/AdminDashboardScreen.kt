package com.ecorvi.schmng.ui.screens

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ecorvi.schmng.ui.components.*
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val manageItems = listOf("Students", "Teachers")
    val expandedStates = remember { mutableStateMapOf<String, Boolean>().apply {
        manageItems.forEach { this[it] = false }
    } }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var showAddInput by remember { mutableStateOf(false) }
    var showViewList by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var schedules by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingFees by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun showMessage(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(Unit) {
        FirestoreDatabase.fetchSchedules(
            onComplete = { fetchedSchedules ->
                schedules = fetchedSchedules
                isLoading = false
            },
            onFailure = { e ->
                errorMessage = "Failed to load schedules: ${e.message}"
                isLoading = false
            }
        )
        FirestoreDatabase.fetchPendingFees(
            onComplete = { fetchedFees ->
                pendingFees = fetchedFees
                isLoading = false
            },
            onFailure = { e ->
                errorMessage = "Failed to load pending fees: ${e.message}"
                isLoading = false
            }
        )
    }

    fun addItem(category: String, item: String) {
        if (item.isBlank()) return
        when (category) {
            "SCHEDULE" -> {
                FirestoreDatabase.addSchedule(item, {
                    showMessage("Schedule added successfully")
                    FirestoreDatabase.fetchSchedules({ schedules = it }, {
                        showMessage("Failed to refresh schedules")
                    })
                }, {
                    showMessage("Error adding schedule: ${it.message}")
                })
            }

            "PENDING FEES" -> {
                FirestoreDatabase.addPendingFee(item, {
                    showMessage("Pending fee added successfully")
                    FirestoreDatabase.fetchPendingFees({ pendingFees = it }, {
                        showMessage("Failed to refresh fees")
                    })
                }, {
                    showMessage("Error adding fee: ${it.message}")
                })
            }
        }
    }

    if (showDialog) {
        val dataList = when (selectedCategory) {
            "SCHEDULE" -> schedules
            "PENDING FEES" -> pendingFees
            else -> emptyList()
        }
        OptionsDialog(
            category = selectedCategory,
            onDismiss = {
                showDialog = false
                showAddInput = false
                showViewList = false
                inputText = ""
            },
            onAddClick = { showAddInput = true; showViewList = false },
            onViewClick = { showViewList = true; showAddInput = false },
            showAddInput = showAddInput,
            showViewList = showViewList,
            inputText = inputText,
            onInputChange = { inputText = it },
            onConfirmAdd = {
                addItem(selectedCategory, it)
                showAddInput = false
                showDialog = false
                inputText = ""
            },
            dataList = dataList
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("DASHBOARD", color = Color.Black, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo("adminDashboard") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }

            errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(errorMessage ?: "Unknown error", color = Color.Red)
            }

            else -> AdminDashboardContent(
                schedules = schedules,
                pendingFees = pendingFees,
                expandedStates = expandedStates,
                onExpandClick = { item -> expandedStates[item] = !(expandedStates[item] == true) },
                onItemClick = { item ->
                    navController.navigate(item.lowercase()) // students or teachers
                },
                onSummaryClick = {
                    selectedCategory = it
                    showDialog = true
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
fun AdminDashboardContent(
    schedules: List<String>,
    pendingFees: List<String>,
    expandedStates: Map<String, Boolean>,
    onExpandClick: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onSummaryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val manageItems = listOf("Students", "Teachers")
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Search") },
                    modifier = Modifier.width(355.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    title = "TOTAL STUDENTS",
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f),
                    onClick = { onItemClick("students") }
                )
                SummaryCard(
                    title = "TOTAL TEACHERS",
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f),
                    onClick = { onItemClick("teachers") }
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    title = "SCHEDULE",
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f),
                    onClick = { onSummaryClick("SCHEDULE") }
                )
                SummaryCard(
                    title = "PENDING FEES",
                    icon = Icons.Default.Money,
                    modifier = Modifier.weight(1f),
                    onClick = { onSummaryClick("PENDING FEES") }
                )
            }
        }
        item {
            Text(
                text = "MANAGE",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        items(manageItems) { item ->
            ExpandableItem(
                title = item,
                isExpanded = expandedStates[item] == true,
                onExpandClick = { onExpandClick(item) },
                onItemClick = { onItemClick(item) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
fun PreviewAdminDashboardScreen() {
    val dummyStates = mapOf("Students" to true, "Teachers" to false)
    AdminDashboardContent(
        schedules = listOf("Period 1 - 9AM", "Period 2 - 10AM"),
        pendingFees = listOf("Student A", "Student B"),
        expandedStates = dummyStates,
        onExpandClick = {},
        onItemClick = {},
        onSummaryClick = {}
    )
}
