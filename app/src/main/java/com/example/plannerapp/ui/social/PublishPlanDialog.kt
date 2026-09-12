package com.example.plannerapp.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.social.CloudUser
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.template.PlanExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishPlanDialog(
    plan: PlanEntity,
    plannerRepository: PlannerRepository,
    socialRepository: SocialRepository,
    userDao: UserDao,
    onDismiss: () -> Unit,
    onPublished: (String) -> Unit
) {
    var title by remember { mutableStateOf(plan.heading) }
    var description by remember { mutableStateOf(plan.description) }
    var tagsInput by remember { mutableStateOf("routine, habits") }
    var selectedCategory by remember { mutableStateOf("Productivity") }
    var isPublishing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPaidPlan by remember { mutableStateOf(false) }
    var selectedCreditPrice by remember { mutableIntStateOf(100) }

    val categories = listOf("Productivity", "Health & Fitness", "Mindfulness", "Career & Study", "General")
    val scope = rememberCoroutineScope()
    val planExporter = remember { PlanExporter() }
    val activeUserFlow by userDao.getActiveUser().collectAsState(initial = null)
    val isCreator = activeUserFlow?.isCreator == true

    AlertDialog(
        onDismissRequest = { if (!isPublishing) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Publish to Community",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, enabled = !isPublishing) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Share your plan template with the community. Other users can discover, fork, and join your plan routine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Plan Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Instructions") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags (comma-separated)") },
                    placeholder = { Text("e.g. morning, 5am, fitness") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                if (isCreator) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Monetize Plan",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isPaidPlan) "Users spend credits to unlock" else "Free for all community users",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isPaidPlan,
                                    onCheckedChange = { isPaidPlan = it }
                                )
                            }

                            if (isPaidPlan) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Unlock Price",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(50, 100, 250, 500).forEach { price ->
                                        FilterChip(
                                            selected = selectedCreditPrice == price,
                                            onClick = { selectedCreditPrice = price },
                                            label = { Text("$price pts", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val creatorEarns = Math.max(1, (selectedCreditPrice * 0.70).toInt())
                                val usdVal = String.format(java.util.Locale.US, "%.2f", creatorEarns * 0.007)
                                Text(
                                    text = "You earn 70%: $creatorEarns credits (~$$usdVal USD) per unlock",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Title cannot be blank"
                        return@Button
                    }
                    isPublishing = true
                    errorMessage = null

                    scope.launch {
                        try {
                            val activeUser = userDao.getActiveUserOnce()
                            val authorEntity = activeUser ?: com.example.plannerapp.data.UserEntity(displayName = "Community Member")
                            
                            val exportResult = plannerRepository.exportPlanTemplate(
                                planId = plan.planId,
                                author = authorEntity,
                                tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                category = selectedCategory
                            )

                            if (exportResult.isSuccess) {
                                val templateDto = exportResult.getOrThrow()
                                val json = planExporter.toJson(templateDto)

                                val cloudAuthor = CloudUser(
                                    userId = authorEntity.cloudUserId ?: authorEntity.userId.toString(),
                                    username = authorEntity.displayName.replace(" ", "_").lowercase(),
                                    displayName = authorEntity.displayName,
                                    avatarUrl = authorEntity.avatarUrl,
                                    isCreator = authorEntity.isCreator
                                )

                                val postResult = socialRepository.createPost(
                                    author = cloudAuthor,
                                    title = title,
                                    description = description,
                                    planTemplateJson = json,
                                    durationDays = templateDto.targetDurationDays,
                                    tags = templateDto.tags,
                                    category = selectedCategory,
                                    isPaid = isPaidPlan && isCreator,
                                    creditCost = if (isPaidPlan && isCreator) selectedCreditPrice else 0
                                )

                                if (postResult.isSuccess) {
                                    val post = postResult.getOrThrow()
                                    onPublished(post.postId)
                                } else {
                                    errorMessage = postResult.exceptionOrNull()?.message ?: "Failed to publish post"
                                }
                            } else {
                                errorMessage = exportResult.exceptionOrNull()?.message ?: "Failed to export plan"
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "An unexpected error occurred"
                        } finally {
                            isPublishing = false
                        }
                    }
                },
                enabled = !isPublishing,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publishing...")
                } else {
                    Text("Publish Plan", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isPublishing) {
                Text("Cancel")
            }
        }
    )
}
