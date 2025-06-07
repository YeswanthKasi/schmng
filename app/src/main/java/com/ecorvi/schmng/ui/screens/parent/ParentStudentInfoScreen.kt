package com.ecorvi.schmng.ui.screens.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.theme.ParentBlue
import com.ecorvi.schmng.ui.components.ParentBottomNavigation
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentStudentInfoScreen(
    navController: NavController,
    childUid: String,
    showBackButton: Boolean = true,
    showBottomBar: Boolean = false,
    currentRoute: String? = null,
    onRouteSelected: ((String) -> Unit)? = null
) {
    var student by remember { mutableStateOf<Person?>(null) }
    var parent by remember { mutableStateOf<Person?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Fetch student and parent information
    LaunchedEffect(childUid, currentUserId) {
        scope.launch {
            try {
                val fetchedStudent = FirestoreDatabase.getStudent(childUid)
                student = fetchedStudent
                
                // Fetch parent information if available
                if (currentUserId != null) {
                    val fetchedParent = FirestoreDatabase.getParent(currentUserId)
                    parent = fetchedParent
                }
                
                isLoading = false
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Information", color = ParentBlue) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = ParentBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            if (showBottomBar && onRouteSelected != null) {
                ParentBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { item -> onRouteSelected(item.route) }
                )
            }
        }
    ) { padding ->
        StudentInfoContent(
            padding = padding,
            isLoading = isLoading,
            error = error,
            student = student,
            parent = parent,
            onBackClick = { navController.popBackStack() }
        )
    }
}

@Composable
private fun StudentInfoContent(
    padding: PaddingValues,
    isLoading: Boolean,
    error: String?,
    student: Person?,
    parent: Person?,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ParentBlue
                )
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ParentBlue
                        )
                    ) {
                        Text("Go Back")
                    }
                }
            }
            student == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Student information not found")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ParentBlue
                        )
                    ) {
                        Text("Go Back")
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Student Information Card
                    StudentInfoCard(student)
                    
                    // Parent Information Card (if available)
                    if (parent != null) {
                        ParentInfoCard(parent)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentInfoCard(student: Person) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Student Information",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1F41BB),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            InfoField("Name", "${student.firstName} ${student.lastName}")
            InfoField("Class", student.className ?: "")
            InfoField("Section", student.section ?: "")
            InfoField("Roll Number", student.rollNumber)
            InfoField("Gender", student.gender)
            InfoField("Date of Birth", student.dateOfBirth)
            InfoField("Mobile", student.mobileNo)
            InfoField("Address", student.address)
            
            // Add academic information if available
            student.className?.let { className ->
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Academic Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1F41BB),
                    fontWeight = FontWeight.Bold
                )
                InfoField("Class", className)
                InfoField("Section", student.section ?: "")
                InfoField("Roll Number", student.rollNumber)
            }
        }
    }
}

@Composable
private fun ParentInfoCard(parent: Person) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Parent Information",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1F41BB),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            InfoField("Name", "${parent.firstName} ${parent.lastName}")
            InfoField("Mobile", parent.mobileNo)
            InfoField("Email", parent.email ?: "")
            InfoField("Address", parent.address)
        }
    }
}

@Composable
private fun InfoField(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF1F41BB),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = Color.Black
        )
    }
} 