package com.example.plannerapp.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {

    /**
     * Schedules a daily exact alarm for a plan reminder.
     *
     * The alarm fires at the specified [reminderTime] (HH:mm). If that time has already
     * passed today, the first alarm fires tomorrow at the same time. The alarm is registered
     * under [planId].toInt() as the request code, so each plan has an independent slot.
     *
     * The [ReminderReceiver] will re-schedule the next day's alarm automatically after firing.
     */
    fun schedule(
        context: Context,
        planId: Long,
        planHeading: String,
        reminderTime: String
    ) {
        val (hour, minute) = parseTime(reminderTime) ?: return

        val triggerAtMillis = nextOccurrenceMillis(hour, minute)

        val intent = buildAlarmIntent(context, planId, planHeading)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            planId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: check permission before scheduling exact alarms
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                // Fallback to inexact alarm if exact alarm permission not granted
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancels the scheduled reminder for a plan.
     */
    fun cancel(context: Context, planId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            planId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return // Already cancelled or never scheduled

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Parses "HH:mm" string into (hour, minute) pair, or null on failure. */
    fun parseTime(timeStr: String): Pair<Int, Int>? {
        val parts = timeStr.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return Pair(hour, minute)
    }

    /**
     * Returns the epoch millis of the next occurrence of [hour]:[minute].
     * If that time has already passed today, returns tomorrow's occurrence.
     */
    fun nextOccurrenceMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    /** Builds the broadcast intent carrying planId and planHeading extras. */
    fun buildAlarmIntent(context: Context, planId: Long, planHeading: String): Intent =
        Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.plannerapp.REMINDER_ALARM"
            putExtra("planId", planId)
            putExtra("planHeading", planHeading)
            putExtra("reminderTime", "")  // filled by ReminderReceiver when rescheduling
        }
}
