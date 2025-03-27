package com.ecorvi.schmng.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OptionsDialog(
    category: String,
    onDismiss: () -> Unit,
    onAddClick: () -> Unit,
    onViewClick: () -> Unit,
    showAddInput: Boolean,
    showViewList: Boolean,
    inputText: String,
    onInputChange: (String) -> Unit,
    onConfirmAdd: (String) -> Unit,
    dataList: List<String>
) {
    if (showAddInput) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("Add $category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        label = { Text("Enter ${category.lowercase()} name/description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { onConfirmAdd(inputText) }) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(onClick = { onDismiss() }) {
                    Text("Cancel")
                }
            }
        )
    } else if (showViewList) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("View $category") },
            text = {
                if (dataList.isEmpty()) {
                    Text("No ${category.lowercase()} available.")
                } else {
                    Column {
                        dataList.forEach { item ->
                            Text(item)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onDismiss() }) {
                    Text("Close")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("Options for $category") },
            text = { Text("Choose an action:") },
            confirmButton = {
                Button(onClick = onAddClick) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(onClick = onViewClick) {
                    Text("View")
                }
            }
        )
    }
}