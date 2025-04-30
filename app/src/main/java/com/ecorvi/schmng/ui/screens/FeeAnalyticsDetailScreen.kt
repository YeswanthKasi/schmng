package com.ecorvi.schmng.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.random.Random
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeAnalyticsDetailScreen(navController: NavController, monthIndex: Int) {
    val months = listOf("January", "February", "March", "April", "May", "June", 
                       "July", "August", "September", "October", "November", "December")
    
    // Dummy data for detailed analytics
    val feeDetails = remember {
        mapOf(
            "Total Collection" to Random.nextInt(800000, 1500000),
            "Pending Fees" to Random.nextInt(100000, 300000),
            "Late Fees" to Random.nextInt(10000, 50000)
        )
    }

    val classWiseCollection = remember {
        listOf(
            "Class 1" to Random.nextInt(50000, 100000),
            "Class 2" to Random.nextInt(50000, 100000),
            "Class 3" to Random.nextInt(50000, 100000),
            "Class 4" to Random.nextInt(50000, 100000),
            "Class 5" to Random.nextInt(50000, 100000)
        )
    }

    val feeCategories = remember {
        listOf(
            "Tuition Fee" to Random.nextInt(300000, 600000),
            "Transport Fee" to Random.nextInt(100000, 200000),
            "Library Fee" to Random.nextInt(50000, 100000),
            "Lab Fee" to Random.nextInt(50000, 100000),
            "Sports Fee" to Random.nextInt(30000, 80000)
        )
    }

    val dailyCollection = remember {
        List(30) { Random.nextInt(10000, 50000) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${months[monthIndex]} Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF1F41BB)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1F41BB)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    feeDetails.forEach { (title, amount) ->
                        OverviewCard(
                            title = title,
                            amount = amount,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Daily Collection Graph
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Daily Collection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            val maxAmount = dailyCollection.maxOrNull()?.toFloat() ?: 0f
                            val width = size.width - 40f
                            val height = size.height - 40f
                            val barWidth = width / dailyCollection.size

                            // Draw axes
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.3f),
                                start = Offset(40f, 0f),
                                end = Offset(40f, height),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.3f),
                                start = Offset(40f, height),
                                end = Offset(size.width, height),
                                strokeWidth = 1f
                            )

                            // Draw line graph
                            val path = Path()
                            dailyCollection.forEachIndexed { index, amount ->
                                val x = 40f + (index * barWidth)
                                val y = height - (amount / maxAmount * height)
                                if (index == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = Color(0xFF1F41BB),
                                style = Stroke(
                                    width = 3f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }

            // Class-wise Collection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Class-wise Collection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        classWiseCollection.forEach { (className, amount) ->
                            ClassCollectionItem(className, amount)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Fee Category Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Fee Category Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        feeCategories.forEach { (category, amount) ->
                            FeeCategoryItem(category, amount, feeCategories.maxOf { it.second })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    title: String,
    amount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when (title) {
                "Total Collection" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                "Pending Fees" -> Color(0xFFF44336).copy(alpha = 0.1f)
                else -> Color(0xFFFFA000).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = when (title) {
                    "Total Collection" -> Color(0xFF4CAF50)
                    "Pending Fees" -> Color(0xFFF44336)
                    else -> Color(0xFFFFA000)
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun ClassCollectionItem(className: String, amount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            className,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
        Text(
            "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F41BB)
        )
    }
}

@Composable
private fun FeeCategoryItem(category: String, amount: Int, maxAmount: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                category,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
            Text(
                "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F41BB)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = amount.toFloat() / maxAmount,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF1F41BB),
            trackColor = Color(0xFF1F41BB).copy(alpha = 0.1f)
        )
    }
} 