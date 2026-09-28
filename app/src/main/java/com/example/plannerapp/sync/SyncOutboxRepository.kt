package com.example.plannerapp.sync

import android.content.Context
import com.example.plannerapp.data.DailyCheckinEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerDao
import com.example.plannerapp.data.SyncOutboxEntity
import com.example.plannerapp.data.TaskTemplateEntity
import com.google.gson.Gson

/**
 * Single-entry-point for writing to the sync outbox.
 *
 * All mutations that need to be reflected in Supabase go through here.
 * Call [enqueueSync] after every Room write, then call
 * [SupabaseSyncWorker.enqueue] to trigger the background drain.
 */
class SyncOutboxRepository(
    private val dao: PlannerDao,
    private val appContext: Context
) {
    private val gson = Gson()

    // ── Plans ────────────────────────────────────────

    suspend fun enqueuePlanUpsert(plan: PlanEntity, ownerCloudUid: String) {
        val payload = mapOf(
            "id"                       to (plan.remoteId ?: ""), // empty = let server generate
            "owner_id"                 to ownerCloudUid,
            "local_id"                 to plan.planId.toString(),
            "heading"                  to plan.heading,
            "description"              to plan.description,
            "start_date"               to plan.startDate,
            "end_date"                 to plan.endDate,
            "is_public"                to plan.isPublic.toString(),
            "default_task_duration_days" to plan.defaultTaskDurationDays.toString(),
            "reminder_enabled"         to plan.reminderEnabled.toString(),
            "reminder_time"            to (plan.reminderTime ?: ""),
            "created_at_ms"            to plan.createdAt.toString(),
            "updated_at_ms"            to plan.updatedAt.toString()
        ).filterValues { it.isNotEmpty() || it == "false" }
        insertOutbox(
            entityType = "PLAN",
            localId = plan.planId,
            remoteId = plan.remoteId,
            operation = "UPSERT",
            payloadJson = gson.toJson(payload)
        )
        SupabaseSyncWorker.enqueue(appContext)
    }

    suspend fun enqueuePlanDelete(planId: Long, remoteId: String?) {
        insertOutbox(
            entityType = "PLAN",
            localId = planId,
            remoteId = remoteId,
            operation = "DELETE",
            payloadJson = null
        )
        SupabaseSyncWorker.enqueue(appContext)
    }

    // ── Task Templates ───────────────────────────────

    suspend fun enqueueTemplateUpsert(template: TaskTemplateEntity, planRemoteId: String?, ownerCloudUid: String) {
        val payload = mapOf(
            "id"               to (template.remoteId ?: ""),
            "owner_id"         to ownerCloudUid,
            "plan_remote_id"   to (planRemoteId ?: ""),
            "local_id"         to template.templateId.toString(),
            "task_description" to template.taskDescription,
            "selected_days"    to template.selectedDays,
            "duration_days"    to template.durationDays.toString(),
            "subtasks"         to template.subtasks,
            "created_at_ms"    to template.createdAt.toString()
        ).filterValues { it.isNotEmpty() || it == "[]" }
        insertOutbox(
            entityType = "TEMPLATE",
            localId = template.templateId,
            remoteId = template.remoteId,
            operation = "UPSERT",
            payloadJson = gson.toJson(payload)
        )
        SupabaseSyncWorker.enqueue(appContext)
    }

    suspend fun enqueueTemplateDelete(templateId: Long, remoteId: String?) {
        insertOutbox(
            entityType = "TEMPLATE",
            localId = templateId,
            remoteId = remoteId,
            operation = "DELETE",
            payloadJson = null
        )
        SupabaseSyncWorker.enqueue(appContext)
    }

    // ── Daily Check-ins ──────────────────────────────

    suspend fun enqueueCheckinUpsert(checkin: DailyCheckinEntity, templateRemoteId: String?, ownerCloudUid: String) {
        val payload = mapOf(
            "id"                  to (checkin.remoteId ?: ""),
            "owner_id"            to ownerCloudUid,
            "template_remote_id"  to (templateRemoteId ?: ""),
            "local_id"            to checkin.checkinId.toString(),
            "exact_date"          to checkin.exactDate,
            "is_completed"        to checkin.isCompleted.toString(),
            "completed_subtasks"  to checkin.completedSubtasks,
            "completed_at_ms"     to (checkin.completedAt?.toString() ?: ""),
            "timezone_offset"     to checkin.timezoneOffset
        ).filterValues { it.isNotEmpty() || it == "false" || it == "[]" }
        insertOutbox(
            entityType = "CHECKIN",
            localId = checkin.checkinId,
            remoteId = checkin.remoteId,
            operation = "UPSERT",
            payloadJson = gson.toJson(payload)
        )
        SupabaseSyncWorker.enqueue(appContext)
    }

    // ── Internal ─────────────────────────────────────

    private suspend fun insertOutbox(
        entityType: String,
        localId: Long,
        remoteId: String?,
        operation: String,
        payloadJson: String?
    ) {
        dao.insertOutboxEvent(
            SyncOutboxEntity(
                entityType  = entityType,
                localId     = localId,
                remoteId    = remoteId,
                operation   = operation,
                payloadJson = payloadJson,
                createdAt   = System.currentTimeMillis()
            )
        )
    }
}
