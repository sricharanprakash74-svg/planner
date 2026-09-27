package com.example.plannerapp.ui.analytics.components

import androidx.compose.ui.graphics.Color
import com.example.plannerapp.data.DailyCheckinEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Lightweight UI-only model representing a single week column partitioned in the multi-week chart.
 */
data class WeekPhaseData(
    val title: String,
    val themeColor: Color,
    val headerBackground: Color,
    val dailyCounts: List<Int>
) {
    val dayCount: Int get() = dailyCounts.size.coerceAtLeast(1)
}

/**
 * Lightweight UI-only state bridging task metrics into modern Compose charts.
 */
data class AnalyticsGraphUiState(
    val monthCurveData: List<Float> = emptyList(), // Daily normalized or raw task completion counts
    val weeklyPhases: List<WeekPhaseData> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * Mapper bridging domain check-in streams into the modern analytics graph contract.
 */
object AnalyticsGraphMapper {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun mapFromCheckins(
        checkins: List<DailyCheckinEntity>,
        referenceDate: LocalDate = LocalDate.now()
    ): AnalyticsGraphUiState {
        val year = referenceDate.year
        val month = referenceDate.monthValue
        val daysInMonth = referenceDate.lengthOfMonth()

        val checkinsByDate = checkins.groupBy { it.exactDate }

        // 1. Month Curve Data: daily completed task counts across the current month
        val monthCurveData = (1..daysInMonth).map { day ->
            val date = LocalDate.of(year, month, day)
            val dateStr = date.format(dateFormatter)
            val dayCheckins = checkinsByDate[dateStr] ?: emptyList()
            dayCheckins.count { it.isCompleted }.toFloat()
        }

        // 2. Weekly Phases (Week 1 through Week 5 with pastel color identity)
        val weekDefs = listOf(
            Triple("WEEK 1", Color(0xFFE28A73), 1..7),         // Coral
            Triple("WEEK 2", Color(0xFFD4828E), 8..14),        // Dusty Rose
            Triple("WEEK 3", Color(0xFF86AFA6), 15..21),       // Sage Green
            Triple("WEEK 4", Color(0xFFDCB773), 22..28),       // Warm Honey
            Triple("WEEK 5", Color(0xFFA5B88F), 29..daysInMonth) // Pastel Olive
        )

        val weeklyPhases = weekDefs.mapNotNull { (title, themeColor, range) ->
            if (range.first > daysInMonth) {
                null
            } else {
                val actualEnd = range.last.coerceAtMost(daysInMonth)
                val dailyCounts = (range.first..actualEnd).map { day ->
                    val date = LocalDate.of(year, month, day)
                    val dateStr = date.format(dateFormatter)
                    checkinsByDate[dateStr]?.count { it.isCompleted } ?: 0
                }
                WeekPhaseData(
                    title = title,
                    themeColor = themeColor,
                    headerBackground = themeColor.copy(alpha = 0.15f),
                    dailyCounts = dailyCounts
                )
            }
        }

        return AnalyticsGraphUiState(
            monthCurveData = monthCurveData,
            weeklyPhases = weeklyPhases,
            isLoading = false
        )
    }
}
