package com.ecorvi.schmng.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ecorvi.schmng.ui.components.*
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Fee
import com.ecorvi.schmng.ui.data.model.Schedule
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.layout.ContentScale
import com.ecorvi.schmng.R
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.filled.Update
import android.content.pm.PackageManager
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import java.text.SimpleDateFormat
import java.util.*

private val PrimaryBlue = Color(0xFF1F41BB)
private val StudentGreen = Color(0xFF4CAF50)
private val TeacherBlue = Color(0xFF2196F3)
private val ScheduleOrange = Color(0xFFFF9800)
private val FeesRed = Color(0xFFE91E63)
private val BackgroundColor = Color.White.copy(alpha = 0.95f)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val manageItems = listOf(
        "STUDENTS" to Icons.Default.Person,
        "TEACHERS" to Icons.Default.School,
        "TIMETABLE" to Icons.Default.Schedule
    )
    val expandedStates = remember { mutableStateMapOf<String, Boolean>().apply {
        manageItems.forEach { this[it.first] = false }
    } }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var showAddInput by remember { mutableStateOf(false) }
    var showViewList by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var schedules by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var pendingFees by remember { mutableStateOf<List<Fee>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var studentCount by remember { mutableStateOf(0) }
    var teacherCount by remember { mutableStateOf(0) }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showNavigationDrawer by remember { mutableStateOf(false) }
    
    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(Unit) {
        isLoading = true
        // Reverted to static calls
        FirestoreDatabase.fetchStudentCount { count -> studentCount = count }
        FirestoreDatabase.fetchTeacherCount { count -> teacherCount = count }
        isLoading = false
    }

    fun showMessage(msg: String) {
        errorMessage = msg
        coroutineScope.launch {
            delay(3000)
            errorMessage = null
        }
    }

    fun addItem(category: String, item: String) {
        if (item.isBlank()) return
        // Reverted to static calls
        when (category) {
            "SCHEDULE" -> {
                val schedule = Schedule(
                    title = item,
                    description = "", date = "", time = "", className = ""
                )
                FirestoreDatabase.addSchedule(
                    schedule = schedule,
                    onSuccess = {
                        showMessage("Schedule added successfully")
                        FirestoreDatabase.fetchSchedules(
                            onComplete = { schedules = it },
                            onFailure = { e -> showMessage("Failed to refresh schedules: ${e.message}") }
                        )
                    },
                    onFailure = { e -> showMessage("Error adding schedule: ${e.message}") }
                )
            }
            "PENDING FEES" -> {
                val fee = Fee(
                    studentName = item,
                    amount = 0.0, dueDate = "", description = ""
                )
                FirestoreDatabase.addFee(
                    fee = fee,
                    onSuccess = {
                        showMessage("Fee added successfully")
                        FirestoreDatabase.fetchPendingFees(
                            onComplete = { pendingFees = it },
                            onFailure = { e -> showMessage("Failed to refresh fees: ${e.message}") }
                        )
                    },
                    onFailure = { e -> showMessage("Error adding fee: ${e.message}") }
                )
            }
        }
    }

    // Function to handle help option - open email client
    fun handleHelpClick() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:info@ecorvi.com")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "No email app found. Please email us at info@ecorvi.com",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Function to handle logout
    fun handleLogout() {
        auth.signOut()
        // Clear cached role on logout
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("user_role")
            .apply()
            
        navController.navigate("login") { // Navigate to login
            popUpTo("admin_dashboard") { inclusive = true } // Correct route name
            launchSingleTop = true // Avoid multiple login screens
        }
    }

    // Function to handle update check
    fun checkForUpdates() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // If Play Store app is not installed, open in browser
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Unable to open Play Store",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    if (showDialog) {
        val dataList = when (selectedCategory) {
            "SCHEDULE" -> schedules.map { it.title }
            "PENDING FEES" -> pendingFees.map { "${it.studentName} - ₹${it.amount}" }
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
            onDelete = { item ->
                // Reverted to static calls
                when (selectedCategory) {
                    "SCHEDULE" -> {
                        val scheduleToDelete = schedules.find { it.title == item }
                        if (scheduleToDelete != null) {
                            FirestoreDatabase.deleteSchedule(
                                scheduleId = scheduleToDelete.id,
                                onSuccess = {
                                    showMessage("Schedule deleted successfully")
                                    FirestoreDatabase.fetchSchedules(
                                        onComplete = { schedules = it },
                                        onFailure = { e -> showMessage("Failed to refresh schedules: ${e.message}") }
                                    )
                                },
                                onFailure = { e -> showMessage("Error deleting schedule: ${e.message}") }
                            )
                        }
                    }
                    "PENDING FEES" -> {
                        val feeToDelete = pendingFees.find { "${it.studentName} - ₹${it.amount}" == item }
                        if (feeToDelete != null) {
                            FirestoreDatabase.deleteFee(
                                feeId = feeToDelete.id,
                                onSuccess = {
                                    showMessage("Fee deleted successfully")
                                    FirestoreDatabase.fetchPendingFees(
                                        onComplete = { pendingFees = it },
                                        onFailure = { e -> showMessage("Failed to refresh fees: ${e.message}") }
                                    )
                                },
                                onFailure = { e -> showMessage("Error deleting fee: ${e.message}") }
                            )
                        }
                    }
                }
            },
            dataList = dataList
        )
    }

    @Composable
    fun DashboardShimmer() {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnim = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer"
        )

        val shimmerColorShades = listOf(
            Color.LightGray.copy(alpha = 0.9f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.9f)
        )

        val brush = Brush.linearGradient(
            colors = shimmerColorShades,
            start = Offset(translateAnim.value - 1000f, translateAnim.value - 1000f),
            end = Offset(translateAnim.value, translateAnim.value)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
        ) {
            // Search bar shimmer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Pie Chart shimmer
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = BackgroundColor,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp) // Updated to match new pie chart height
                                .clip(RoundedCornerShape(8.dp))
                                .background(brush)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Summary Row shimmer
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp) // Updated to match new summary card height
                            .clip(RoundedCornerShape(12.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp) // Updated to match new summary card height
                            .clip(RoundedCornerShape(12.dp))
                            .background(brush)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Manage section shimmer
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(80.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Manage items shimmer
            items(3) { _ ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = Color.White.copy(alpha = 0.95f)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // App Logo and Name
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ecorvilogo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(80.dp)
                            .padding(8.dp)
                    )
                    Text(
                        text = "Ecorvi School Management",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.Gray.copy(alpha = 0.2f)
                )

                // Navigation Items
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        // Navigate to profile
                    }
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Timetable") },
                    label = { Text("Timetable Management") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("timetable_management")
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Version and Update Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    Text(
                        text = "Version ${packageInfo.versionName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { checkForUpdates() },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Update,
                                contentDescription = "Check for updates",
                                tint = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "Check for Updates",
                                color = PrimaryBlue,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    ) {
        CommonBackground {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    "DASHBOARD",
                                    color = PrimaryBlue,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (drawerState.isClosed) drawerState.open()
                                        else drawerState.close()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = PrimaryBlue
                                )
                            }
                        },
                        actions = {
                            Box {
                                IconButton(onClick = { showDropdownMenu = true }) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = "Profile Menu",
                                        tint = PrimaryBlue
                                    )
                                }

                                DropdownMenu(
                                    expanded = showDropdownMenu,
                                    onDismissRequest = { showDropdownMenu = false },
                                    modifier = Modifier
                                        .background(Color.White)
                                        .width(160.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Help,
                                                    contentDescription = "Help",
                                                    tint = PrimaryBlue
                                                )
                                                Text(
                                                    "Help",
                                                    color = PrimaryBlue
                                                )
                                            }
                                        },
                                        onClick = {
                                            showDropdownMenu = false
                                            handleHelpClick()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Logout,
                                                    contentDescription = "Logout",
                                                    tint = PrimaryBlue
                                                )
                                                Text(
                                                    "Logout",
                                                    color = PrimaryBlue
                                                )
                                            }
                                        },
                                        onClick = {
                                            showDropdownMenu = false
                                            handleLogout()
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White.copy(alpha = 0.95f)
                        )
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        if (isLoading) {
                            DashboardShimmer()
                        } else if (errorMessage != null) {
                            // Error state
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Error loading dashboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Red
                                )
                                Text(
                                    text = errorMessage ?: "Unknown error occurred",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                Button(
                                    onClick = {
                                        isLoading = true
                                        errorMessage = null
                                        // Retry loading data - Reverted to static calls
                                        FirestoreDatabase.fetchStudentCount { count ->
                                            studentCount = count
                                        }
                                        FirestoreDatabase.fetchTeacherCount { count ->
                                            teacherCount = count
                                        }
                                        isLoading = false
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                        } else {
                            // Dashboard content
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(
                                    top = 12.dp,
                                    bottom = 16.dp
                                )
                            ) {
                                item {
                                    SearchBar()
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                item {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = BackgroundColor,
                                        shadowElevation = 2.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .padding(8.dp)
                                            ) {
                                                PieChart(
                                                    studentCount = studentCount,
                                                    teacherCount = teacherCount,
                                                    onStudentClick = { navController.navigate("students") },
                                                    onTeacherClick = { navController.navigate("teachers") }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        AnimatedSummaryCard(
                                            title = "SCHEDULE",
                                            icon = Icons.Default.Schedule,
                                            onClick = { navController.navigate("schedules") },
                                            backgroundColor = ScheduleOrange,
                                            modifier = Modifier.weight(1f)
                                        )
                                        AnimatedSummaryCard(
                                            title = "PENDING FEES",
                                            icon = Icons.Default.CurrencyRupee,
                                            onClick = { navController.navigate("pending_fees") },
                                            backgroundColor = FeesRed,
                                            modifier = Modifier.weight(1f)
                                    )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                item {
                                    Text(
                                        text = "MANAGE",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                }

                                items(manageItems) { (item, icon) ->
                                    val isExpanded = expandedStates[item] == true
                                    EnhancedExpandableItem(
                                        title = item,
                                        isExpanded = isExpanded,
                                        onExpandClick = { expandedStates[item] = !isExpanded },
                                        onItemClick = {
                                            when (item) {
                                                "STUDENTS" -> navController.navigate("students")
                                                "TEACHERS" -> navController.navigate("teachers")
                                                "TIMETABLE" -> navController.navigate("timetable_management")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SearchBar() {
    var searchText by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isFocused) 4.dp else 2.dp,
        color = Color.White.copy(alpha = 0.95f)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .onFocusChanged { state ->
                    isFocused = state.isFocused
                },
            placeholder = {
                Text(
                    text = "Search...",
                    fontSize = 14.sp,
                    color = if (isFocused) PrimaryBlue.copy(alpha = 0.6f) else Color.Gray
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isFocused) PrimaryBlue else Color.Gray,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(start = 4.dp)
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                cursorColor = PrimaryBlue,
                focusedLeadingIconColor = PrimaryBlue,
                unfocusedLeadingIconColor = Color.Gray.copy(alpha = 0.7f)
            ),
            singleLine = true,
            interactionSource = interactionSource,
            textStyle = TextStyle(fontSize = 14.sp)
        )
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Red,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun AnimatedSummaryRow(
    leftTitle: String,
    rightTitle: String,
    leftIcon: ImageVector,
    rightIcon: ImageVector,
    leftClick: () -> Unit,
    rightClick: () -> Unit,
    leftColor: Color,
    rightColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedSummaryCard(
            title = leftTitle,
            icon = leftIcon,
            onClick = leftClick,
            backgroundColor = leftColor,
            modifier = Modifier.weight(1f)
        )
        AnimatedSummaryCard(
            title = rightTitle,
            icon = rightIcon,
            onClick = rightClick,
            backgroundColor = rightColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AnimatedSummaryCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )

    Surface(
        modifier = modifier
            .height(120.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (isPressed) 2.dp else 4.dp,
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .clickable {
                    isPressed = true
                    onClick()
                    isPressed = false
                }
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        scaleX = if (isPressed) 0.9f else 1f
                        scaleY = if (isPressed) 0.9f else 1f
                    }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer {
                    alpha = if (isPressed) 0.8f else 1f
                }
            )
        }
    }
}

@Composable
fun OptionsDialog(
    category: String,
    onDismiss: () -> Unit,
    onAddClick: () -> Unit,
    onViewClick: () -> Unit,
    showAddInput: Boolean,
    showViewList: Boolean,
    inputText: String,
    onInputChange: (String) -> Unit,
    onConfirmAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
    dataList: List<String>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(category) },
        text = {
            Column {
                if (!showAddInput && !showViewList) {
                    Button(
                        onClick = onAddClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add")
                    }
                    Button(
                        onClick = onViewClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View")
                    }
                }

                if (showAddInput) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        label = { Text("Enter ${category.lowercase()}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (showViewList) {
                    LazyColumn {
                        items(dataList) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDelete(item) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showAddInput) {
                TextButton(onClick = { onConfirmAdd(inputText) }) {
                    Text("Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EnhancedExpandableItem(
    title: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onItemClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isPressed) 1.dp else 2.dp,
        color = Color.White.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    isPressed = true
                    onItemClick()
                    isPressed = false
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = PrimaryBlue,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = onExpandClick,
                modifier = Modifier
                    .size(32.dp)
                    .scale(if (isPressed) 0.9f else 1f)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAdminDashboardScreen() {
    AdminDashboardScreen(rememberNavController())
}

@Composable
fun CommonBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_ui),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        content()
    }
}

@Composable
fun PieChart(
    studentCount: Int,
    teacherCount: Int,
    onStudentClick: () -> Unit,
    onTeacherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = studentCount + teacherCount
    if (total == 0) return // prevent division by zero

    val gapAngle = 3.5f
    val totalGapAngle = gapAngle * 2 // Total gap space to distribute
    val availableAngle = 360f - totalGapAngle // Available angle for segments

    // Calculate the angle proportions dynamically based on counts
    val teacherRatio = teacherCount.toFloat() / total
    val studentRatio = studentCount.toFloat() / total
    
    // Dynamically calculate segment angles
    val teacherAngle = teacherRatio * availableAngle
    val studentAngle = studentRatio * availableAngle

    // Define the precise start angles for each segment
    val teacherStartAngle = gapAngle
    val studentStartAngle = teacherStartAngle + teacherAngle + gapAngle

    var studentPressed by remember { mutableStateOf(false) }
    var teacherPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 100.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val radius = (minOf(canvasWidth, canvasHeight) / 2.2f)
                        val centerX = canvasWidth / 2
                        val centerY = canvasHeight / 2
                        val strokeWidth = radius * 0.6f

                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val distanceFromCenter = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())

                        // Only detect touches within the ring
                        if (distanceFromCenter >= (radius - strokeWidth) && 
                            distanceFromCenter <= radius) {
                            
                            // Calculate angle in degrees from 0-360
                            // In Canvas, 0 degrees is at 3 o'clock, moving clockwise
                            var angle = Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat()
                            if (angle < 0) angle += 360f // Convert negative angles to 0-360 range

                            // Define segment boundaries precisely
                            val teacherEndAngle = teacherStartAngle + teacherAngle
                            val studentEndAngle = studentStartAngle + studentAngle

                            // Debug information
                            println("Touch at angle: $angle")
                            println("Teacher segment: $teacherStartAngle to $teacherEndAngle")
                            println("Student segment: $studentStartAngle to $studentEndAngle")

                            // Determine which segment was clicked
                            when {
                                // Check if angle is within teacher segment
                                (angle >= teacherStartAngle && angle <= teacherEndAngle) -> {
                                    teacherPressed = true
                                    onTeacherClick()
                                    println("Teacher segment clicked")
                                }
                                // Check if angle is within student segment
                                (angle >= studentStartAngle && angle <= studentEndAngle || 
                                 angle >= 0f && angle < gapAngle) -> { // Handle wraparound at 360°
                                    studentPressed = true
                                    onStudentClick()
                                    println("Student segment clicked")
                                }
                            }
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val radius = (minOf(canvasWidth, canvasHeight) / 2.2f)
            val centerX = canvasWidth / 2
            val centerY = canvasHeight / 2
            val strokeWidth = radius * 0.6f

            // Draw teacher segment (blue)
            drawArc(
                color = TeacherBlue.copy(alpha = if (teacherPressed) 0.7f else 0.85f),
                startAngle = teacherStartAngle,
                sweepAngle = teacherAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(centerX - radius, centerY - radius)
            )

            // Draw student segment (green)
            drawArc(
                color = StudentGreen.copy(alpha = if (studentPressed) 0.7f else 0.85f),
                startAngle = studentStartAngle,
                sweepAngle = studentAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(centerX - radius, centerY - radius)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { onTeacherClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(TeacherBlue, RoundedCornerShape(2.dp))
                )
                Column {
                    Text(
                        text = "Teachers",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.DarkGray
                        )
                    )
                    Text(
                        text = "$teacherCount",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { onStudentClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(StudentGreen, RoundedCornerShape(2.dp))
                )
                Column {
                    Text(
                        text = "Students",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.DarkGray
                        )
                    )
                    Text(
                        text = "$studentCount",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                    )
                }
            }
        }

        LaunchedEffect(studentPressed, teacherPressed) {
            if (studentPressed || teacherPressed) {
                delay(50)
                studentPressed = false
                teacherPressed = false
            }
        }
    }
}


@Composable
private fun AttendanceCountRow(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
