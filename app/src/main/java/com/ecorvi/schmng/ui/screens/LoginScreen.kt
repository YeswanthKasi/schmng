package com.ecorvi.schmng.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ecorvi.schmng.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Toast.makeText(
            context,
            if (isGranted) "Notification permission granted" else "Notification permission denied",
            Toast.LENGTH_SHORT
        ).show()
    }

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                .padding(horizontal = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(210.dp))

            Text(
                text = "Login",
                style = TextStyle(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F41BB)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = ""
                },
                placeholder = { Text("Email", color = Color.Gray, fontSize = 16.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
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
                onValueChange = {
                    password = it
                    errorMessage = ""
                },
                placeholder = { Text("Password", color = Color.Gray, fontSize = 16.sp) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        val icon = if (passwordVisible) R.drawable.visibility else R.drawable.visibilityoff
                        Image(
                            painter = painterResource(id = icon),
                            contentDescription = "Toggle Password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
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
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { /* TODO: Forgot Password */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter email and password"
                    } else {
                        errorMessage = ""
                        LoginUser(
                            email = email,
                            password = password,
                            navController = navController,
                            onLoginResult = { success, message ->
                                if (success) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    errorMessage = message ?: "Authentication failed"
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F41BB))
            ) {
                Text("Sign in", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Or continue with",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F41BB))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { /* TODO: Google Sign-In */ }) {
                    Image(painter = painterResource(id = R.drawable.google), contentDescription = "Google", modifier = Modifier.size(60.dp))
                }
                IconButton(onClick = { /* TODO: Facebook Sign-In */ }) {
                    Image(painter = painterResource(id = R.drawable.facebook), contentDescription = "Facebook", modifier = Modifier.size(60.dp))
                }
                IconButton(onClick = { /* TODO: Apple Sign-In */ }) {
                    Image(painter = painterResource(id = R.drawable.apple), contentDescription = "Apple", modifier = Modifier.size(60.dp))
                }
            }
        }
    }
}

fun LoginUser(
    email: String,
    password: String,
    navController: NavController,
    onLoginResult: (Boolean, String?) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

            // Get FCM token
            FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                val deviceName = "${Build.BRAND} ${Build.MODEL}"
                val tokenMap = mapOf(
                    "fcmTokens.$deviceName" to fcmToken,
                    "lastTokenUpdate" to com.google.firebase.Timestamp.now()
                )

                // Upload token
                db.collection("users")
                    .document(userId)
                    .set(tokenMap, SetOptions.merge())
                    .addOnSuccessListener {
                        // Fetch user role
                        db.collection("users").document(userId).get()
                            .addOnSuccessListener { document ->
                                val role = document.getString("role")?.lowercase(Locale.ROOT)

                                when (role) {
                                    "admin" -> {
                                        FirebaseMessaging.getInstance().subscribeToTopic("admins")
                                        navController.navigate("adminDashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }

                                    "student" -> {
                                        // TODO: Navigate to student dashboard
                                        FirebaseMessaging.getInstance().subscribeToTopic("students")
                                        navController.navigate("studentDashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }

                                    "parent" -> {
                                        // TODO: Navigate to parent dashboard
                                        FirebaseMessaging.getInstance().subscribeToTopic("parents")
                                        navController.navigate("parentDashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }

                                    else -> {
                                        onLoginResult(false, "User role not found or unknown.")
                                    }
                                }

                                onLoginResult(true, null)
                            }
                            .addOnFailureListener {
                                onLoginResult(false, "Failed to fetch user role: ${it.localizedMessage}")
                            }
                    }
                    .addOnFailureListener { e ->
                        onLoginResult(false, "Token upload failed: ${e.localizedMessage}")
                    }
            }.addOnFailureListener {
                onLoginResult(false, "Failed to get FCM token")
            }
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

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    val navController = rememberNavController()
    LoginScreen(navController)
}
