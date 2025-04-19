package com.ecorvi.schmng.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecorvi.schmng.ui.data.model.Person

@Composable
fun StudentListItem(
    student: Person,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onItemClick)
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Student Icon
            Surface(
                shape = CircleShape,
                color = Color(0xFF1F41BB).copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Student",
                    tint = Color(0xFF1F41BB),
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Student Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "${student.firstName} ${student.lastName}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Class: ${student.className}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Gray
                    )
                )
            }

            // Delete Button
            IconButton(
                onClick = onDeleteClick,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Student"
                )
            }
        }
    }
}

@Preview
@Composable
fun StudentListItemPreview() {
    val student = Person(
        id = "1",
        firstName = "John",
        lastName = "Doe",
        className = "Class A"
    )
    MaterialTheme {
        StudentListItem(
            student = student,
            onItemClick = {},
            onDeleteClick = {}
        )
    }
}


