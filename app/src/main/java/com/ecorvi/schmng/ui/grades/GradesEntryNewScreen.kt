package com.ecorvi.schmng.ui.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.ExamTypes
import com.ecorvi.schmng.ui.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private val StudentGreen = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesEntryNewScreen(navController: NavController, vm: GradesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val students by vm.students.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val baseline by vm.gradeBaseline.collectAsStateWithLifecycle()
    val dirty by vm.dirty.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val selectedClass by vm.selectedClass.collectAsStateWithLifecycle()
    val selectedExamType by vm.examType.collectAsStateWithLifecycle()
    val academicYear by vm.academicYear.collectAsStateWithLifecycle()
    val examDate by vm.examDate.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var classMenuExpanded by remember { mutableStateOf(false) }
    var examMenuExpanded by remember { mutableStateOf(false) }
    var newSubject by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.setAcademicYear(FirestoreDatabase.getCurrentAcademicYear())
        vm.load()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Grade Entry", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudentGreen)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = StudentGreen) }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                // Filters
                Card(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExposedDropdownMenuBox(expanded = classMenuExpanded, onExpandedChange = { classMenuExpanded = it }) {
                            OutlinedTextField(
                                value = selectedClass,
                                onValueChange = {},
                                label = { Text("Class") },
                                readOnly = true
                            )
                            ExposedDropdownMenu(expanded = classMenuExpanded, onDismissRequest = { classMenuExpanded = false }) {
                                Constants.CLASS_OPTIONS.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                        classMenuExpanded = false
                                        vm.setClass(option)
                                    })
                                }
                            }
                        }

                        ExposedDropdownMenuBox(expanded = examMenuExpanded, onExpandedChange = { examMenuExpanded = it }) {
                            OutlinedTextField(
                                value = selectedExamType,
                                onValueChange = {},
                                label = { Text("Exam Type") },
                                readOnly = true
                            )
                            ExposedDropdownMenu(expanded = examMenuExpanded, onDismissRequest = { examMenuExpanded = false }) {
                                ExamTypes.ALL.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = {
                                        examMenuExpanded = false
                                        vm.setExamType(option)
                                    })
                                }
                            }
                        }

                        OutlinedTextField(value = academicYear, onValueChange = { vm.setAcademicYear(it) }, label = { Text("Academic Year") })
                        OutlinedTextField(value = examDate, onValueChange = { vm.setExamDate(it) }, label = { Text("Exam Date") })

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = newSubject,
                                onValueChange = { newSubject = it },
                                label = { Text("Add Subject") },
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = {
                                vm.addSubject(newSubject)
                                newSubject = ""
                            }) { Text("Add") }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { vm.applyFilters() }) { Text("Apply / Refresh") }
                        }
                    }
                }

                // Table
                Card(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp)) {
                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
                    val screenWidthDp = configuration.screenWidthDp.dp
                    val rollNoWidth = if (isLandscape) 80.dp else 75.dp
                    val nameWidth = if (isLandscape) 150.dp else 130.dp
                    val subjectWidth = if (isLandscape) 110.dp else 100.dp
                    val spacingWidth = 10.dp * (subjects.size + 2)
                    val subjectsWidth = subjectWidth * subjects.size
                    val totalTableWidth = rollNoWidth + nameWidth + subjectsWidth + spacingWidth
                    val horizontalScrollState = rememberScrollState()

                    Column(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(StudentGreen).horizontalScroll(horizontalScrollState).padding(vertical = 14.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(Modifier.width(rollNoWidth), contentAlignment = Alignment.Center) { Text("Roll No", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center) }
                            Box(Modifier.width(nameWidth), contentAlignment = Alignment.CenterStart) { Text("Student Name", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                            subjects.forEach { subject -> Box(Modifier.width(subjectWidth), contentAlignment = Alignment.Center) { Text(subject, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center) } }
                        }
                        HorizontalDivider(color = StudentGreen.copy(alpha = 0.4f), thickness = 2.dp)
                        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                            items(students.size) { index ->
                                val st = students[index]
                                Row(
                                    modifier = Modifier.width(totalTableWidth.coerceAtLeast(screenWidthDp)).horizontalScroll(horizontalScrollState).padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.width(rollNoWidth), contentAlignment = Alignment.Center) { Text(st.rollNumber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = Color.Black) }
                                    Box(Modifier.width(nameWidth), contentAlignment = Alignment.CenterStart) { Text("${st.firstName} ${st.lastName}", fontSize = 14.sp, color = Color.Black) }
                                    subjects.forEach { subject ->
                                        val value = vm.effectiveMarks(st.id, subject)
                                        val isInvalid = value.isNotBlank() && (value.toDoubleOrNull()?.let { it < 0.0 || it > 100.0 } ?: true)
                                        OutlinedTextField(
                                            value = value,
                                            onValueChange = { s ->
                                                var cleaned = s.filter { ch -> ch.isDigit() || ch == '.' }
                                                val firstDot = cleaned.indexOf('.')
                                                if (firstDot != -1) {
                                                    val before = cleaned.substring(0, firstDot + 1)
                                                    val after = cleaned.substring(firstDot + 1).replace(".", "")
                                                    cleaned = before + after.take(2)
                                                }
                                                vm.updateMark(st.id, subject, cleaned)
                                            },
                                            modifier = Modifier.width(subjectWidth), singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(8.dp),
                                            isError = isInvalid,
                                            supportingText = { if (isInvalid) Text("Enter 0 to 100", color = Color.Red, fontSize = 10.sp) }
                                        )
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            val res = vm.deleteGrade(st.id)
                                            if (res.isSuccess) snackbarHostState.showSnackbar("Grade cleared for ${st.firstName}")
                                            else snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Nothing to delete")
                                        }
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete Grade")
                                    }
                                }
                                if (index < students.size - 1) HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 12.dp))
                            }
                        }
                    }
                }

                Button(onClick = {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user == null) {
                        scope.launch { snackbarHostState.showSnackbar("Sign in to save grades") }
                    } else {
                        scope.launch {
                            val res = vm.save(user.uid)
                            if (res.isSuccess) snackbarHostState.showSnackbar("Grades saved") else snackbarHostState.showSnackbar("Save failed: ${res.exceptionOrNull()?.message}")
                        }
                    }
                }, modifier = Modifier.fillMaxWidth().padding(16.dp), colors = ButtonDefaults.buttonColors(containerColor = StudentGreen)) {
                    Text("Save Grades", color = Color.White)
                }
            }
        }
    }
}
