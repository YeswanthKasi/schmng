package com.ecorvi.schmng.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecorvi.schmng.ui.data.model.AttendanceOption
import com.ecorvi.schmng.ui.data.model.Person

@Composable
fun AttendanceSummary(students: List<Person>) {
    val total = students.size
    val present = students.count { it.attendance == AttendanceOption.PRESENT }
    val permission = students.count { it.attendance == AttendanceOption.PERMISSION }
    val absent = students.count { it.attendance == AttendanceOption.ABSENT }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(Color(0xFFEFF3F6), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(8.dp))

        SummaryItem("Total Students", total.toString())
        SummaryItem("Present", present.toString())
        SummaryItem("Permission", permission.toString())
        SummaryItem("Absent", absent.toString())
    }
}

@Composable
fun SummaryItem(label: String, count: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold)
        Text(text = count)
    }
}
