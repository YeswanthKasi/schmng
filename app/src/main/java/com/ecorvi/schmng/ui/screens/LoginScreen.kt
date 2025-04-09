package com.ecorvi.schmng.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ecorvi.schmng.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showRollbackDialog by remember { mutableStateOf(false) }

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

        // Top Right Help Icon with Dropdown Menu Properly Aligned
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            var iconOffset by remember { mutableStateOf(Offset.Zero) }

            Box(modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    iconOffset = coordinates.localToWindow(Offset.Zero)
                }
            ) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_help),
                        contentDescription = "Help",
                        modifier = Modifier.size(28.dp),
                        tint = Color(0xFF777C8D)
                    )
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                offset = DpOffset(x = 0.dp, y = 8.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("Help") },
                    onClick = {
                        menuExpanded = false
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("info@ecorvi.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "Support Request")
                        }
                        ContextCompat.startActivity(context, emailIntent, null)
                    }
                )

                DropdownMenuItem(
                    text = { Text("Rollback") },
                    onClick = {
                        menuExpanded = false
                        showRollbackDialog = true
                    }
                )
            }
        }

        // Rollback Confirmation Dialog
        if (showRollbackDialog) {
            AlertDialog(
                onDismissRequest = { showRollbackDialog = false },
                title = { Text("Switch to Beta Version?") },
                text = { Text("You will be redirected to the beta version of the app on the Play Store.") },
                confirmButton = {
                    TextButton(onClick = {
                        showRollbackDialog = false
                        try {
                            val playStoreIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.ecorvi.schmng")
                            )
                            ContextCompat.startActivity(context, playStoreIntent, null)
                        } catch (e: ActivityNotFoundException) {
                            try {
                                val webIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=com.ecorvi.schmng")
                                )
                                ContextCompat.startActivity(context, webIntent, null)
                            } catch (ex: Exception) {
                                Toast.makeText(
                                    context,
                                    "Unable to open Play Store or browser. Please try again later.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }) {
                        Text("Continue")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRollbackDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Login Form
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(210.dp))

            Text(
                text = "Login",
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F41BB))
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
                    .clickable {
                        // TODO: Forgot Password logic
                    }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter email and password"
                    } else {
                        errorMessage = ""
                        LoginUser(email, password, navController) { success, message ->
                            if (!success) {
                                errorMessage = message ?: "Authentication failed"
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F41BB))
            ) {
                Text(text = "Sign in", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    Image(
                        painter = painterResource(id = R.drawable.google),
                        contentDescription = "Google Sign-In",
                        modifier = Modifier.size(60.dp)
                    )
                }

                IconButton(onClick = { /* TODO: Facebook Sign-In */ }) {
                    Image(
                        painter = painterResource(id = R.drawable.facebook),
                        contentDescription = "Facebook Sign-In",
                        modifier = Modifier.size(60.dp)
                    )
                }

                IconButton(onClick = { /* TODO: Apple Sign-In */ }) {
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

fun LoginUser(
    email: String,
    password: String,
    navController: NavController,
    onLoginResult: (Boolean, String?) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                navController.navigate("adminDashboard") {
                    popUpTo("login") { inclusive = true }
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

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    val navController = rememberNavController()
    LoginScreen(navController)
}
