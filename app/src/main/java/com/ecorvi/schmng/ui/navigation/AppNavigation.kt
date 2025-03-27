package com.ecorvi.schmng.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ecorvi.schmng.ui.screens.*
import com.google.firebase.auth.FirebaseAuth
import android.content.Context

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    // Use SharedPreferences to track if this is the first launch
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val isFirstLaunch = sharedPreferences.getBoolean("is_first_launch", true)

    // Determine the start destination
    val startDestination = if (isFirstLaunch) {
        // Mark that the app has been launched
        sharedPreferences.edit().putBoolean("is_first_launch", false).apply()
        "welcome"
    } else {
        // If not the first launch, check the user's login state
        if (auth.currentUser != null) "adminDashboard" else "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier,
        builder = {
            composable("welcome") { // Changed route name from "Get Started" to "welcome"
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
            composable(
                route = "profile/{personId}/{personType}",
                arguments = listOf(
                    navArgument("personId") { type = NavType.IntType },
                    navArgument("personType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getInt("personId") ?: 0
                val personType = backStackEntry.arguments?.getString("personType") ?: "student"
                ProfileScreen(navController, personId, personType)
            }
        }
    )
}