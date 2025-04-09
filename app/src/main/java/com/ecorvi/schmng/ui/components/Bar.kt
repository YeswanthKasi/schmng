package com.ecorvi.schmng.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Bar(height: Dp, percentage: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .height(height)
                .background(Color(0xFF3F51B5), shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = percentage,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Preview
@Composable
fun PreviewBar() {
    Bar(height = 100.dp, percentage = "50%")
}


