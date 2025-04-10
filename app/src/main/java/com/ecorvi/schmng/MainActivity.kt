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
import com.google.android.play.core.appupdate.*
import com.google.android.play.core.install.model.*
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var appUpdateManager: AppUpdateManager
    private val MY_REQUEST_CODE = 100
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 200

    private var showUpdateDialog by mutableStateOf(false)
    private var isUpdating by mutableStateOf(false)

    private var isUserLoggedIn by mutableStateOf(false)
    private var isFirstLaunch by mutableStateOf(true)

    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdates()

        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        isFirstLaunch = sharedPrefs.getBoolean("is_first_launch", true)
        if (isFirstLaunch) {
            sharedPrefs.edit().putBoolean("is_first_launch", false).apply()
        }

        isUserLoggedIn = FirebaseAuth.getInstance().currentUser != null

        // 🔹 REMOVED: Don't request permission here. We'll do it after login.

        setContent {
            SchmngTheme {
                navController = rememberNavController()

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        Text("Updating... Please wait")
                    }

                    if (showUpdateDialog) {
                        AlertDialog(
                            onDismissRequest = { showUpdateDialog = false },
                            title = { Text("Update Ready") },
                            text = { Text("A new version has been downloaded. Restart to install.") },
                            confirmButton = {
                                Button(onClick = {
                                    showUpdateDialog = false
                                    appUpdateManager.completeUpdate()
                                }) {
                                    Text("Restart Now")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showUpdateDialog = false }) {
                                    Text("Later")
                                }
                            }
                        )
                    }

                    AppNavigation(
                        navController = navController,
                        isUserLoggedIn = if (isFirstLaunch) false else isUserLoggedIn,
                        isFirstLaunch = isFirstLaunch
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null && ::navController.isInitialized) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != "adminDashboard") {
                navController.navigate("adminDashboard") {
                    popUpTo("login") { inclusive = true }
                }
            }
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
}
