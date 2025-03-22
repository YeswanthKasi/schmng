package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.ecorvi.schmng.R

// Composable function for Welcome Screen
@Composable
fun WelcomeScreen(navController: NavController) {
    // Box composable to hold the entire screen content
    Box(
        modifier = Modifier
            .fillMaxSize() // Make the Box fill the entire screen
            .background(Color.White) // Set the background color to white
    ) {
        // Image composable for the background image
        Image(
            painter = painterResource(id = R.drawable.bg_ui), // Load the background image from resources
            contentDescription = "bg_ui", // Description for accessibility
            modifier = Modifier.fillMaxSize() // Make the image fill the entire screen
        )
        // Column composable to arrange elements vertically
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, // Center the items horizontally
            modifier = Modifier
                .fillMaxSize() // Make the column fill the entire screen
                .padding(16.dp) // Add padding around the column
        ) {
            // Column composable for the top section (logo, heading, subheading)
            Column(
                modifier = Modifier
                    .weight(1f) // Use the available space
                    .fillMaxWidth(), // Make the column fill the width of the parent
                horizontalAlignment = Alignment.CenterHorizontally, // Center items horizontally
                verticalArrangement = Arrangement.Center // Center items vertically
            ) {
                // Image composable for the logo
                Image(
                    painter = painterResource(id = R.drawable.ecorvi_logo), // Load the logo from resources
                    contentDescription = "Ecorvi Logo", // Description for accessibility
                    contentScale = ContentScale.Crop, // Scale the image to crop
                    modifier = Modifier
                        .width(182.dp) // Set the width of the logo
                        .height(64.dp) // Set the height of the logo
                )
                // Spacer composable to add vertical space
                Spacer(modifier = Modifier.height(26.dp)) // Add space below the logo
                // Text composable for the heading
                Text(
                    text = "Welcome to Ecorvi School Management", // Set the heading text
                    color = Color(0xFF1F41BB), // Set the text color
                    fontSize = 20.sp, // Set the font size
                    fontWeight = FontWeight.SemiBold, // Set the font weight to semi-bold
                    textAlign = TextAlign.Center, // Center the text
                    modifier = Modifier.padding(horizontal = 20.dp) // Add horizontal padding
                )
                // Spacer composable to add vertical space
                Spacer(modifier = Modifier.height(72.dp)) // Add space below the heading
                // Text composable for the subheading
                Text(
                    text = "Streamline your school operations with ease and efficiency.", // Set the subheading text
                    color = Color.Black, // Set the text color
                    fontSize = 14.sp, // Set the font size
                    textAlign = TextAlign.Center, // Center the text
                    modifier = Modifier.padding(horizontal = 30.dp) // Add horizontal padding
                )
            }
            // Button composable for navigation
            Button(
                onClick = {
                    navController.navigate("login") // Navigate to the login screen when clicked
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFE1F40BA)), // Set the button color
                shape = RoundedCornerShape(size = 10.dp), // Set the button shape
                modifier = Modifier
                    .width(180.dp) // Set the width of the button
                    .height(50.dp) // Set the height of the button
            ) {
                Text(text = "Get Started", color = Color.White, fontSize = 16.sp) // Set the button text
            }
            // Spacer composable to add vertical space
            Spacer(modifier = Modifier.height(16.dp)) // Add space at the bottom
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    // Preview composable for the Welcome Screen
    WelcomeScreen(
        navController = NavController(LocalContext.current)
    ) // Provide a dummy NavController for preview
}
