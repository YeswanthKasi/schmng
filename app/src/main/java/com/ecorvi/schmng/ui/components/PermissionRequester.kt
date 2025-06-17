package com.ecorvi.schmng.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import com.ecorvi.schmng.ui.utils.PermissionHandler

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequester(
    permission: String,
    rationale: String,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(permission = permission)
    
    LaunchedEffect(permissionState.status.isGranted) {
        when {
            permissionState.status.isGranted -> {
                onPermissionGranted()
            }
            permissionState.status.shouldShowRationale -> {
                PermissionHandler.showPermissionRationale(
                    snackbarHostState = snackbarHostState,
                    message = rationale,
                    actionLabel = "Settings",
                    context = context
                )
                onPermissionDenied()
            }
            else -> {
                permissionState.launchPermissionRequest()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MultiplePermissionRequester(
    permissions: Array<String>,
    rationale: String,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    onAllPermissionsGranted: () -> Unit = {},
    onPermissionsDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val permissionStates = permissions.map { permission ->
        rememberPermissionState(permission = permission)
    }
    
    val allGranted = permissionStates.all { it.status.isGranted }
    val shouldShowRationale = permissionStates.any { it.status.shouldShowRationale }
    
    LaunchedEffect(allGranted, shouldShowRationale) {
        when {
            allGranted -> {
                onAllPermissionsGranted()
            }
            shouldShowRationale -> {
                PermissionHandler.showPermissionRationale(
                    snackbarHostState = snackbarHostState,
                    message = rationale,
                    actionLabel = "Settings",
                    context = context
                )
                onPermissionsDenied()
            }
            else -> {
                permissionStates.forEach { it.launchPermissionRequest() }
            }
        }
    }
} 