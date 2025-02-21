package com.ecorvi.schmng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ecorvi.schmng.ui.screens.WelcomeScreen
import com.ecorvi.schmng.ui.screens.LoginScreen
import com.ecorvi.schmng.ui.theme.OnboardingJetpackComposeTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ecorvi.schmng.ui.theme.OnboardingJetpackComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnboardingJetpackComposeTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController() // ✅ Create NavController

                    NavHost(navController = navController, startDestination = "welcome") {
                        composable("welcome") {
                            WelcomeScreen(navController) // ✅ Pass NavController
                        }
                        composable("login") {  // Ensure route name matches navigation calls
                            LoginScreen(navController) // ✅ Fix: Pass NavController here
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
fun PreviewWelcomeScreen() {
    OnboardingJetpackComposeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            WelcomeScreen(navController = rememberNavController())
        }
    }
}
