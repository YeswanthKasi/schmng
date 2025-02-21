package com.ecorvi.schmng.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ecorvi.schmng.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var selectedOption by remember { mutableStateOf("Choose an Option") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf(TextFieldValue()) }
    var password by remember { mutableStateOf(TextFieldValue()) }

    val loginOptions = listOf("Admin", "Student", "Teacher")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.bg_ui),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // Heading
            Text(
                text = "Login as",
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F41BB))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Dropdown for Role Selection
            Box(
                modifier = Modifier
                    .border(2.dp, Color(0xFF1F41BB), RoundedCornerShape(100.dp))
                    .background(Color(0xFFF1F4FF), RoundedCornerShape(100.dp))
                    .clickable { isDropdownExpanded = true }
                    .padding(16.dp)
                    .width(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = selectedOption, fontSize = 16.sp, color = Color(0xFF626262))
                    Image(
                        painter = painterResource(id = R.drawable.arrowdown),
                        contentDescription = "Dropdown",
                        modifier = Modifier.size(24.dp)
                    )
                }
                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    loginOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedOption = option
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Email", color = Color.Gray, fontSize = 16.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(2.dp, Color(0xFF003f87), RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2F5FF), shape = RoundedCornerShape(12.dp)), // Light blue background
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = Color.Transparent, // Use containerColor instead of backgroundColor
                    focusedBorderColor = Color(0xFF003f87),
                    unfocusedBorderColor = Color(0xFF003f87)
                )
            )


            Spacer(modifier = Modifier.height(11.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password", color = Color.Gray, fontSize = 16.sp) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(2.dp, Color(0xFF003f87), RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2F5FF), shape = RoundedCornerShape(12.dp)), // Light blue background
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = Color.Transparent, // Use containerColor instead of backgroundColor
                    focusedBorderColor = Color(0xFF003f87),
                    unfocusedBorderColor = Color(0xFF003f87)
                )
            )


            Spacer(modifier = Modifier.height(16.dp))

            // Forgot Password
            Text(
                text = "Forgot Password?",
                color = Color(0xFF1F41BB),
                modifier = Modifier.align(Alignment.End).clickable { }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = { /* Handle login */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(50)) // Adds shadow effect
                    .background(Color(0xFF1F41BB), shape = RoundedCornerShape(50)), // Button background
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F41BB), // Primary color
                    contentColor = Color.White // White text color
                )
            ) {
                Text(text = "Sign in", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    val navController = rememberNavController()
    LoginScreen(navController)
}
