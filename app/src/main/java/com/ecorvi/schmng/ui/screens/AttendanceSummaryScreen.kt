package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecorvi.schmng.models.AttendanceRecord
import com.ecorvi.schmng.models.AttendanceStatus
import com.ecorvi.schmng.models.UserType
import com.ecorvi.schmng.viewmodels.AttendanceViewModel

@Composable
fun AttendanceSummaryScreen(
    viewModel: AttendanceViewModel,
    userType: UserType,
    date: Long
) {
    val users by viewModel.users.collectAsState(initial = emptyList())
    val attendanceRecords by viewModel.attendanceRecords.collectAsState(initial = emptyMap())

    val totalCount = users.size
    val presentCount = attendanceRecords.values.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = attendanceRecords.values.count { it.status == AttendanceStatus.ABSENT }
    val permissionCount = attendanceRecords.values.count { it.status == AttendanceStatus.PERMISSION }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "Total",
                    count = totalCount,
                    color = MaterialTheme.colorScheme.primary
                )
                SummaryItem(
                    label = "Present",
                    count = presentCount,
                    color = Color.Green
                )
                SummaryItem(
                    label = "Absent",
                    count = absentCount,
                    color = Color.Red
                )
                SummaryItem(
                    label = "Permission",
                    count = permissionCount,
                    color = Color(0xFFFFA000)
                )
            }

            if (totalCount > 0) {
                LinearProgressIndicator(
                    progress = presentCount.toFloat() / totalCount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .height(8.dp),
                    color = Color.Green,
                    trackColor = Color.LightGray
                )

                Text(
                    text = "Attendance Rate: ${(presentCount.toFloat() / totalCount * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
} 