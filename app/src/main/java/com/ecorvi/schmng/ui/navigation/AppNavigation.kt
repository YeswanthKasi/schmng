package com.ecorvi.schmng.ui.navigation

import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ecorvi.schmng.ui.screens.*
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.utils.getCurrentDate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@Composable
fun AppNavigation(
    navController: NavHostController,
    isUserLoggedIn: Boolean,
    isFirstLaunch: Boolean,
    initialRoute: String
) {
    // Remember current route for bottom navigation
    var currentRoute by remember { mutableStateOf(initialRoute) }
    
    // Update currentRoute when navigation changes
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            currentRoute = backStackEntry.destination.route ?: initialRoute
        }
    }
    
    // Create a function to handle route selection
    val onRouteSelected: (String) -> Unit = { route ->
        currentRoute = route
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = initialRoute
    ) {
        // Authentication
        composable("welcome") { WelcomeScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }

        // Main Screens with Bottom Navigation
        composable(BottomNavItem.Home.route) { 
            AdminDashboardScreen(
                navController = navController,
                currentRoute = currentRoute,
                onRouteSelected = onRouteSelected
            )
        }
        
        composable(BottomNavItem.Profile.route) { 
            ProfileScreen(
                navController = navController,
                currentRoute = currentRoute,
                onRouteSelected = onRouteSelected
            )
        }
        
        composable(BottomNavItem.Notifications.route) { 
            NotificationsScreen(
                navController = navController,
                currentRoute = currentRoute,
                onRouteSelected = onRouteSelected
            )
        }
        
        composable(BottomNavItem.Messages.route) { 
            MessagesScreen(
                navController = navController,
                currentRoute = currentRoute,
                onRouteSelected = onRouteSelected
            )
        }

        // Other screens without bottom navigation
        composable("admin_profile") { AdminProfileScreen(navController) }
        composable("student_dashboard") { StudentDashboardScreen(navController) }
        composable("new_message") { NewMessageScreen(navController) }
        composable(
            "chat/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            ChatScreen(
                navController = navController,
                otherUserId = backStackEntry.arguments?.getString("userId") ?: ""
            )
        }
        composable("schedules") { SchedulesScreen(navController) }
        composable(
            "fee_analytics/{monthIndex}",
            arguments = listOf(navArgument("monthIndex") { type = NavType.IntType })
        ) { backStackEntry ->
            FeeAnalyticsDetailScreen(
                navController = navController,
                monthIndex = backStackEntry.arguments?.getInt("monthIndex") ?: 0
            )
        }

        // Management Screens
        composable("students") { StudentsScreen(navController) }
        composable("teachers") { TeachersScreen(navController) }
        composable("staff") { StaffScreen(navController) }
        composable("schedules") { SchedulesScreen(navController) }
        composable("pending_fees") { PendingFeesScreen(navController) }
        composable("add_student") { AddPersonScreen(navController, "student") }
        composable("add_teacher") { AddPersonScreen(navController, "teacher") }
        composable("add_staff") { AddStaffScreen(navController) }
        composable("add_schedule") { AddScheduleScreen(navController) }
        composable("add_fee") { AddFeeScreen(navController) }
        
        // Add timetable management routes
        composable("timetable_management") { TimetableManagementScreen(navController) }
        composable("view_timetable") { ViewTimetableScreen(navController) }
        composable("add_timetable") { 
            AddTimetableScreen(navController, null)
        }
        composable(
            route = "add_timetable/{timetableId}",
            arguments = listOf(
                navArgument("timetableId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val timetableId = backStackEntry.arguments?.getString("timetableId")
            AddTimetableScreen(navController, timetableId)
        }
        
        // Add student-specific routes
        composable("student_schedule") { StudentScheduleScreen(navController) }
        composable("student_fees") { StudentFeesScreen(navController) }
        composable("student_announcements") { AnnouncementsScreen(navController) }
        composable("student_teacher_info") { ClassTeacherInfoScreen(navController) }
        composable("student_timetable") { StudentTimetableScreen(navController) }
        
        // Detail routes with bottom navigation
        composable(
            route = "student_detail/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            ProfileScreen(
                navController = navController,
                currentRoute = currentRoute,
                onRouteSelected = onRouteSelected,
                id = studentId,
                type = "student"
            )
        }
        
        composable(
            route = "teacher_detail/{teacherId}",
            arguments = listOf(navArgument("teacherId") { type = NavType.StringType })
        ) { backStackEntry ->
            val teacherId = backStackEntry.arguments?.getString("teacherId") ?: ""
            ProfileScreen(
                navController = navController,
                currentRoute = currentRoute,
                onRouteSelected = onRouteSelected,
                id = teacherId,
                type = "teacher"
            )
        }
        
        composable(
            route = "staff_details/{staffId}",
            arguments = listOf(navArgument("staffId") { type = NavType.StringType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getString("staffId") ?: ""
            ProfileScreen(
                navController = navController,
                currentRoute = currentRoute,
                onRouteSelected = onRouteSelected,
                id = staffId,
                type = "staff"
            )
        }
    }
}
