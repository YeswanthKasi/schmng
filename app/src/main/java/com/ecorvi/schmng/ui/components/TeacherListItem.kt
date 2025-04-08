package com.ecorvi.schmng.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ecorvi.schmng.R
import com.ecorvi.schmng.ui.data.model.Person

@Composable
fun TeacherListItem(
    teacher: Person,
    navController: NavController,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Teacher Icon",
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "${teacher.firstName} ${teacher.lastName}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "ID: ${teacher.id}, Class: ${teacher.className}, Gender: ${teacher.gender}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = {
                    navController.navigate("profile/${teacher.id}/teacher")
                }) {
                    Image(
                        painter = painterResource(id = R.drawable.visibility),
                        contentDescription = "View",
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewTeacherListItem() {
    val context = LocalContext.current
    val navController = remember { NavController(context) }

    TeacherListItem(
        teacher = Person(
            id = "T2024-001",
            firstName = "Anita",
            lastName = "Sharma",
            className = "Class 3",
            email = "anita@school.com",
            gender = "Female",
            dateOfBirth = "1985-04-12",
            mobileNo = "9876543210",
            address = "School Road"
        ),
        navController = navController,
        onDeleteClick = {}
    )
}
