package com.ecorvi.schmng

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ecorvi.schmng.ui.screens.AdminDashboardScreen
import com.ecorvi.schmng.ui.screens.WelcomeScreen
import com.ecorvi.schmng.ui.screens.LoginScreen
import com.ecorvi.schmng.ui.screens.CompanyWebsiteScreen // Import new screen
import com.ecorvi.schmng.ui.theme.OnboardingJetpackComposeTheme
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    // AppUpdateManager for handling in-app updates
    private lateinit var appUpdateManager: AppUpdateManager
    // Request code for starting the update flow
    private val REQUEST_CODE_UPDATE = 100

    // Called when the activity is first created.
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call the superclass implementation of onCreate to do standard setup.
        super.onCreate(savedInstanceState)

        // Initialize Firebase to enable its services.
        FirebaseApp.initializeApp(this)

        // Initialize AppUpdateManager to check for and manage app updates.
        appUpdateManager = AppUpdateManagerFactory.create(this)

        // Check for app updates when the activity is created.
        checkForAppUpdate()

        // Get the current Firebase authentication instance.
        val auth = FirebaseAuth.getInstance()
        // Determine the start destination of the navigation based on whether a user is logged in.
        val startDestination = if (auth.currentUser != null) "admin_dashboard" else "welcome"

        setContent {
            OnboardingJetpackComposeTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("welcome") { WelcomeScreen(navController) }
                        composable("login") { LoginScreen(navController) }
                        composable("company_website") { CompanyWebsiteScreen(navController) }
                        composable("admin_dashboard") { AdminDashboardScreen(navController) }
                        composable("manage_students") { /* Add a screen for managing students */ }
                    }
                }
            }
        }
    }

    // Function to check for available app updates.
    private fun checkForAppUpdate() {
        // Get the AppUpdateInfo task from the AppUpdateManager.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Add a success listener to the task to check the update availability.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            // Check if an update is available and if the flexible update type is allowed.
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                // Start the update flow for a flexible update.
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    this,
                    REQUEST_CODE_UPDATE
                )
            } // If no update is available or not allowed, nothing happens.
        }

        // Register a listener to get notified of the update state changes.
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    // Listener for receiving updates on the state of the update installation.
    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        // Check if the update has been downloaded and is ready to be installed.
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // Prompt the user to restart the app to complete the update.
            popupSnackbarForCompleteUpdate()
        }
    }

    // Display a snackbar to prompt the user to restart the app for completing the update.
    private fun popupSnackbarForCompleteUpdate() {
        // Make a snackbar that displays a message about the completed download.
        Snackbar.make(
            // Find the root view to attach the snackbar.
            findViewById(android.R.id.content),
            "An update has just been downloaded.",
            // Display the snackbar indefinitely until an action is taken.
            Snackbar.LENGTH_INDEFINITE
            // Set the action button "Restart" that will trigger the update completion.
        ).setAction("Restart") {
            // Complete the update by restarting the app.
            appUpdateManager.completeUpdate()
            // Show the SnackBar
        }.show()
    }

    // Called when an activity you launched exits, giving you the requestCode you started it with, the resultCode it returned, and any additional data from it.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Check if the result is from the update flow.
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_UPDATE) {
            // Check if the update flow was not successful.
            if (resultCode != RESULT_OK) {
                // Log an error indicating the update failure.
                Log.e("MainActivity", "Update flow failed! Result code: $resultCode")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the update listener to prevent memory leaks.
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }
}

// Composable function for previewing the WelcomeScreen.
@Composable
@Preview(showBackground = true)
fun PreviewWelcomeScreen() {
    // Apply the OnboardingJetpackComposeTheme to the UI.
    OnboardingJetpackComposeTheme {
        // A surface container using the 'background' color from the theme.
        Surface(
            // Make the Surface fill the maximum available space.
            modifier = Modifier.fillMaxSize(),
            // Set the background color to the color scheme's background.
            color = MaterialTheme.colorScheme.background
        ) {
            // Display the WelcomeScreen with a remembered NavController.
            WelcomeScreen(navController = rememberNavController())
        }
    }
}
