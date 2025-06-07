package com.ecorvi.schmng.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.ui.graphics.vector.ImageVector

sealed class StaffBottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : StaffBottomNavItem(
        route = "staff_dashboard",
        icon = Icons.Default.Home,
        label = "Home"
    )

    object Profile : StaffBottomNavItem(
        route = "staff_profile/{staffId}",
        icon = Icons.Default.Person,
        label = "Profile"
    ) {
        fun createRoute(staffId: String) = "staff_profile/$staffId"
    }
    
    object Attendance : StaffBottomNavItem(
        route = "staff_attendance",
        icon = Icons.Default.DateRange,
        label = "Attendance"
    )
    
    object Messages : StaffBottomNavItem(
        route = "staff_messages",
        icon = Icons.AutoMirrored.Filled.Message,
        label = "Messages"
    )

    companion object {
        fun values() = listOf(Home, Profile, Attendance, Messages)
    }
} 