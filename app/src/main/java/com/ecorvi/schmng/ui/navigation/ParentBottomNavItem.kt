package com.ecorvi.schmng.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class ParentBottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : ParentBottomNavItem(
        route = "parent_dashboard",
        title = "Home",
        icon = Icons.Default.Home
    )
    object Attendance : ParentBottomNavItem(
        route = "parent_attendance",
        title = "Attendance",
        icon = Icons.Default.CheckCircleOutline
    )
    object Events : ParentBottomNavItem(
        route = "parent_events",
        title = "Events",
        icon = Icons.Default.Event
    )
    object Notices : ParentBottomNavItem(
        route = "parent_notices",
        title = "Notices",
        icon = Icons.AutoMirrored.Filled.Announcement
    )
    object Messages : ParentBottomNavItem(
        route = "parent_messages",
        title = "Messages",
        icon = Icons.AutoMirrored.Filled.Message
    )
} 