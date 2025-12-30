package com.ecorvi.schmng.ui.screens

import DeleteConfirmationDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.components.ProfilePhotoComponent
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import kotlinx.coroutines.launch

private val StudentGreen = Color(0xFF4CAF50) // Green color for student theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    navController: NavController,
    studentId: String,
    isAdmin: Boolean = false,
    currentRoute: String? = null,
    onRouteSelected: ((String) -> Unit)? = null
) {
    var student by remember { mutableStateOf<Person?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var studentGrades by remember { mutableStateOf<List<com.ecorvi.schmng.ui.data.model.StudentGrade>>(emptyList()) }
    var isLoadingGrades by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load student data
    LaunchedEffect(studentId) {
        try {
            student = FirestoreDatabase.getStudent(studentId)
            try {
                profilePhotoUrl = FirestoreDatabase.getProfilePhotoUrl(studentId)
            } catch (e: Exception) {
                // Handle photo loading error silently
            }
            
            // Load student grades
            isLoadingGrades = true
            try {
                studentGrades = FirestoreDatabase.getStudentGrades(studentId)
            } catch (e: Exception) {
                // Handle grades loading error silently
            }
            isLoadingGrades = false
            
            isLoading = false
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to load student profile: ${e.message}")
            }
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    if (isAdmin || currentRoute == null || onRouteSelected == null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isAdmin) {
                        // Edit button
                        IconButton(
                            onClick = { 
                                navController.navigate("add_person/student?personId=$studentId") 
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color.White
                            )
                        }
                        // Delete button
                        IconButton(
                            onClick = { showDeleteDialog = true }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudentGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (!isAdmin && currentRoute != null && onRouteSelected != null) {
                StudentBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { item -> onRouteSelected(item.route) }
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StudentGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo Holder (fully visible below the top bar)
                Box(
                    modifier = Modifier
                        .offset(y = 24.dp)
                        .size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProfilePhotoComponent(
                        userId = studentId,
                        photoUrl = profilePhotoUrl,
                        isEditable = false, // Always view-only for students
                        themeColor = StudentGreen
                    )
                }
                Text(
                    text = "Profile Photo",
                    color = StudentGreen,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .offset(y = 28.dp)
                        .padding(bottom = 0.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Profile Details
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Personal Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StudentGreen
                        )
                        
                        student?.let { student ->
                            ProfileField("Name", "${student.firstName} ${student.lastName}")
                            ProfileField("Admission Number", student.admissionNumber)
                            ProfileField("Email", student.email)
                            ProfileField("Class", student.className ?: "")
                            ProfileField("Roll Number", student.rollNumber)
                            ProfileField("Gender", student.gender ?: "")
                            ProfileField("Date of Birth", student.dateOfBirth ?: "")
                            ProfileField("Age", student.age?.toString() ?: "")
                            ProfileField("Mobile No", student.mobileNo ?: "")
                            ProfileField("Phone", student.phone ?: "")
                            ProfileField("Address", student.address ?: "")

                            // Admin-only: show admissions fields and IDs
                            if (isAdmin) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = "Admissions",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StudentGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                ProfileField("Date of Admission", student.admissionDate)
                                ProfileField("Academic Year", student.academicYear)
                                ProfileField("Aadhar Number", student.aadharNumber)
                                ProfileField("AAPAR ID", student.aaparId)
                                
                                // Category Information
                                if (student.caste.isNotBlank() || student.category.isNotBlank() || student.subCaste.isNotBlank()) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text(
                                        text = "Category Information",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = StudentGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (student.caste.isNotBlank()) ProfileField("Caste", student.caste)
                                    if (student.category.isNotBlank()) ProfileField("Category", student.category)
                                    if (student.subCaste.isNotBlank()) ProfileField("Sub Caste", student.subCaste)
                                }
                                
                                // Mode of Transport
                                if (student.modeOfTransport.isNotBlank()) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text(
                                        text = "Transport",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = StudentGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    ProfileField("Mode of Transport", student.modeOfTransport)
                                }
                                
                                // Fee Structure
                                if (student.feeStructure > 0) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text(
                                        text = "Fee Structure",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = StudentGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    ProfileField("Total Fee", "₹${String.format("%.2f", student.feeStructure)}")
                                    ProfileField("Amount Paid", "₹${String.format("%.2f", student.feePaid)}")
                                    ProfileField(
                                        "Remaining Amount", 
                                        "₹${String.format("%.2f", student.feeRemaining)}",
                                        if (student.feeRemaining > 0) Color.Red else StudentGreen
                                    )
                                }
                            }
                            
                            // Show parent information if available
                            student.parentInfo?.let { parent ->
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = "Parent Information",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = StudentGreen,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                ProfileField("Parent Name", parent.name)
                                ProfileField("Parent Email", parent.email)
                                ProfileField("Parent Phone", parent.phone)
                            }
                            
                            // Student Grades
                            if (studentGrades.isNotEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = "Academic Performance",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = StudentGreen,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                
                                studentGrades.groupBy { it.examType }.forEach { (examType, grades) ->
                                    grades.forEach { grade ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = StudentGreen.copy(alpha = 0.1f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "$examType - ${grade.examDate}",
                                                        fontWeight = FontWeight.Bold,
                                                        color = StudentGreen
                                                    )
                                                    Text(
                                                        text = "Overall: ${String.format("%.2f", grade.percentage)}% (${grade.grade})",
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (grade.percentage >= 60) StudentGreen else Color.Red
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                grade.subjects.forEach { (subjectName, subjectGrade) ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(subjectName, fontSize = 14.sp)
                                                        Text(
                                                            "${String.format("%.0f", subjectGrade.obtainedMarks)}/${String.format("%.0f", subjectGrade.maxMarks)} (${subjectGrade.grade})",
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        } ?: run {
                            Text(
                                text = "No student data available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                onConfirm = {
                    isDeleting = true
                    FirestoreDatabase.deleteStudent(
                        studentId = studentId,
                        onSuccess = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Student deleted successfully")
                                navController.navigateUp()
                            }
                        },
                        onFailure = { e ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to delete student: ${e.message}")
                                isDeleting = false
                                showDeleteDialog = false
                            }
                        }
                    )
                },
                onDismiss = { showDeleteDialog = false },
                itemType = "student"
            )
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String, valueColor: Color = Color.Black) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value.ifEmpty { "Not provided" },
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
} 