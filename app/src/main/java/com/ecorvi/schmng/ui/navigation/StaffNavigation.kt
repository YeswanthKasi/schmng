package com.ecorvi.schmng.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.ecorvi.schmng.models.User
import com.ecorvi.schmng.ui.screens.staff.*
import com.ecorvi.schmng.ui.data.FirestoreDatabase

object StaffRoutes {
    const val DASHBOARD = "staff_dashboard"
    const val PROFILE = "staff_profile/{staffId}"
    const val ATTENDANCE = "staff_attendance"
    const val MESSAGES = "staff_messages"
    const val CHAT = "chat/{chatId}"
    const val LEAVE_APPLICATION = "staff_leave_application"
    const val LEAVE_HISTORY = "staff_leave_history"
}

@Composable
fun StaffNavigation(
    navController: NavHostController,
    onSignOut: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = StaffRoutes.DASHBOARD
    ) {
        composable(StaffRoutes.DASHBOARD) { entry ->
            StaffDashboardScreen(
                navController = navController,
                currentRoute = entry.destination.route,
                onSignOut = onSignOut
            )
        }

        composable(
            route = StaffRoutes.PROFILE,
            arguments = listOf(
                navArgument("staffId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            StaffProfileScreen(
                navController = navController,
                currentRoute = entry.destination.route,
                staffId = entry.arguments?.getString("staffId") ?: ""
            )
        }

        composable(StaffRoutes.ATTENDANCE) { entry ->
            StaffAttendanceScreen(
                navController = navController
            )
        }

        composable(StaffRoutes.MESSAGES) { entry ->
            StaffMessagesScreen(
                navController = navController,
                currentRoute = entry.destination.route
            )
        }

        composable(
            route = StaffRoutes.CHAT,
            arguments = listOf(
                navArgument("chatId") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            StaffChatScreen(
                navController = navController,
                chatId = entry.arguments?.getString("chatId") ?: ""
            )
        }

        composable(StaffRoutes.LEAVE_APPLICATION) { entry ->
            StaffLeaveApplicationScreen(
                navController = navController
            )
        }

        composable(StaffRoutes.LEAVE_HISTORY) { entry ->
            StaffLeaveHistoryScreen(
                navController = navController,
                currentRoute = entry.destination.route
            )
        }
    }
} 