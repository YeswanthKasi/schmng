package com.ecorvi.schmng.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.viewmodels.TeacherAttendanceViewModel
import com.ecorvi.schmng.viewmodels.TeacherAttendanceState

private const val TAG = "TeacherAttendanceCard"

@Composable
fun TeacherAttendanceCard(
    className: String,
    modifier: Modifier = Modifier,
    viewModel: TeacherAttendanceViewModel = viewModel()
) {
    val attendanceState by viewModel.attendanceState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(className) {
        Log.d(TAG, "Fetching teacher attendance for class: $className")
        viewModel.fetchTeacherAttendance(className)
    }

    when (attendanceState) {
        is TeacherAttendanceState.Loading -> {
            Log.d(TAG, "Loading teacher attendance...")
        }
        is TeacherAttendanceState.Error -> {
            val error = (attendanceState as TeacherAttendanceState.Error).message
            Log.e(TAG, "Error loading teacher attendance: $error")
        }
        is TeacherAttendanceState.NotTakenYet -> {
            Log.d(TAG, "Attendance not taken yet")
        }
        is TeacherAttendanceState.Success -> {
            val state = attendanceState as TeacherAttendanceState.Success
            
            Log.d(TAG, "Attendance state: Regular teacher=${state.regularTeacher.name}, " +
                      "Status=${state.attendanceStatus}, " +
                      "Replacement=${state.replacementTeacher?.name}")
            
            // Only show the card if teacher is absent/on leave AND a replacement is assigned
            if ((state.attendanceStatus == AttendanceStatus.ABSENT || 
                 state.attendanceStatus == AttendanceStatus.PERMISSION) && 
                state.replacementTeacher != null) {
                
                Log.d(TAG, "Showing replacement teacher card")
                
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
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Teacher Change Notice",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Teacher Change for Today",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${state.replacementTeacher.name} will be taking your classes today",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                Log.d(TAG, "Not showing replacement card: " +
                          "Status=${state.attendanceStatus}, " +
                          "Has replacement=${state.replacementTeacher != null}")
            }
        }
    }
} 