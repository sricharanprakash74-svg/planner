package com.example.plannerapp.sharing

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.plannerapp.auth.SupabaseConfig
import com.example.plannerapp.credits.CreditRepository
import com.example.plannerapp.data.DailyCheckinEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerDatabase
import com.example.plannerapp.data.TaskTemplateEntity
import com.example.plannerapp.data.social.SupabaseSocialRepository
import com.example.plannerapp.data.template.PlanImporter
import com.example.plannerapp.data.template.PlanTemplateDto
import com.example.plannerapp.widget.StreakWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ImportPlanDialog(
    deepLinkUri: Uri,
    onDismissRequest: () -> Unit,
    onPlanCloned: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val code = remember(deepLinkUri) { deepLinkUri.getQueryParameter("code") ?: "" }
    val remoteId = remember(deepLinkUri) { deepLinkUri.getQueryParameter("remoteId") ?: "" }
    val referrerId = remember(deepLinkUri) { deepLinkUri.getQueryParameter("ref")?.toLongOrNull() ?: 0L }
    val initialTitle = remember(deepLinkUri) { deepLinkUri.getQueryParameter("title") ?: "Shared Plan Protocol" }

    var planTitle by remember { mutableStateOf(initialTitle) }
    var durationDays by remember { mutableIntStateOf(30) }
    var isCloning by remember { mutableStateOf(false) }
    var isLoadingRemote by remember { mutableStateOf(false) }
    var loadedTemplate by remember { mutableStateOf<PlanTemplateDto?>(null) }

    // Query shared plan metadata locally first, then fallback to Supabase for cross-device sharing
    LaunchedEffect(deepLinkUri) {
        withContext(Dispatchers.IO) {
            val db = PlannerDatabase.getDatabase(context)
            // 1. Check local sharing DB
            if (code.isNotBlank()) {
                try {
                    val sharedPlan = db.sharingDao().getSharedPlanByCode(code)
                    if (sharedPlan != null) {
                        planTitle = sharedPlan.planTitle
                        durationDays = sharedPlan.durationDays
                        if (sharedPlan.templatePayloadJson.isNotBlank() && sharedPlan.templatePayloadJson != "{}") {
                            val parsed = PlanImporter().parseJson(sharedPlan.templatePayloadJson).getOrNull()
                            if (parsed != null) {
                                loadedTemplate = parsed
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Fallback to remote or query params
                }
            }

            // 2. If not found locally or template is missing, query Supabase
            if (loadedTemplate == null && SupabaseConfig.isConfigured) {
                val targetRemoteId = remoteId.ifBlank { code }
                if (targetRemoteId.isNotBlank()) {
                    withContext(Dispatchers.Main) { isLoadingRemote = true }
                    try {
                        val socialRepo = SupabaseSocialRepository()
                        val versions = socialRepo.getPlanVersions(targetRemoteId).getOrNull()
                        val latestVersion = versions?.firstOrNull()
                        if (latestVersion != null && latestVersion.templateJson.isNotBlank()) {
                            val parsed = PlanImporter().parseJson(latestVersion.templateJson).getOrNull()
                            if (parsed != null) {
                                loadedTemplate = parsed
                                planTitle = parsed.title.ifBlank { planTitle }
                                durationDays = parsed.targetDurationDays.coerceAtLeast(1)
                            }
                        } else {
                            val publicPlan = socialRepo.getPublicPlanByIdDirect(targetRemoteId).getOrNull()
                            if (publicPlan != null) {
                                planTitle = publicPlan.title
                                durationDays = publicPlan.durationDays
                                val pv = socialRepo.getPlanVersions(publicPlan.id).getOrNull()?.firstOrNull()
                                if (pv != null && pv.templateJson.isNotBlank()) {
                                    val parsed = PlanImporter().parseJson(pv.templateJson).getOrNull()
                                    if (parsed != null) {
                                        loadedTemplate = parsed
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Fallback to query params
                    } finally {
                        withContext(Dispatchers.Main) { isLoadingRemote = false }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CLONE SHARED PLAN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A peer has invited you to adopt their daily protocol.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Plan Overview Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = planTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$durationDays-Day Daily Track",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            val taskCount = loadedTemplate?.tasks?.size ?: 0
                            if (taskCount > 0) {
                                Text(
                                    text = "• $taskCount tasks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isLoadingRemote) {
                                Spacer(modifier = Modifier.width(4.dp))
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bilateral Bonus Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Paid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "+100 Bilateral Credits Bonus",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Both you and the referrer will earn 100 reward credits when this plan is cloned.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Decline")
                    }

                    Button(
                        onClick = {
                            isCloning = true
                            coroutineScope.launch {
                                try {
                                    val db = PlannerDatabase.getDatabase(context)
                                    val currentUser = withContext(Dispatchers.IO) {
                                        db.userDao().getActiveUserOnce()
                                    }
                                    val myUserId = currentUser?.userId ?: 0L
                                    val today = LocalDate.now()
                                    val targetRemoteId = remoteId.ifBlank { code }
                                    val currentTemplate = loadedTemplate

                                    val newPlanId = withContext(Dispatchers.IO) {
                                        val createdId = if (currentTemplate != null) {
                                            PlanImporter().importToLocalPlan(
                                                template = currentTemplate,
                                                targetUserId = myUserId,
                                                startDate = today,
                                                sourcePlanId = referrerId,
                                                dao = db.plannerDao()
                                            )
                                        } else {
                                            val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
                                            val startDateStr = today.format(dateFormatter)
                                            val endDateStr = today.plusDays((durationDays - 1).toLong()).format(dateFormatter)

                                            // Create cloned PlanEntity fallback
                                            val newPlan = PlanEntity(
                                                userId = myUserId,
                                                heading = planTitle,
                                                description = "Cloned via peer invitation ($code)",
                                                startDate = startDateStr,
                                                endDate = endDateStr,
                                                sourcePlanId = referrerId
                                            )

                                            val template = TaskTemplateEntity(
                                                planId = 0,
                                                taskDescription = "Daily Check-in: $planTitle",
                                                selectedDays = "1,2,3,4,5,6,7"
                                            )

                                            val checkins = (0 until durationDays).map { offset ->
                                                DailyCheckinEntity(
                                                    templateId = 0,
                                                    exactDate = today.plusDays(offset.toLong()).format(dateFormatter),
                                                    isCompleted = false
                                                )
                                            }

                                            db.plannerDao().createFullPlan(
                                                plan = newPlan,
                                                templatesWithCheckins = mapOf(template to checkins)
                                            )
                                        }

                                        val creditRepo = CreditRepository(db.creditDao())
                                        val sharingRepo = SharingRepository(db.sharingDao(), creditRepo)
                                        sharingRepo.recordPlanClone(
                                            shareCode = code,
                                            recipientUserId = myUserId,
                                            authorUserId = referrerId
                                        )

                                        if (targetRemoteId.isNotBlank() && SupabaseConfig.isConfigured) {
                                            try {
                                                SupabaseSocialRepository().usePlan(targetRemoteId, myUserId.toString())
                                            } catch (_: Exception) {}
                                        }

                                        createdId
                                    }

                                    // Update widget & notify
                                    StreakWidgetUpdater.update(context)

                                    onPlanCloned(newPlanId)
                                    onDismissRequest()
                                } catch (e: Exception) {
                                    onDismissRequest()
                                } finally {
                                    isCloning = false
                                }
                            }
                        },
                        enabled = !isCloning && !isLoadingRemote,
                        modifier = Modifier
                            .weight(1.6f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isCloning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cloning...")
                        } else {
                            Text("Clone Plan (+100 Pts)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
