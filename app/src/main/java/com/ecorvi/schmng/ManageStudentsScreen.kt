package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun ManageStudentsScreen(navController: NavController) {
    Scaffold(content = { innerPadding ->


            Text(
                text = "Manage Students Screen",
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
        }
    )

}