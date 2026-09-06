package com.example.plannerapp.sharing

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.plannerapp.credits.CreditRepository
import com.example.plannerapp.data.PlannerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SharePlanStoryDialog(
    planId: Long,
    planTitle: String,
    durationDays: Int,
    streakDays: Int,
    consistencyPercent: Int,
    completedTasksCount: Int,
    onDismissRequest: () -> Unit,
    onShared: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPORT STORY CARD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 9:16 Mini Story Preview Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF141414),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(9f / 13f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2E7D32)
                            ) {
                                Text(
                                    text = "DAILY PROTOCOL",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = planTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2
                            )
                        }

                        // Metrics Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF222222),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "$streakDays",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                                Text(
                                    text = "DAYS ACTIVE STREAK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$consistencyPercent% CONSISTENCY · $completedTasksCount TASKS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        Text(
                            text = "CLONE ON PLANNER APP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Credit Rewards Notice
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Paid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+25 Credits awarded for sharing. When peers clone your link, both get +100 bonus credits!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Share Button
                Button(
                    onClick = {
                        isExporting = true
                        coroutineScope.launch {
                            try {
                                val db = PlannerDatabase.getDatabase(context)
                                val user = withContext(Dispatchers.IO) { db.userDao().getActiveUserOnce() }
                                val userId = user?.userId ?: 0L

                                val creditRepo = CreditRepository(db.creditDao())
                                val sharingRepo = SharingRepository(db.sharingDao(), creditRepo)

                                val (shareCode, deepLinkUri) = withContext(Dispatchers.IO) {
                                    sharingRepo.createAndSharePlan(
                                        planId = planId,
                                        planTitle = planTitle,
                                        planDescription = "",
                                        durationDays = durationDays,
                                        templatePayloadJson = "{}",
                                        authorUserId = userId
                                    )
                                }

                                val storyCardBitmap = withContext(Dispatchers.Default) {
                                    PlanSharingManager.renderStoryCard(
                                        context = context,
                                        planTitle = planTitle,
                                        streakDays = streakDays,
                                        consistencyPercent = consistencyPercent,
                                        totalTasksCompleted = completedTasksCount
                                    )
                                }

                                val shareIntent = PlanSharingManager.shareStoryCardIntent(
                                    context = context,
                                    bitmap = storyCardBitmap,
                                    shareDeepLink = deepLinkUri
                                )

                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Plan Story"))
                                onShared(shareCode)
                                onDismissRequest()
                            } catch (e: Exception) {
                                // Fallback
                            } finally {
                                isExporting = false
                            }
                        }
                    },
                    enabled = !isExporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rendering Story Card...")
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share 9:16 Story Card (+25 Pts)")
                    }
                }
            }
        }
    }
}
