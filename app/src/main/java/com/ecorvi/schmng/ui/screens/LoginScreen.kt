package com.ecorvi.schmng.ui.screens

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.provider.Settings.Global.putString
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentReference
import com.google.android.gms.auth.api.credentials.*
import android.app.Activity
import com.google.android.gms.common.api.ResolvableApiException
import kotlinx.coroutines.tasks.await

private const val REQUEST_SAVE_CREDENTIALS = 123

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val credentialsClient = remember { Credentials.getClient(activity) }
    val scope = rememberCoroutineScope()

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
    var isLoading by remember { mutableStateOf(false) }
    var staySignedIn by rememberSaveable { mutableStateOf(false) }

    // Credential request launcher
    val credentialRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val credential = result.data?.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
            credential?.let { cred ->
                email = cred.id ?: ""
                password = cred.password ?: ""
                // Remove auto-login, just fill the fields
            }
        }
    }

    // Request saved credentials on screen launch
    LaunchedEffect(Unit) {
        val request = CredentialRequest.Builder()
            .setPasswordLoginSupported(true)
            .build()

        try {
            val result = credentialsClient.request(request).await()
            result.credential?.let { cred ->
                email = cred.id ?: ""
                password = cred.password ?: ""
                // Remove auto-login, just fill the fields
            }
        } catch (e: Exception) {
            when (e) {
                is ResolvableApiException -> {
                    // Show credential picker
                    credentialRequestLauncher.launch(
                        IntentSenderRequest.Builder(e.resolution).build()
                    )
                }
                else -> {
                    Log.e("LoginScreen", "Error getting credentials", e)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Background Image with Overlay
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.bg_ui),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF6479C5).copy(alpha = 0.1f))
            )
        }

        // Content wrapped in ScrollView
        androidx.compose.foundation.rememberScrollState().let { scrollState ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .systemBarsPadding()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // App Logo or Icon
                Image(
                    painter = painterResource(id = R.drawable.ecorvilogo), // Make sure to add your app logo
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Welcome Text
                Text(
                    text = "Ecorvi School Management",
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F41BB)
                    )
                )

                Text(
                    text = "Sign in to continue",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Gray
                    ),
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )

                // Login Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                    ) {
                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = ""
                            },
                            label = { Text("Email") },
                            placeholder = { Text("Enter your email") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF1F41BB),
                                focusedLabelColor = Color(0xFF1F41BB)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = Color(0xFF1F41BB)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = ""
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter your password") },
                            visualTransformation = if (passwordVisible) 
                                VisualTransformation.None 
                            else 
                                PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF1F41BB),
                                focusedLabelColor = Color(0xFF1F41BB)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Password",
                                    tint = Color(0xFF1F41BB)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) 
                                            Icons.Default.Visibility 
                                        else 
                                            Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) 
                                            "Hide password" 
                                        else 
                                            "Show password",
                                        tint = Color(0xFF1F41BB)
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            )
                        )

                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Stay Signed In Checkbox
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = staySignedIn,
                                onCheckedChange = { staySignedIn = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF1F41BB),
                                    uncheckedColor = Color(0xFF1F41BB).copy(alpha = 0.6f)
                                )
                            )
                            Text(
                                text = "Stay signed in",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1F41BB),
                                modifier = Modifier.clickable { staySignedIn = !staySignedIn }
                            )
                        }

                        // Forgot Password
                        Text(
                            text = "Forgot Password?",
                            color = Color(0xFF1F41BB),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable { /* TODO: Implement forgot password */ }
                                .padding(top = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Login Button
                        Button(
                            onClick = {
                                isLoading = true
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter email and password"
                                    isLoading = false
                                } else {
                                    errorMessage = ""
                                    LoginUser(
                                        email = email,
                                        password = password,
                                        staySignedIn = staySignedIn,
                                        context = context,
                                        navController = navController
                                    ) { success, message ->
                                        isLoading = false
                                        if (success) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        } else {
                                            errorMessage = message ?: "Authentication failed"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1F41BB)
                            ),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    "Sign In",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Social Login Section
                Text(
                    text = "Or continue with",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Social Login Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SocialLoginButton(
                        icon = R.drawable.google,
                        onClick = { /* TODO: Implement Google login */ }
                    )
                    SocialLoginButton(
                        icon = R.drawable.facebook,
                        onClick = { /* TODO: Implement Facebook login */ }
                    )
                    SocialLoginButton(
                        icon = R.drawable.apple,
                        onClick = { /* TODO: Implement Apple login */ }
                    )
                }
            }
        }
    }
}

