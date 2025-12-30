package com.ecorvi.schmng.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.data.model.StudentGrade
import com.ecorvi.schmng.ui.data.model.SubjectGrade
import com.ecorvi.schmng.ui.utils.Constants
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksScreen(navController: NavController) {
    var selectedClass by remember { mutableStateOf("") }
    var selectedExam by remember { mutableStateOf("FA1") }
    var selectedSubject by remember { mutableStateOf("Mathematics") }
    var students by remember { mutableStateOf<List<Person>>(emptyList()) }
    var marksMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) } // studentId -> marks
    var isLoading by remember { mutableStateOf(false) }
    var showClassDialog by remember { mutableStateOf(false) }
    var showExamDialog by remember { mutableStateOf(false) }
    var showSubjectDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val examTypes = listOf("FA1", "FA2", "SA1", "FA3", "FA4", "SA2")
    val subjects = listOf("Mathematics", "Science", "Social Studies", "English", "Hindi", "Telugu", "Computer Science")

    // Fetch students when class changes
    LaunchedEffect(selectedClass) {
        if (selectedClass.isNotBlank()) {
            isLoading = true
            try {
                FirestoreDatabase.getStudentsByClass(selectedClass) { fetchedStudents ->
                    students = fetchedStudents.sortedBy { it.rollNumber }
                    isLoading = false
                    // Reset marks when class changes
                    marksMap = emptyMap()
                }
            } catch (e: Exception) {
                isLoading = false
                scope.launch {
                    snackbarHostState.showSnackbar("Error fetching students: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Marks Entry",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Filters Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Select Exam Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Class Selector
                        OutlinedButton(
                            onClick = { showClassDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selectedClass.isBlank()) Color.Gray else Color(0xFF2E7D32)
                            )
                        ) {
                            Icon(
                                Icons.Default.Class,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                selectedClass.ifBlank { "Class" },
                                fontSize = 14.sp
                            )
                        }

                        // Exam Selector
                        OutlinedButton(
                            onClick = { showExamDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF2E7D32)
                            )
                        ) {
                            Icon(
                                Icons.Default.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(selectedExam, fontSize = 14.sp)
                        }
                    }
                    
                    // Subject Selector
                    OutlinedButton(
                        onClick = { showSubjectDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Subject: $selectedSubject", fontSize = 14.sp)
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF2E7D32),
                            strokeWidth = 3.dp
                        )
                        Text(
                            "Loading students...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (selectedClass.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Text(
                            "Please select a class to start",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            } else if (students.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Text(
                            "No students found in this class",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                // Marks Entry Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFF2E7D32).copy(alpha = 0.1f),
                                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Roll",
                                modifier = Modifier.width(50.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                "Student Name",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                "Marks",
                                modifier = Modifier.width(90.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(students) { student ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Roll Number
                                    Surface(
                                        modifier = Modifier.width(50.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF2E7D32).copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = student.rollNumber,
                                            modifier = Modifier.padding(8.dp),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    // Student Name
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${student.firstName} ${student.lastName}",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = student.admissionNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                    
                                    // Marks Input
                                    OutlinedTextField(
                                        value = marksMap[student.id] ?: "",
                                        onValueChange = { newValue ->
                                            if (newValue.isEmpty() || (newValue.toDoubleOrNull() != null && newValue.toDouble() <= 100)) {
                                                marksMap = marksMap.toMutableMap().apply {
                                                    put(student.id, newValue)
                                                }
                                            }
                                        },
                                        modifier = Modifier.width(90.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF2E7D32),
                                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                        ),
                                        placeholder = { Text("0-100", fontSize = 12.sp) }
                                    )
                                }
                                if (student != students.last()) {
                                    Divider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = Color.Gray.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Save Button
                Button(
                    onClick = {
                        scope.launch {
                            saveMarks(
                                students,
                                marksMap,
                                selectedClass,
                                selectedExam,
                                selectedSubject,
                                context
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Save Marks",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Dialogs
        if (showClassDialog) {
            AlertDialog(
                onDismissRequest = { showClassDialog = false },
                title = { Text("Select Class") },
                text = {
                    LazyColumn {
                        items(Constants.CLASS_OPTIONS) { className ->
                            Text(
                                text = className,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedClass = className
                                        showClassDialog = false
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (showExamDialog) {
            AlertDialog(
                onDismissRequest = { showExamDialog = false },
                title = { Text("Select Exam") },
                text = {
                    Column {
                        examTypes.forEach { exam ->
                            Text(
                                text = exam,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedExam = exam
                                        showExamDialog = false
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }
        
        if (showSubjectDialog) {
            AlertDialog(
                onDismissRequest = { showSubjectDialog = false },
                title = { Text("Select Subject") },
                text = {
                    LazyColumn {
                        items(subjects) { subject ->
                            Text(
                                text = subject,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSubject = subject
                                        showSubjectDialog = false
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

private suspend fun saveMarks(
    students: List<Person>,
    marksMap: Map<String, String>,
    className: String,
    examType: String,
    subject: String,
    context: android.content.Context
) = withContext(Dispatchers.IO) {
    var successCount = 0
    var failCount = 0
    
    try {
        students.forEach { student ->
            val marks = marksMap[student.id]?.toDoubleOrNull()
            if (marks != null) {
                try {
                    // Fetch existing grade record for this student and exam
                    val existingGrade = FirestoreDatabase.getStudentGrade(student.id, examType)
                    
                    // Update or create subject grade
                    val subjectGrade = SubjectGrade(
                        subjectName = subject,
                        obtainedMarks = marks,
                        maxMarks = 100.0,
                        grade = calculateGrade(marks)
                    )
                    
                    val updatedSubjects = existingGrade?.subjects?.toMutableMap() ?: mutableMapOf()
                    updatedSubjects[subject] = subjectGrade
                    
                    // Calculate totals
                    val totalMarks = updatedSubjects.values.sumOf { it.obtainedMarks }
                    val maxTotalMarks = updatedSubjects.values.sumOf { it.maxMarks }
                    val percentage = if (maxTotalMarks > 0) (totalMarks / maxTotalMarks) * 100 else 0.0
                    
                    val newGrade = StudentGrade(
                        id = existingGrade?.id ?: "",
                        studentId = student.id,
                        studentName = "${student.firstName} ${student.lastName}",
                        className = className,
                        examType = examType,
                        academicYear = FirestoreDatabase.getCurrentAcademicYear(),
                        subjects = updatedSubjects,
                        totalMarks = maxTotalMarks,
                        obtainedMarks = totalMarks,
                        percentage = percentage,
                        grade = calculateGrade(percentage),
                        examDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    )
                    
                    val result = FirestoreDatabase.saveStudentGrade(newGrade)
                    if (result.isSuccess) {
                        successCount++
                        android.util.Log.d("MarksScreen", "Successfully saved marks for ${student.firstName} ${student.lastName}")
                    } else {
                        failCount++
                        android.util.Log.e("MarksScreen", "Failed to save marks for ${student.firstName} ${student.lastName}")
                    }
                    
                } catch (e: Exception) {
                    failCount++
                    android.util.Log.e("MarksScreen", "Error saving marks for ${student.firstName} ${student.lastName}: ${e.message}", e)
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("MarksScreen", "Error in saveMarks: ${e.message}", e)
    }
    
    withContext(Dispatchers.Main) {
        Toast.makeText(context, "Saved: $successCount, Failed: $failCount", Toast.LENGTH_LONG).show()
    }
}

private fun calculateGrade(marks: Double): String {
    return when {
        marks >= 90 -> "A+"
        marks >= 80 -> "A"
        marks >= 70 -> "B+"
        marks >= 60 -> "B"
        marks >= 50 -> "C"
        marks >= 40 -> "D"
        else -> "F"
    }
}
