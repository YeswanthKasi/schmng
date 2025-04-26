package com.ecorvi.schmng

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.*
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var prefs: SharedPreferences
    private lateinit var navController: NavHostController
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var showUpdateDialog by mutableStateOf(false)
    private var isUpdating by mutableStateOf(false)
    private var isUserLoggedIn by mutableStateOf(false)
    private var isFirstLaunch by mutableStateOf(true)
    private var isLoading by mutableStateOf(true)
    private var initialRoute by mutableStateOf("login")

    companion object {
        private const val MY_REQUEST_CODE = 100
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 200
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_STAY_SIGNED_IN = "stay_signed_in"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            initializeComponents()
            setupContent()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showErrorDialog("Failed to initialize app")
        }
    }

    private fun initializeComponents() {
        try {
            // Initialize SharedPreferences
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // Initialize Firebase
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }

            // Initialize Firebase components
            firebaseAnalytics = FirebaseAnalytics.getInstance(this)

            // Initialize app update manager
            appUpdateManager = AppUpdateManagerFactory.create(this)

            // Setup initial state and auth listener
            setupInitialState()
            setupAuthStateListener()

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing components", e)
            throw e
        }
    }

    private fun setupContent() {
        setContent {
            SchmngTheme {
                navController = rememberNavController()
                
                if (!isLoading) {
                    AppNavigation(
                        navController = navController,
                        isUserLoggedIn = isUserLoggedIn,
                        isFirstLaunch = isFirstLaunch,
                        initialRoute = initialRoute
                    )
                } else {
                    CommonBackground { 
                        Box(modifier = Modifier.fillMaxSize()) 
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupInitialState() {
        try {
            isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
            if (isFirstLaunch) {
                prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
            }

            val auth = FirebaseAuth.getInstance()
            val staySignedIn = prefs.getBoolean(KEY_STAY_SIGNED_IN, false)
            isUserLoggedIn = auth.currentUser != null && staySignedIn

            if (!staySignedIn && auth.currentUser != null) {
                auth.signOut()
                clearUserData()
                isUserLoggedIn = false
            }

            determineInitialRoute(auth)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up initial state", e)
            resetToLogin()
        }
    }

    private fun determineInitialRoute(auth: FirebaseAuth) {
        try {
            when {
                isFirstLaunch -> {
                    initialRoute = "welcome"
                    isLoading = false
                }
                isUserLoggedIn -> {
                    val userId = auth.currentUser?.uid
                    val cachedRole = prefs.getString(KEY_USER_ROLE, null)

                    if (userId != null && cachedRole != null) {
                        initialRoute = getRouteFromRole(cachedRole)
                        isLoading = false
                    } else if (userId != null) {
                        fetchUserRole(userId)
                    } else {
                        resetToLogin()
                    }
                }
                else -> resetToLogin()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error determining initial route", e)
            resetToLogin()
        }
    }

    private fun setupAuthStateListener() {
        try {
            FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth ->
                val loggedIn = firebaseAuth.currentUser != null
                if (!loggedIn && isUserLoggedIn) {
                    clearUserData()
                }
                isUserLoggedIn = loggedIn
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up auth state listener", e)
        }
    }

    private fun clearUserData() {
        try {
            prefs.edit().apply {
                remove(KEY_USER_ROLE)
                remove(KEY_STAY_SIGNED_IN)
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing user data", e)
        }
    }

    private fun resetToLogin() {
        initialRoute = "login"
        clearUserData()
        isLoading = false
    }

    private fun getRouteFromRole(role: String): String {
        return when (role.lowercase(Locale.ROOT)) {
            "admin" -> "admin_dashboard"
            "student" -> "student_dashboard"
            "parent" -> "parent_dashboard"
            else -> "login"
        }
    }

    private fun fetchUserRole(userId: String) {
        try {
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        val role = document.getString("role")?.lowercase(Locale.ROOT)
                        if (role != null) {
                            prefs.edit().putString(KEY_USER_ROLE, role).apply()
                            initialRoute = getRouteFromRole(role)
                        } else {
                            resetToLogin()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing user role", e)
                        resetToLogin()
                    } finally {
                        isLoading = false
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching user role", e)
                    resetToLogin()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchUserRole", e)
            resetToLogin()
        }
    }

    private fun showErrorDialog(message: String) {
        try {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing error dialog", e)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            scope.cancel()
            appUpdateManager.unregisterListener(installStateListener)
            FirestoreDatabase.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }

    private val installStateListener = InstallStateUpdatedListener { state ->
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in install state listener", e)
        }
    }
}
