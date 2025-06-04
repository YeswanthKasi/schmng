package com.ecorvi.schmng.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import com.ecorvi.schmng.ui.navigation.StudentBottomNavItem

@Composable
fun StudentBottomNavigation(
    currentRoute: String?,
    onNavigate: (StudentBottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.95f)),
        containerColor = Color.White.copy(alpha = 0.95f),
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            StudentBottomNavItem.Home,
            StudentBottomNavItem.Schedule,
            StudentBottomNavItem.Attendance,
            StudentBottomNavItem.Notices,
            StudentBottomNavItem.Profile
        )

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1F41BB),
                    selectedTextColor = Color(0xFF1F41BB),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.White
                )
            )
        }
    }
} 