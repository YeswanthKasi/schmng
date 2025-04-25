package com.ecorvi.schmng.ui.navigation


import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    NavHost(
        navController = navController,
        startDestination = initialRoute
    ) {
        composable("welcome") { WelcomeScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("admin_dashboard") { AdminDashboardScreen(navController) }
        composable("student_dashboard") { StudentDashboardScreen(navController) }
        composable("students") { StudentsScreen(navController) }
        composable("teachers") { TeachersScreen(navController) }
        composable("schedules") { SchedulesScreen(navController) }
        composable("pending_fees") { PendingFeesScreen(navController) }
        composable("add_student") { AddPersonScreen(navController, "student") }
        composable("add_teacher") { AddPersonScreen(navController, "teacher") }
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
                navArgument("timetableId") { 
                    type = NavType.StringType 
                }
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
        composable("student_timetable") {
            StudentTimetableScreen(navController)
        }
        
        // Add edit routes
        composable(
            route = "add_student/{personId}",
            arguments = listOf(
                navArgument("personId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getString("personId") ?: ""
            AddPersonScreen(navController, "student", personId)
        }
        
        composable(
            route = "add_teacher/{personId}",
            arguments = listOf(
                navArgument("personId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getString("personId") ?: ""
            AddPersonScreen(navController, "teacher", personId)
        }

        // Add view profile routes
        composable(
            route = "view_profile/{type}/{id}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: ""
            val id = backStackEntry.arguments?.getString("id") ?: ""
            ProfileScreen(navController, id, type)
        }
    }
}
