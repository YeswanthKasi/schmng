package com.ecorvi.schmng.ui.screens.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ecorvi.schmng.ui.theme.ParentBlue

@Composable
fun ParentDashboardContent(
    childId: String?,
    navController: NavController,
    onRouteSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (childId == null) {
                Text(
                    text = "No child associated with this account. Please contact the school administration.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                // Dashboard items will be added here
                DashboardCard(
                    title = "Student Information",
                    onClick = { onRouteSelected("student/$childId") }
                )
                
                DashboardCard(
                    title = "Attendance",
                    onClick = { onRouteSelected("attendance/$childId") }
                )
                
                DashboardCard(
                    title = "Notices",
                    onClick = { onRouteSelected("notices/$childId") }
                )
                
                DashboardCard(
                    title = "Messages",
                    onClick = { onRouteSelected("messages") }
                )
            }
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ParentBlue
            )
        }
    }
} 