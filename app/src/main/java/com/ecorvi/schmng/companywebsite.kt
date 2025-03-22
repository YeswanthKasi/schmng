package com.ecorvi.schmng.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CompanyWebsiteScreen(navController: NavController) {
    // Get an instance of FirebaseAuth for handling user authentication
    val auth = FirebaseAuth.getInstance()

    // Box composable to stack UI elements on top of each other
    Box(modifier = Modifier.fillMaxSize()) {
        // AndroidView composable to integrate an Android WebView into the Compose UI
        AndroidView(
            // Set the modifier to fill the entire available space
            modifier = Modifier.fillMaxSize(),
            // Factory lambda to create and configure the WebView
            factory = { context ->
                // Create a WebView instance
                WebView(context).apply {
                    // Enable JavaScript in the WebView
                    settings.javaScriptEnabled = true
                    // Set a WebViewClient to handle page navigation within the WebView
                    webViewClient = WebViewClient()
                    // Load the specified URL (company website)
                    loadUrl("https://www.ecorvi.com")
                }
            }
        )

        // TextButton composable for the Logout button
        TextButton(
            // Lambda to handle the button click event
            onClick = {
                // Sign out the current user
                auth.signOut()
                // Navigate to the "login" screen
                navController.navigate("login") {
                    // Clear the back stack up to the "company_website" route
                    popUpTo("company_website") { inclusive = true }
                }
            },
            // Set the modifier to align the button to the top-right and add padding
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color(0xFF1F41BB), shape = RoundedCornerShape(8.dp))
        ) {
            // Text composable for the button's label
            Text("Logout", color = Color.White)
        }
    }
}
// Preview function for the CompanyWebsiteScreen


@Preview(showBackground = true)
@Composable
fun CompanyWebsiteScreenPreview() {
    CompanyWebsiteScreen(navController = NavController(context = LocalContext.current))
}
