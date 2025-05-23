package com.ecorvi.schmng.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class StudentBottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : StudentBottomNavItem(
        route = "student_dashboard",
        title = "Home",
        icon = Icons.Default.Home
    )
    object Schedule : StudentBottomNavItem(
        route = "student_schedule",
        title = "Schedule",
        icon = Icons.Default.Schedule
    )
    object Attendance : StudentBottomNavItem(
        route = "student_attendance",
        title = "Attendance",
        icon = Icons.Default.CheckCircleOutline
    )
    object Notices : StudentBottomNavItem(
        route = "student_announcements",
        title = "Notices",
        icon = Icons.Default.Announcement
    )
    object Profile : StudentBottomNavItem(
        route = "student_profile",
        title = "Profile",
        icon = Icons.Default.Person
    )
} 