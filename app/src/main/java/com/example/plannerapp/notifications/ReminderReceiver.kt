package com.example.plannerapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that fires when an AlarmManager reminder alarm goes off.
 *
 * Responsibilities:
 * 1. Post a notification to the user via [NotificationHelper].
 * 2. Re-schedule the next day's alarm so reminders repeat daily.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val planId = intent.getLongExtra("planId", -1L)
        val planHeading = intent.getStringExtra("planHeading") ?: return
        val reminderTime = intent.getStringExtra("reminderTime") ?: ""

        if (planId == -1L) return

        // 1. Post the notification
        NotificationHelper.postReminderNotification(context, planId, planHeading)

        // 2. Reschedule for tomorrow (daily repeat)
        if (reminderTime.isNotBlank()) {
            ReminderScheduler.schedule(
                context = context,
                planId = planId,
                planHeading = planHeading,
                reminderTime = reminderTime
            )
        } else {
            // If reminderTime wasn't forwarded in intent, reschedule for 24h later as fallback
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val nextTrigger = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                planId.toInt(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                nextTrigger,
                pendingIntent
            )
        }

        // Refresh the home screen widget for the daily rollover
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.example.plannerapp.widget.StreakWidgetUpdater.update(context)
        }
    }
}
