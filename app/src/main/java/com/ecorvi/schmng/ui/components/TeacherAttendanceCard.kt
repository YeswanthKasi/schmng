package com.ecorvi.schmng.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Log
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.UserType
import com.ecorvi.schmng.viewmodels.AttendanceViewModel
import com.ecorvi.schmng.ui.data.model.Person
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "TeacherAttendanceCard"

@Composable
fun TeacherAttendanceCard(
    className: String,
    modifier: Modifier = Modifier,
    viewModel: AttendanceViewModel
) {
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    var substituteTeachers by remember { mutableStateOf<Map<String, Person>>(emptyMap()) }
    var absentTeacherDetails by remember { mutableStateOf<Map<String, Person>>(emptyMap()) }

    // Standardize class name to match admin panel format
    val standardizedClassName = if (!className.startsWith("Class ")) "Class $className" else className

    LaunchedEffect(className, attendanceRecords) {
        Log.d(TAG, "Loading attendance for class: $standardizedClassName")
        viewModel.loadUsers(UserType.TEACHER)
        viewModel.loadAttendance(System.currentTimeMillis(), UserType.TEACHER)
        
        // Fetch substitute teachers
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val db = FirebaseFirestore.getInstance()
            
            // Find teachers who are absent
            val absentTeachers = attendanceRecords.filter { (_, record) ->
                record.status == AttendanceStatus.ABSENT || record.status == AttendanceStatus.PERMISSION
            }
            
            Log.d(TAG, "Found ${absentTeachers.size} absent teachers")
            val substitutes = mutableMapOf<String, Person>()
            val absentDetails = mutableMapOf<String, Person>()
            
            absentTeachers.forEach { (teacherId, _) ->
                try {
                    // First get the absent teacher's details
                    val teacherDoc = db.collection("teachers")
                        .document(teacherId)
                        .get()
                        .await()
                    
                    val absentTeacher = teacherDoc.toObject(Person::class.java)
                    if (absentTeacher != null) {
                        absentTeacher.id = teacherId
                        absentDetails[teacherId] = absentTeacher
                        
                        Log.d(TAG, "Checking substitutes for teacher $teacherId in class $standardizedClassName on date $today")
                        
                        // Get substitute assignment using the correct path
                        val substituteDoc = db.collection("substitute_teachers")
                            .document(today)
                            .collection(standardizedClassName)
                            .document("assigned_teacher")
                            .get()
                            .await()

                        if (substituteDoc.exists()) {
                            Log.d(TAG, "Found substitute document: ${substituteDoc.data}")
                            val substituteId = substituteDoc.getString("teacherId")
                            Log.d(TAG, "Found substitute assignment: $substituteId")
                            
                            if (substituteId != null) {
                                val substituteTeacherDoc = db.collection("teachers")
                                    .document(substituteId)
                                    .get()
                                    .await()
                                
                                val substituteTeacher = substituteTeacherDoc.toObject(Person::class.java)
                                if (substituteTeacher != null) {
                                    substituteTeacher.id = substituteId
                                    substitutes[teacherId] = substituteTeacher
                                    Log.d(TAG, "Successfully found substitute teacher ${substituteTeacher.firstName} for teacher $teacherId")
                                } else {
                                    Log.d(TAG, "Substitute teacher document exists but couldn't be converted to Person object")
                                }
                            } else {
                                Log.d(TAG, "Substitute document exists but no teacherId field found")
                            }
                        } else {
                            Log.d(TAG, "No substitute assignment found for teacher $teacherId in class $standardizedClassName")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching substitute for teacher $teacherId: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            substituteTeachers = substitutes
            absentTeacherDetails = absentDetails
            Log.d(TAG, "Updated substitute teachers map: ${substitutes.size} entries")
            Log.d(TAG, "Substitute teachers data: $substitutes")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching substitute teachers: ${e.message}")
            e.printStackTrace()
        }
    }

    if (loading) {
        Log.d(TAG, "Loading teacher attendance...")
        return
    }

    if (error != null) {
        Log.e(TAG, "Error loading teacher attendance: $error")
        return
    }

    // Find teachers who are absent or on permission
    val absentTeachers = attendanceRecords.filter { (_, record) ->
        record.status == AttendanceStatus.ABSENT || record.status == AttendanceStatus.PERMISSION
    }

    if (absentTeachers.isNotEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
            shape = RoundedCornerShape(20.dp)
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
                    Text(
                        text = "Important Notice",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Important Notice",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                absentTeachers.forEach { (teacherId, record) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Teacher Absence Notice",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            val absentTeacher = absentTeacherDetails[teacherId]
                            Text(
                                text = "${absentTeacher?.firstName ?: ""} ${absentTeacher?.lastName ?: ""} is ${
                                    when (record.status) {
                                        AttendanceStatus.ABSENT -> "absent"
                                        AttendanceStatus.PERMISSION -> "on leave"
                                        else -> record.status.toString().lowercase()
                                    }
                                } today",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val substituteTeacher = substituteTeachers[teacherId]
                            Text(
                                text = if (substituteTeacher != null) {
                                    "${substituteTeacher.firstName} ${substituteTeacher.lastName} will be taking the classes"
                                } else {
                                    "No substitute teacher assigned yet"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (teacherId != absentTeachers.keys.last()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
} 