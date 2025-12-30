package com.ecorvi.schmng.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalConfiguration
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.ExamTypes
import com.ecorvi.schmng.ui.data.model.GradeCalculator
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.data.model.StandardSubjects
import com.ecorvi.schmng.ui.data.model.StudentGrade
import com.ecorvi.schmng.ui.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import kotlinx.coroutines.delay

private val StudentGreen = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeEntryScreen(navController: NavController) {
    // Filters
    var selectedClass by remember { mutableStateOf(Constants.CLASS_OPTIONS.first()) }
    var selectedExamType by remember { mutableStateOf(ExamTypes.FA1) }
    var inputAcademicYear by remember { mutableStateOf(FirestoreDatabase.getCurrentAcademicYear()) }
    var academicYear by remember { mutableStateOf(inputAcademicYear) }
    var examDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }

    LaunchedEffect(inputAcademicYear) {
        delay(800)
        academicYear = inputAcademicYear
    }

    // Data
    var students by remember { mutableStateOf<List<Person>>(emptyList()) }
    var existingGrades by remember { mutableStateOf<Map<String, StudentGrade>>(emptyMap()) }
    var gradeData by remember { mutableStateOf<Map<String, MutableMap<String, String>>>(emptyMap()) } // cloud baseline
    var dirtyGradeData by remember { mutableStateOf<Map<String, MutableMap<String, String>>>(emptyMap()) } // unsaved edits

    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showClassDialog by remember { mutableStateOf(false) }
    var showExamTypeDialog by remember { mutableStateOf(false) }
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showUnsavedFilterDialog by remember { mutableStateOf(false) }
    var pendingFilterAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteTargetStudentId by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Subjects for class
    val defaultSubjects = remember(selectedClass) {
        when {
            selectedClass.contains("1") || selectedClass.contains("2") || selectedClass.contains("3") || selectedClass.contains("4") || selectedClass.contains("5") -> StandardSubjects.PRIMARY
            selectedClass.contains("6") || selectedClass.contains("7") || selectedClass.contains("8") || selectedClass.contains("9") -> StandardSubjects.SECONDARY
            else -> StandardSubjects.HIGHER_SECONDARY
        }
    }
    var selectedSubjects by remember { mutableStateOf(defaultSubjects.toMutableSet()) }
    LaunchedEffect(defaultSubjects) {
        if (!selectedSubjects.containsAll(defaultSubjects) || !defaultSubjects.containsAll(selectedSubjects)) {
            selectedSubjects = defaultSubjects.toMutableSet()
        }
    }

    // Helpers
    fun normalizeClassName(raw: String): String {
        val t = raw.trim()
        if (t.startsWith("Class ")) return t
        val num = t.replace(Regex("[^0-9]"), "")
        return if (num.isNotBlank()) "Class $num" else "Class $t"
    }
    fun effectiveMarks(studentId: String, subject: String): String {
        val dirty = dirtyGradeData[studentId]?.get(subject)
        return dirty ?: gradeData[studentId]?.get(subject) ?: ""
    }
    fun updateDirty(studentId: String, subject: String, value: String) {
        val m = dirtyGradeData.toMutableMap()
        val row = m[studentId]?.toMutableMap() ?: mutableMapOf()
        row[subject] = value
        m[studentId] = row
        dirtyGradeData = m
    }
    fun hasUnsavedChanges(): Boolean = dirtyGradeData.any { (sid, subjMap) -> subjMap.any { (subj, v) -> v.trim() != existingGrades[sid]?.subjects?.get(subj)?.obtainedMarks?.toString().orEmpty() } }
    fun validateEffective(): Pair<Boolean, List<String>> {
        val warnings = mutableListOf<String>()
        var valid = true
        // Academic year format: YYYY-YY (e.g., 2025-26)
        val ayOk = Regex("^\\d{4}-\\d{2}$").matches(academicYear)
        if (!ayOk) { valid = false; warnings.add("Academic year must be like 2025-26") }
        // Exam date format: YYYY-MM-DD
        val dateOk = Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(examDate)
        if (!dateOk) { valid = false; warnings.add("Exam date must be YYYY-MM-DD") }
        students.forEach { st ->
            selectedSubjects.forEach { subj ->
                val raw = effectiveMarks(st.id, subj).trim()
                if (raw.isNotEmpty()) {
                    val num = raw.toDoubleOrNull()
                    if (num == null || num < 0 || num > 100) { valid = false; warnings.add("${st.firstName} ${st.lastName} - $subj invalid (0-100)") }
                }
            }
        }
        val any = students.any { st -> selectedSubjects.any { subj -> effectiveMarks(st.id, subj).isNotBlank() } }
        if (!any) { valid = false; warnings.add("No marks entered") }
        return valid to warnings
    }
    fun buildGradesForSave(): List<StudentGrade> {
        val out = mutableListOf<StudentGrade>()
        students.forEach { st ->
            val subjectGrades = mutableMapOf<String, com.ecorvi.schmng.ui.data.model.SubjectGrade>()
            var total = 0.0; var obtained = 0.0
            selectedSubjects.forEach { subj ->
                val raw = effectiveMarks(st.id, subj).trim()
                val v = raw.toDoubleOrNull()
                if (v != null && v in 0.0..100.0) {
                    total += 100.0; obtained += v
                    subjectGrades[subj] = com.ecorvi.schmng.ui.data.model.SubjectGrade(
                        subjectName = subj,
                        maxMarks = 100.0,
                        obtainedMarks = v,
                        grade = GradeCalculator.calculateGrade(v),
                        remarks = ""
                    )
                }
            }
            if (subjectGrades.isNotEmpty()) {
                val pct = if (total > 0) (obtained / total) * 100 else 0.0
                val overallGrade = GradeCalculator.calculateGrade(pct)
                val existing = existingGrades[st.id]
                out.add(
                    StudentGrade(
                        id = existing?.id ?: "",
                        studentId = st.id,
                        studentName = "${st.firstName} ${st.lastName}",
                        className = normalizeClassName(selectedClass),
                        academicYear = academicYear,
                        examType = selectedExamType,
                        examDate = examDate,
                        subjects = subjectGrades,
                        totalMarks = total,
                        obtainedMarks = obtained,
                        percentage = pct,
                        grade = overallGrade,
                        createdBy = FirebaseAuth.getInstance().currentUser?.uid ?: "admin",
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                )
            }
        }
        return out
    }

    // Fetch students + grades (and union subjects)
    LaunchedEffect(selectedClass, selectedExamType, academicYear, refreshTick) {
        isLoading = true
        dirtyGradeData = emptyMap()
        try {
            val cls = normalizeClassName(selectedClass)
            val rawStudents = FirestoreDatabase.getStudentsByClass(cls)
            val enriched = mutableListOf<Person>()
            for (s in rawStudents) { FirestoreDatabase.getStudent(s.id)?.let { enriched.add(it) } }
            students = enriched.sortedBy { it.rollNumber.toIntOrNull() ?: 999 }

            val grades = FirestoreDatabase.getClassGrades(cls, selectedExamType, academicYear)
            existingGrades = grades.associateBy { it.studentId }

            // Union subjects with cloud
            val cloudSubjects = existingGrades.values.flatMap { it.subjects.keys }.toSet()
            val union = (selectedSubjects + cloudSubjects).toMutableSet()
            selectedSubjects = union

            // Build cloud baseline
            val base = mutableMapOf<String, MutableMap<String, String>>()
            students.forEach { st ->
                val row = mutableMapOf<String, String>()
                selectedSubjects.forEach { subj ->
                    val gradeById = existingGrades[st.id]
                    val gradeByName = existingGrades.values.find { it.studentName == "${st.firstName} ${st.lastName}" && it.className == normalizeClassName(selectedClass) && it.examType == selectedExamType && it.academicYear == academicYear }
                    val src = gradeById ?: gradeByName
                    val m = src?.subjects?.get(subj)?.obtainedMarks
                    row[subj] = if (m != null) { if (m % 1.0 == 0.0) m.toInt().toString() else m.toString() } else ""
                }
                base[st.id] = row
            }
            gradeData = base
        } catch (e: Exception) {
            scope.launch { snackbarHostState.showSnackbar("Load error: ${e.message}") }
        }
        isLoading = false
    }

    // Real-time listener
    var gradesListener by remember { mutableStateOf<com.google.firebase.firestore.ListenerRegistration?>(null) }
    LaunchedEffect(selectedClass, selectedExamType, academicYear) {
        gradesListener?.remove()
        gradesListener = FirestoreDatabase.listenClassGrades(
            normalizeClassName(selectedClass), selectedExamType, academicYear,
            onUpdate = { grades ->
                existingGrades = grades.associateBy { it.studentId }
                // Update baseline without overwriting dirty edits
                val updated = gradeData.toMutableMap()
                students.forEach { st ->
                    val row = updated[st.id]?.toMutableMap() ?: mutableMapOf()
                    // expand subjects
                    val egById = existingGrades[st.id]
                    val egByName = existingGrades.values.find { it.studentName == "${st.firstName} ${st.lastName}" && it.className == normalizeClassName(selectedClass) && it.examType == selectedExamType && it.academicYear == academicYear }
                    val cloudSubjects = (egById?.subjects?.keys ?: egByName?.subjects?.keys) ?: emptySet()
                    val toAdd = cloudSubjects.filterNot { selectedSubjects.contains(it) }
                    if (toAdd.isNotEmpty()) {
                        selectedSubjects = (selectedSubjects + toAdd).toMutableSet()
                    }
                    selectedSubjects.forEach { subj ->
                        if (dirtyGradeData[st.id]?.containsKey(subj) != true) {
                            val src = egById ?: egByName
                            val m = src?.subjects?.get(subj)?.obtainedMarks
                            val s = if (m != null) { if (m % 1.0 == 0.0) m.toInt().toString() else m.toString() } else ""
                            row[subj] = s
                        }
                    }
                    updated[st.id] = row
                }
                gradeData = updated
            },
            onError = { err -> scope.launch { snackbarHostState.showSnackbar("Realtime error: ${err.message}") } }
        )
    }
    DisposableEffect(Unit) { onDispose { gradesListener?.remove() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Grade Entry", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges()) showUnsavedDialog = true else navController.navigateUp()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudentGreen)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = StudentGreen) }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                // Filters card
                Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Class:", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.weight(1f).clickable { 
                                if (hasUnsavedChanges()) { pendingFilterAction = { showClassDialog = true }; showUnsavedFilterDialog = true } 
                                else showClassDialog = true 
                            }) {
                                OutlinedTextField(value = selectedClass, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { }, enabled = false)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Exam Type:", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.weight(1f).clickable { 
                                if (hasUnsavedChanges()) { pendingFilterAction = { showExamTypeDialog = true }; showUnsavedFilterDialog = true } 
                                else showExamTypeDialog = true 
                            }) {
                                OutlinedTextField(value = selectedExamType, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { }, enabled = false)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = inputAcademicYear, onValueChange = { inputAcademicYear = it }, label = { Text("Academic Year") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = examDate, onValueChange = { examDate = it }, label = { Text("Exam Date") }, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Table
                Card(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(12.dp)) {
                    if (students.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.PersonOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                                Text("No students found for this class", color = Color.Gray)
                            }
                        }
                    } else {
                        val configuration = LocalConfiguration.current
                        val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
                        val screenWidthDp = configuration.screenWidthDp.dp
                        val rollNoWidth = if (isLandscape) 80.dp else 75.dp
                        val nameWidth = if (isLandscape) 150.dp else 130.dp
                        val subjectWidth = if (isLandscape) 110.dp else 100.dp
                        val spacingWidth = 10.dp * (selectedSubjects.size + 2)
                        val subjectsWidth = subjectWidth * selectedSubjects.size
                        val totalTableWidth = rollNoWidth + nameWidth + subjectsWidth + spacingWidth
                        val horizontalScrollState = rememberScrollState()

                        Column(Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().background(StudentGreen).horizontalScroll(horizontalScrollState).padding(vertical = 14.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(Modifier.width(rollNoWidth), contentAlignment = Alignment.Center) { Text("Roll No", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center) }
                                Box(Modifier.width(nameWidth), contentAlignment = Alignment.CenterStart) { Text("Student Name", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                                selectedSubjects.forEach { subject -> Box(Modifier.width(subjectWidth), contentAlignment = Alignment.Center) { Text(subject, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center) } }
                            }
                            HorizontalDivider(color = StudentGreen.copy(alpha = 0.4f), thickness = 2.dp)
                            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                                items(students.size) { index ->
                                    val student = students[index]
                                    Row(
                                        modifier = Modifier.width(totalTableWidth.coerceAtLeast(screenWidthDp)).background(if (index % 2 == 0) Color.White else Color(0xFFF8F9FA)).horizontalScroll(horizontalScrollState).padding(vertical = 12.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier.width(rollNoWidth), contentAlignment = Alignment.Center) { Text(student.rollNumber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = Color.Black) }
                                        Box(Modifier.width(nameWidth), contentAlignment = Alignment.CenterStart) { Text("${student.firstName} ${student.lastName}", fontSize = 14.sp, color = Color.Black) }
                                        selectedSubjects.forEach { subject ->
                                            val value = effectiveMarks(student.id, subject)
                                            val isInvalid = value.isNotBlank() && (value.toDoubleOrNull()?.let { it < 0.0 || it > 100.0 } ?: true)
                                            OutlinedTextField(
                                                value = value,
                                                onValueChange = { s ->
                                                    // keep only digits and a single dot; cap to two decimals
                                                    var cleaned = s.filter { ch -> ch.isDigit() || ch == '.' }
                                                    val firstDot = cleaned.indexOf('.')
                                                    if (firstDot != -1) {
                                                        val before = cleaned.substring(0, firstDot + 1)
                                                        val after = cleaned.substring(firstDot + 1).replace(".", "")
                                                        cleaned = before + after
                                                        if (after.length > 2) cleaned = before + after.take(2)
                                                    }
                                                    updateDirty(student.id, subject, cleaned)
                                                },
                                                modifier = Modifier.width(subjectWidth), singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                shape = RoundedCornerShape(8.dp),
                                                isError = isInvalid,
                                                supportingText = {
                                                    if (isInvalid) Text("Enter 0 to 100", color = Color.Red, fontSize = 10.sp)
                                                }
                                            )
                                        }
                                        if (existingGrades[student.id] != null) {
                                            IconButton(onClick = { deleteTargetStudentId = student.id; showDeleteDialog = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red) }
                                        }
                                    }
                                    if (index < students.size - 1) HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 12.dp))
                                }
                            }
                        }
                    }
                }

                Button(onClick = { showSaveConfirmation = true }, modifier = Modifier.fillMaxWidth().padding(16.dp), colors = ButtonDefaults.buttonColors(containerColor = StudentGreen), enabled = !isSaving && students.isNotEmpty()) {
                    if (isSaving) { CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                    Text("Save Grades", color = Color.White)
                }
            }
        }

        // Class dialog
        if (showClassDialog) {
            AlertDialog(onDismissRequest = { showClassDialog = false }, title = { Text("Select Class") }, text = {
                LazyColumn { items(Constants.CLASS_OPTIONS.size) { i ->
                    val cls = Constants.CLASS_OPTIONS[i]
                    Row(Modifier.fillMaxWidth().clickable { selectedClass = cls; showClassDialog = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedClass == cls, onClick = { selectedClass = cls; showClassDialog = false }, colors = RadioButtonDefaults.colors(selectedColor = StudentGreen))
                        Spacer(Modifier.width(8.dp)); Text(cls)
                    }
                } }
            }, confirmButton = { TextButton(onClick = { showClassDialog = false }) { Text("Close") } })
        }
        // Exam dialog
        if (showExamTypeDialog) {
            AlertDialog(onDismissRequest = { showExamTypeDialog = false }, title = { Text("Select Exam Type") }, text = {
                LazyColumn { items(ExamTypes.ALL.size) { i ->
                    val exam = ExamTypes.ALL[i]
                    Row(Modifier.fillMaxWidth().clickable { selectedExamType = exam; showExamTypeDialog = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedExamType == exam, onClick = { selectedExamType = exam; showExamTypeDialog = false }, colors = RadioButtonDefaults.colors(selectedColor = StudentGreen))
                        Spacer(Modifier.width(8.dp)); Text(exam)
                    }
                } }
            }, confirmButton = { TextButton(onClick = { showExamTypeDialog = false }) { Text("Close") } })
        }
        // Save confirmation
        if (showSaveConfirmation) {
            val (isValid, warns) = validateEffective()
            val overwrite = mutableListOf<String>()
            students.forEach { st -> selectedSubjects.forEach { subj ->
                val entered = effectiveMarks(st.id, subj).trim()
                val prev = existingGrades[st.id]?.subjects?.get(subj)?.obtainedMarks
                if (entered.isNotBlank() && prev != null && prev.toString() != entered) overwrite.add("${st.firstName} ${st.lastName} - $subj (${prev} → $entered)")
            } }
            AlertDialog(
                onDismissRequest = { showSaveConfirmation = false },
                title = { Text("Confirm Save Grades") },
                text = {
                    Column {
                        Text("Class: ${normalizeClassName(selectedClass)}\nExam: $selectedExamType\nStudents: ${students.size}\nAcademic Year: $academicYear")
                        if (overwrite.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text("Overwriting:", color = Color(0xFF6A1B9A), fontSize = 12.sp); overwrite.take(6).forEach { Text("• $it", color = Color(0xFF6A1B9A), fontSize = 12.sp) } }
                        if (!isValid) { Spacer(Modifier.height(8.dp)); Text("Fix before saving:", color = Color.Red, fontSize = 12.sp); warns.take(6).forEach { Text("• $it", color = Color.Red, fontSize = 12.sp) } }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showSaveConfirmation = false
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user == null) { scope.launch { snackbarHostState.showSnackbar("You must be signed in to save grades") }; return@Button }
                        if (!isValid) { scope.launch { snackbarHostState.showSnackbar("Fix validation issues before saving") }; return@Button }
                        isSaving = true
                        scope.launch {
                            try {
                                var canSave = true
                                FirestoreDatabase.getUserRole(user.uid, onComplete = { role -> canSave = role == "admin" || role == "teacher" }, onFailure = { })
                                if (!canSave) { isSaving = false; snackbarHostState.showSnackbar("You don't have permission to save grades"); return@launch }
                                val toSave = buildGradesForSave()
                                if (toSave.isEmpty()) { isSaving = false; snackbarHostState.showSnackbar("No valid grades to save"); return@launch }
                                val res = FirestoreDatabase.saveBatchGrades(toSave, user.uid)
                                isSaving = false
                                if (res.isSuccess) { dirtyGradeData = emptyMap(); refreshTick++; snackbarHostState.showSnackbar("Grades saved and synced!") } else { snackbarHostState.showSnackbar("Save failed: ${res.exceptionOrNull()?.message}") }
                            } catch (e: Exception) { isSaving = false; snackbarHostState.showSnackbar("Error: ${e.message}") }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = StudentGreen), enabled = isValid) { Text("Confirm Save") }
                },
                dismissButton = { TextButton(onClick = { showSaveConfirmation = false }) { Text("Cancel") } }
            )
        }
        // Unsaved dialog
        if (showUnsavedDialog) {
            val (isValid, warns) = validateEffective()
            AlertDialog(onDismissRequest = { showUnsavedDialog = false }, title = { Text("Unsaved Changes") }, text = {
                Column { Text("You have unsaved marks. Save before leaving?"); if (!isValid) { Spacer(Modifier.height(8.dp)); warns.take(4).forEach { Text("• $it", color = Color.Red, fontSize = 12.sp) } } }
            }, confirmButton = {
                Button(onClick = {
                    showUnsavedDialog = false
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user == null) { scope.launch { snackbarHostState.showSnackbar("Sign in to save marks") }; return@Button }
                    if (!isValid) { scope.launch { snackbarHostState.showSnackbar("Cannot save: fix validation issues") }; return@Button }
                    isSaving = true
                    scope.launch {
                        try {
                            var canSave = true
                            FirestoreDatabase.getUserRole(user.uid, onComplete = { role -> canSave = role == "admin" || role == "teacher" }, onFailure = { })
                            if (!canSave) { isSaving = false; snackbarHostState.showSnackbar("You don't have permission to save grades"); return@launch }
                            val res = FirestoreDatabase.saveBatchGrades(buildGradesForSave(), user.uid)
                            isSaving = false
                            if (res.isSuccess) { dirtyGradeData = emptyMap(); refreshTick++; snackbarHostState.showSnackbar("Grades saved"); navController.navigateUp() } else { snackbarHostState.showSnackbar("Save failed: ${res.exceptionOrNull()?.message}") }
                        } catch (e: Exception) { isSaving = false; snackbarHostState.showSnackbar("Error: ${e.message}") }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = StudentGreen)) { Text("Save & Leave") }
            }, dismissButton = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { showUnsavedDialog = false; navController.navigateUp() }) { Text("Discard") }; TextButton(onClick = { showUnsavedDialog = false }) { Text("Stay") } } })
        }
        // Delete dialog
        if (showDeleteDialog && deleteTargetStudentId != null) {
            val target = students.find { it.id == deleteTargetStudentId }
            AlertDialog(onDismissRequest = { showDeleteDialog = false; deleteTargetStudentId = null }, title = { Text("Delete Grades") }, text = { Text("Delete saved marks for ${target?.firstName} ${target?.lastName}? This cannot be undone.") }, confirmButton = {
                Button(onClick = {
                    val grade = existingGrades[deleteTargetStudentId]
                    showDeleteDialog = false
                    if (grade == null) { scope.launch { snackbarHostState.showSnackbar("No saved marks to delete") }; deleteTargetStudentId = null; return@Button }
                    scope.launch {
                        val res = FirestoreDatabase.deleteStudentGrade(grade.id)
                        if (res.isSuccess) { snackbarHostState.showSnackbar("Deleted marks"); deleteTargetStudentId = null } else { snackbarHostState.showSnackbar("Delete failed: ${res.exceptionOrNull()?.message}") }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete") }
            }, dismissButton = { TextButton(onClick = { showDeleteDialog = false; deleteTargetStudentId = null }) { Text("Cancel") } })
        }
        // Unsaved Filter Dialog
        if (showUnsavedFilterDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedFilterDialog = false; pendingFilterAction = null },
                title = { Text("Unsaved Changes") },
                text = { Text("You have unsaved marks. Discard changes and switch?") },
                confirmButton = {
                    Button(onClick = {
                        showUnsavedFilterDialog = false
                        dirtyGradeData = emptyMap() // Discard
                        pendingFilterAction?.invoke()
                        pendingFilterAction = null
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Discard & Switch") }
                },
                dismissButton = {
                    TextButton(onClick = { showUnsavedFilterDialog = false; pendingFilterAction = null }) { Text("Cancel") }
                }
            )
        }
    }
}
