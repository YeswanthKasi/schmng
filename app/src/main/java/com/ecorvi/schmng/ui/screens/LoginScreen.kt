package com.ecorvi.schmng.ui.screens

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ecorvi.schmng.R
import com.google.firebase.auth.*
import com.google.firebase.firestore.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.android.gms.auth.api.credentials.*
import android.app.Activity
import com.google.android.gms.common.api.ResolvableApiException
import kotlinx.coroutines.tasks.await
import android.util.Log
import androidx.compose.foundation.text.KeyboardOptions
import com.ecorvi.schmng.ui.navigation.BottomNavItem
import com.ecorvi.schmng.ui.navigation.StudentBottomNavItem
import com.google.android.gms.auth.api.credentials.IdentityProviders

private const val REQUEST_SAVE_CREDENTIALS = 123

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val activity = context as ComponentActivity
    val credentialsClient = remember { Credentials.getClient(activity) }
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val credentialsSaved = remember { prefs.getBoolean("credentials_saved", false) }

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
    var showSaveCredentialsDialog by remember { mutableStateOf(false) }
    var loginSuccessful by remember { mutableStateOf(false) }
    var userRole by remember { mutableStateOf("") }

    // Credential save launcher
    val credentialSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("LoginScreen", "Credentials saved successfully")
        }
        // Navigate after credential save result (whether successful or not)
        if (loginSuccessful && userRole.isNotEmpty()) {
            navigateToRoleDashboard(navController, userRole, context)
        }
    }

    // Credential request launcher
    val credentialRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val credential = result.data?.getParcelableExtra<Credential>(Credential.EXTRA_KEY)
            credential?.let { cred ->
                email = cred.id ?: ""
                password = cred.password ?: ""
            }
        }
    }

    // Request saved credentials on launch
    LaunchedEffect(Unit) {
        // Request credentials immediately when screen loads, not waiting for field focus
        try {
            val request = CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setAccountTypes(IdentityProviders.GOOGLE)
                .build()

            try {
                val result = credentialsClient.request(request).await()
                result.credential?.let { cred ->
                    email = cred.id ?: ""
                    password = cred.password ?: ""
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
        } catch (e: Exception) {
            Log.e("LoginScreen", "Error setting up credential request", e)
        }
    }

    // Check if this is first time login
    val isFirstLogin = remember { prefs.getBoolean("is_first_login", true) }

    // If login was successful and showSaveCredentialsDialog changed to false, navigate to dashboard
    LaunchedEffect(showSaveCredentialsDialog) {
        if (!showSaveCredentialsDialog && loginSuccessful && userRole.isNotEmpty() && !isFirstLogin) {
            navigateToRoleDashboard(navController, userRole, context)
        }
    }

    // Save credentials dialog
    if (showSaveCredentialsDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSaveCredentialsDialog = false
                if (loginSuccessful && userRole.isNotEmpty()) {
                    navigateToRoleDashboard(navController, userRole, context)
                }
            },
            title = { Text("Save Credentials") },
            text = { Text("Would you like to save your login credentials for next time?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveCredentialsDialog = false
                        val credential = Credential.Builder(email)
                            .setPassword(password)
                            .build()

                        credentialsClient.save(credential).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("LoginScreen", "Credentials saved successfully")
                                // Save the flag indicating credentials have been saved
                                prefs.edit()
                                    .putBoolean("credentials_saved", true)
                                    .apply()
                                
                                if (loginSuccessful && userRole.isNotEmpty()) {
                                    navigateToRoleDashboard(navController, userRole, context)
                                }
                            } else if (task.exception is ResolvableApiException) {
                                try {
                                    val rae = task.exception as ResolvableApiException
                                    credentialSaveLauncher.launch(
                                        IntentSenderRequest.Builder(rae.resolution).build()
                                    )
                                } catch (e: IntentSender.SendIntentException) {
                                    Log.e("LoginScreen", "Failed to send resolution", e)
                                } finally {
                                    if (loginSuccessful && userRole.isNotEmpty()) {
                                        navigateToRoleDashboard(navController, userRole, context)
                                    }
                                }
                            } else {
                                if (loginSuccessful && userRole.isNotEmpty()) {
                                    navigateToRoleDashboard(navController, userRole, context)
                                }
                            }
                        }
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveCredentialsDialog = false
                        if (loginSuccessful && userRole.isNotEmpty()) {
                            navigateToRoleDashboard(navController, userRole, context)
                        }
                    }
                ) {
                    Text("No")
                }
            }
        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // App Logo or Icon
            Image(
                painter = painterResource(id = R.drawable.ecorvilogo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(80.dp)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Welcome Text (split into two lines, centered, responsive)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ecorvi",
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F41BB)
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "School Management",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F41BB)
                    ),
                    textAlign = TextAlign.Center
                )
            }

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
                    modifier = Modifier.padding(24.dp)
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
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
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
                                    imageVector = if (passwordVisible) {
                                        Icons.Default.Visibility
                                    } else {
                                        Icons.Default.VisibilityOff
                                    },
                                    contentDescription = if (passwordVisible) {
                                        "Hide password"
                                    } else {
                                        "Show password"
                                    },
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
                            .clickable { navController.navigate("forgot_password") }
                            .padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            clearFocusAndHideKeyboard(context)
                            isLoading = true
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter email and password"
                                isLoading = false
                            } else {
                                errorMessage = ""
                                loginSuccessful = false
                                userRole = ""
                                
                                LoginUser(
                                    email = email,
                                    password = password,
                                    staySignedIn = staySignedIn,
                                    context = context,
                                    navController = navController,
                                    delayNavigation = !credentialsSaved
                                ) { success, role, message ->
                                    isLoading = false
                                    if (success) {
                                        loginSuccessful = true
                                        userRole = role ?: ""
                                        
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        
                                        // Only show save credentials dialog if credentials haven't been saved before
                                        if (!credentialsSaved) {
                                            showSaveCredentialsDialog = true
                                        } else {
                                            // If credentials are already saved, navigate immediately
                                            navigateToRoleDashboard(navController, userRole, context)
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
    delayNavigation: Boolean = false,
    onLoginResult: (Boolean, String?, String?) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val analytics = Firebase.analytics
    
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                
                // Save stay signed in preference immediately
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("stay_signed_in", staySignedIn)
                    .apply()
                
                // Log successful login event
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.METHOD, "email")
                    putString(FirebaseAnalytics.Param.SUCCESS, "true")
                    putString("user_id", user?.uid ?: "")
                }
                analytics.logEvent(FirebaseAnalytics.Event.LOGIN, params)

                // Get FCM token and update in Firestore
                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { tokenTask ->
                            val userDoc = FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(user?.uid ?: "")

                            val updates = hashMapOf<String, Any>(
                            "lastActive" to FieldValue.serverTimestamp(),
                            "staySignedIn" to staySignedIn  // Also store in Firestore
                        )

                        // Only update token if successfully retrieved
                        if (tokenTask.isSuccessful) {
                            val token = tokenTask.result
                            updates["fcmToken"] = token
                            updates["tokenUpdatedAt"] = FieldValue.serverTimestamp()
                            
                            // Save token in SharedPreferences
                            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("fcm_token", token)
                                .apply()
                        }

                            userDoc.update(updates)
                                .addOnSuccessListener {
                                Log.d("LoginScreen", "User data updated successfully")
                                    // Continue with role check and callback
                                    fetchUserRoleAndNotify(userDoc, context, delayNavigation, onLoginResult)
                                }
                                .addOnFailureListener { e ->
                                Log.e("LoginScreen", "Failed to update user data", e)
                                    // Continue anyway as this is not critical
                            fetchUserRoleAndNotify(userDoc, context, delayNavigation, onLoginResult)
                        }
                    }
            } else {
                val errorMsg = when (task.exception) {
                    is FirebaseAuthInvalidUserException -> "User not found. Check your email."
                    is FirebaseAuthInvalidCredentialsException -> "Invalid password. Try again."
                    else -> "Authentication failed. Please try again."
                }
                onLoginResult(false, null, errorMsg)
                
                // Log failed login attempt
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.METHOD, "email")
                    putString("error_type", task.exception?.javaClass?.simpleName ?: "unknown")
                }
                analytics.logEvent("login_failed", params)
            }
        }
}

