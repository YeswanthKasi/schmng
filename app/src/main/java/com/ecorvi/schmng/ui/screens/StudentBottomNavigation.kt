package com.ecorvi.schmng.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
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
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(18.dp).padding(horizontal = 0.dp)
                    )
                },
                label = {
                    Text(
                        item.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                        modifier = Modifier.padding(start = 0.dp, end = 0.dp, top = 0.dp)
                    )
                },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1F41BB),
                    selectedTextColor = Color(0xFF1F41BB),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.White
                ),
                alwaysShowLabel = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
} 