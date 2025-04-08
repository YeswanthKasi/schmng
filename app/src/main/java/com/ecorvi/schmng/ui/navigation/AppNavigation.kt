package com.ecorvi.schmng.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ecorvi.schmng.ui.screens.*

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
        startDestination = startDestination,
        modifier = Modifier
    ) {
        composable("welcome") {
            WelcomeScreen(navController)
        }
        composable("login") {
            LoginScreen(navController)
        }
        composable("adminDashboard") {
            AdminDashboardScreen(navController)
        }
        composable("students") {
            StudentsScreen(navController)
        }
        composable("teachers") {
            TeachersScreen(navController)
        }
        composable("add_student") {
            AddPersonScreen(navController, personType = "student")
        }
        composable("add_teacher") {
            AddPersonScreen(navController, personType = "teacher")
        }
        composable(
            route = "profile/{personId}/{personType}",
            arguments = listOf(
                navArgument("personId") { type = NavType.StringType },
                navArgument("personType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getString("personId") ?: ""
            val personType = backStackEntry.arguments?.getString("personType") ?: "student"
            ProfileScreen(navController, personId, personType)
        }
    }
}
