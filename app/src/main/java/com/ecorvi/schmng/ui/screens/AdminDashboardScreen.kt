package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.R
import com.ecorvi.schmng.ui.components.Bar
import com.ecorvi.schmng.ui.components.ExpandableItem
import com.ecorvi.schmng.ui.components.OptionsDialog
import com.ecorvi.schmng.ui.components.SummaryCard
import com.ecorvi.schmng.ui.data.InMemoryDatabase
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {
    // Firebase Auth instance for logout
    val auth = FirebaseAuth.getInstance()

    // State for expandable "Manage" section
    val manageItems = listOf("Classroom", "Students", "Teachers", "Others Staff")
    val expandedStates = remember { mutableStateMapOf<String, Boolean>().apply {
        manageItems.forEach { this[it] = false }
    } }

    // State for bottom navigation
    var selectedTab by remember { mutableIntStateOf(0) }

    // State for dialog visibility and selected category
    var showDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var showAddInput by remember { mutableStateOf(false) }
    var showViewList by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    // Initialize the in-memory database with sample data (only once)
    LaunchedEffect(Unit) {
        if (InMemoryDatabase.schedulesList.isEmpty()) {
            InMemoryDatabase.schedulesList.addAll(listOf("Math Class - 10 AM", "Science Class - 11 AM"))
        }
        if (InMemoryDatabase.pendingFeesList.isEmpty()) {
            InMemoryDatabase.pendingFeesList.addAll(listOf("John Doe - $100", "Jane Smith - $50"))
        }
    }

    // Function to get the appropriate list based on the category
    fun getDataList(category: String): List<String> {
        return when (category) {
            "SCHEDULE" -> InMemoryDatabase.schedulesList
            "PENDING FEES" -> InMemoryDatabase.pendingFeesList
            else -> emptyList()
        }
    }

    // Function to add an item to the appropriate list
    fun addItem(category: String, item: String) {
        when (category) {
            "SCHEDULE" -> InMemoryDatabase.schedulesList.add(item)
            "PENDING FEES" -> InMemoryDatabase.pendingFeesList.add(item)
        }
    }

    // Show the dialog for SCHEDULE and PENDING FEES
    if (showDialog) {
        OptionsDialog(
            category = selectedCategory,
            onDismiss = {
                showDialog = false
                showAddInput = false
                showViewList = false
                inputText = ""
            },
            onAddClick = {
                showAddInput = true
                showViewList = false
            },
            onViewClick = {
                showViewList = true
                showAddInput = false
            },
            showAddInput = showAddInput,
            showViewList = showViewList,
            inputText = inputText,
            onInputChange = { inputText = it },
            onConfirmAdd = { newItem ->
                if (newItem.isNotBlank()) {
                    addItem(selectedCategory, newItem)
                }
                showAddInput = false
                showDialog = false
                inputText = ""
            },
            dataList = getDataList(selectedCategory)
        )
    }

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ecorvilogo),
                            contentDescription = "EI Logo",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(50.dp))
                        Text(
                            text = "DASHBOARD",
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle notifications */ }) {
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.analytics_icon),
                            contentDescription = "Analytics",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Analytics") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.chat_icon),
                            contentDescription = "Chat",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Chat") }
                )
            }
        },
        snackbarHost = { /* Empty */ },
        floatingActionButton = { /* Empty */ },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = contentColorFor(MaterialTheme.colorScheme.background),
        content = { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = { /* Handle search */ },
                            placeholder = { Text("Search") },
                            modifier = Modifier
                                .width(355.dp)
                                .height(58.dp)
                                .clip(RoundedCornerShape(28.dp)),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            },
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color(0xFF1F41BB),
                                unfocusedIndicatorColor = Color.Gray,
                                unfocusedContainerColor = Color(0xFFECE6F0),
                                focusedContainerColor = Color(0xFFECE6F0)
                            )
                        )
                    }
                }

                // Summary Cards
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
                            onClick = { category ->
                                if (category == "TOTAL STUDENTS") {
                                    navController.navigate("students")
                                }
                            }
                        )
                        SummaryCard(
                            title = "TOTAL TEACHERS",
                            image = painterResource(id = R.drawable.teacher_icon),
                            modifier = Modifier.weight(1f),
                            onClick = { category ->
                                if (category == "TOTAL TEACHERS") {
                                    navController.navigate("teachers")
                                }
                            }
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
                            image = painterResource(id = R.drawable.schedule),
                            modifier = Modifier.weight(1f),
                            onClick = { category ->
                                selectedCategory = category
                                showDialog = true
                            }
                        )
                        SummaryCard(
                            title = "PENDING FEES",
                            image = painterResource(id = R.drawable.money),
                            modifier = Modifier.weight(1f),
                            onClick = { category ->
                                selectedCategory = category
                                showDialog = true
                            }
                        )
                    }
                }

                // Attendance Pie Chart (Simulated)
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "ATTENDANCE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .background(Color(0xFFE91E63), shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("86 - 43.4%", color = Color(0xFFE91E63))
                                Text("31 - 15.7%", color = Color(0xFF2196F3))
                                Text("35 - 17.7%", color = Color(0xFF3F51B5))
                                Text("46 - 23.2%", color = Color(0xFF795548))
                            }
                        }
                    }
                }

                // Attendance Bar Chart (Simulated)
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "ATTENDANCE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Bar(height = 30.dp, percentage = "13.7%")
                            Bar(height = 40.dp, percentage = "17.7%")
                            Bar(height = 50.dp, percentage = "23.2%")
                            Bar(height = 80.dp, percentage = "43.4%")
                        }
                    }
                }

                // Manage Section
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
                        onExpandClick = {
                            expandedStates[item] = !(expandedStates[item] == true)
                        },
                        onItemClick = {
                            when (item) {
                                "Students" -> navController.navigate("students")
                                "Teachers" -> navController.navigate("teachers")
                                // Add routes for other items as needed
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    )
}

