package com.ecorvi.schmng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecorvi.schmng.ui.navigation.AppNavigation
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : ComponentActivity() {
    private lateinit var appUpdateManager: AppUpdateManager
    private val MY_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the AppUpdateManager
        appUpdateManager = AppUpdateManagerFactory.create(this)

        // Set up the update flow
        setContent {
            var showUpdateDialog by remember { mutableStateOf(false) }
            var isDownloading by remember { mutableStateOf(false) }
            var downloadProgress by remember { mutableStateOf(0) }

            // Listener for update state changes
            val listener = InstallStateUpdatedListener { state ->
                if (state.installStatus() == InstallStatus.DOWNLOADING) {
                    isDownloading = true
                    val bytesDownloaded = state.bytesDownloaded()
                    val totalBytesToDownload = state.totalBytesToDownload()
                    downloadProgress = ((bytesDownloaded * 100) / totalBytesToDownload).toInt()
                } else if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    isDownloading = false
                    showUpdateDialog = true
                }
            }

            // Register the listener
            LaunchedEffect(Unit) {
                appUpdateManager.registerListener(listener)
            }

            // Check for updates when the app starts
            LaunchedEffect(Unit) {
                val appUpdateInfoTask = appUpdateManager.appUpdateInfo
                appUpdateInfoTask
                    .addOnSuccessListener { appUpdateInfo ->
                        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                        ) {
                            // Start the flexible update
                            try {
                                appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    AppUpdateType.FLEXIBLE,
                                    this@MainActivity,
                                    MY_REQUEST_CODE
                                )
                            } catch (e: Exception) {
                                // Handle the error (e.g., show a toast or log the error)
                                android.widget.Toast.makeText(
                                    this@MainActivity,
                                    "Failed to start update: ${e.message}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        // Handle the failure to check for updates
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Failed to check for updates: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
            }

            // Clean up the listener when the activity is destroyed
            DisposableEffect(Unit) {
                onDispose {
                    appUpdateManager.unregisterListener(listener)
                }
            }

            // Show the update dialog when the download is complete
            if (showUpdateDialog) {
                AlertDialog(
                    onDismissRequest = { showUpdateDialog = false },
                    title = { Text("Update Available") },
                    text = { Text("A new version of the app has been downloaded. Please install it to continue.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                appUpdateManager.completeUpdate()
                                showUpdateDialog = false
                            }
                        ) {
                            Text("Install Now")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUpdateDialog = false }) {
                            Text("Later")
                        }
                    }
                )
            }

            // Show download progress if the update is downloading
            if (isDownloading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Downloading Update: $downloadProgress%", fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = downloadProgress / 100f)
                    }
                }
            }

            // Your app's main content
            AppNavigation()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if a flexible update has been downloaded and is ready to install
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                setContent {
                    var showUpdateDialog by remember { mutableStateOf(true) }
                    if (showUpdateDialog) {
                        AlertDialog(
                            onDismissRequest = { showUpdateDialog = false },
                            title = { Text("Update Available") },
                            text = { Text("A new version of the app has been downloaded. Please install it to continue.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        appUpdateManager.completeUpdate()
                                        showUpdateDialog = false
                                    }
                                ) {
                                    Text("Install Now")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showUpdateDialog = false }) {
                                    Text("Later")
                                }
                            }
                        )
                    }
                    AppNavigation()
                }
            }
        }
    }
}