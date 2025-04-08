package com.ecorvi.schmng

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ecorvi.schmng.ui.navigation.AppNavigation
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.InstallStateUpdatedListener

class MainActivity : ComponentActivity() {
    private lateinit var appUpdateManager: AppUpdateManager
    private var showUpdateDialog by mutableStateOf(false)
    private var isUpdating by mutableStateOf(false) // 🚀 Shows update progress
    private val MY_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdates()

        setContent {
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

                AppNavigation()
            }
        }
    }

    private fun checkForUpdates() {
        val packageManager = applicationContext.packageManager
        val installer = packageManager.getInstallerPackageName(packageName)

        // 🚀 Skip update check if app is sideloaded (not installed from Play Store)
        if (installer != "com.android.vending") {
            Toast.makeText(
                this,
                "Updates disabled: App not installed from Play Store.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            when {
                // IMMEDIATE UPDATE
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this@MainActivity,
                            MY_REQUEST_CODE
                        )
                    } catch (e: Exception) {
                        Toast.makeText(
                            this,
                            "Immediate update error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                // FLEXIBLE UPDATE
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    try {
                        isUpdating = true // ✅ Show progress
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            this@MainActivity,
                            MY_REQUEST_CODE
                        )
                        appUpdateManager.registerListener(installStateListener) // 🔄 Listen for update progress
                    } catch (e: Exception) {
                        isUpdating = false
                        Toast.makeText(
                            this,
                            "Flexible update error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to check updates: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 🚀 Listen for install progress updates
    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                isUpdating = true // ✅ Show progress
            }

            InstallStatus.DOWNLOADED -> {
                isUpdating = false
                showUpdateDialog = true // ✅ Show "Restart Now" popup
            }

            InstallStatus.INSTALLED -> {
                isUpdating = false
                Toast.makeText(this, "Update installed!", Toast.LENGTH_SHORT).show()
            }

            InstallStatus.FAILED -> {
                isUpdating = false
                Toast.makeText(this, "Update failed! Try again later.", Toast.LENGTH_LONG).show()
            }

            else -> {}
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                showUpdateDialog = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                isUpdating = false
                Toast.makeText(this, "Update failed! Please try again.", Toast.LENGTH_LONG).show()
                checkForUpdates() // Re-check for updates if the user cancels
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateListener) // ✅ Prevent memory leaks
    }
}
