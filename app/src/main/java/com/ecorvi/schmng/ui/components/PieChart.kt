package com.ecorvi.schmng.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize.Companion.Zero
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val StudentGreen = Color(0xFF4CAF50)
private val TeacherBlue = Color(0xFF2196F3)

// Define color map for segments
private val segmentColorMap = mapOf(
    TeacherBlue to "teacher",
    StudentGreen to "student"
)

@Composable
fun PieChart(
    studentCount: Int,
    teacherCount: Int,
    onStudentClick: () -> Unit,
    onTeacherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = (studentCount + teacherCount).coerceAtLeast(1)
    
    var studentPressed by remember { mutableStateOf(false) }
    var teacherPressed by remember { mutableStateOf(false) }
    var componentSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .onSizeChanged { componentSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val centerY = componentSize.height / 2f
                        val centerX = componentSize.width / 2f
                        val radius = minOf(componentSize.width, componentSize.height) / 2.2f
                        
                        // Calculate distance from center
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                        
                        // Only process click if it's within the pie chart radius
                        if (distance <= radius) {
                            // Check if click is in top or bottom half
                            if (offset.y < centerY) {
                                // Top half - Teachers
                                if (teacherCount > 0) {
                                    teacherPressed = true
                                    onTeacherClick()
                                }
                            } else {
                                // Bottom half - Students
                                if (studentCount > 0) {
                                    studentPressed = true
                                    onStudentClick()
                                }
                            }
                        }
                    }
                }
        ) {
            val radius = minOf(componentSize.width, componentSize.height) / 2.2f
            val centerX = componentSize.width / 2
            val centerY = componentSize.height / 2
            val strokeWidth = radius * 5f

            if (total > 0) {
                // Draw teacher section (top half)
                drawArc(
                    color = TeacherBlue.copy(
                        alpha = if (teacherPressed) 0.7f else 0.85f
                    ),
                    startAngle = -180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    size = Size(radius * 5f, radius * 5f),
                    topLeft = Offset(centerX - radius, centerY - radius)
                )

                // Draw student section (bottom half)
                drawArc(
                    color = StudentGreen.copy(
                        alpha = if (studentPressed) 0.7f else 0.85f
                    ),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    size = Size(radius * 2f, radius * 2f),
                    topLeft = Offset(centerX - radius, centerY - radius)
                )
            }
        }

        // Legend
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Teachers count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clickable(enabled = teacherCount > 0) { 
                        teacherPressed = true
                        onTeacherClick()
                    }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TeacherBlue, RoundedCornerShape(2.dp))
                )
                Column {
                    Text(
                        text = "Teachers",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.DarkGray
                        )
                    )
                    Text(
                        text = "$teacherCount",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                    )
                }
            }

            // Students count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clickable(enabled = studentCount > 0) { 
                        studentPressed = true
                        onStudentClick()
                    }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(StudentGreen, RoundedCornerShape(2.dp))
                )
                Column {
                    Text(
                        text = "Students",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.DarkGray
                        )
                    )
                    Text(
                        text = "$studentCount",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                    )
                }
            }
        }

        // Reset pressed states after a short delay
        LaunchedEffect(studentPressed, teacherPressed) {
            if (studentPressed || teacherPressed) {
                kotlinx.coroutines.delay(200)
                studentPressed = false
                teacherPressed = false
            }
        }
    }
} 