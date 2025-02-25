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
    val auth = FirebaseAuth.getInstance()

    Box(modifier = Modifier.fillMaxSize()) {
        // WebView to load the company website
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true // Enable JavaScript
                    webViewClient = WebViewClient() // Ensure navigation inside WebView
                    loadUrl("https://www.ecorvi.com") // Load your company website
                }
            }
        )

        // Logout Button (Floating on Top-Right)
        TextButton(
            onClick = {
                auth.signOut() // Sign out the user
                navController.navigate("login") { // Navigate to LoginScreen
                    popUpTo("company_website") { inclusive = true } // Clear back stack
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd) // Aligns to top-right
                .padding(16.dp) // Adds spacing from edges
                .background(Color(0xFF1F41BB), shape = RoundedCornerShape(8.dp)) // Blue background
        ) {
            Text("Logout", color = Color.White)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompanyWebsiteScreenPreview() {
    CompanyWebsiteScreen(navController = NavController(context = LocalContext.current))
}
