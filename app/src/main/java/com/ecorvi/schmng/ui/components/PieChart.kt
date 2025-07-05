package com.ecorvi.schmng.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import androidx.compose.runtime.saveable.rememberSaveable

private val TeacherBlue = Color(0xFF2196F3)
private val StudentGreen = Color(0xFF4CAF50)
private val StaffPurple = Color(0xFF9C27B0)  // New color for non-teaching staff

@Composable
fun AnalyticsPieChart(
    studentCount: Int,
    teacherCount: Int,
    staffCount: Int,  // Added parameter
    onStudentClick: () -> Unit,
    onTeacherClick: () -> Unit,
    onStaffClick: () -> Unit,  // Added parameter
    modifier: Modifier = Modifier,
    hasAnimated: Boolean = false,
    setHasAnimated: () -> Unit = {}
) {
    val total = studentCount + teacherCount + staffCount
    if (total == 0) return

    var studentPressed by remember { mutableStateOf(false) }
    var teacherPressed by remember { mutableStateOf(false) }
    var staffPressed by remember { mutableStateOf(false) }  // Added state

    // REMOVE ANIMATION: always use 1f for progress
    val animatedProgress = 1f

    // Get screen width to determine layout
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isLargeScreen = screenWidth > 600.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isLargeScreen) {
                    Modifier.heightIn(max = 220.dp)
                } else {
                    Modifier.heightIn(max = 220.dp)
                }
            )
            .padding(horizontal = if (isLargeScreen) 16.dp else 10.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pie Chart
            Box(
                modifier = Modifier
                    .weight(if (isLargeScreen) 0.5f else 0.45f)
                    .aspectRatio(1f)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val center = size.width / 2f
                            val touchX = offset.x - center
                            val touchY = offset.y - center
                            
                            val distance = sqrt(touchX * touchX + touchY * touchY)
                            val ringRadius = size.width / 2f * 0.9f
                            val innerRadius = ringRadius - (ringRadius * 0.45f)
                            
                            if (distance >= innerRadius && distance <= ringRadius) {
                                var angle = atan2(touchY, touchX) * (180f / PI.toFloat())
                                if (angle < 0) angle += 360f
                                
                                when {
                                    angle in 0f..120f -> {
                                        teacherPressed = true
                                        onTeacherClick()
                                    }
                                    angle in 120f..240f -> {
                                        studentPressed = true
                                        onStudentClick()
                                    }
                                    else -> {
                                        staffPressed = true
                                        onStaffClick()
                                    }
                                }
                            }
                        }
                    }
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val center = size.width / 2f
                    val radius = if (isLargeScreen) center * 0.45f else center * 0.8f
                    val strokeWidth = if (isLargeScreen) radius * 0.6f else radius * 0.6f
                    
                    // Background ring
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.15f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(center - radius, center - radius)
                    )

                    // Calculate proportions
                    val teacherProportion = teacherCount.toFloat() / total
                    val studentProportion = studentCount.toFloat() / total
                    val staffProportion = staffCount.toFloat() / total

                    // Gap angles
                    val gapSize = 3f
                    val halfGap = gapSize / 2f
                    val totalAngle = 360f - gapSize * 3 // Total available angle minus gaps
                    
                    // Calculate sweep angles based on proportions
                    val teacherSweep = totalAngle * teacherProportion * animatedProgress
                    val studentSweep = totalAngle * studentProportion * animatedProgress
                    val staffSweep = totalAngle * staffProportion * animatedProgress
                    
                    // Teacher segment
                    drawArc(
                        color = TeacherBlue.copy(alpha = if (teacherPressed) 0.7f else 0.9f),
                        startAngle = halfGap,
                        sweepAngle = teacherSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(center - radius, center - radius)
                    )
                    
                    // Student segment
                    drawArc(
                        color = StudentGreen.copy(alpha = if (studentPressed) 0.7f else 0.9f),
                        startAngle = teacherSweep + gapSize + halfGap,
                        sweepAngle = studentSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(center - radius, center - radius)
                    )

                    // Staff segment
                    drawArc(
                        color = StaffPurple.copy(alpha = if (staffPressed) 0.7f else 0.9f),
                        startAngle = teacherSweep + studentSweep + gapSize * 2 + halfGap,
                        sweepAngle = staffSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(center - radius, center - radius)
                    )
                }
            }

            // Labels and Counts
            Column(
                modifier = Modifier
                    .weight(if (isLargeScreen) 0.5f else 0.55f)
                    .padding(start = if (isLargeScreen) 32.dp else 32.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Teachers row
                LegendItem(
                    color = TeacherBlue,
                    label = "Teachers",
                    count = teacherCount,
                    onClick = onTeacherClick,
                    isLargeScreen = isLargeScreen
                )

                // Students row
                LegendItem(
                    color = StudentGreen,
                    label = "Students",
                    count = studentCount,
                    onClick = onStudentClick,
                    isLargeScreen = isLargeScreen
                )

                // Staff row
                LegendItem(
                    color = StaffPurple,
                    label = "Non-Teaching Staff",
                    count = staffCount,
                    onClick = onStaffClick,
                    isLargeScreen = isLargeScreen
                )
            }
        }
    }

    LaunchedEffect(studentPressed, teacherPressed, staffPressed) {
        if (studentPressed || teacherPressed || staffPressed) {
            delay(100)
            studentPressed = false
            teacherPressed = false
            staffPressed = false
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    count: Int,
    onClick: () -> Unit,
    isLargeScreen: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontSize = if (isLargeScreen) 16.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray
                ),
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = count.toString(),
            style = TextStyle(
                fontSize = if (isLargeScreen) 16.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
} 