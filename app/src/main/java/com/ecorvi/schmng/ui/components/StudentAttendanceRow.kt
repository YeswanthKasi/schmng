package com.ecorvi.schmng.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecorvi.schmng.ui.data.model.AttendanceOption
import com.ecorvi.schmng.ui.data.model.Person

@Composable
fun StudentAttendanceRow(
    student: Person,
    onStatusSelected: (AttendanceOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .padding(4.dp)
        )

        Text(
            text = "${student.firstName} ${student.lastName}",
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            fontWeight = FontWeight.SemiBold
        )

        AttendanceRadioButton("Present", AttendanceOption.PRESENT, student.attendance, onStatusSelected)
        AttendanceRadioButton("Permission", AttendanceOption.PERMISSION, student.attendance, onStatusSelected)
        AttendanceRadioButton("Absent", AttendanceOption.ABSENT, student.attendance, onStatusSelected)
    }
}

@Composable
fun AttendanceRadioButton(
    label: String,
    value: AttendanceOption,
    selected: AttendanceOption,
    onSelect: (AttendanceOption) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onSelect(value) }
            .padding(horizontal = 4.dp)
    ) {
        Checkbox(
            checked = selected == value,
            onCheckedChange = { onSelect(value) }
        )
        Text(text = label, fontSize = 12.sp)
    }
}
