package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.ecorvi.schmng.R
import androidx.compose.foundation.layout.Arrangement

@Composable
fun WelcomeScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
               color = Color.White
            )
    ) {
        Image(painter = painterResource(id = R.drawable.bg_ui), contentDescription = "bg_ui", modifier = Modifier.fillMaxSize())
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp) // Add padding to the Column
        ) {
            Column(
                modifier = Modifier
                    .weight(1f) // Takes up available space
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.ecorvi_logo),
                    contentDescription = "image description",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(182.dp).height(64.dp)

                )

                Spacer(modifier = Modifier.height(26.dp))

                // Heading
                Text(
                    text = "Welcome to Ecorvi School Management",
                    color = Color(0xFF1F41BB),
                    fontSize = 20.sp,
                    fontWeight = FontWeight(600),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(72.dp))

                // Subheading
                Text(
                    text = "Streamline your school operations with ease and efficiency.",
                    color = Color(0xFF000000),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 30.dp)
                )
            }

            Row(
               horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
               verticalAlignment = Alignment.CenterVertically,
            ) {
             // Get Started Button (Moved to the bottom)
              Button(
                  onClick = {
                      navController.navigate("login") // ✅ Navigate to Next Screen
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFE1F40BA)),
                  shape = RoundedCornerShape(size = 10.dp),
                  modifier = Modifier
                      .width(180.dp)
                      .height(50.dp)
              ) {
                  Text(text = "Get Started", color = Color.White, fontSize = 16.sp)
              }


            }
            Spacer(modifier = Modifier.height(16.dp)) // Small gap at the bottom
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    // You can pass a dummy NavController here for preview purposes
    WelcomeScreen(navController = NavController(LocalContext.current))
}
