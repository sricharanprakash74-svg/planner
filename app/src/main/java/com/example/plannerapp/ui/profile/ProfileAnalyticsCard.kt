package com.example.plannerapp.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.plannerapp.data.DailyCheckinEntity
import com.example.plannerapp.theme.AppDimens
import com.example.plannerapp.ui.analytics.ConsistencyStockChartCard

/**
 * Personal Analytics section for the Profile screen.
 *
 * Deliberately flat — the chart renders directly on the scroll surface without
 * any surrounding card wrapper. This removes the box-inside-box nesting that was
 * previously: ProfileAnalyticsCard (Card) -> ConsistencyStockChartCard (Card) -> content.
 */
@Composable
fun ProfileAnalyticsCard(
    weeklyCheckins: List<DailyCheckinEntity>,
    streak: Int,
    onAnalyticsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header — no Card wrapper, just a row on the scroll surface
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Personal Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Consistency & performance curve",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onAnalyticsClick) {
                Text(
                    text = "Full View",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.Space12))

        // Chart renders directly on the surface — no outer Card nesting
        ConsistencyStockChartCard(
            weeklyCheckins = weeklyCheckins,
            streak = streak
        )
    }
}
