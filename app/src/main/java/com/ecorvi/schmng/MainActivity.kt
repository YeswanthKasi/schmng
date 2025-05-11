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
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.*
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.google.android.play.core.install.model.ActivityResult
import kotlinx.coroutines.*
import android.net.Uri
import android.app.PendingIntent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import com.google.android.play.core.install.InstallState

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

    private var updateRetryCount = 0
    private var lastUpdateCheck = 0L
    private var isNetworkAvailable = true

    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            isNetworkAvailable = true
            // If we had a failed update due to network, retry
            if (updateRetryCount > 0 && updateRetryCount < MAX_UPDATE_RETRIES) {
                scope.launch {
                    delay(UPDATE_RETRY_DELAY)
                    checkForAppUpdate()
                }
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            isNetworkAvailable = false
            if (updateInProgress) {
                showToast("Network connection lost. Update will continue when connection is restored.")
            }
        }
    }

    companion object {
        private const val MY_REQUEST_CODE = 100
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 200
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_STAY_SIGNED_IN = "stay_signed_in"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_UPDATE = 500
        private const val MAX_UPDATE_RETRIES = 3
        private const val UPDATE_RETRY_DELAY = 5000L // 5 seconds
    }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        try {
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> handleDownloadStatus(state)
                InstallStatus.DOWNLOADED -> handleDownloadComplete()
                InstallStatus.FAILED -> handleInstallationError(state)
                InstallStatus.CANCELED -> handleInstallationCanceled()
                InstallStatus.INSTALLED -> handleInstallationSuccess()
                InstallStatus.PENDING -> handleInstallationPending()
                InstallStatus.INSTALLING -> handleInstalling()
                else -> Log.d(TAG, "Unhandled install status: ${state.installStatus()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in install state listener", e)
            handleUpdateError(e)
        }
    }

    private fun getUpdateErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            0 -> "Update failed: Unknown error"
            1 -> "Update failed: Update in progress"
            2 -> "Update failed: API not available"
            3 -> "Update failed: Invalid request"
            4 -> "Update failed: Download not found"
            5 -> "Update failed: Installation not allowed"
            6 -> "Update failed: Installation unavailable"
            7 -> "Update failed: Internal error"
            else -> "Update failed: Error code $errorCode"
        }
    }

    private fun cleanupUpdateResources() {
        try {
            updateInProgress = false
            try {
                appUpdateManager.unregisterListener(installStateUpdatedListener)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering update listener", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in cleanup", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            registerNetworkCallback()
            initializeComponents()
            setupContent()
            if (shouldCheckForUpdate()) {
                checkForAppUpdate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showErrorDialog("Failed to initialize app")
        }
    }

    private fun registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val networkRequest = NetworkRequest.Builder().build()
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Error registering network callback", e)
            }
        }
    }

    private fun shouldCheckForUpdate(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastCheck = currentTime - lastUpdateCheck
        // Check if it's been at least 4 hours since last check
        return timeSinceLastCheck > 4 * 60 * 60 * 1000
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
        if (!isNetworkAvailable) {
            Log.d(TAG, "Update check skipped - no network connection")
            return
        }

        if (updateInProgress) {
            Log.d(TAG, "Update check skipped - update already in progress")
            return
        }

        if (!isPlayStoreAvailable()) {
            Log.d(TAG, "Play Store not available on this device")
            return
        }

        lastUpdateCheck = System.currentTimeMillis()
        
        try {
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo
            appUpdateInfoTask
                .addOnSuccessListener { appUpdateInfo ->
                    try {
                        handleUpdateInfo(appUpdateInfo)
                    } catch (e: Exception) {
                        handleUpdateError(e)
                    }
                }
                .addOnFailureListener { e ->
                    handleUpdateError(e)
                }
        } catch (e: Exception) {
            handleUpdateError(e)
        }
    }

    private fun handleUpdateInfo(appUpdateInfo: AppUpdateInfo) {
        when (appUpdateInfo.updateAvailability()) {
            UpdateAvailability.UPDATE_AVAILABLE -> {
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    startFlexibleUpdate(appUpdateInfo)
                } else {
                    Log.d(TAG, "Flexible update not supported, trying Play Store")
                    showUpdateAvailableDialog()
                }
            }
            UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                Log.d(TAG, "No update available")
                updateRetryCount = 0
            }
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                Log.d(TAG, "Update already in progress")
                if (!updateInProgress) {
                    // Reconnect to existing update
                    updateInProgress = true
                    appUpdateManager.registerListener(installStateUpdatedListener)
                }
            }
            else -> {
                Log.d(TAG, "Unexpected update availability: ${appUpdateInfo.updateAvailability()}")
            }
        }
    }

    private fun startFlexibleUpdate(appUpdateInfo: AppUpdateInfo) {
        try {
            updateInProgress = true
            appUpdateManager.registerListener(installStateUpdatedListener)
            
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                AppUpdateType.FLEXIBLE,
                this,
                REQUEST_CODE_UPDATE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting update flow", e)
            handleUpdateError(e)
        }
    }

    private fun handleUpdateError(error: Exception) {
        Log.e(TAG, "Update error", error)
        cleanupUpdateResources()
        
        if (!isNetworkAvailable) {
            updateRetryCount++
            if (updateRetryCount < MAX_UPDATE_RETRIES) {
                showToast("Will retry update when network is available")
            } else {
                updateRetryCount = 0
                showToast("Unable to check for updates. Please try again later.")
            }
        } else {
            showToast("Update check failed. Please try again later.")
        }
    }

    private fun showUpdateAvailableDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("Update Available")
                .setMessage("A new version is available on the Play Store. Would you like to update?")
                .setPositiveButton("Update") { dialog, _ ->
                    openPlayStore()
                    dialog.dismiss()
                }
                .setNegativeButton("Later") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing update available dialog", e)
            openPlayStore()
        }
    }

    private fun handleDownloadStatus(state: InstallState) {
        if (!updateInProgress) return
        try {
            val bytesDownloaded = state.bytesDownloaded()
            val totalBytesToDownload = state.totalBytesToDownload()
            if (totalBytesToDownload <= 0) {
                Log.e(TAG, "Invalid total bytes in download status")
                return
            }
            val progress = (bytesDownloaded * 100) / totalBytesToDownload
            Log.d(TAG, "Update download progress: $progress%")
            showToast("Downloading: $progress%")
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating download progress", e)
        }
    }

    private fun handleDownloadComplete() {
        if (!updateInProgress) return
        showUpdateChoiceDialog()
    }

    private fun handleInstallationError(state: InstallState) {
        val errorCode = try {
            state.installErrorCode()
        } catch (e: Exception) {
            -1
        }
        Log.e(TAG, "Update installation failed with code: $errorCode")
        cleanupUpdateResources()
        val errorMessage = getUpdateErrorMessage(errorCode)
        showToast(errorMessage)
    }

    private fun handleInstallationCanceled() {
        Log.d(TAG, "Update cancelled by user or system")
        cleanupUpdateResources()
        showToast("Update cancelled")
    }

    private fun handleInstallationSuccess() {
        Log.d(TAG, "Update installed successfully")
        cleanupUpdateResources()
        showToast("Update installed successfully")
    }

    private fun handleInstallationPending() {
        Log.d(TAG, "Update is pending")
        showToast("Update is queued for download")
    }

    private fun handleInstalling() {
        Log.d(TAG, "Update is being installed")
        showToast("Installing update...")
    }

    private fun showUpdateChoiceDialog() {
        try {
            val builder = AlertDialog.Builder(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setIcon(getDrawable(R.drawable.ecorvilogo))
            }
            
            builder.setTitle("Update Downloaded")
                .setMessage("The update is ready. Would you like to install it now or when you next open the app?")
                .setPositiveButton("Install Now") { dialog, _ ->
                    try {
                        appUpdateManager.completeUpdate()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting update installation", e)
                        showToast("Failed to start installation. Update will be installed on next launch.")
                        cleanupUpdateResources()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Install Later") { dialog, _ ->
                    showToast("Update will be installed when you next open the app")
                    dialog.dismiss()
                }
                .setOnCancelListener {
                    showToast("Update will be installed when you next open the app")
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing update choice dialog", e)
            showToast("Update will be installed when you next open the app")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    try {
                        when (appUpdateInfo.installStatus()) {
                            InstallStatus.DOWNLOADED -> {
                                // If app is reopened and update was not installed, install it now
                                if (!updateInProgress) {
                                    showToast("Installing pending update...")
                                    try {
                                        appUpdateManager.completeUpdate()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error completing update on resume", e)
                                        showToast("Failed to install update. Please restart the app.")
                                    }
                                }
                            }
                            InstallStatus.FAILED, InstallStatus.CANCELED -> {
                                cleanupUpdateResources()
                            }
                            else -> {
                                // No action needed
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing install status", e)
                        cleanupUpdateResources()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking update status in onResume", e)
                    cleanupUpdateResources()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onResume", e)
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
                    Log.d(TAG, "Update flow cancelled by user")
                    cleanupUpdateResources()
                    showToast("Update cancelled")
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    Log.e(TAG, "Update flow failed")
                    cleanupUpdateResources()
                    showToast("Update failed to start. Please try again later.")
                }
                else -> {
                    Log.d(TAG, "Update flow returned unexpected result code: $resultCode")
                    cleanupUpdateResources()
                    showToast("Update process interrupted")
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                } catch (e: Exception) {
                    Log.e(TAG, "Error unregistering network callback", e)
                }
            }
            scope.cancel()
            cleanupUpdateResources()
            FirestoreDatabase.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }

    private fun isPlayStoreAvailable(): Boolean {
        val playStorePackage = "com.android.vending"
        return try {
            packageManager.getPackageInfo(playStorePackage, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun openPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // If Play Store app is not available, open in browser
            try {
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(webIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening Play Store", e)
                showToast("Unable to open Play Store")
            }
        }
    }
}

