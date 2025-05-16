package com.ecorvi.schmng.ui.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "SubstituteTeacherDialog"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignSubstituteTeacherDialog(
    className: String,
    absentTeacherId: String,
    onDismiss: () -> Unit,
    onTeacherAssigned: () -> Unit
) {
    var availableTeachers by remember { mutableStateOf<List<Person>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentSubstitute by remember { mutableStateOf<Person?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Standardize class name
    val standardizedClassName = if (!className.startsWith("Class ")) "Class $className" else className
    Log.d(TAG, "Standardized class name: $standardizedClassName")
    Log.d(TAG, "Absent teacher ID: $absentTeacherId")

    // Function to check current substitute
    suspend fun checkCurrentSubstitute() {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val assignmentDoc = FirebaseFirestore.getInstance()
                .collection("substitute_teachers")
                .document(today)
                .collection(standardizedClassName)
                .document("assigned_teacher")
                .get()
                .await()

            if (assignmentDoc.exists()) {
                val substituteId = assignmentDoc.getString("teacherId")
                if (substituteId != null) {
                    val substituteDoc = FirebaseFirestore.getInstance()
                        .collection("teachers")
                        .document(substituteId)
                        .get()
                        .await()
                    currentSubstitute = substituteDoc.toObject(Person::class.java)?.apply { id = substituteId }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking current substitute: ${e.message}")
        }
    }

    // Function to delete substitute assignment
    suspend fun deleteSubstituteAssignment() {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            FirebaseFirestore.getInstance()
                .collection("substitute_teachers")
                .document(today)
                .collection(standardizedClassName)
                .document("assigned_teacher")
                .delete()
                .await()
            
            Log.d(TAG, "Successfully deleted substitute assignment")
            currentSubstitute = null
            onTeacherAssigned()
            onDismiss()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting substitute assignment: ${e.message}")
            error = "Error removing substitute teacher: ${e.message}"
        }
    }

    // Verify admin role and check current substitute
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            try {
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                val isAdmin = userDoc.getString("role")?.lowercase() == "admin"
                if (!isAdmin) {
                    error = "Only administrators can assign substitute teachers"
                    return@LaunchedEffect
                }

                // Check current substitute
                checkCurrentSubstitute()
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying admin role: ${e.message}")
                error = "Error verifying permissions: ${e.message}"
                return@LaunchedEffect
            }
        } else {
            error = "Please log in to continue"
            return@LaunchedEffect
        }
    }

    // Fetch available teachers
    LaunchedEffect(Unit) {
        try {
            // Get today's date
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            Log.d(TAG, "Fetching available teachers for date: $today")
            
            // Get all teachers
            val teachersSnapshot = FirebaseFirestore.getInstance()
                .collection("teachers")
                .get()
                .await()

            Log.d(TAG, "Found ${teachersSnapshot.size()} total teachers")

            // Get teachers who are present today and not already assigned as substitutes
            val presentTeachers = mutableListOf<Person>()
            
            for (doc in teachersSnapshot.documents) {
                try {
                    val teacher = doc.toObject(Person::class.java)
                    if (teacher != null && doc.id != absentTeacherId) {
                        teacher.id = doc.id // Ensure ID is set
                        Log.d(TAG, "Checking attendance for teacher: ${teacher.firstName} ${teacher.lastName}")
                        
                        // Check if teacher is present today
                        val attendanceDoc = FirebaseFirestore.getInstance()
                            .collection("attendance")
                            .document(today)
                            .collection("teacher")
                            .document(doc.id)
                            .get()
                            .await()

                        val status = attendanceDoc.getString("status")
                        Log.d(TAG, "Teacher ${teacher.firstName} attendance status: $status")

                        // Check if teacher is not already assigned as a substitute
                        val isAlreadySubstitute = try {
                            val substituteAssignments = FirebaseFirestore.getInstance()
                                .collection("substitute_teachers")
                                .document(today)
                                .collection(standardizedClassName)
                                .whereEqualTo("teacherId", doc.id)
                                .get()
                                .await()
                            !substituteAssignments.isEmpty
                        } catch (e: Exception) {
                            Log.e(TAG, "Error checking substitute status: ${e.message}")
                            false
                        }

                        if (status == "PRESENT" && !isAlreadySubstitute) {
                            Log.d(TAG, "Adding ${teacher.firstName} to available teachers list")
                            presentTeachers.add(teacher)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing teacher: ${e.message}")
                    // Continue with next teacher
                }
            }

            Log.d(TAG, "Found ${presentTeachers.size} available teachers")
            availableTeachers = presentTeachers
            isLoading = false
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching teachers: ${e.message}", e)
            error = e.message
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
                .animateContentSize(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Assign Substitute",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "For $className",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(Icons.Rounded.Close, "Close")
                    }
                }

                // Current Substitute Section
                AnimatedVisibility(
                    visible = currentSubstitute != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    currentSubstitute?.let { substitute ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Current Substitute",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${substitute.firstName} ${substitute.lastName}",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        Text(
                                            text = substitute.className ?: "No Class Assigned",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    FilledIconButton(
                                        onClick = { showDeleteConfirmation = true },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Rounded.Delete, "Remove")
                                    }
                                }
                            }
                        }
                    }
                }

                // Main Content
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    error != null -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = error ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    availableTeachers.isEmpty() -> {
                        EmptyStateCard()
                    }
                    else -> {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically()
                        ) {
                            Column {
                                if (currentSubstitute != null) {
                                    Text(
                                        text = "Available Teachers",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                }
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(availableTeachers) { teacher ->
                                        TeacherCard(
                                            teacher = teacher,
                                            onAssign = {
                                                scope.launch {
                                                    try {
                                                        // Verify admin role again before write
                                                        val userDoc = FirebaseFirestore.getInstance()
                                                            .collection("users")
                                                            .document(currentUser?.uid ?: "")
                                                            .get()
                                                            .await()
                                                        
                                                        val isAdmin = userDoc.getString("role")?.lowercase() == "admin"
                                                        if (!isAdmin) {
                                                            error = "Only administrators can assign substitute teachers"
                                                            return@launch
                                                        }

                                                        // Get today's date
                                                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                                                        // Create the assignment document
                                                        val assignmentData = mapOf(
                                                            "teacherId" to teacher.id,
                                                            "originalTeacherId" to absentTeacherId,
                                                            "assignedAt" to com.google.firebase.Timestamp.now(),
                                                            "assignedBy" to (currentUser?.uid ?: return@launch)
                                                        )

                                                        // Write to Firestore
                                                        FirebaseFirestore.getInstance()
                                                            .collection("substitute_teachers")
                                                            .document(today)
                                                            .collection(standardizedClassName)
                                                            .document("assigned_teacher")
                                                            .set(assignmentData)
                                                            .await()

                                                        Log.d(TAG, "Successfully assigned substitute teacher: ${teacher.firstName} ${teacher.lastName}")
                                                        onTeacherAssigned()
                                                        onDismiss()
                                                    } catch (e: Exception) {
                                                        Log.e(TAG, "Error assigning substitute teacher: ${e.message}")
                                                        error = "Error assigning substitute teacher: ${e.message}"
                                                    }
                                                }
                                            }
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

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null) },
            title = { Text("Remove Substitute") },
            text = { 
                Text("Are you sure you want to remove the current substitute teacher assignment?") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            deleteSubstituteAssignment()
                            showDeleteConfirmation = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TeacherCard(
    teacher: Person,
    onAssign: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${teacher.firstName} ${teacher.lastName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = teacher.className ?: "No Class Assigned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = onAssign,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Rounded.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Assign")
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Rounded.Groups,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No Available Teachers",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "There are no teachers available for substitution at this time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
} 