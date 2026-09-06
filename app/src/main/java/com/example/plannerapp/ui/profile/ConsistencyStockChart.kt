package com.example.plannerapp.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plannerapp.data.DailyCheckinEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class TimeRange(val days: Int, val label: String) {
    WEEK(7, "7D"),
    TWO_WEEKS(14, "14D"),
    MONTH(30, "30D")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsistencyStockChartCard(
    weeklyCheckins: List<DailyCheckinEntity>,
    streak: Int,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(TimeRange.WEEK) }
    val today = remember { LocalDate.now() }
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val labelFormatter = DateTimeFormatter.ofPattern("MMM d")

    // Filter checkins for the selected time range
    val startDate = remember(selectedRange, today) { today.minusDays((selectedRange.days - 1).toLong()) }
    val relevantCheckins = remember(weeklyCheckins, startDate, today) {
        val startStr = startDate.format(dateFormatter)
        val endStr = today.format(dateFormatter)
        weeklyCheckins.filter { it.exactDate in startStr..endStr }
    }

    val totalScheduledInPeriod = relevantCheckins.size
    val totalCompletedInPeriod = relevantCheckins.count { it.isCompleted }

    // Group checkins by date
    val checkinsByDate = remember(relevantCheckins) {
        relevantCheckins.groupBy { it.exactDate }
    }

    // Build the list of dates for the range
    val dateList = remember(selectedRange, today) {
        (selectedRange.days - 1 downTo 0).map { today.minusDays(it.toLong()) }
    }

    // Compute REAL daily completion percentage (0.0 to 100.0) for each day
    val dailyPercentages = remember(dateList, checkinsByDate) {
        dateList.map { date ->
            val dateStr = date.format(dateFormatter)
            val dayTasks = checkinsByDate[dateStr] ?: emptyList()
            if (dayTasks.isNotEmpty()) {
                (dayTasks.count { it.isCompleted }.toFloat() / dayTasks.size) * 100f
            } else {
                0f
            }
        }
    }

    val hasAnyData = totalScheduledInPeriod > 0
    val periodConsistency = if (hasAnyData) {
        ((totalCompletedInPeriod.toFloat() / totalScheduledInPeriod) * 100).toInt()
    } else {
        0
    }

    // Calculate real trend: compare 2nd half of period vs 1st half
    val halfSize = (dateList.size / 2).coerceAtLeast(1)
    val firstHalfPercentages = dailyPercentages.take(halfSize)
    val secondHalfPercentages = dailyPercentages.takeLast(halfSize)
    val firstHalfAvg = if (firstHalfPercentages.isNotEmpty()) firstHalfPercentages.average().toFloat() else 0f
    val secondHalfAvg = if (secondHalfPercentages.isNotEmpty()) secondHalfPercentages.average().toFloat() else 0f
    val trendDifference = secondHalfAvg - firstHalfAvg
    val isPositive = trendDifference >= 0

    val trendColor = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444) // Emerald vs Red
    val chartColor = if (isPositive) MaterialTheme.colorScheme.primary else Color(0xFFEF4444)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Title & Time Range Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Consistency Rate (${selectedRange.label})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (hasAnyData) {
                        Text(
                            text = "$periodConsistency%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "No Data",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                // Growth / Trend badge (shown only when real data exists)
                if (hasAnyData) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = trendColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%s%.0f%%".format(if (isPositive) "+" else "", trendDifference),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = trendColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Range Selectors (7D, 14D, 30D)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TimeRange.entries.forEach { range ->
                    val isSelected = selectedRange == range
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedRange = range },
                        label = { Text(range.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasAnyData) {
                // Meaningful Empty State when no tasks exist
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No task activity recorded in this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Complete daily tasks to track your consistency momentum here",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                // Stock Chart Line & Gradient Area using REAL data points
                RealStockLineChart(
                    points = dailyPercentages,
                    lineColor = chartColor,
                    fillColor = chartColor.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateList.first().format(labelFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (dateList.size > 2) {
                    Text(
                        text = dateList[dateList.size / 2].format(labelFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = dateList.last().format(labelFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RealStockLineChart(
    points: List<Float>,
    lineColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    // Scale from 0% to 100%
    val minVal = 0f
    val maxVal = 100f
    val valRange = maxVal - minVal

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = width / (points.size - 1)

        val strokePath = Path()
        val fillPath = Path()

        val coordinates = points.mapIndexed { index, value ->
            val x = index * spacing
            val normalizedY = (value.coerceIn(0f, 100f) - minVal) / valRange
            val y = height - (normalizedY * height)
            Offset(x, y)
        }

        // Build smooth cubic bezier curve
        strokePath.moveTo(coordinates[0].x, coordinates[0].y)
        fillPath.moveTo(coordinates[0].x, coordinates[0].y)

        for (i in 0 until coordinates.size - 1) {
            val p0 = coordinates[i]
            val p1 = coordinates[i + 1]
            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

            strokePath.cubicTo(
                controlPoint1.x, controlPoint1.y,
                controlPoint2.x, controlPoint2.y,
                p1.x, p1.y
            )
            fillPath.cubicTo(
                controlPoint1.x, controlPoint1.y,
                controlPoint2.x, controlPoint2.y,
                p1.x, p1.y
            )
        }

        // Close gradient fill area
        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()

        // Draw background gradient
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw stock price line
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw end indicator point
        val lastPoint = coordinates.last()
        drawCircle(
            color = lineColor.copy(alpha = 0.25f),
            radius = 8.dp.toPx(),
            center = lastPoint
        )
        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx(),
            center = lastPoint
        )
    }
}
