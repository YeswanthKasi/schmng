package com.ecorvi.schmng.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.painter.BitmapPainter
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import id.zelory.compressor.constraint.size
import java.io.File
import com.ecorvi.schmng.ui.data.FirestoreDatabase
import com.ecorvi.schmng.ui.utils.ImagePickerWithPermissions

@Composable
fun ProfilePhotoComponent(
    userId: String,
    photoUrl: String?,
    isEditable: Boolean = false,
    themeColor: Color = Color(0xFF1F41BB),
    onPhotoUpdated: (String) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val db = FirebaseFirestore.getInstance()
    
    // Load existing photo from Base64 string
    LaunchedEffect(photoUrl) {
        if (!photoUrl.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(photoUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageBitmap = bitmap?.asImageBitmap()
            } catch (e: Exception) {
                onError("Failed to load profile photo")
            }
        }
    }

    // Function to process the selected image
    fun processImage(uri: Uri?) {
        if (uri != null) {
            isUploading = true
            uploadProgress = 0f
            scope.launch {
                try {
                    // Convert Uri to File and compress
                    val originalFile = File(context.cacheDir, "profile_photo")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        originalFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Compress the image
                    val compressedFile = Compressor.compress(context, originalFile) {
                        default(width = 500, format = Bitmap.CompressFormat.JPEG)
                        size(500_000) // 500KB max
                    }

                    // Convert to Base64
                    val bytes = compressedFile.readBytes()
                    val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)

                    // Update profile photo using FirestoreDatabase
                    val success = FirestoreDatabase.updateProfilePhoto(userId, base64String)
                    if (success) {
                        onPhotoUpdated(base64String)
                        // Update local bitmap
                        val bitmap = BitmapFactory.decodeFile(compressedFile.absolutePath)
                        imageBitmap = bitmap?.asImageBitmap()
                    } else {
                        onError("Failed to update profile photo")
                    }

                    // Clean up files
                    originalFile.delete()
                    compressedFile.delete()
                    
                    isUploading = false
                    uploadProgress = 0f

                } catch (e: Exception) {
                    onError(e.message ?: "Failed to process photo")
                    isUploading = false
                }
            }
        }
    }

    ImagePickerWithPermissions(
        snackbarHostState = snackbarHostState,
        coroutineScope = scope,
        onImagePicked = { uri -> processImage(uri) }
    ) { imagePicker, showPicker ->
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(2.dp, themeColor, CircleShape)
                .clickable(enabled = isEditable && !isUploading) {
                    if (isEditable && !isUploading) {
                        showPicker()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    progress = uploadProgress,
                    color = themeColor,
                    modifier = Modifier.size(40.dp)
                )
            } else if (imageBitmap != null) {
                Image(
                    painter = BitmapPainter(imageBitmap!!),
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Default Profile Photo",
                    modifier = Modifier.size(80.dp),
                    tint = themeColor
                )
            }

            if (isEditable && !isUploading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(themeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Photo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Snackbar host for permission messages
        Box(modifier = Modifier.fillMaxSize()) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
} 