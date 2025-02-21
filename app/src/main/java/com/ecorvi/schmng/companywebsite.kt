package com.ecorvi.schmng.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CompanyWebsiteScreen() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true // Enable JavaScript
                webViewClient = WebViewClient() // Ensure navigation inside WebView
                loadUrl("https://www.ecorvi.com") // Change this to your company website
            }
        }
    )
}
