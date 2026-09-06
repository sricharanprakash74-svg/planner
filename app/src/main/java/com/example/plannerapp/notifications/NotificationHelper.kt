package com.example.plannerapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.plannerapp.MainActivity
import com.example.plannerapp.R

object NotificationHelper {

    const val CHANNEL_ID = "plan_reminders"
    private const val CHANNEL_NAME = "Plan Reminders"
    private const val CHANNEL_DESCRIPTION = "Daily reminder notifications for your active plans"

    /**
     * Creates the notification channel. Safe to call multiple times — Android ignores
     * subsequent calls if the channel already exists.
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a reminder notification for a plan. Tapping the notification opens the app.
     *
     * @param planId   Used as the notification ID so each plan has its own notification slot.
     * @param planHeading  Title text displayed in the notification.
     */
    fun postReminderNotification(context: Context, planId: Long, planHeading: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tap-to-open: launch MainActivity (which re-navigates to the correct screen)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("planId", planId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            planId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Daily Reminder: $planHeading")
            .setContentText("Time to check in on your plan and complete today's tasks.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Time to check in on \"$planHeading\" and mark today's tasks complete. Stay consistent — every day counts.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(planId.toInt(), notification)
    }
}
