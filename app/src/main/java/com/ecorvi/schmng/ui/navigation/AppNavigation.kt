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
import com.ecorvi.schmng.models.UserType
import com.ecorvi.schmng.ui.screens.*
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.data.model.Person
import com.ecorvi.schmng.ui.utils.getCurrentDate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import com.ecorvi.schmng.ui.screens.StudentProfileScreen
import com.ecorvi.schmng.ui.screens.TeacherProfileScreen
import com.ecorvi.schmng.ui.screens.StudentAttendanceScreen
import com.ecorvi.schmng.ui.screens.admin.AdminNoticeScreen
import com.ecorvi.schmng.ui.screens.teacher.TeacherAttendanceScreen
import com.ecorvi.schmng.ui.screens.teacher.TeacherDashboardScreen
import com.ecorvi.schmng.ui.screens.teacher.TeacherNoticeCreateScreen
import com.ecorvi.schmng.ui.screens.teacher.TeacherNoticeListScreen
import com.ecorvi.schmng.ui.screens.teacher.TeacherStudentsScreen
import com.ecorvi.schmng.viewmodels.TeacherStudentsViewModel
import com.ecorvi.schmng.viewmodels.AttendanceViewModel
import com.ecorvi.schmng.ui.screens.teacher.TeacherLeaveListScreen
import com.ecorvi.schmng.ui.screens.teacher.TeacherLeaveApplyScreen
import com.ecorvi.schmng.ui.screens.teacher.TeacherLeaveDetailsScreen
import com.ecorvi.schmng.ui.screens.admin.AdminLeaveListScreen
import com.ecorvi.schmng.ui.screens.admin.AdminLeaveDetailsScreen

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
        if (currentRoute != route) {
            currentRoute = route
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
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

        // Admin Screens with Bottom Navigation
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

        // Student Bottom Navigation Screens
        composable(StudentBottomNavItem.Home.route) { 
            StudentDashboardScreen(
                navController = navController,
                currentRoute = currentRoute,
                onRouteSelected = onRouteSelected
            )
        }

        composable(StudentBottomNavItem.Schedule.route) {
            StudentScheduleScreen(
                navController = navController,
                currentRoute = currentRoute,
                onRouteSelected = onRouteSelected
            )
        }

        composable(StudentBottomNavItem.Attendance.route) {
            StudentAttendanceScreen(
                navController = navController,
                currentRoute = StudentBottomNavItem.Attendance.route,
                onRouteSelected = { route -> onRouteSelected(route) }
            )
        }

        composable(StudentBottomNavItem.Notices.route) {
            NoticeListScreen(
                navController = navController,
                currentRoute = currentRoute,
                onRouteSelected = onRouteSelected
            )
        }

        composable(StudentBottomNavItem.Profile.route) {
            StudentProfileScreen(
                navController = navController,
                studentId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                isAdmin = false,
                currentRoute = currentRoute,
                onRouteSelected = onRouteSelected
            )
        }

        // Other student screens (non-bottom nav)
        composable("student_timetable") { 
            StudentTimetableScreen(navController) 
        }

        composable("student_teacher_info") { 
            StudentTeacherInfoScreen(navController) 
        }

        composable("student_messages") { 
            StudentMessagesScreen(navController) 
        }

        composable("student_new_message") { 
            StudentNewMessageScreen(navController) 
        }

        // Admin Message Routes
        composable("new_message") { NewMessageScreen(navController) }
        composable(
            "chat/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            ChatScreen(
                navController = navController,
                chatId = chatId
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
        composable("pending_fees") { PendingFeesScreen(navController) }
        
        // Schedule Management Routes
        composable("schedules") { SchedulesScreen(navController) }
        composable("add_schedule") { AddScheduleScreen(navController) }
        composable("view_schedule") { ViewScheduleScreen(navController) }
        composable(
            route = "edit_schedule/{scheduleId}",
            arguments = listOf(navArgument("scheduleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getString("scheduleId")
            EditScheduleScreen(navController, scheduleId)
        }
        
        // Timetable Management Routes
        composable("timetable_management") { AdminTimetableScreen(navController) }
        composable("view_timetable") { ViewTimetableScreen(navController) }
        composable("teacher_timetable") { TeacherTimetableScreen(navController) }
        composable("add_timetable") { AddTimetableScreen(navController) }
        composable("edit_timetable/{timetableId}") { backStackEntry ->
            val timetableId = backStackEntry.arguments?.getString("timetableId")
            AddTimetableScreen(navController, timetableId)
        }
        
        // Student-specific Routes
        composable("student_fees") { StudentFeesScreen(navController) }
        
        // Attendance Routes
        composable("attendance") {
            AttendanceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("attendance_analytics") {
            AttendanceAnalyticsScreen(navController = navController)
        }
        composable("manage_teacher_absences") {
            ManageTeacherAbsencesScreen(navController = navController)
        }
        composable(
            "attendance/{userType}",
            arguments = listOf(navArgument("userType") { type = NavType.StringType })
        ) { backStackEntry ->
            val userType = backStackEntry.arguments?.getString("userType") ?: "STUDENT"
            AttendanceScreen(
                onNavigateBack = { navController.popBackStack() },
                initialUserType = UserType.valueOf(userType)
            )
        }
        
        // Profile routes with multiple path support
        composable(
            route = "view_profile/student/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            val currentUser = FirebaseAuth.getInstance().currentUser
            var isAdmin by remember { mutableStateOf(false) }

            LaunchedEffect(currentUser?.uid) {
                if (currentUser?.uid != null) {
                    FirestoreDatabase.getUserRole(
                        userId = currentUser.uid,
                        onComplete = { role ->
                            isAdmin = role?.lowercase() == "admin"
                        },
                        onFailure = { e ->
                            isAdmin = false
                        }
                    )
                }
            }

            StudentProfileScreen(
                navController = navController,
                studentId = studentId,
                isAdmin = isAdmin
            )
        }

        composable(
            route = "student_profile/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            val currentUser = FirebaseAuth.getInstance().currentUser
            var isAdmin by remember { mutableStateOf(false) }

            LaunchedEffect(currentUser?.uid) {
                if (currentUser?.uid != null) {
                    FirestoreDatabase.getUserRole(
                        userId = currentUser.uid,
                        onComplete = { role ->
                            isAdmin = role?.lowercase() == "admin"
                        },
                        onFailure = { e ->
                            isAdmin = false
                        }
                    )
                }
            }

            StudentProfileScreen(
                navController = navController,
                studentId = studentId,
                isAdmin = isAdmin
            )
        }

        composable(
            route = "teacher_profile/{teacherId}",
            arguments = listOf(navArgument("teacherId") { type = NavType.StringType })
        ) { backStackEntry ->
            val teacherId = backStackEntry.arguments?.getString("teacherId") ?: ""
            val currentUser = FirebaseAuth.getInstance().currentUser
            var isAdmin by remember { mutableStateOf(false) }

            LaunchedEffect(currentUser?.uid) {
                if (currentUser?.uid != null) {
                    FirestoreDatabase.getUserRole(
                        userId = currentUser.uid,
                        onComplete = { role ->
                            isAdmin = role?.lowercase() == "admin"
                        },
                        onFailure = { e ->
                            isAdmin = false
                        }
                    )
                }
            }

            TeacherProfileScreen(
                navController = navController,
                teacherId = teacherId,
                isAdmin = isAdmin
            )
        }

        // Fee Management Routes
        composable("pending_fees") { PendingFeesScreen(navController) }
        composable("add_fee") { AddFeeScreen(navController) }
        
        // Staff routes
        composable("staff") { StaffScreen(navController) }
        composable(
            route = "staff_profile/{staffId}",
            arguments = listOf(navArgument("staffId") { type = NavType.StringType })
        ) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getString("staffId") ?: ""
            val currentUser = FirebaseAuth.getInstance().currentUser
            var isAdmin by remember { mutableStateOf(false) }

            LaunchedEffect(currentUser?.uid) {
                if (currentUser?.uid != null) {
                    FirestoreDatabase.getUserRole(
                        userId = currentUser.uid,
                        onComplete = { role ->
                            isAdmin = role?.lowercase() == "admin"
                        },
                        onFailure = { e ->
                            // Handle error silently, defaulting to non-admin
                            isAdmin = false
                        }
                    )
                }
            }

            StaffProfileScreen(
                navController = navController,
                staffId = staffId,
                isAdmin = isAdmin
            )
        }

        // Add/Edit Person routes
        composable(
            route = "add_person/{personType}?personId={personId}",
            arguments = listOf(
                navArgument("personType") { 
                    type = NavType.StringType 
                },
                navArgument("personId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val personType = backStackEntry.arguments?.getString("personType") ?: ""
            val personId = backStackEntry.arguments?.getString("personId")
            
            when (personType) {
                "staff" -> AddStaffScreen(navController, personId)
                else -> AddPersonScreen(
                    navController = navController,
                    personType = personType,
                    personId = personId
                )
            }
        }

        // Teacher Routes
        composable("teacher_dashboard") {
            TeacherDashboardScreen(
                navController = navController,
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            )
        }

        composable("teacher_attendance") {
            TeacherAttendanceScreen(
                navController = navController,
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel<AttendanceViewModel>()
            )
        }

        composable("teacher_profile") {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            TeacherProfileScreen(
                navController = navController,
                teacherId = currentUserId,
                isAdmin = false
            )
        }

        // Admin routes
        composable("admin_notices") {
            AdminNoticeScreen(navController = navController)
        }

        // Teacher notice routes
        composable("teacher_notice_list") {
            TeacherNoticeListScreen(navController = navController)
        }
        composable("teacher_notice_create") {
            TeacherNoticeCreateScreen(navController = navController)
        }
        composable(
            "teacher_notice_create/{noticeId}",
            arguments = listOf(navArgument("noticeId") { type = NavType.StringType })
        ) { backStackEntry ->
            TeacherNoticeCreateScreen(
                navController = navController,
                noticeId = backStackEntry.arguments?.getString("noticeId")
            )
        }

        composable("teacher_students") {
            TeacherStudentsScreen(
                navController = navController,
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel<TeacherStudentsViewModel>()
            )
        }

        // Teacher leave routes
        composable("teacher_leave_list") {
            TeacherLeaveListScreen(navController = navController)
        }
        composable("teacher_leave_apply") {
            TeacherLeaveApplyScreen(navController = navController)
        }
        composable(
            "teacher_leave_details/{leaveId}",
            arguments = listOf(navArgument("leaveId") { type = NavType.StringType })
        ) { backStackEntry ->
            TeacherLeaveDetailsScreen(
                navController = navController,
                leaveId = backStackEntry.arguments?.getString("leaveId") ?: ""
            )
        }
        composable("admin_leave_list") {
            AdminLeaveListScreen(navController = navController)
        }
        composable(
            "admin_leave_details/{leaveId}",
            arguments = listOf(navArgument("leaveId") { type = NavType.StringType })
        ) { backStackEntry ->
            AdminLeaveDetailsScreen(
                navController = navController,
                leaveId = backStackEntry.arguments?.getString("leaveId") ?: ""
            )
        }
    }
}
