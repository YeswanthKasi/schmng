package com.ecorvi.schmng

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ecorvi.schmng.ui.navigation.AppNavigation
import com.ecorvi.schmng.ui.theme.SchmngTheme
import com.ecorvi.schmng.ui.components.CommonBackground
import com.google.android.play.core.appupdate.*
import com.google.android.play.core.install.model.*
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import java.util.*
import com.ecorvi.schmng.ui.data.FirestoreDatabase

class MainActivity : ComponentActivity() {
    private lateinit var appUpdateManager: AppUpdateManager
    private val MY_REQUEST_CODE = 100
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 200

    private var showUpdateDialog by mutableStateOf(false)
    private var isUpdating by mutableStateOf(false)

    private var isUserLoggedIn by mutableStateOf(false)
    private var isFirstLaunch by mutableStateOf(true)
    private var isLoading by mutableStateOf(true)
    private var initialRoute by mutableStateOf("login")

    private lateinit var navController: NavHostController

    private val PREFS_NAME = "app_prefs"
    private val KEY_USER_ROLE = "user_role"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase if not already initialized
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdates()

        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isFirstLaunch = sharedPrefs.getBoolean("is_first_launch", true)
        if (isFirstLaunch) {
            sharedPrefs.edit().putBoolean("is_first_launch", false).apply()
        }

        // Check initial authentication state
        val auth = FirebaseAuth.getInstance()
        isUserLoggedIn = auth.currentUser != null

        // Determine initial route
        if (isFirstLaunch) {
            initialRoute = "welcome"
            isLoading = false
        } else if (isUserLoggedIn) {
            val userId = auth.currentUser?.uid
            val cachedRole = sharedPrefs.getString(KEY_USER_ROLE, null)

            if (userId != null && cachedRole != null) {
                // Role found in cache, use it immediately
                initialRoute = getRouteFromRole(cachedRole)
                isLoading = false
                // Optional: Fetch role in background to verify/update cache if needed
                // verifyRoleInBackground(userId, sharedPrefs)
            } else if (userId != null) {
                // Role not cached, fetch from Firestore
                FirebaseFirestore.getInstance().collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        val role = document.getString("role")?.lowercase(Locale.ROOT)
                        if (role != null) {
                            // Save fetched role to cache
                            sharedPrefs.edit().putString(KEY_USER_ROLE, role).apply()
                            initialRoute = getRouteFromRole(role)
                        } else {
                            initialRoute = "login" // Fallback if role is null
                            sharedPrefs.edit().remove(KEY_USER_ROLE).apply() // Clear potentially invalid cache
                        }
                        isLoading = false
                    }
                    .addOnFailureListener {
                        initialRoute = "login" // Fallback on failure
                        isLoading = false
                        sharedPrefs.edit().remove(KEY_USER_ROLE).apply() // Clear cache on failure
                    }
            } else {
                // User ID is null, should not happen if logged in, but handle defensively
                initialRoute = "login"
                isLoading = false
                sharedPrefs.edit().remove(KEY_USER_ROLE).apply() // Clear cache
            }
        } else {
            // User not logged in
            initialRoute = "login"
            isLoading = false
            sharedPrefs.edit().remove(KEY_USER_ROLE).apply() // Clear cache
        }

        setContent {
            SchmngTheme {
                navController = rememberNavController()
                
                // Only compose AppNavigation once the initial route is determined
                if (!isLoading) {
                    AppNavigation(
                        navController = navController,
                        isUserLoggedIn = isUserLoggedIn,
                        isFirstLaunch = isFirstLaunch,
                        initialRoute = initialRoute
                    )
                } else {
                    // Show the CommonBackground while determining the initial route
                    CommonBackground { 
                        // Display nothing inside the background during this phase
                        Box(modifier = Modifier.fillMaxSize()) 
                    }
                }
            }
        }

        // Set up auth state listener - IMPORTANT for clearing cache on logout
        auth.addAuthStateListener { firebaseAuth ->
            val loggedIn = firebaseAuth.currentUser != null
            if (!loggedIn && isUserLoggedIn) { // User just logged out
                // Clear the cached role when the user logs out
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(KEY_USER_ROLE)
                    .apply()
                // Optional: Navigate to login screen immediately if needed
                // if (::navController.isInitialized) { 
                //     navController.navigate("login") { popUpTo(0) { inclusive = true } } 
                // }
            }
            isUserLoggedIn = loggedIn
        }
    }

    private fun navigateToDashboard(userId: String) {
        try {
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val role = document.getString("role")?.lowercase(Locale.ROOT)
                        when (role) {
                            "admin" -> {
                                navController.navigate("admin_dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                            "student" -> {
                                navController.navigate("student_dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                            "parent" -> {
                                navController.navigate("parent_dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                            else -> {
                                Toast.makeText(this, "Unknown user role", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(this, "Failed to fetch user role", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            isLoading = false
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkForUpdates() {
        val installer = applicationContext.packageManager.getInstallerPackageName(packageName)
        if (installer != "com.android.vending") {
            Toast.makeText(this, "Updates disabled: App not installed from Play Store.", Toast.LENGTH_LONG).show()
            return
        }

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            when {
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            MY_REQUEST_CODE
                        )
                    } catch (e: Exception) {
                        Toast.makeText(this, "Immediate update error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    try {
                        isUpdating = true
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            this,
                            MY_REQUEST_CODE
                        )
                        appUpdateManager.registerListener(installStateListener)
                    } catch (e: Exception) {
                        isUpdating = false
                        Toast.makeText(this, "Flexible update error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Update check failed: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> isUpdating = true
            InstallStatus.DOWNLOADED -> {
                isUpdating = false
                showUpdateDialog = true
            }
            InstallStatus.INSTALLED -> {
                isUpdating = false
                Toast.makeText(this, "Update installed!", Toast.LENGTH_SHORT).show()
            }
            InstallStatus.FAILED -> {
                isUpdating = false
                Toast.makeText(this, "Update failed!", Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                showUpdateDialog = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_REQUEST_CODE && resultCode != RESULT_OK) {
            isUpdating = false
            Toast.makeText(this, "Update canceled. Trying again.", Toast.LENGTH_SHORT).show()
            checkForUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateListener)
        FirestoreDatabase.cleanup()
    }

    // ✅ NOW PUBLIC
    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper function to map role to route
    private fun getRouteFromRole(role: String): String {
        return when (role) {
            "admin" -> "admin_dashboard"
            "student" -> "student_dashboard"
            "parent" -> "parent_dashboard" // Add other roles if needed
            else -> "login" // Fallback for unknown roles
        }
    }

    // Optional: Background verification function (Example)
    /*
    private fun verifyRoleInBackground(userId: String, prefs: SharedPreferences) {
        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val freshRole = document.getString("role")?.lowercase(Locale.ROOT)
                val cachedRole = prefs.getString(KEY_USER_ROLE, null)
                if (freshRole != null && freshRole != cachedRole) {
                    prefs.edit().putString(KEY_USER_ROLE, freshRole).apply()
                    // Optional: Force navigation if role changed drastically?
                    // Or just update cache for next time.
                } else if (freshRole == null) {
                     prefs.edit().remove(KEY_USER_ROLE).apply()
                }
            }
            // No need to handle failure explicitly for background check
            // unless you want to log it.
    }
    */
}
