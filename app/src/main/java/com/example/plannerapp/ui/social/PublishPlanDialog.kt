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

    val categories = listOf("Productivity", "Health & Fitness", "Mindfulness", "Career & Study", "General")
    val scope = rememberCoroutineScope()
    val planExporter = remember { PlanExporter() }

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
                                    category = selectedCategory
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
