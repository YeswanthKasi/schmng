package com.ecorvi.schmng

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebAppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        val webView = findViewById<WebView>(R.id.webView)

        // Enable JavaScript for your web app
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true // Enable DOM storage for modern web apps

        // Load your web app URL
        webView.webViewClient = WebViewClient() // Ensures links open in WebView
        webView.loadUrl("https://ecorvi.com/") // Replace with your web app's URL
    }
}
