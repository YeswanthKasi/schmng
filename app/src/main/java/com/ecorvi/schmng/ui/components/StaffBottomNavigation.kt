package com.ecorvi.schmng.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ecorvi.schmng.ui.navigation.StaffBottomNavItem
import com.google.firebase.auth.FirebaseAuth

@Composable
fun StaffBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
    ) {
        StaffBottomNavItem.values().forEach { item ->
            val isSelected = when (item) {
                is StaffBottomNavItem.Profile -> {
                    currentRoute?.startsWith("staff_profile/") == true
                }
                is StaffBottomNavItem.Messages -> {
                    currentRoute == item.route || currentRoute?.startsWith("chat/") == true
                }
                else -> currentRoute == item.route
            }
            
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    when (item) {
                        is StaffBottomNavItem.Profile -> {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            currentUser?.uid?.let { uid ->
                                onNavigate(item.createRoute(uid))
                            }
                        }
                        else -> onNavigate(item.route)
                    }
                },
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