@Composable
fun SocialLoginButton(
    icon: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize()
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(navController = rememberNavController())
}

fun LoginUser(
    email: String,
    password: String,
    staySignedIn: Boolean,
    context: Context,
    navController: NavController,
    onLoginResult: (Boolean, String?) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val analytics = Firebase.analytics
    
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Save credentials if login successful and stay signed in is checked
                if (staySignedIn) {
                    val credential = Credential.Builder(email)
                        .setPassword(password)
                        .build()

                    val credentialsClient = Credentials.getClient(context as Activity)
                    credentialsClient.save(credential).addOnCompleteListener { saveTask ->
                        if (saveTask.isSuccessful) {
                            Log.d("LoginScreen", "Credentials saved successfully")
                        } else {
                            if (saveTask.exception is ResolvableApiException) {
                                // Try to resolve the save request
                                try {
                                    val rae = saveTask.exception as ResolvableApiException
                                    rae.startResolutionForResult(context as Activity, REQUEST_SAVE_CREDENTIALS)
                                } catch (e: IntentSender.SendIntentException) {
                                    Log.e("LoginScreen", "Failed to send resolution", e)
                                }
                            }
                        }
                    }
                }

                val user = auth.currentUser
                
                // Log successful login event
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.METHOD, "email")
                    putString(FirebaseAnalytics.Param.SUCCESS, "true")
                    putString("user_id", user?.uid ?: "")
                }
                analytics.logEvent(FirebaseAnalytics.Event.LOGIN, params)
                
                // Also log a custom event for in-app messaging
                val inAppParams = Bundle().apply {
                    putString("event_type", "user_login")
                    putString("user_id", user?.uid ?: "")
                }
                analytics.logEvent("trigger_in_app_message", inAppParams)
                
                // Save stay signed in preference
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("stay_signed_in", staySignedIn)
                    .apply()

                // Get FCM token and update in Firestore
                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val token = tokenTask.result
                            val userDoc = FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(user?.uid ?: "")

                            // Update token and timestamps
                            val updates = hashMapOf<String, Any>(
                                "fcmToken" to token,
                                "tokenUpdatedAt" to FieldValue.serverTimestamp(),
                                "lastActive" to FieldValue.serverTimestamp()
                            )

                            userDoc.update(updates)
                                .addOnSuccessListener {
                                    Log.d("LoginScreen", "FCM token updated successfully")
                                    // Continue with role check and navigation
                                    checkRoleAndNavigate(userDoc, context, navController, onLoginResult)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("LoginScreen", "Failed to update FCM token", e)
                                    // Continue anyway as this is not critical
                                    checkRoleAndNavigate(userDoc, context, navController, onLoginResult)
                                }
                        } else {
                            Log.e("LoginScreen", "Failed to get FCM token", tokenTask.exception)
                            // Continue without token update
                            val userDoc = FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(user?.uid ?: "")
                            checkRoleAndNavigate(userDoc, context, navController, onLoginResult)
                        }
                    }
            } else {
                val errorMsg = when (task.exception) {
                    is FirebaseAuthInvalidUserException -> "User not found. Check your email."
                    is FirebaseAuthInvalidCredentialsException -> "Invalid password. Try again."
                    else -> "Authentication failed. Please try again."
                }
                onLoginResult(false, errorMsg)
                
                // Log failed login attempt
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.METHOD, "email")
                    putString("error_type", task.exception?.javaClass?.simpleName ?: "unknown")
                }
                analytics.logEvent("login_failed", params)
            }
        }
}

private fun checkRoleAndNavigate(
    userDoc: DocumentReference,
    context: Context,
    navController: NavController,
    onLoginResult: (Boolean, String?) -> Unit
) {
    userDoc.get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val role = document.getString("role")
                if (role != null) {
                    // Save user role in SharedPreferences
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("user_role", role.lowercase())
                        .apply()

                    when (role.lowercase()) {
                        "admin" -> {
                            onLoginResult(true, null)
                            navController.navigate("admin_dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                        "student" -> {
                            onLoginResult(true, null)
                            navController.navigate("student_dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                        else -> onLoginResult(false, "Invalid user role")
                    }
                } else {
                    onLoginResult(false, "User role not found")
                }
            } else {
                onLoginResult(false, "User data not found")
            }
        }
        .addOnFailureListener { e ->
            onLoginResult(false, "Failed to fetch user role: ${e.localizedMessage}")
        }
}


