package com.ecorvi.schmng.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ecorvi.schmng.ui.utils.FileUtils
import com.ecorvi.schmng.ui.utils.ImagePicker

@Composable
fun ImagePickerDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    imagePicker: ImagePicker
) {
    if (showDialog) {
        val context = LocalContext.current
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Select Image")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ImagePickerOption(
                        icon = Icons.Default.Camera,
                        text = "Take Photo",
                        onClick = {
                            val uri = FileUtils.createImageUri(context)
                            imagePicker.captureImage(uri)
                            onDismiss()
                        }
                    )
                    
                    ImagePickerOption(
                        icon = Icons.Default.PhotoLibrary,
                        text = "Choose from Gallery",
                        onClick = {
                            imagePicker.pickImage()
                            onDismiss()
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ImagePickerOption(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
} 