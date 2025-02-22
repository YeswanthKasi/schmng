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
import com.ecorvi.schmng.ui.screens.CompanyWebsiteScreen //  Import new screen
import com.ecorvi.schmng.ui.theme.OnboardingJetpackComposeTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.FirebaseApp //  Import FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this) // Initialize Firebase

        val auth = FirebaseAuth.getInstance()
        val startDestination = if (auth.currentUser != null) "company_website" else "welcome"

        setContent {
            OnboardingJetpackComposeTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("welcome") { WelcomeScreen(navController) }
                        composable("login") { LoginScreen(navController) }
                        composable("company_website") { CompanyWebsiteScreen() }
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
