package com.example.plannerapp.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.plannerapp.data.PlannerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
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
            
            // Get user
            val user = db.userDao().getActiveUserOnce()
            if (user == null || user.cloudUserId == null) {
                Log.d("SyncWorker", "No active cloud user. Skipping sync.")
                return@withContext Result.success()
            }

            // 2. Make Network Call
            if (pendingPlans.isNotEmpty() || pendingTemplates.isNotEmpty() || pendingCheckins.isNotEmpty()) {
                Log.d("SyncWorker", "Found items to sync.")
                
                val payload = com.example.plannerapp.data.SyncPayload(
                    userId = user.userId,
                    plans = pendingPlans,
                    templates = pendingTemplates,
                    checkins = pendingCheckins
                )

                val response = com.example.plannerapp.data.NetworkClient.api.syncData(payload)
                
                if (response.success) {
                    // 3. Mark as synced
                    if (pendingPlans.isNotEmpty()) dao.markPlansSynced(pendingPlans.map { it.planId })
                    if (pendingTemplates.isNotEmpty()) dao.markTemplatesSynced(pendingTemplates.map { it.templateId })
                    if (pendingCheckins.isNotEmpty()) dao.markCheckinsSynced(pendingCheckins.map { it.checkinId })
                    if (pendingBadges.isNotEmpty()) badgeDao.markBadgesSynced(pendingBadges.map { it.badgeId })
                    
                    Log.d("SyncWorker", "Sync complete via Retrofit!")
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
}
