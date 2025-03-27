package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.R
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
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
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
                            popUpTo("admin_dashboard") { inclusive = true }
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
                    OutlinedTextField(
                        value = "",
                        onValueChange = { /* Handle search */ },
                        placeholder = { Text("Search") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp) // Add padding to prevent edge clipping
                            .clip(RoundedCornerShape(50.dp)),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color(0xFF1F41BB),
                            unfocusedIndicatorColor = Color.Gray,
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor = Color(0xFFF5F5F5)
                        )
                    )
                }

                // Summary Cards
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp), // Add padding to prevent edge clipping
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SummaryCard(
                            title = "TOTAL STUDENTS",
                            icon = Icons.Default.Person,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "TOTAL TEACHERS",
                            image = painterResource(id = R.drawable.teacher_icon),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp), // Add padding to prevent edge clipping
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SummaryCard(
                            title = "SCHEDULE",
                            image = painterResource(id = R.drawable.schedule),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "PENDING FEES",
                            image = painterResource(id = R.drawable.money),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Attendance Pie Chart (Simulated)
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp) // Add padding to prevent edge clipping
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
                        modifier = Modifier.padding(horizontal = 16.dp) // Add padding to prevent edge clipping
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
                        modifier = Modifier.padding(horizontal = 16.dp) // Add padding to prevent edge clipping
                    )
                }
                items(manageItems) { item ->
                    ExpandableItem(
                        title = item,
                        isExpanded = expandedStates[item] ?: false,
                        onExpandClick = {
                            expandedStates[item] = !(expandedStates[item] ?: false)
                        },
                        onItemClick = {
                            when (item) {
                                "Students" -> navController.navigate("manage_students")
                                // Add routes for other items as needed
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp) // Add padding to prevent edge clipping
                    )
                }
            }
        }
    )
}

// Partial Preview (Existing)
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun AdminDashboardScreenPreview() {
    Scaffold(
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
                        Spacer(modifier = Modifier.width(35.dp))
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
                    IconButton(onClick = { /* Handle logout */ }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = "",
                        onValueChange = { /* Handle search */ },
                        placeholder = { Text("Search") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(45.dp)),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color(0xFF1F41BB),
                            unfocusedIndicatorColor = Color.Gray,
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor = Color(0xFFF5F5F5)
                        )
                    )
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
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "TOTAL TEACHERS",
                            image = painterResource(id = R.drawable.teacher_icon),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    )
}

// Full Layout Preview
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, heightDp = 800)
@Composable
fun AdminDashboardScreenFullPreview() {
    // Mock state for expandable "Manage" section
    val manageItems = listOf("Classroom", "Students", "Teachers", "Others Staff")
    val expandedStates = remember { mutableStateMapOf<String, Boolean>().apply {
        manageItems.forEach { this[it] = false }
        this["Students"] = true // Mock one item as expanded
    } }

    // Mock state for bottom navigation
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
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
                    IconButton(onClick = { /* Handle logout */ }) {
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
                    OutlinedTextField(
                        value = "",
                        onValueChange = { /* Handle search */ },
                        placeholder = { Text("Search") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(50.dp)),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color(0xFF1F41BB),
                            unfocusedIndicatorColor = Color.Gray,
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor = Color(0xFFF5F5F5)
                        )
                    )
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
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "TOTAL TEACHERS",
                            image = painterResource(id = R.drawable.teacher_icon),
                            modifier = Modifier.weight(1f)
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
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "PENDING FEES",
                            image = painterResource(id = R.drawable.money),
                            modifier = Modifier.weight(1f)
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
                        isExpanded = expandedStates[item] ?: false,
                        onExpandClick = {
                            expandedStates[item] = !(expandedStates[item] ?: false)
                        },
                        onItemClick = { /* Mock navigation */ },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    )
}

// Composable for Summary Cards
@Composable
fun SummaryCard(title: String, image: Painter? = null, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (image != null) {
                Image(
                    painter = image,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Composable for Simulated Bar Chart
@Composable
fun Bar(height: Dp, percentage: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .height(height)
                .background(Color(0xFF3F51B5), shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = percentage,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

// Composable for Expandable Manage Items
@Composable
fun ExpandableItem(
    title: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onItemClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Image(
                painter = if (isExpanded) painterResource(id = R.drawable.expand_less) else painterResource(id = R.drawable.expand_more),
                contentDescription = "Expand",
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onExpandClick() }
                    .size(24.dp)
            )
        }
    }
}