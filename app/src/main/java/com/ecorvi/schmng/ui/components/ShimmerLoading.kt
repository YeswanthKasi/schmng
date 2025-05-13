package com.ecorvi.schmng.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerLoading(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12,
    shimmerColors: List<Color> = listOf(
        Color.LightGray.copy(alpha = 0.9f),
        Color.LightGray.copy(alpha = 0.4f),
        Color.LightGray.copy(alpha = 0.9f)
    )
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 1000f, translateAnim.value - 1000f),
        end = Offset(translateAnim.value, translateAnim.value)
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = brush)
        )
    }
}

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: Int = 100,
    showIcon: Boolean = true,
    showContent: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showIcon) {
                    ShimmerLoading(
                        modifier = Modifier.size(24.dp),
                        cornerRadius = 12
                    )
                }
                ShimmerLoading(
                    modifier = Modifier
                        .height(24.dp)
                        .width(120.dp),
                    cornerRadius = 4
                )
            }
            if (showContent) {
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerLoading(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    cornerRadius = 4
                )
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerLoading(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp),
                    cornerRadius = 4
                )
            }
        }
    }
}

@Composable
fun ShimmerCircle(
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    ShimmerLoading(
        modifier = modifier.size(size.dp),
        cornerRadius = size / 2
    )
}

@Composable
fun ShimmerText(
    modifier: Modifier = Modifier,
    width: Int = 200,
    height: Int = 20
) {
    ShimmerLoading(
        modifier = modifier
            .width(width.dp)
            .height(height.dp),
        cornerRadius = 4
    )
} 