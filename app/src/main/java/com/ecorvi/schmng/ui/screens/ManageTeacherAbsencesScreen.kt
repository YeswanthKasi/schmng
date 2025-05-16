package com.ecorvi.schmng.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.ui.components.AssignSubstituteTeacherDialog
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ManageTeacherAbsences"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTeacherAbsencesScreen(
    navController: NavController
) {
    val context = LocalContext.current
    var absentTeachers by remember { mutableStateOf<List<Pair<Person, AttendanceStatus>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAssignDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var replacementAssignments by remember { mutableStateOf<Map<String, Person>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    var isAdmin by remember { mutableStateOf(false) }
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Check if user is admin
    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            try {
                FirestoreDatabase.getUserRole(
                    userId = currentUser.uid,
                    onComplete = { role ->
                        isAdmin = role?.lowercase() == "admin"
                        if (!isAdmin) {
                            Toast.makeText(context, "Only administrators can manage teacher substitutions", Toast.LENGTH_LONG).show()
                            navController.navigateUp()
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Error checking admin role: ${e.message}")
                        Toast.makeText(context, "Error verifying permissions: ${e.message}", Toast.LENGTH_LONG).show()
                        navController.navigateUp()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in admin check: ${e.message}")
                Toast.makeText(context, "Error checking permissions: ${e.message}", Toast.LENGTH_LONG).show()
                navController.navigateUp()
            }
        } else {
            Toast.makeText(context, "Please log in to continue", Toast.LENGTH_LONG).show()
            navController.navigate("login")
        }
    }

    // Function to standardize class name
    fun standardizeClassName(className: String?): String {
        if (className == null || className.isBlank()) return "Unknown Class"
        return if (!className.startsWith("Class ")) "Class $className" else className
    }

    // Function to fetch data
    fun fetchData() {
        scope.launch {
            try {
                isLoading = true
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                Log.d(TAG, "Fetching data for date: $today")
                
                val teachersSnapshot = FirebaseFirestore.getInstance()
                    .collection("teachers")
                    .get()
                    .await()

                Log.d(TAG, "Found ${teachersSnapshot.size()} teachers")

                val absentList = mutableListOf<Pair<Person, AttendanceStatus>>()
                val assignments = mutableMapOf<String, Person>()

                teachersSnapshot.documents.forEach { doc ->
                    try {
                        val teacher = doc.toObject(Person::class.java)
                        if (teacher != null) {
                            teacher.id = doc.id
                            
                            val attendanceDoc = FirebaseFirestore.getInstance()
                                .collection("attendance")
                                .document(today)
                                .collection("teacher")
                                .document(doc.id)
                                .get()
                                .await()

                            val status = when(attendanceDoc.getString("status")) {
                                "ABSENT" -> AttendanceStatus.ABSENT
                                "PERMISSION" -> AttendanceStatus.PERMISSION
                                else -> null
                            }

                            if (status != null) {
                                Log.d(TAG, "Teacher ${teacher.firstName} is $status")
                                absentList.add(teacher to status)
                                
                                val standardizedClassName = standardizeClassName(teacher.className)
                                
                                try {
                                    val substituteDoc = FirebaseFirestore.getInstance()
                                        .collection("substitute_teachers")
                                        .document(today)
                                        .collection(standardizedClassName)
                                        .document("assigned_teacher")
                                        .get()
                                        .await()

                                    if (substituteDoc.exists()) {
                                        val substituteId = substituteDoc.getString("teacherId")
                                        if (substituteId != null) {
                                            // Fetch substitute teacher details
                                            val substituteTeacherDoc = FirebaseFirestore.getInstance()
                                                .collection("teachers")
                                                .document(substituteId)
                                                .get()
                                                .await()
                                            
                                            val substituteTeacher = substituteTeacherDoc.toObject(Person::class.java)
                                            if (substituteTeacher != null) {
                                                substituteTeacher.id = substituteId
                                                assignments[teacher.id] = substituteTeacher
                                                Log.d(TAG, "Found substitute teacher ${substituteTeacher.firstName} for ${teacher.firstName}")
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error checking substitute assignment: ${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing teacher document: ${e.message}")
                    }
                }

                Log.d(TAG, "Found ${absentList.size} absent teachers with ${assignments.size} substitute assignments")
                absentTeachers = absentList
                replacementAssignments = assignments
                isLoading = false
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data: ${e.message}")
                error = "Error loading teacher data: ${e.message}"
                isLoading = false
            }
        }
    }

    // Initial fetch
    LaunchedEffect(Unit) {
        fetchData()
    }

    // Add animation states
    val countTransition = updateTransition(
        targetState = absentTeachers.size,
        label = "Count Transition"
    )
    
    val countScale by countTransition.animateFloat(
        label = "Count Scale",
        transitionSpec = { spring(dampingRatio = 0.8f) }
    ) { count ->
        if (count > 0) 1f else 0.8f
    }

    val countAlpha by countTransition.animateFloat(
        label = "Count Alpha",
        transitionSpec = { spring(dampingRatio = 0.8f) }
    ) { count ->
        if (count > 0) 1f else 0.6f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Teacher Absences",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = countScale
                                    scaleY = countScale
                                    alpha = countAlpha
                                }
                        ) {
                            Text(
                                text = "(${absentTeachers.size})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() }
                    ) {
                        Icon(Icons.Rounded.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "Loading")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "Loading Scale"
                        )
                        CircularProgressIndicator(
                            modifier = Modifier.scale(scale)
                        )
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val errorTransition = rememberInfiniteTransition(label = "Error")
                        val rotation by errorTransition.animateFloat(
                            initialValue = -5f,
                            targetValue = 5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "Error Rotation"
                        )
                        Icon(
                            Icons.Rounded.Error,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer { rotationZ = rotation },
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                absentTeachers.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val emptyTransition = rememberInfiniteTransition(label = "Empty")
                        val scale by emptyTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "Empty Scale"
                        )
                        Icon(
                            Icons.Rounded.Celebration,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .scale(scale),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "All Teachers Present",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "There are no teacher absences to manage today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = absentTeachers,
                            key = { (teacher, _) -> teacher.id }
                        ) { (teacher, status) ->
                            var itemVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                itemVisible = true
                            }
                            val itemTransition = updateTransition(
                                targetState = itemVisible,
                                label = "Item Transition"
                            )
                            val itemScale by itemTransition.animateFloat(
                                label = "Item Scale",
                                transitionSpec = { spring(dampingRatio = 0.8f) }
                            ) { visible ->
                                if (visible) 1f else 0.8f
                            }
                            val itemAlpha by itemTransition.animateFloat(
                                label = "Item Alpha",
                                transitionSpec = { spring(dampingRatio = 0.8f) }
                            ) { visible ->
                                if (visible) 1f else 0f
                            }
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = itemScale
                                        scaleY = itemScale
                                        alpha = itemAlpha
                                    }
                            ) {
                                AbsentTeacherCard(
                                    teacher = teacher,
                                    status = status,
                                    substituteTeacher = replacementAssignments[teacher.id],
                                    onAssignSubstitute = {
                                        showAssignDialog = teacher.className to teacher.id
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Show assignment dialog if needed
    showAssignDialog?.let { (className, teacherId) ->
        AssignSubstituteTeacherDialog(
            className = standardizeClassName(className),
            absentTeacherId = teacherId,
            onDismiss = { showAssignDialog = null },
            onTeacherAssigned = {
                Log.d(TAG, "Substitute teacher assigned, refreshing data...")
                fetchData()
            }
        )
    }
}

@Composable
private fun AbsentTeacherCard(
    teacher: Person,
    status: AttendanceStatus,
    substituteTeacher: Person?,
    onAssignSubstitute: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${teacher.firstName} ${teacher.lastName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = teacher.className ?: "No Class Assigned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(status = status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (substituteTeacher != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Substitute Assigned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${substituteTeacher.firstName} ${substituteTeacher.lastName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onAssignSubstitute,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Change Substitute",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onAssignSubstitute,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Icon(
                        Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Assign Substitute",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: AttendanceStatus) {
    val (backgroundColor, textColor) = when (status) {
        AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f) to MaterialTheme.colorScheme.error
        AttendanceStatus.PERMISSION -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) to MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) to MaterialTheme.colorScheme.primary
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
} 