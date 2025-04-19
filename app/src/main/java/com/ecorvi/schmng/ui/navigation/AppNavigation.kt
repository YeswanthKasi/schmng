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
import com.ecorvi.schmng.ui.screens.attendance.StudentAttendanceScreen
import com.ecorvi.schmng.ui.utils.getCurrentDate

@Composable
fun AppNavigation(
    navController: NavHostController,
    isUserLoggedIn: Boolean,
    isFirstLaunch: Boolean
) {
    val startDestination = when {
        isFirstLaunch -> "welcome"
        isUserLoggedIn -> "adminDashboard"
        else -> "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("welcome") { WelcomeScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("adminDashboard") { AdminDashboardScreen(navController) }
        composable("students") { StudentsScreen(navController) }
        composable("teachers") { TeachersScreen(navController) }
        composable("add_student") { AddPersonScreen(navController, "student") }
        composable("add_teacher") { AddPersonScreen(navController, "teacher") }
        composable("student_attendance") {
            var students by remember { mutableStateOf<List<Person>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                FirestoreDatabase.listenForStudentUpdates(
                    onUpdate = { 
                        students = it
                        isLoading = false
                    },
                    onError = { 
                        Log.e("StudentAttendance", "Error loading students: ${it.message}")
                        isLoading = false
                    }
                )
            }

            StudentAttendanceScreen(
                students = students,
                isLoading = isLoading,
                onSubmit = { updatedStudents ->
                    updatedStudents.forEach { student ->
                        FirestoreDatabase.addAttendanceRecord(
                            student.id,
                            student.attendance.name,
                            getCurrentDate(),
                            onSuccess = {
                                Log.d("Attendance", "Recorded attendance for student: ${student.id}")
                            },
                            onFailure = { e ->
                                Log.e("Attendance", "Failed to record attendance: ${e.message}")
                            }
                        )
                    }
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = "profile/{personId}/{personType}",
            arguments = listOf(
                navArgument("personId") { type = NavType.StringType },
                navArgument("personType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getString("personId") ?: ""
            val personType = backStackEntry.arguments?.getString("personType") ?: ""
            ProfileScreen(navController, personId, personType)
        }
        composable("schedules") { SchedulesScreen(navController) }
        composable("add_schedule") { AddScheduleScreen(navController) }
        composable("pending_fees") { PendingFeesScreen(navController) }
        composable("add_fee") { AddFeeScreen(navController) }
    }
}
