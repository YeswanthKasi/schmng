package com.ecorvi.schmng.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun SchmngTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}