package com.ecorvi.schmng.ui.utils

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import com.ecorvi.schmng.ui.components.PermissionRequester
import com.ecorvi.schmng.ui.components.ImagePickerDialog

object ImagePickerHelper {
    // Permission rationale messages
    const val CAMERA_PERMISSION_RATIONALE = "Camera permission is required to take profile pictures and capture documents. Please grant the permission in Settings."
    const val STORAGE_PERMISSION_RATIONALE = "Storage permission is required to select images from your gallery. Please grant the permission in Settings."

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun rememberImagePicker(
        snackbarHostState: SnackbarHostState,
        coroutineScope: CoroutineScope,
        onImagePicked: (Uri?) -> Unit
    ): ImagePicker {
        val context = LocalContext.current
        
        // Create image picker launcher
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            onImagePicked(uri)
        }

        // Create camera launcher
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                // The URI will be provided when launching the camera
                // We'll handle it in the captureImage function
            }
        }

        return remember(snackbarHostState, coroutineScope) {
            ImagePicker(
                context = context,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                imagePickerLauncher = imagePickerLauncher,
                cameraLauncher = cameraLauncher
            )
        }
    }
}

open class ImagePicker(
    protected val context: Context,
    protected val snackbarHostState: SnackbarHostState,
    protected val coroutineScope: CoroutineScope,
    val imagePickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    val cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>
) {
    open fun pickImage() {
        // Launch image picker with proper mime type
        imagePickerLauncher.launch("image/*")
    }

    open fun captureImage(outputUri: Uri) {
        // Launch camera with the provided output URI
        cameraLauncher.launch(outputUri)
    }
}

@Composable
fun ImagePickerWithPermissions(
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    onImagePicked: (Uri?) -> Unit,
    content: @Composable (ImagePicker, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var needsStoragePermission by remember { mutableStateOf(false) }
    var needsCameraPermission by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = ImagePickerHelper.rememberImagePicker(
        snackbarHostState = snackbarHostState,
        coroutineScope = coroutineScope,
        onImagePicked = { uri ->
            // Clear any pending camera URI
            pendingCameraUri = null
            onImagePicked(uri)
        }
    )

    // Storage permission handling
    if (needsStoragePermission) {
        PermissionRequester(
            permission = PermissionHandler.STORAGE_PERMISSIONS[0],
            rationale = ImagePickerHelper.STORAGE_PERMISSION_RATIONALE,
            snackbarHostState = snackbarHostState,
            coroutineScope = coroutineScope,
            onPermissionGranted = {
                needsStoragePermission = false
                imagePicker.pickImage()
            },
            onPermissionDenied = {
                needsStoragePermission = false
            }
        )
    }

    // Camera permission handling
    if (needsCameraPermission) {
        PermissionRequester(
            permission = PermissionHandler.CAMERA_PERMISSION,
            rationale = ImagePickerHelper.CAMERA_PERMISSION_RATIONALE,
            snackbarHostState = snackbarHostState,
            coroutineScope = coroutineScope,
            onPermissionGranted = {
                needsCameraPermission = false
                pendingCameraUri?.let { uri ->
                    imagePicker.captureImage(uri)
                }
            },
            onPermissionDenied = {
                needsCameraPermission = false
                pendingCameraUri = null
            }
        )
    }

    // Show image picker dialog
    ImagePickerDialog(
        showDialog = showDialog,
        onDismiss = { showDialog = false },
        imagePicker = object : ImagePicker(context, snackbarHostState, coroutineScope, imagePicker.imagePickerLauncher, imagePicker.cameraLauncher) {
            override fun pickImage() {
                if (!PermissionHandler.arePermissionsGranted(context, PermissionHandler.STORAGE_PERMISSIONS)) {
                    needsStoragePermission = true
                } else {
                    super.pickImage()
                }
            }

            override fun captureImage(outputUri: Uri) {
                if (!PermissionHandler.isPermissionGranted(context, PermissionHandler.CAMERA_PERMISSION)) {
                    pendingCameraUri = outputUri
                    needsCameraPermission = true
                } else {
                    super.captureImage(outputUri)
                }
            }
        }
    )

    // Pass the imagePicker and a function to show the dialog to the content
    content(imagePicker) { showDialog = true }
} 