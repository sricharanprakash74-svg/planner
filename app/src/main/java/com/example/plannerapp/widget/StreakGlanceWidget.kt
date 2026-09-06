package com.example.plannerapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.plannerapp.MainActivity
import com.example.plannerapp.data.PlannerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StreakGlanceWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_BOX = DpSize(120.dp, 100.dp)
        private val WIDE_BOX = DpSize(240.dp, 100.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL_BOX, WIDE_BOX))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetData = loadWidgetData(context)

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                if (size.width >= 220.dp) {
                    WideStreakLayout(widgetData)
                } else {
                    CompactStreakLayout(widgetData)
                }
            }
        }
    }

    @Composable
    private fun CompactStreakLayout(data: WidgetStreakState) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(24.dp)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "STREAK",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(2.dp))

                // Streak Number
                Text(
                    text = "${data.streakCount}",
                    style = TextStyle(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.primary
                    )
                )

                Text(
                    text = "DAYS IN A ROW",
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Status Pill
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(10.dp)
                        .background(
                            if (data.allTodayDone) GlanceTheme.colors.primaryContainer
                            else if (data.pendingTasks > 0) GlanceTheme.colors.errorContainer
                            else GlanceTheme.colors.surfaceVariant
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (data.allTodayDone) "TODAY COMPLETE"
                        else if (data.pendingTasks > 0) "${data.pendingTasks} REMAINING"
                        else "NO TASKS TODAY",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (data.allTodayDone) GlanceTheme.colors.onPrimaryContainer
                            else if (data.pendingTasks > 0) GlanceTheme.colors.onErrorContainer
                            else GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun WideStreakLayout(data: WidgetStreakState) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(24.dp)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Streak Count
                Column(
                    modifier = GlanceModifier.fillMaxHeight().width(110.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ACTIVE STREAK",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    Text(
                        text = "${data.streakCount}",
                        style = TextStyle(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.primary
                        )
                    )

                    Text(
                        text = "DAYS IN A ROW",
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                // Right Column: Daily Status & Freeze Shield
                Column(
                    modifier = GlanceModifier.fillMaxHeight().fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Daily Completion Status Pill
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(10.dp)
                            .background(
                                if (data.allTodayDone) GlanceTheme.colors.primaryContainer
                                else if (data.pendingTasks > 0) GlanceTheme.colors.errorContainer
                                else GlanceTheme.colors.surfaceVariant
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (data.allTodayDone) "TODAY COMPLETE"
                            else if (data.pendingTasks > 0) "${data.pendingTasks} TASKS REMAINING"
                            else "ALL TASKS COMPLETED",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (data.allTodayDone) GlanceTheme.colors.onPrimaryContainer
                                else if (data.pendingTasks > 0) GlanceTheme.colors.onErrorContainer
                                else GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Streak Freeze Shield Indicator (from Credit System)
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(8.dp)
                            .background(GlanceTheme.colors.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (data.availableFreezes > 0) "SHIELD ACTIVE: ${data.availableFreezes} FREEZE"
                            else "SHIELD: 0 FREEZES",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (data.availableFreezes > 0) GlanceTheme.colors.primary
                                else GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // Interactive Refresh Tap Target
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.clickable(actionRunCallback<RefreshStreakAction>())
                    ) {
                        Text(
                            text = "SYNC WIDGET",
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.primary
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadWidgetData(context: Context): WidgetStreakState = withContext(Dispatchers.IO) {
        try {
            val db = PlannerDatabase.getDatabase(context)
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val user = db.userDao().getActiveUserOnce() ?: return@withContext WidgetStreakState()

            // Calculate historical streak
            val pastCheckins = db.plannerDao().getPastCheckins(user.userId, today)
            var streak = 0
            val checkinsByDate = pastCheckins.groupBy { it.exactDate }.toSortedMap(reverseOrder())
            for ((_, checkins) in checkinsByDate) {
                if (checkins.isNotEmpty() && checkins.all { it.isCompleted }) {
                    streak++
                } else {
                    break
                }
            }

            // Calculate today's completion status
            val todayTasks = db.plannerDao().getTasksForDate(today).first()
            val pendingTasks = todayTasks.count { !it.isCompleted }
            val allTodayDone = todayTasks.isNotEmpty() && pendingTasks == 0

            if (allTodayDone) {
                streak++
            }

            // Available streak freezes from Credit System
            val freezes = db.creditDao().getAvailableFreezes(user.userId).first()

            WidgetStreakState(
                streakCount = streak,
                pendingTasks = pendingTasks,
                totalTodayTasks = todayTasks.size,
                allTodayDone = allTodayDone,
                availableFreezes = freezes
            )
        } catch (e: Exception) {
            WidgetStreakState()
        }
    }
}

class RefreshStreakAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        StreakWidgetUpdater.update(context)
    }
}

data class WidgetStreakState(
    val streakCount: Int = 0,
    val pendingTasks: Int = 0,
    val totalTodayTasks: Int = 0,
    val allTodayDone: Boolean = false,
    val availableFreezes: Int = 0
)

object StreakWidgetUpdater {
    suspend fun update(context: Context) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(StreakGlanceWidget::class.java)
            glanceIds.forEach { id ->
                StreakGlanceWidget().update(context, id)
            }
        } catch (e: Exception) {
            // Safe guard against widget updates during app init
        }
    }
}