private fun fetchUserRoleAndNotify(
    userDoc: DocumentReference,
    context: Context,
    delayNavigation: Boolean,
    onLoginResult: (Boolean, String?, String?) -> Unit
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

                    // Call back with success and role
                    onLoginResult(true, role.lowercase(), null)
                } else {
                    onLoginResult(false, null, "User role not found")
                }
            } else {
                onLoginResult(false, null, "User data not found")
            }
        }
        .addOnFailureListener { e ->
            onLoginResult(false, null, "Failed to fetch user role: ${e.localizedMessage}")
        }
}

// Helper function to clear focus and hide keyboard
fun clearFocusAndHideKeyboard(context: Context) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val window = (context as? Activity)?.window
    imm?.hideSoftInputFromWindow(window?.decorView?.windowToken, 0)
}

// Helper function to navigate to appropriate dashboard
private fun navigateToRoleDashboard(navController: NavController, role: String, context: Context) {
    clearFocusAndHideKeyboard(context)
    
    when (role.lowercase()) {
        "admin" -> {
            navController.navigate(BottomNavItem.Home.route) {
                popUpTo("login") { inclusive = true }
            }
        }
        "student" -> {
            navController.navigate(StudentBottomNavItem.Home.route) {
                popUpTo("login") { inclusive = true }
            }
        }
        "teacher" -> {
            navController.navigate("teacher_dashboard") {
                popUpTo("login") { inclusive = true }
            }
        }
        "parent" -> {
            navController.navigate("parent_dashboard") {
                popUpTo("login") { inclusive = true }
            }
        }
        "staff" -> {
            navController.navigate("staff_dashboard") {
                popUpTo("login") { inclusive = true }
            }
        }
    }
}


