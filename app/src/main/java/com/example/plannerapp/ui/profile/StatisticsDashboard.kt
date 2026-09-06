package com.example.plannerapp.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.plannerapp.data.DailyCheckinEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StatisticsDashboard(
    weeklyCheckins: List<DailyCheckinEntity>,
    modifier: Modifier = Modifier
) {
    val totalCompleted = weeklyCheckins.count { it.isCompleted }
    
    // Group by date to find perfect days and daily counts
    val checkinsByDate = weeklyCheckins.groupBy { it.exactDate }
    val perfectDays = checkinsByDate.count { (_, tasks) -> tasks.isNotEmpty() && tasks.all { it.isCompleted } }

    Column(modifier = modifier.fillMaxWidth()) {
        // --- Top Stat Cards ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Completed Tasks",
                value = totalCompleted.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Perfect Days",
                value = perfectDays.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Weekly Bar Chart ---
        WeeklyBarChartCard(checkinsByDate = checkinsByDate)
        
        Spacer(modifier = Modifier.height(16.dp))

        // --- Mini Heatmap ---
        HeatmapCard(weeklyCheckins = weeklyCheckins)
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WeeklyBarChartCard(checkinsByDate: Map<String, List<DailyCheckinEntity>>) {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")

    // Map last 7 days to counts
    val last7Days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val counts = last7Days.map { date ->
        val dateStr = date.format(formatter)
        checkinsByDate[dateStr]?.count { it.isCompleted } ?: 0
    }

    val maxCount = counts.maxOrNull()?.coerceAtLeast(5) ?: 5
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Daily Completed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Bar Chart Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val barWidth = 24.dp.toPx()
                val cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                val spacing = (size.width - (barWidth * 7)) / 6

                for (i in 0 until 7) {
                    val count = counts[i]
                    val heightRatio = count.toFloat() / maxCount
                    val barHeight = size.height * heightRatio
                    val startX = i * (barWidth + spacing)

                    // Draw Track
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(startX, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = cornerRadius
                    )

                    // Draw Fill
                    if (count > 0) {
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(startX, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // X-Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                last7Days.forEach { date ->
                    Text(
                        text = date.format(dayFormatter).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapCard(weeklyCheckins: List<DailyCheckinEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Recent Activity Map",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // A simplified grid simulating the "Annual Heatmap" from the design, but just for recent data
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            
            // 3 rows, 7 columns (3 weeks)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0 until 7) {
                            val daysAgo = (2 - row) * 7 + (6 - col)
                            val date = today.minusDays(daysAgo.toLong())
                            val dateStr = date.format(formatter)
                            
                            val completedCount = weeklyCheckins.filter { it.exactDate == dateStr && it.isCompleted }.size
                            
                            val color = when {
                                completedCount == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                completedCount < 2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                completedCount < 4 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}
