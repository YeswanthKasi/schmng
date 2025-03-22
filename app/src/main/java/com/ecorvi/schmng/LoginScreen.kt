package com.ecorvi.schmng.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.content.Context
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.ecorvi.schmng.R

@OptIn(ExperimentalMaterial3Api::class)
// Composable function for the login screen
@Composable
fun LoginScreen(navController: NavController) {
    // State variables to hold email, password, password visibility, and error message
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    // Box to contain the entire login screen layout
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.bg_ui),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Column to arrange the content vertically
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Spacer for top margin
            // Title text
            // Spacer below title

            Spacer(modifier = Modifier.height(210.dp))

            Text(
                text = "Login to Company Website",
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F41BB))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Email input field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Email", color = Color.Gray, fontSize = 16.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true, // Prevents enlarging when pressing enter
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF003f87),
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = Color(0xFF003f87)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Password input field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password", color = Color.Gray, fontSize = 16.sp) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        val icon = if (passwordVisible) R.drawable.visibility else R.drawable.visibilityoff
                        Image(painter = painterResource(id = icon), contentDescription = "Toggle Password")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true, // Prevents enlarging when pressing enter
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF003f87),
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = Color(0xFF003f87)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display error message if any
            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = Color.Red, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }


            // Forgot password text
            Text(
                text = "Forgot Password?",
                color = Color(0xFF1F41BB),
                modifier = Modifier.align(Alignment.End).clickable { }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sign in button
            Button(
                onClick = {
                    // Validate email and password fields
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter email and password"
                    } else {
                        errorMessage = ""
                        // Attempt to log in the user
                        LoginUser(email, password, navController) { success, message ->
                            if (!success) {
                                errorMessage = message ?: "Authentication failed"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F41BB))
            ) {
                Text(text = "Sign in", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
            // 'Or continue with' text
            Text(
                  text = "Or continue with",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F41BB))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Social Media Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Google sign in button
                IconButton(onClick = { /* Handle Google Sign-In */ }) {
                    Image(
                        painter = painterResource(id = R.drawable.google),
                        contentDescription = "Google Sign-In",
                        modifier = Modifier.size(60.dp)
                    )
                }

                //Facebook sign in button
                IconButton(onClick = { /* Handle Facebook Sign-In */ }) {
                    Image(
                        painter = painterResource(id = R.drawable.facebook),
                        contentDescription = "Facebook Sign-In",
                        modifier = Modifier.size(60.dp)
                    )
                }
                //Apple sign in button
                IconButton(onClick = { /* Handle Apple Sign-In */ }) {
                    Image(
                        painter = painterResource(id = R.drawable.apple),
                        contentDescription = "Apple Sign-In",
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        }
    }
}

// Function to handle user login with email and password
fun LoginUser(email: String, password: String, navController: NavController, onLoginResult: (Boolean, String?) -> Unit) {
    // Get an instance of FirebaseAuth
    val auth = FirebaseAuth.getInstance()
    // Sign in the user with the provided email and password
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            // Check if the sign-in task was successful
            if (task.isSuccessful) {
                // Navigate to the company website screen upon successful login
                navController.navigate("company_website") {
                    // Clear the back stack to prevent going back to the login screen
                    popUpTo("login") { inclusive = true }
                }
                // Invoke the callback with success status and no error message
                onLoginResult(true, null)
            } else {
                // Determine the error message based on the exception type
                val errorMsg = when (task.exception) {
                    is FirebaseAuthInvalidUserException -> "User not found. Check your email."
                    is FirebaseAuthInvalidCredentialsException -> "Invalid password. Try again."
                    else -> "Authentication failed. Please try again."
                }
                onLoginResult(false, errorMsg)
            }
        }
}

// Preview function to visualize the screen in Android Studio
// Preview function to visualize the screen in Android Studio
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    val navController = rememberNavController()
    LoginScreen(navController)
}
