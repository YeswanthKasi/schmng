package com.ecorvi.schmng.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AttendancePieChart(present: Int, absent: Int, leave: Int, total: Int) {
    val data = if (total > 0) {
        listOf(
            present.toFloat() / total.coerceAtLeast(1),
            absent.toFloat() / total.coerceAtLeast(1),
            leave.toFloat() / total.coerceAtLeast(1)
        )
    } else {
        listOf(0f, 0f, 0f)
    }
    
    val colors = listOf(
        Color(0xFF4CAF50), // Green for present
        Color(0xFFD32F2F), // Red for absent
        Color(0xFFFFA000)  // Orange for leave
    )
    val hasData = data.any { it > 0f }

    Box(
        modifier = Modifier
            .size(200.dp)
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (hasData) {
                var startAngle = -90f
                data.forEachIndexed { index, value ->
                    if (value > 0f) {
                        drawArc(
                            color = colors[index],
                            startAngle = startAngle,
                            sweepAngle = value * 360f,
                            useCenter = true
                        )
                        startAngle += value * 360f
                    }
                }
            } else {
                // Draw empty circle if no data
                drawCircle(
                    color = Color.LightGray,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
} 