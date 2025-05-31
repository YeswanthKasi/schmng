@file:OptIn(ExperimentalMaterial3Api::class)
package com.ecorvi.schmng.ui.screens.teacher

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.viewmodels.TeacherLeaveViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import com.ecorvi.schmng.ui.data.FirestoreDatabase

@Composable
fun TeacherLeaveApplyScreen(navController: NavController, viewModel: TeacherLeaveViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val loading by viewModel.loading.collectAsState()
    val submitSuccess by viewModel.submitSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var fromDate by remember { mutableStateOf<Long?>(null) }
    var toDate by remember { mutableStateOf<Long?>(null) }
    var reason by remember { mutableStateOf("") }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }
    var teacherName by remember { mutableStateOf("") }
    var isLoadingTeacher by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Load teacher name when screen opens
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            FirestoreDatabase.getTeacherByUserId(
                userId = currentUser.uid,
                onComplete = { teacher ->
                    if (teacher != null) {
                        teacherName = "${teacher.firstName} ${teacher.lastName}"
                    }
                    isLoadingTeacher = false
                },
                onFailure = { e ->
                    Log.e("TeacherLeaveApply", "Error loading teacher data", e)
                    isLoadingTeacher = false
                }
            )
        }
    }

    // Show Snackbar for error or success
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetError()
        }
    }
    LaunchedEffect(submitSuccess) {
        if (submitSuccess) {
            snackbarHostState.showSnackbar("Leave application submitted!")
            fromDate = null
            toDate = null
            reason = ""
            viewModel.resetSubmitSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apply for Leave") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoadingTeacher) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // From Date
                    OutlinedTextField(
                        value = fromDate?.let { dateFormat.format(Date(it)) } ?: "",
                        onValueChange = {},
                        label = { Text("From Date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFromDatePicker = true },
                        enabled = false,
                        trailingIcon = {
                            IconButton(onClick = { showFromDatePicker = true }) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = "Pick From Date")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // To Date
                    OutlinedTextField(
                        value = toDate?.let { dateFormat.format(Date(it)) } ?: "",
                        onValueChange = {},
                        label = { Text("To Date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showToDatePicker = true },
                        enabled = false,
                        trailingIcon = {
                            IconButton(onClick = { showToDatePicker = true }) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = "Pick To Date")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Reason
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    // Submit Button
                    Button(
                        onClick = {
                            if (fromDate != null && toDate != null && reason.isNotBlank()) {
                                if (teacherName.isBlank()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Error: Teacher name not available")
                                    }
                                } else {
                                    viewModel.submitLeave(fromDate!!, toDate!!, reason, teacherName)
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please fill all fields.")
                                }
                            }
                        },
                        enabled = !loading && !isLoadingTeacher && fromDate != null && toDate != null && reason.isNotBlank(),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Submit")
                        }
                    }
                }
            }
        }
        // Date Pickers
        if (showFromDatePicker) {
            val calendar = Calendar.getInstance()
            fromDate?.let { calendar.timeInMillis = it }
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val cal = Calendar.getInstance()
                    cal.set(year, month, dayOfMonth, 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    fromDate = cal.timeInMillis
                    showFromDatePicker = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                setOnCancelListener { showFromDatePicker = false }
                setOnDismissListener { showFromDatePicker = false }
            }.show()
        }
        if (showToDatePicker) {
            val calendar = Calendar.getInstance()
            toDate?.let { calendar.timeInMillis = it }
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val cal = Calendar.getInstance()
                    cal.set(year, month, dayOfMonth, 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    toDate = cal.timeInMillis
                    showToDatePicker = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                setOnCancelListener { showToDatePicker = false }
                setOnDismissListener { showToDatePicker = false }
            }.show()
        }
    }
} 