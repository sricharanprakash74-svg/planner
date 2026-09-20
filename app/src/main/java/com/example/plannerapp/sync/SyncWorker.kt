package com.example.plannerapp.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.plannerapp.data.PlannerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (runAttemptCount > 3) {
            Log.w("SyncWorker", "Max retry attempts reached ($runAttemptCount). Aborting sync.")
            return@withContext Result.failure()
        }

        try {
            Log.d("SyncWorker", "Starting sync job...")
            
            val db = PlannerDatabase.getDatabase(applicationContext)
            val dao = db.plannerDao()
            val badgeDao = db.badgeDao()

            // 1. Fetch pending items
            val pendingPlans = dao.getPendingSyncPlans()
            val pendingTemplates = dao.getPendingSyncTemplates()
            val pendingCheckins = dao.getPendingSyncCheckins()
            val pendingBadges = badgeDao.getPendingSyncBadges()
            
            // Get user — must have a cloud UUID to sync
            val user = db.userDao().getActiveUserOnce()
            val cloudUserId = user?.cloudUserId
            if (user == null || cloudUserId == null) {
                Log.d("SyncWorker", "No active cloud user (cloudUserId is null). Skipping sync.")
                return@withContext Result.success()
            }

            // 2. Make Network Call if there is anything pending
            if (pendingPlans.isNotEmpty() || pendingTemplates.isNotEmpty() || pendingCheckins.isNotEmpty() || pendingBadges.isNotEmpty()) {
                Log.d("SyncWorker", "Found items to sync.")
                
                val payload = com.example.plannerapp.data.SyncPayload(
                    userId = cloudUserId,          // ✅ Supabase UUID, not local Room ID
                    plans = pendingPlans,
                    templates = pendingTemplates,
                    checkins = pendingCheckins,
                    badges = pendingBadges         // ✅ Badges included in payload
                )

                val response = com.example.plannerapp.data.NetworkClient.api.syncData(payload)
                
                if (response.success) {
                    // 3. Mark as synced
                    if (pendingPlans.isNotEmpty()) dao.markPlansSynced(pendingPlans.map { it.planId })
                    if (pendingTemplates.isNotEmpty()) dao.markTemplatesSynced(pendingTemplates.map { it.templateId })
                    if (pendingCheckins.isNotEmpty()) dao.markCheckinsSynced(pendingCheckins.map { it.checkinId })
                    if (pendingBadges.isNotEmpty()) badgeDao.markBadgesSynced(pendingBadges.map { it.badgeId })
                    
                    Log.d("SyncWorker", "Sync complete!")
                } else {
                    Log.e("SyncWorker", "Server rejected sync: ${response.message}")
                    return@withContext Result.retry()
                }
            } else {
                Log.d("SyncWorker", "Nothing to sync.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_SYNC_WORK_NAME = "com.example.plannerapp.sync.UNIQUE_SYNC_WORK"
        const val PERIODIC_SYNC_WORK_NAME = "com.example.plannerapp.sync.PERIODIC_SYNC_WORK"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
