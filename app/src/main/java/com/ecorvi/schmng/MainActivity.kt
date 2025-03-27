package com.ecorvi.schmng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ecorvi.schmng.ui.navigation.AppNavigation
import com.ecorvi.schmng.ui.theme.SchmngTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SchmngTheme {
                AppNavigation()
            }
        }
    }
}