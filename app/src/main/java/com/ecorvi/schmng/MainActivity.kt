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
import com.google.android.play.core.ktx.AppUpdateResult
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

    private var showUpdateDialog: Boolean by mutableStateOf(false)
    private var isUpdating: Boolean by mutableStateOf(false)
    private var isUserLoggedIn: Boolean by mutableStateOf(false)
    private var isFirstLaunch: Boolean by mutableStateOf(true)
    private var isLoading: Boolean by mutableStateOf(true)
    private var initialRoute: String by mutableStateOf("login")
    private var updateInProgress: Boolean = false

    companion object {
        private const val MY_REQUEST_CODE = 100
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 200
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_STAY_SIGNED_IN = "stay_signed_in"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_UPDATE = 500
    }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        try {
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    if (!updateInProgress) return@InstallStateUpdatedListener
                    val bytesDownloaded = state.bytesDownloaded()
                    val totalBytesToDownload = state.totalBytesToDownload()
                    val progress = (bytesDownloaded * 100) / totalBytesToDownload
                    Log.d(TAG, "Update download progress: $progress%")
                    showToast("Downloading update: $progress%")
                }
                InstallStatus.DOWNLOADED -> {
                    if (!updateInProgress) return@InstallStateUpdatedListener
                    showUpdateDownloadedDialog()
                }
                InstallStatus.FAILED -> {
                    Log.e(TAG, "Update download failed: ${state.installErrorCode()}")
                    cleanupUpdateResources()
                    showToast("Update download failed, please try again later")
                }
                InstallStatus.CANCELED -> {
                    Log.d(TAG, "Update cancelled")
                    cleanupUpdateResources()
                }
                InstallStatus.INSTALLED -> {
                    Log.d(TAG, "Update installed successfully")
                    cleanupUpdateResources()
                    showToast("Update installed successfully")
                }
                InstallStatus.PENDING -> {
                    Log.d(TAG, "Update is pending")
                }
                else -> {
                    Log.d(TAG, "Update status: ${state.installStatus()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in install state listener", e)
            cleanupUpdateResources()
        }
    }

    private fun cleanupUpdateResources() {
        try {
            updateInProgress = false
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up update resources", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            initializeComponents()
            setupContent()
            checkForAppUpdate()
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

    private fun checkForAppUpdate() {
        if (updateInProgress) return
        
        try {
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo
            appUpdateInfoTask
                .addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                    ) {
                        try {
                            updateInProgress = true
                            appUpdateManager.registerListener(installStateUpdatedListener)
                            
                            // Show confirmation dialog before starting update
                            showUpdateAvailableDialog(appUpdateInfo)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error starting update flow", e)
                            cleanupUpdateResources()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking for update", e)
                    cleanupUpdateResources()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkForAppUpdate", e)
            cleanupUpdateResources()
        }
    }

    private fun showUpdateAvailableDialog(appUpdateInfo: AppUpdateInfo) {
        try {
            AlertDialog.Builder(this)
                .setTitle("Update Available")
                .setMessage("A new version of the app is available. Would you like to update now?")
                .setPositiveButton("Update") { dialog, _ ->
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            this,
                            REQUEST_CODE_UPDATE
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting update flow", e)
                        cleanupUpdateResources()
                        showToast("Failed to start update")
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Later") { dialog, _ ->
                    cleanupUpdateResources()
                    dialog.dismiss()
                }
                .setOnCancelListener {
                    cleanupUpdateResources()
                }
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing update available dialog", e)
            cleanupUpdateResources()
        }
    }

    private fun showUpdateDownloadedDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("Update Ready")
                .setMessage("An update has been downloaded. Would you like to install it now?")
                .setPositiveButton("Install") { dialog, _ ->
                    try {
                        appUpdateManager.completeUpdate()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error completing update", e)
                        showToast("Failed to install update. Please try again later.")
                        cleanupUpdateResources()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Later") { dialog, _ ->
                    dialog.dismiss()
                }
                .setOnCancelListener {
                    // Keep the update available for later
                }
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing update dialog", e)
            cleanupUpdateResources()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    when (appUpdateInfo.installStatus()) {
                        InstallStatus.DOWNLOADED -> {
                            if (updateInProgress) {
                                showUpdateDownloadedDialog()
                            }
                        }
                        InstallStatus.FAILED, InstallStatus.CANCELED -> {
                            cleanupUpdateResources()
                        }
                        else -> {
                            // No action needed
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking update status in onResume", e)
                    cleanupUpdateResources()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
            cleanupUpdateResources()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_UPDATE) {
            when (resultCode) {
                RESULT_OK -> {
                    Log.d(TAG, "Update flow started successfully")
                    // Keep updateInProgress true as the download is starting
                }
                RESULT_CANCELED -> {
                    Log.d(TAG, "Update flow cancelled")
                    cleanupUpdateResources()
                    showToast("Update cancelled")
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    Log.e(TAG, "Update flow failed")
                    cleanupUpdateResources()
                    showToast("Update failed, please try again later")
                }
                else -> {
                    Log.d(TAG, "Update flow failed with result code: $resultCode")
                    cleanupUpdateResources()
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            scope.cancel()
            cleanupUpdateResources()
            FirestoreDatabase.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }
}
