package com.ecorvi.schmng.ui.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object PermissionHandler {
    // Permission groups
    val CAMERA_PERMISSION = Manifest.permission.CAMERA
    val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // Check if a single permission is granted
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    // Check if all permissions in a group are granted
    fun arePermissionsGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Handle permission result
    fun handlePermissionResult(
        permissions: Map<String, Boolean>,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        if (permissions.values.all { it }) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    // Show permission rationale in a Snackbar
    suspend fun showPermissionRationale(
        snackbarHostState: SnackbarHostState,
        message: String,
        actionLabel: String,
        context: Context,
        onActionPerformed: () -> Unit = {}
    ) {
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long
        )
        when (result) {
            SnackbarResult.ActionPerformed -> {
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                onActionPerformed()
            }
            SnackbarResult.Dismissed -> {
                // User dismissed the Snackbar
            }
        }
    }

    // Composable to remember permission state
    @Composable
    fun rememberPermissionState(
        permission: String,
        snackbarHostState: SnackbarHostState,
        coroutineScope: CoroutineScope
    ): PermissionState {
        val context = LocalContext.current
        return remember(permission) {
            PermissionState(
                permission = permission,
                context = context,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope
            )
        }
    }
}

// Permission state class to handle individual permission
class PermissionState(
    private val permission: String,
    private val context: Context,
    private val snackbarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope
) {
    val isGranted: Boolean
        get() = PermissionHandler.isPermissionGranted(context, permission)

    fun showRationale(message: String) {
        coroutineScope.launch {
            PermissionHandler.showPermissionRationale(
                snackbarHostState = snackbarHostState,
                message = message,
                actionLabel = "Settings",
                context = context
            )
        }
    }
}