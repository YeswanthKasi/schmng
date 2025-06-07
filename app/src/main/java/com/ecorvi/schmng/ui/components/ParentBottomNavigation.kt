package com.ecorvi.schmng.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ecorvi.schmng.ui.navigation.ParentBottomNavItem
import com.ecorvi.schmng.ui.theme.ParentBlue

@Composable
fun ParentBottomNavigation(
    currentRoute: String?,
    onNavigate: (ParentBottomNavItem) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            ParentBottomNavItem.Home,
            ParentBottomNavItem.Attendance,
            ParentBottomNavItem.Events,
            ParentBottomNavItem.Notices,
            ParentBottomNavItem.Messages
        )

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute?.startsWith(item.route) == true,
                onClick = { onNavigate(item) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ParentBlue,
                    selectedTextColor = ParentBlue,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.White
                )
            )
        }
    }
} 