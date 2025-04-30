package com.ecorvi.schmng.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment

// Sealed class for bottom navigation items
sealed class BottomNavItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    object Home : BottomNavItem("admin_dashboard", Icons.Default.Home, "Home")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
    object Notifications : BottomNavItem("notifications", Icons.Default.Notifications, "Notifications")
    object Messages : BottomNavItem("messages", Icons.Default.Message, "Messages")
}

@Composable
fun BottomNav(
    navController: NavController,
    currentRoute: String,
    onItemSelected: (BottomNavItem) -> Unit
) {
    // Get screen width to calculate responsive sizes
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    // Calculate responsive sizes
    val iconSize = (screenWidth * 0.06f).coerceIn(24f, 32f).dp
    val labelSize = (screenWidth * 0.028f).coerceIn(10f, 14f).sp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        NavigationBar(
            modifier = Modifier
                .height(72.dp)
                .fillMaxWidth(),
            containerColor = Color.White,
            tonalElevation = 0.dp
        ) {
            listOf(
                BottomNavItem.Home,
                BottomNavItem.Profile,
                BottomNavItem.Notifications,
                BottomNavItem.Messages
            ).forEach { item ->
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = { onItemSelected(item) },
                    icon = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(iconSize)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.label,
                                style = TextStyle(
                                    fontSize = labelSize,
                                    fontWeight = if (currentRoute == item.route) 
                                        FontWeight.Medium else FontWeight.Normal
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    label = null, // We're handling the label in the icon composable
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1F41BB),
                        selectedTextColor = Color(0xFF1F41BB),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF1F41BB).copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
} 