package com.ecorvi.schmng.ui.screens.teacher

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ecorvi.schmng.models.Notice
import com.ecorvi.schmng.viewmodels.NoticeViewModel
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherNoticeCreateScreen(
    navController: NavController,
    noticeId: String? = null,
    viewModel: NoticeViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var targetClass by remember { mutableStateOf("all") }
    var priority by remember { mutableStateOf(Notice.PRIORITY_NORMAL) }
    var attachments by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // State for dropdowns
    var isTargetClassExpanded by remember { mutableStateOf(false) }
    var isPriorityExpanded by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Available classes list
    val classes = remember { listOf("all", "Class 1", "Class 2", "Class 3", "Class 4", "Class 5", "Class 6", "Class 7", "Class 8", "Class 9", "Class 10") }
    
    // Priority options
    val priorities = remember {
        listOf(
            Notice.PRIORITY_LOW to "Low",
            Notice.PRIORITY_NORMAL to "Normal",
            Notice.PRIORITY_HIGH to "High"
        )
    }
    
    // Initialize Firebase Storage using Firebase KTX
    val storageRef = Firebase.storage.reference
    
    // Load notice data if editing
    LaunchedEffect(noticeId) {
        if (noticeId != null) {
            viewModel.getNotice(noticeId)?.let { notice ->
                title = notice.title
                content = notice.content
                targetClass = notice.targetClass
                priority = notice.priority
                attachments = notice.attachments
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val fileName = UUID.randomUUID().toString()
                    val fileRef = storageRef.child("notices/$fileName")
                    
                    // Upload file and wait for completion
                    fileRef.putFile(uri).await()
                    
                    // Get download URL and wait for completion
                    val downloadUrl = fileRef.downloadUrl.await().toString()
                    attachments = attachments + downloadUrl
                    
                    snackbarHostState.showSnackbar("File uploaded successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to upload file: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noticeId == null) "Create Notice" else "Edit Notice") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    if (noticeId == null) {
                                        viewModel.createNotice(
                                            title = title,
                                            content = content,
                                            targetClass = targetClass,
                                            priority = priority,
                                            attachments = attachments
                                        )
                                    } else {
                                        viewModel.updateNotice(
                                            Notice(
                                                id = noticeId,
                                                title = title,
                                                content = content,
                                                targetClass = targetClass,
                                                priority = priority,
                                                attachments = attachments
                                            )
                                        )
                                    }
                                    navController.navigateUp()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                }
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )
            }
            
            // Target Class Dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = isTargetClassExpanded,
                    onExpandedChange = { isTargetClassExpanded = it }
                ) {
                    OutlinedTextField(
                        value = targetClass,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Class") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTargetClassExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = isTargetClassExpanded,
                        onDismissRequest = { isTargetClassExpanded = false }
                    ) {
                        classes.forEach { classOption ->
                            DropdownMenuItem(
                                text = { Text(if (classOption == "all") "All Classes" else classOption) },
                                onClick = {
                                    targetClass = classOption
                                    isTargetClassExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            // Priority Dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = isPriorityExpanded,
                    onExpandedChange = { isPriorityExpanded = it }
                ) {
                    OutlinedTextField(
                        value = priorities.find { it.first == priority }?.second ?: "Normal",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPriorityExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = isPriorityExpanded,
                        onDismissRequest = { isPriorityExpanded = false }
                    ) {
                        priorities.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    priority = value
                                    isPriorityExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            item {
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Attach File")
                }
            }
            
            items(attachments) { attachment ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = attachment.substringAfterLast('/'),
                            maxLines = 1
                        )
                        IconButton(
                            onClick = {
                                attachments = attachments - attachment
                            }
                        ) {
                            Icon(Icons.Default.Delete, "Remove attachment")
                        }
                    }
                }
            }
        }
    }
}

private suspend fun uploadFile(uri: Uri, context: Context): String {
    return try {
        val timestamp = System.currentTimeMillis()
        val filename = "notice_attachments/${timestamp}_${uri.lastPathSegment}"
        val storageRef = Firebase.storage.reference.child(filename)
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bytes = inputStream.readBytes()
            storageRef.putBytes(bytes).await()
            return storageRef.downloadUrl.await().toString()
        } ?: throw Exception("Failed to read file")
    } catch (e: Exception) {
        throw Exception("Failed to upload file: ${e.message}")
    }
} 