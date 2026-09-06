package com.example.plannerapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.plannerapp.data.PlannerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that fires after the device boots (or after a reboot).
 *
 * AlarmManager alarms are cleared when the device restarts, so we must re-schedule
 * all active plan reminders here to ensure notifications keep firing.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        // Query the Room DB on an IO coroutine and re-schedule all active reminders
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PlannerDatabase.getDatabase(context)
                val plansWithReminders = db.plannerDao().getPlansWithRemindersEnabled()

                plansWithReminders.forEach { plan ->
                    val time = plan.reminderTime ?: "08:00"
                    ReminderScheduler.schedule(
                        context = context,
                        planId = plan.planId,
                        planHeading = plan.heading,
                        reminderTime = time
                    )
                }

                // Refresh the home screen widget after reboot
                com.example.plannerapp.widget.StreakWidgetUpdater.update(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
