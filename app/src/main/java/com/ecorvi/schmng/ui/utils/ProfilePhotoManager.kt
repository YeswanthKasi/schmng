package com.ecorvi.schmng.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfilePhotoManager(private val context: Context) {
    private var tempPhotoUri: Uri? = null
    private var photoFile: File? = null

    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            photoFile = this
        }
    }

    fun getTempPhotoUri(): Uri? {
        val file = createImageFile()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        ).also { tempPhotoUri = it }
    }

    fun getCameraIntent(): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, getTempPhotoUri())
        }
    }

    fun getGalleryIntent(): Intent {
        return Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    }

    fun getCropIntent(sourceUri: Uri): Intent {
        val destinationUri = Uri.fromFile(File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        return UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1024, 1024)
            .getIntent(context)
    }

    fun cleanup() {
        photoFile?.delete()
        tempPhotoUri = null
        photoFile = null
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberProfilePhotoState(
    context: Context,
    onPhotoSelected: (Uri) -> Unit
): ProfilePhotoState {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val storagePermissionState = rememberPermissionState(
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    val photoManager = remember { ProfilePhotoManager(context) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoManager.getTempPhotoUri()?.let { uri ->
                onPhotoSelected(uri)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onPhotoSelected(it) }
    }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { onPhotoSelected(it) }
        }
    }

    return remember(photoManager, cameraLauncher, galleryLauncher, cropLauncher) {
        ProfilePhotoState(
            photoManager = photoManager,
            cameraPermissionState = cameraPermissionState,
            storagePermissionState = storagePermissionState,
            cameraLauncher = cameraLauncher,
            galleryLauncher = galleryLauncher,
            cropLauncher = cropLauncher
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
class ProfilePhotoState(
    private val photoManager: ProfilePhotoManager,
    val cameraPermissionState: PermissionState,
    val storagePermissionState: PermissionState,
    private val cameraLauncher: ActivityResultLauncher<Uri>,
    private val galleryLauncher: ActivityResultLauncher<String>,
    private val cropLauncher: ActivityResultLauncher<Intent>
) {
    fun launchCamera() {
        photoManager.getTempPhotoUri()?.let { uri ->
            cameraLauncher.launch(uri)
        }
    }

    fun launchGallery() {
        galleryLauncher.launch("image/*")
    }

    fun launchCrop(sourceUri: Uri) {
        cropLauncher.launch(photoManager.getCropIntent(sourceUri))
    }

    fun cleanup() {
        photoManager.cleanup()
    }
} 