package com.ecorvi.schmng.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun PermissionExplanationDialog(
    showDialog: Boolean,
    permissionType: String, // "camera" or "storage"
    onDismiss: () -> Unit,
    onProceed: () -> Unit
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = if (permissionType == "camera") 
                            "Camera Permission" else "Photo Access Permission",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (permissionType == "camera")
                            "We need camera access to help you:\n\n" +
                            "• Take profile photos\n" +
                            "• Capture educational documents\n" +
                            "• Submit assignments\n\n" +
                            "Photos are stored securely and never used for advertising."
                        else
                            "We need photo access to help you:\n\n" +
                            "• Select profile photos\n" +
                            "• Upload existing documents\n" +
                            "• Submit assignments\n\n" +
                            "You control which photos to share.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text("NOT NOW")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = onProceed
                        ) {
                            Text("CONTINUE")
                        }
                    }
                }
            }
        }
    }
} 