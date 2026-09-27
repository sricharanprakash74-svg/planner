package com.example.plannerapp.ui.analytics.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Component B: Multi-Week Pastel Bar Chart
 *
 * Visuals: Renders the current month partitioned by week columns (Week 1 through Week 5),
 * matching an editorial habit tracker grid.
 *
 * Layout: Each week column uses Modifier.weight(week.dayCount.toFloat()) so weeks with
 * fewer days (e.g. 2-3 days in Week 5) scale proportionally.
 *
 * Color Identity:
 * - Week 1: Coral (#E28A73)
 * - Week 2: Dusty Rose (#D4828E)
 * - Week 3: Sage Green (#86AFA6)
 * - Week 4: Warm Honey (#DCB773)
 * - Week 5: Pastel Olive (#A5B88F)
 *
 * Animation: Bars rise smoothly from 0 to target height using animateFloatAsState (800ms duration).
 */
@Composable
fun MultiWeekPastelChart(
    uiState: AnalyticsGraphUiState,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val containerBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        Color(0xFFFBF8F5)
    }

    // Animation trigger: bars rise smoothly from 0 to target height
    var hasAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.weeklyPhases) {
        hasAppeared = false
        hasAppeared = true
    }

    val barProgress by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "multiWeekBarRise"
    )

    // Compute consistent scale across all weekly phases
    val maxScale = remember(uiState.weeklyPhases) {
        val maxVal = uiState.weeklyPhases.flatMap { it.dailyCounts }.maxOrNull() ?: 0
        maxVal.coerceAtLeast(4)
    }

    val currentMonthTitle = remember {
        val now = LocalDate.now()
        now.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.20f else 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFFE28A73),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Monthly Grid Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Partitioned by week phases",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = currentMonthTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Multi-Week Columns Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                uiState.weeklyPhases.forEach { week ->
                    // Proportional sizing: weight based on number of days in the week
                    Column(
                        modifier = Modifier
                            .weight(week.dayCount.toFloat())
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Rounded capsule header with unique pastel accent
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = week.headerBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = week.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = week.themeColor,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Daily Bars Row inside this week
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            week.dailyCounts.forEach { count ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    // Subtle background track
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(week.themeColor.copy(alpha = 0.12f))
                                    )

                                    // Active rising bar
                                    val ratio = (count.toFloat() / maxScale).coerceIn(0f, 1f)
                                    val currentHeightFraction = (ratio * barProgress).coerceIn(0f, 1f)

                                    if (currentHeightFraction > 0.01f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(currentHeightFraction)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(week.themeColor)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Week Total Footer
                        Text(
                            text = "${week.dailyCounts.sum()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = week.themeColor
                        )
                    }
                }
            }
        }
    }
}
