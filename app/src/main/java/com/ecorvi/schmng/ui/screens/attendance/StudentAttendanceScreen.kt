package com.ecorvi.schmng.ui.screens.attendance

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecorvi.schmng.R
import com.ecorvi.schmng.ui.components.AttendanceSummary
import com.ecorvi.schmng.ui.components.StudentAttendanceRow
import com.ecorvi.schmng.ui.data.model.Person

@Composable
fun StudentAttendanceScreen(
    students: List<Person>,
    isLoading: Boolean,
    onSubmit: (List<Person>) -> Unit
) {
    val studentList = remember { mutableStateListOf<Person>().apply { addAll(students) } }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_ui),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                Text(
                    text = "Students",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(studentList) { student ->
                StudentAttendanceRow(
                    student = student,
                    onStatusSelected = { option ->
                        val index = studentList.indexOfFirst { it.id == student.id }
                        if (index != -1) {
                            studentList[index] = student.copy(attendance = option)
                        }
                    }
                )
            }

            item {
                AttendanceSummary(studentList.toList())
            }

            item {
                Button(
                    onClick = { onSubmit(studentList.toList()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text("Submit Attendance")
                }
            }
        }
    }
}
