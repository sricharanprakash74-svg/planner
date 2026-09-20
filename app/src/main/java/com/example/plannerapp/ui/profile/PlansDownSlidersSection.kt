package com.example.plannerapp.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.ui.components.FluidProgressCard
import com.example.plannerapp.ui.components.PlanListItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Plans tab content: two collapsible sections.
 *
 * Order (per design spec):
 *   1. Saved & Public Plans  (top)
 *   2. Following Plans       (bottom)
 *
 * "Following Plans" are identified by [joinedCommunityPlanIds] � the set of
 * localPlanIds that have a record in the joined_communities Room table.
 * This is populated from a Room Flow so it is accurate offline and online.
 *
 * The FluidProgressCard fluid level is driven by [planCompletionMap], which
 * is also Room-backed. Offline completions update the map immediately; synced
 * completions from the server write to the same Room table and trigger the
 * same Flow � the UI code does not distinguish between the two modes.
 *
 * @param userPlans               Full list of user's plans from Room.
 * @param consistencyPercentage   Overall consistency % (used for Saved/Public PlanListItem).
 * @param planCompletionMap       planId -> completion fraction [0.0, 1.0] from Room.
 * @param joinedCommunityPlanIds  Set of localPlanIds that belong to "Following Plans".
 * @param isAnimationEnabled      Whether system animations are active (reduced-motion).
 * @param followingExpanded       Expansion state of the Following Plans section.
 * @param onToggleFollowing       Toggle callback for Following Plans section.
 * @param savedPublicExpanded     Expansion state of the Saved & Public Plans section.
 * @param onToggleSavedPublic     Toggle callback for Saved & Public Plans section.
 * @param onPlanClick             Navigation callback for tapping a plan.
 */
@Composable
fun PlansDownSlidersSection(
    userPlans: List<PlanEntity>,
    consistencyPercentage: Int,
    planCompletionMap: Map<Long, Float> = emptyMap(),
    joinedCommunityPlanIds: Set<Long> = emptySet(),
    isAnimationEnabled: Boolean = true,
    followingExpanded: Boolean,
    onToggleFollowing: () -> Unit,
    savedPublicExpanded: Boolean,
    onToggleSavedPublic: () -> Unit,
    onPlanClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // "Following Plans": plans whose localPlanId is in the joined_communities table.
    // Previously (bug): filtered by sourcePlanId == null (incorrect).
    // Now: filtered by joinedCommunityPlanIds set (correct, Room-backed, offline-safe).
    // "Following Plans": active plans followed by user (local personal plans + community joined plans)
    val followingPlans = remember(userPlans, joinedCommunityPlanIds) {
        userPlans.filter { it.sourcePlanId == null || it.planId in joinedCommunityPlanIds }
    }

    // "Saved & Public Plans": saved community templates (not actively joined) and publicly shared plans
    val savedAndPublicPlans = remember(userPlans, joinedCommunityPlanIds) {
        userPlans.filter { it.isPublic || (it.sourcePlanId != null && it.planId !in joinedCommunityPlanIds) }
    }

    val dateFormatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // -- Section 1: Saved & Public Plans (top, per design spec) ---------
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleSavedPublic)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Saved & Public Plans",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${savedAndPublicPlans.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (savedPublicExpanded) Icons.Filled.KeyboardArrowUp
                                      else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (savedPublicExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = savedPublicExpanded,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (savedAndPublicPlans.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No saved or public plans yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Plans saved from creators and your publicly shared plans will appear here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            savedAndPublicPlans.forEach { plan ->
                                PlanListItem(
                                    heading = plan.heading,
                                    dateRange = "${plan.startDate} - ${plan.endDate}",
                                    consistencyPercent = consistencyPercentage,
                                    onClick = { onPlanClick(plan.planId) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // -- Section 2: Following Plans (bottom, per design spec) -----------
        // Each plan gets a FluidProgressCard with a real completion fraction
        // sourced from planCompletionMap (Room-backed, offline-safe).
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleFollowing)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.FolderSpecial,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Following Plans",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${followingPlans.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (followingExpanded) Icons.Filled.KeyboardArrowUp
                                      else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (followingExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = followingExpanded,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (followingPlans.isEmpty()) {
                            Text(
                                text = "Nothing here yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            followingPlans.forEach { plan ->
                                val fraction = planCompletionMap[plan.planId] ?: 0f
                                val isCompleted = fraction >= 1f

                                // Compute current day and total days from plan dates.
                                // Uses LocalDate arithmetic � no network call, works offline.
                                val today = LocalDate.now()
                                val totalDays = computeTotalDays(
                                    plan.startDate, plan.endDate, dateFormatter
                                )
                                val currentDay = computeCurrentDay(
                                    plan.startDate, today, totalDays, dateFormatter
                                )

                                FluidProgressCard(
                                    planName            = plan.heading,
                                    dateRange           = formatDateRange(plan.startDate, plan.endDate),
                                    completionFraction  = fraction,
                                    currentDay          = currentDay,
                                    totalDays           = totalDays,
                                    isCompleted         = isCompleted,
                                    isAnimationEnabled  = isAnimationEnabled,
                                    onClick             = { onPlanClick(plan.planId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Private date helpers � all local arithmetic, no network dependency
// ---------------------------------------------------------------------------

private fun computeTotalDays(
    startDate: String,
    endDate: String,
    formatter: DateTimeFormatter,
): Int {
    return try {
        val start = LocalDate.parse(startDate, formatter)
        val end   = LocalDate.parse(endDate, formatter)
        (ChronoUnit.DAYS.between(start, end) + 1).toInt().coerceAtLeast(1)
    } catch (_: Exception) { 1 }
}

private fun computeCurrentDay(
    startDate: String,
    today: LocalDate,
    totalDays: Int,
    formatter: DateTimeFormatter,
): Int {
    return try {
        val start = LocalDate.parse(startDate, formatter)
        (ChronoUnit.DAYS.between(start, today) + 1)
            .toInt()
            .coerceIn(1, totalDays)
    } catch (_: Exception) { 1 }
}

/**
 * Formats a "YYYY-MM-DD" date range into a human-readable form.
 * Example: "2026-09-18" to "2026-09-24" -> "Sept 18 - Sept 24"
 * Falls back to raw strings if parsing fails (offline-safe, no crash).
 */
private fun formatDateRange(startDate: String, endDate: String): String {
    return try {
        val fmt   = DateTimeFormatter.ISO_LOCAL_DATE
        val disp  = DateTimeFormatter.ofPattern("MMM d")
        val start = LocalDate.parse(startDate, fmt)
        val end   = LocalDate.parse(endDate, fmt)
        "${start.format(disp)} - ${end.format(disp)}"
    } catch (_: Exception) {
        "$startDate - $endDate"
    }
}
