package com.ecorvi.schmng.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ecorvi.schmng.ui.components.*
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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

    var studentCount by remember { mutableStateOf(0) }
    var teacherCount by remember { mutableStateOf(0) }

    fun showMessage(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        FirestoreDatabase.fetchSchedules(
            onComplete = { fetchedSchedules -> schedules = fetchedSchedules; isLoading = false },
            onFailure = { e -> errorMessage = "Failed to load schedules: ${e.message}"; isLoading = false }
        )
        FirestoreDatabase.fetchPendingFees(
            onComplete = { fetchedFees -> pendingFees = fetchedFees; isLoading = false },
            onFailure = { e -> errorMessage = "Failed to load pending fees: ${e.message}"; isLoading = false }
        )
        FirestoreDatabase.fetchStudentCount { studentCount = it }
        FirestoreDatabase.fetchTeacherCount { teacherCount = it }
    }

    fun addItem(category: String, item: String) {
        if (item.isBlank()) return
        when (category) {
            "SCHEDULE" -> FirestoreDatabase.addSchedule(item, {
                showMessage("Schedule added successfully")
                FirestoreDatabase.fetchSchedules({ schedules = it }, { showMessage("Failed to refresh schedules") })
            }, { e -> showMessage("Error adding schedule: ${e.message}") })

            "PENDING FEES" -> FirestoreDatabase.addPendingFee(item, {
                showMessage("Pending fee added successfully")
                FirestoreDatabase.fetchPendingFees({ pendingFees = it }, { showMessage("Failed to refresh fees") })
            }, { e -> showMessage("Error adding fee: ${e.message}") })
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
            onConfirmAdd = { newItem ->
                addItem(selectedCategory, newItem)
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
                        Text("DASHBOARD", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage ?: "Unknown error", color = Color.Red)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(padding),
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
                        AnimatedSummaryRow(
                            leftTitle = "TOTAL STUDENTS\n$studentCount",
                            rightTitle = "TOTAL TEACHERS\n$teacherCount",
                            leftIcon = Icons.Default.Person,
                            rightIcon = Icons.Default.Person,
                            leftClick = { navController.navigate("students") },
                            rightClick = { navController.navigate("teachers") }
                        )
                    }
                    item {
                        AnimatedSummaryRow(
                            leftTitle = "SCHEDULE",
                            rightTitle = "PENDING FEES",
                            leftIcon = Icons.Default.Schedule,
                            rightIcon = Icons.Default.Money,
                            leftClick = { selectedCategory = "SCHEDULE"; showDialog = true },
                            rightClick = { selectedCategory = "PENDING FEES"; showDialog = true }
                        )
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
                        val isExpanded = expandedStates[item] == true
                        ExpandableItem(
                            title = item,
                            isExpanded = isExpanded,
                            onExpandClick = { expandedStates[item] = !isExpanded },
                            onItemClick = {
                                when (item) {
                                    "Students" -> navController.navigate("students")
                                    "Teachers" -> navController.navigate("teachers")
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun AnimatedSummaryRow(
    leftTitle: String,
    rightTitle: String,
    leftIcon: ImageVector,
    rightIcon: ImageVector,
    leftClick: () -> Unit,
    rightClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedSummaryCard(title = leftTitle, icon = leftIcon, onClick = leftClick, modifier = Modifier.weight(1f))
        AnimatedSummaryCard(title = rightTitle, icon = rightIcon, onClick = rightClick, modifier = Modifier.weight(1f))
    }
}

@Composable
fun AnimatedSummaryCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100)
    )

    Card(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                onClick = {
                    isPressed = true
                    onClick()
                    isPressed = false
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}



@Preview(showBackground = true)
@Composable
fun PreviewAdminDashboardScreen() {
    AdminDashboardScreen(rememberNavController())
}
