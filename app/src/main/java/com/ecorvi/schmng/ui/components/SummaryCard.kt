package com.ecorvi.schmng.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SummaryCard(
    title: String,
    image: Painter? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick(title) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (image != null) {
                Image(
                    painter = image,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}