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
@Composable
fun LoginScreen(navController: NavController) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
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
            Spacer(modifier = Modifier.height(270.dp))

            Text(
                text = "Login to Company Website",
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F41BB))
            )

            Spacer(modifier = Modifier.height(20.dp))

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

            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = Color.Red, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Forgot Password?",
                color = Color(0xFF1F41BB),
                modifier = Modifier.align(Alignment.End).clickable { }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter email and password"
                    } else {
                        errorMessage = "" // Clear previous errors
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

            // Or continue with text
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
                IconButton(onClick = { /* Handle Google Sign-In */ }) {
                    Image(
                        painter = painterResource(id = R.drawable.google),
                        contentDescription = "Google Sign-In",
                        modifier = Modifier.size(60.dp)
                    )
                }
                IconButton(onClick = { /* Handle Facebook Sign-In */ }) {
                    Image(
                        painter = painterResource(id = R.drawable.facebook),
                        contentDescription = "Facebook Sign-In",
                        modifier = Modifier.size(60.dp)
                    )
                }
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

fun LoginUser(email: String, password: String, navController: NavController, onLoginResult: (Boolean, String?) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Navigate to the company website screen after successful login
                navController.navigate("company_website") {
                    popUpTo("login") { inclusive = true } // Clears back stack
                }
                onLoginResult(true, null)
            } else {
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
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    val navController = rememberNavController()
    LoginScreen(navController)
}
