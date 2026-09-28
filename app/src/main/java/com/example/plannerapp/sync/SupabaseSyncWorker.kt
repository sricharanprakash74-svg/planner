package com.example.plannerapp.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.plannerapp.auth.SupabaseConfig
import com.example.plannerapp.data.PlannerDatabase
import com.example.plannerapp.data.SyncOutboxEntity
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.TimeUnit

/**
 * Drains the sync_outbox table row-by-row and mirrors each mutation to
 * the private Supabase tables (user_plans, user_task_templates, user_daily_checkins).
 *
 * Design principles:
 *  - Each outbox event is processed atomically: on success it is deleted, on transient
 *    failure its retryCount is incremented and WorkManager's exponential back-off retries.
 *  - After the first UPSERT of a new entity the server-assigned UUID is written back to
 *    Room (remoteId field), establishing the permanent local↔cloud identity link.
 *  - DELETE operations remove the cloud row by remoteId; if remoteId is null the
 *    operation is treated as a no-op (entity never reached the cloud).
 *  - Events that exceed MAX_RETRIES are pruned to prevent outbox bloat.
 */
class SupabaseSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val WORK_NAME = "supabase_sync_worker"
        private const val MAX_RETRIES = 5
        private const val BATCH_SIZE = 50

        /**
         * Enqueues one sync pass. Safe to call multiple times — WorkManager deduplicates
         * using KEEP policy so an already-queued run is not replaced.
         */
        fun enqueue(context: Context) {
            if (!SupabaseConfig.isConfigured) return
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<SupabaseSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }

    override suspend fun doWork(): Result {
        val db = PlannerDatabase.getDatabase(applicationContext)
        val dao = db.plannerDao()

        // Prune permanently-failed events first
        dao.pruneExhaustedOutboxEvents(MAX_RETRIES)

        val events = dao.peekOutbox(BATCH_SIZE)
        if (events.isEmpty()) return Result.success()

        val supabase = SupabaseConfig.client
        val cloudUid = try {
            SupabaseConfig.auth.currentUserOrNull()?.id ?: return Result.success()
        } catch (_: Exception) {
            return Result.retry()
        }

        var anyFailure = false

        for (event in events) {
            val success = processEvent(supabase, dao, event, cloudUid)
            if (!success) {
                dao.incrementOutboxRetry(event.outboxId)
                anyFailure = true
            }
        }

        return if (anyFailure) Result.retry() else Result.success()
    }

    private suspend fun processEvent(
        supabase: io.github.jan.supabase.SupabaseClient,
        dao: com.example.plannerapp.data.PlannerDao,
        event: SyncOutboxEntity,
        ownerUid: String
    ): Boolean {
        return try {
            val tableName = when (event.entityType) {
                "PLAN"     -> "user_plans"
                "TEMPLATE" -> "user_task_templates"
                "CHECKIN"  -> "user_daily_checkins"
                else       -> return false
            }

            when (event.operation) {
                "UPSERT" -> {
                    val payload = event.payloadJson ?: return false
                    val jsonObj = Json.parseToJsonElement(payload).jsonObject

                    // Attach owner_id for RLS
                    val mutableMap = jsonObj.toMutableMap()
                    mutableMap["owner_id"] = kotlinx.serialization.json.JsonPrimitive(ownerUid)
                    val finalPayload = JsonObject(mutableMap)

                    val response = supabase.postgrest.from(tableName)
                        .upsert(finalPayload) {
                            select()
                        }
                        .decodeList<JsonObject>()

                    // Write back the server-assigned UUID (remoteId) on first creation
                    val serverRemoteId = response.firstOrNull()
                        ?.get("id")?.jsonPrimitive?.content
                        ?.takeIf { it.isNotBlank() }

                    if (serverRemoteId != null && event.remoteId == null) {
                        when (event.entityType) {
                            "PLAN"     -> dao.setPlanRemoteId(event.localId, serverRemoteId)
                            "TEMPLATE" -> dao.setTemplateRemoteId(event.localId, serverRemoteId)
                            "CHECKIN"  -> dao.setCheckinRemoteId(event.localId, serverRemoteId)
                        }
                    } else {
                        // remoteId was already known — just mark synced
                        when (event.entityType) {
                            "PLAN"     -> dao.markPlansSynced(listOf(event.localId))
                            "TEMPLATE" -> dao.markTemplatesSynced(listOf(event.localId))
                            "CHECKIN"  -> dao.markCheckinsSynced(listOf(event.localId))
                        }
                    }

                    dao.deleteOutboxEvent(event.outboxId)
                    true
                }

                "DELETE" -> {
                    val remoteId = event.remoteId
                    if (remoteId != null) {
                        supabase.postgrest.from(tableName)
                            .delete { filter { eq("id", remoteId); eq("owner_id", ownerUid) } }
                    }
                    // Regardless of whether the row existed, the local row was already
                    // physically deleted before the outbox event was enqueued. We're done.
                    dao.deleteOutboxEvent(event.outboxId)
                    true
                }

                else -> {
                    // Unknown operation — discard
                    dao.deleteOutboxEvent(event.outboxId)
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
