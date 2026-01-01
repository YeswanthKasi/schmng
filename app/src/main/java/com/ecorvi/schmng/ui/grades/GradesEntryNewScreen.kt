package com.ecorvi.schmng.ui.grades

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.ExamTypes
import com.ecorvi.schmng.ui.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.*

private val StudentGreen = Color(0xFF4CAF50)
private val HeaderBg = Color(0xFF388E3C)
private val RowAltBg = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesEntryNewScreen(navController: NavController, vm: GradesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val students by vm.students.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val baseline by vm.gradeBaseline.collectAsStateWithLifecycle()
    val dirty by vm.dirty.collectAsStateWithLifecycle()
    val existing by vm.existing.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val selectedClass by vm.selectedClass.collectAsStateWithLifecycle()
    val selectedExamType by vm.examType.collectAsStateWithLifecycle()
    val academicYear by vm.academicYear.collectAsStateWithLifecycle()
    val examDate by vm.examDate.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    
    var classDropdownExpanded by remember { mutableStateOf(false) }
    var examDropdownExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(true) }

    // Check if there are any changes (for Update vs Save button)
    val hasChanges = dirty.isNotEmpty()
    val hasExistingGrades = existing.isNotEmpty()
    
    // Calendar picker
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                vm.setExamDate(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    LaunchedEffect(Unit) {
        vm.setAcademicYear(FirestoreDatabase.getCurrentAcademicYear())
        vm.load()
    }
    
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Grade Entry", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Toggle filters visibility
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            if (showFilters) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle Filters",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { vm.load() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudentGreen)
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLandscape) {
                // Landscape layout: Filters on left, Table on right
                Row(Modifier.fillMaxSize()) {
                    // Left side: Filters
                    if (showFilters) {
                        Column(
                            Modifier
                                .width(300.dp)
                                .fillMaxHeight()
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Class Dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = classDropdownExpanded,
                                        onExpandedChange = { classDropdownExpanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedClass,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Class", fontSize = 11.sp) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded)
                                            },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = StudentGreen,
                                                focusedLabelColor = StudentGreen
                                            ),
                                            singleLine = true,
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = classDropdownExpanded,
                                            onDismissRequest = { classDropdownExpanded = false }
                                        ) {
                                            Constants.CLASS_OPTIONS.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        vm.setClass(option)
                                                        classDropdownExpanded = false
                                                    },
                                                    leadingIcon = {
                                                        if (selectedClass == option) {
                                                            Icon(Icons.Filled.Check, null, tint = StudentGreen)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Exam Type Dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = examDropdownExpanded,
                                        onExpandedChange = { examDropdownExpanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedExamType,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Exam Type", fontSize = 11.sp) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = examDropdownExpanded)
                                            },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = StudentGreen,
                                                focusedLabelColor = StudentGreen
                                            ),
                                            singleLine = true,
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = examDropdownExpanded,
                                            onDismissRequest = { examDropdownExpanded = false }
                                        ) {
                                            ExamTypes.ALL.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        vm.setExamType(option)
                                                        examDropdownExpanded = false
                                                    },
                                                    leadingIcon = {
                                                        if (selectedExamType == option) {
                                                            Icon(Icons.Filled.Check, null, tint = StudentGreen)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = academicYear,
                                        onValueChange = { vm.setAcademicYear(it) },
                                        label = { Text("Academic Year", fontSize = 11.sp) },
                                        placeholder = { Text("2025-26", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = StudentGreen,
                                            focusedLabelColor = StudentGreen
                                        ),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                    )

                                    OutlinedTextField(
                                        value = examDate,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Exam Date", fontSize = 11.sp) },
                                        trailingIcon = {
                                            IconButton(onClick = { datePickerDialog.show() }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Filled.CalendarMonth, "Select Date", tint = StudentGreen, modifier = Modifier.size(18.dp))
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { datePickerDialog.show() },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = StudentGreen,
                                            focusedLabelColor = StudentGreen
                                        ),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                    )

                                    Button(
                                        onClick = { vm.applyFilters() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = StudentGreen),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Search, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Load", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Right side: Table
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        if (isLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = StudentGreen)
                                    Spacer(Modifier.height(12.dp))
                                    Text("Loading...", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else if (students.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.PersonOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                                    Spacer(Modifier.height(8.dp))
                                    Text("No students", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else {
                            GradeEntryTable(
                                students = students,
                                subjects = subjects.toList(),
                                existing = existing,
                                vm = vm,
                                isLandscape = isLandscape,
                                onDeleteGrade = { studentId ->
                                    scope.launch {
                                        val res = vm.deleteGrade(studentId)
                                        if (res.isSuccess) {
                                            snackbarHostState.showSnackbar("Grade deleted")
                                        } else {
                                            snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Delete failed")
                                        }
                                    }
                                },
                                onSaveStudent = { studentId ->
                                    val user = FirebaseAuth.getInstance().currentUser
                                    if (user == null) {
                                        scope.launch { snackbarHostState.showSnackbar("Please sign in") }
                                    } else {
                                        scope.launch {
                                            val res = vm.saveStudent(studentId, user.uid)
                                            if (res.isSuccess) {
                                                snackbarHostState.showSnackbar("Saved")
                                            } else {
                                                snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Save failed")
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // Bottom action bar for landscape
                        if (!isLoading && students.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shadowElevation = 8.dp,
                                color = Color.White
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${students.size} students • ${subjects.size} subjects",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            val user = FirebaseAuth.getInstance().currentUser
                                            if (user == null) {
                                                scope.launch { snackbarHostState.showSnackbar("Please sign in") }
                                            } else {
                                                isSaving = true
                                                scope.launch {
                                                    val res = vm.save(user.uid)
                                                    isSaving = false
                                                    if (res.isSuccess) {
                                                        snackbarHostState.showSnackbar("Saved!")
                                                    } else {
                                                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Save failed")
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (hasExistingGrades) Color(0xFF1976D2) else StudentGreen
                                        ),
                                        enabled = !isSaving,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        if (isSaving) {
                                            CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 1.5.dp)
                                        } else {
                                            Icon(if (hasExistingGrades) Icons.Filled.Update else Icons.Filled.Save, null, Modifier.size(16.dp))
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (hasExistingGrades) "Update" else "Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Portrait layout (original vertical layout)
                Column(Modifier.fillMaxSize()) {
                    if (showFilters) {
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ExposedDropdownMenuBox(
                                        expanded = classDropdownExpanded,
                                        onExpandedChange = { classDropdownExpanded = it },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = selectedClass,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Class", fontSize = 12.sp) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded)
                                            },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = StudentGreen,
                                                focusedLabelColor = StudentGreen
                                            ),
                                            singleLine = true,
                                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = classDropdownExpanded,
                                            onDismissRequest = { classDropdownExpanded = false }
                                        ) {
                                            Constants.CLASS_OPTIONS.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        vm.setClass(option)
                                                        classDropdownExpanded = false
                                                    },
                                                    leadingIcon = {
                                                        if (selectedClass == option) {
                                                            Icon(Icons.Filled.Check, null, tint = StudentGreen)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    ExposedDropdownMenuBox(
                                        expanded = examDropdownExpanded,
                                        onExpandedChange = { examDropdownExpanded = it },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = selectedExamType,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Exam Type", fontSize = 12.sp) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = examDropdownExpanded)
                                            },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = StudentGreen,
                                                focusedLabelColor = StudentGreen
                                            ),
                                            singleLine = true,
                                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = examDropdownExpanded,
                                            onDismissRequest = { examDropdownExpanded = false }
                                        ) {
                                            ExamTypes.ALL.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        vm.setExamType(option)
                                                        examDropdownExpanded = false
                                                    },
                                                    leadingIcon = {
                                                        if (selectedExamType == option) {
                                                            Icon(Icons.Filled.Check, null, tint = StudentGreen)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = academicYear,
                                        onValueChange = { vm.setAcademicYear(it) },
                                        label = { Text("Academic Year", fontSize = 12.sp) },
                                        placeholder = { Text("2025-26", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = StudentGreen,
                                            focusedLabelColor = StudentGreen
                                        ),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                                    )

                                    OutlinedTextField(
                                        value = examDate,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Exam Date", fontSize = 12.sp) },
                                        trailingIcon = {
                                            IconButton(onClick = { datePickerDialog.show() }) {
                                                Icon(Icons.Filled.CalendarMonth, "Select Date", tint = StudentGreen)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { datePickerDialog.show() },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = StudentGreen,
                                            focusedLabelColor = StudentGreen
                                        ),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                                    )
                                }

                                Button(
                                    onClick = { vm.applyFilters() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = StudentGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Search, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Load Students", fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    // Content Area for portrait
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = StudentGreen)
                                Spacer(Modifier.height(12.dp))
                                Text("Loading students...", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    } else if (students.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.PersonOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No students found",
                                    color = Color.Gray,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "for $selectedClass",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { vm.load() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudentGreen)
                                ) {
                                    Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    } else {
                        // Grade Entry Table with fixed columns
                        Column(Modifier.weight(1f)) {
                            GradeEntryTable(
                                students = students,
                                subjects = subjects.toList(),
                                existing = existing,
                        vm = vm,
                        isLandscape = isLandscape,
                        onDeleteGrade = { studentId ->
                            scope.launch {
                                val res = vm.deleteGrade(studentId)
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Grade deleted successfully")
                                } else {
                                    snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Delete failed")
                                }
                            }
                        },
                        onSaveStudent = { studentId ->
                            val user = FirebaseAuth.getInstance().currentUser
                            if (user == null) {
                                scope.launch { snackbarHostState.showSnackbar("Please sign in") }
                            } else {
                                scope.launch {
                                    val res = vm.saveStudent(studentId, user.uid)
                                    if (res.isSuccess) {
                                        snackbarHostState.showSnackbar("Student grade saved")
                                    } else {
                                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Save failed")
                                    }
                                }
                            }
                        }
                    )
                }

                // Bottom Action Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Info text
                        Text(
                            "${students.size} students • ${subjects.size} subjects",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )

                        // Save/Update All Button
                        Button(
                            onClick = {
                                val user = FirebaseAuth.getInstance().currentUser
                                if (user == null) {
                                    scope.launch { snackbarHostState.showSnackbar("Please sign in to save grades") }
                                } else {
                                    isSaving = true
                                    scope.launch {
                                        val res = vm.save(user.uid)
                                        isSaving = false
                                        if (res.isSuccess) {
                                            snackbarHostState.showSnackbar("All grades saved successfully!")
                                        } else {
                                            snackbarHostState.showSnackbar("Save failed: ${res.exceptionOrNull()?.message}")
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasExistingGrades) Color(0xFF1976D2) else StudentGreen
                            ),
                            enabled = !isSaving,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    if (hasExistingGrades) Icons.Filled.Update else Icons.Filled.Save,
                                    null,
                                    Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (hasExistingGrades) "Update All" else "Save All",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeEntryTable(
    students: List<com.ecorvi.schmng.ui.data.model.Person>,
    subjects: List<String>,
    existing: Map<String, com.ecorvi.schmng.ui.data.model.StudentGrade>,
    vm: GradesViewModel,
    isLandscape: Boolean,
    onDeleteGrade: (String) -> Unit,
    onSaveStudent: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // Column widths - responsive
    val rollWidth = if (isLandscape) 60.dp else 50.dp
    val nameWidth = if (isLandscape) 140.dp else 100.dp
    val subjectWidth = if (isLandscape) 80.dp else 70.dp
    val actionWidth = if (isLandscape) 100.dp else 90.dp
    
    val fixedColumnsWidth = rollWidth + nameWidth + 8.dp
    val scrollableWidth = (subjectWidth * subjects.size) + actionWidth + (8.dp * (subjects.size + 1))
    
    val subjectsScrollState = rememberScrollState()

    Card(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header Row
            Row(Modifier.fillMaxWidth()) {
                // Fixed header columns (Roll, Name)
                Row(
                    Modifier
                        .width(fixedColumnsWidth)
                        .background(HeaderBg)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier.width(rollWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Roll",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        Modifier.width(nameWidth),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            "Student Name",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Scrollable header (Subjects + Actions)
                Row(
                    Modifier
                        .weight(1f)
                        .background(HeaderBg)
                        .horizontalScroll(subjectsScrollState)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.forEach { subject ->
                        Box(
                            Modifier.width(subjectWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                subject.take(6),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Box(
                        Modifier.width(actionWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Actions",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 2.dp, color = StudentGreen)

            // Data rows
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(students) { index, student ->
                    val hasExistingGrade = existing.containsKey(student.id)
                    val rowBg = if (index % 2 == 0) Color.White else RowAltBg

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                    ) {
                        // Fixed columns (Roll, Name)
                        Row(
                            Modifier
                                .width(fixedColumnsWidth)
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.width(rollWidth),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    student.rollNumber.ifBlank { "${index + 1}" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.DarkGray
                                )
                            }
                            Box(
                                Modifier.width(nameWidth),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Column {
                                    Text(
                                        "${student.firstName} ${student.lastName}".trim().ifBlank { "Student ${index + 1}" },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (hasExistingGrade) {
                                        Text(
                                            "✓ Has grades",
                                            fontSize = 9.sp,
                                            color = StudentGreen
                                        )
                                    }
                                }
                            }
                        }

                        // Scrollable columns (Subjects + Actions)
                        Row(
                            Modifier
                                .weight(1f)
                                .horizontalScroll(subjectsScrollState)
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            subjects.forEach { subject ->
                                val value = vm.effectiveMarks(student.id, subject)
                                val isInvalid = value.isNotBlank() && 
                                    (value.toDoubleOrNull()?.let { it < 0.0 || it > 100.0 } ?: true)

                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { input ->
                                        var cleaned = input.filter { it.isDigit() || it == '.' }
                                        val dotIndex = cleaned.indexOf('.')
                                        if (dotIndex != -1) {
                                            cleaned = cleaned.substring(0, dotIndex + 1) +
                                                cleaned.substring(dotIndex + 1).replace(".", "").take(1)
                                        }
                                        if (cleaned.length <= 5) {
                                            vm.updateMark(student.id, subject, cleaned)
                                        }
                                    },
                                    modifier = Modifier
                                        .width(subjectWidth)
                                        .height(48.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = isInvalid,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = StudentGreen,
                                        unfocusedBorderColor = Color.LightGray,
                                        errorBorderColor = Color.Red
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }

                            // Action buttons
                            Row(
                                Modifier.width(actionWidth),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Save individual student
                                IconButton(
                                    onClick = { onSaveStudent(student.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        if (hasExistingGrade) Icons.Filled.Update else Icons.Filled.Save,
                                        contentDescription = if (hasExistingGrade) "Update" else "Save",
                                        tint = if (hasExistingGrade) Color(0xFF1976D2) else StudentGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Delete grade
                                IconButton(
                                    onClick = { onDeleteGrade(student.id) },
                                    modifier = Modifier.size(36.dp),
                                    enabled = hasExistingGrade
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = if (hasExistingGrade) Color.Red.copy(alpha = 0.7f) else Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (index < students.size - 1) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
    }
}
