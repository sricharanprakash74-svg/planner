package com.example.plannerapp.ui.creator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.plannerapp.data.creator.PlanVersionEntity
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanMonetizationSettingsCard(
    plans: List<PlanVersionEntity>,
    onUpdatePricing: (planId: String, newCredits: Int, previewDays: Int) -> Unit,
    onTogglePacing: (planId: String, strictLock: Boolean) -> Unit,
    onToggleReflection: (planId: String, mandatory: Boolean) -> Unit,
    onCreateVersion: (planId: String, isBreaking: Boolean, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (plans.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.LibraryBooks,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Published Plans",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Publish a routine or plan to configure credit pricing, freemium teasers, and progression governance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    var selectedPlanIndex by remember { mutableIntStateOf(0) }
    val selectedPlan = plans.getOrNull(selectedPlanIndex) ?: plans.first()

    var creditPriceInput by remember(selectedPlan.versionId) {
        mutableStateOf(selectedPlan.creditPrice.toString())
    }
    var previewDaysValue by remember(selectedPlan.versionId) {
        mutableFloatStateOf(selectedPlan.freemiumPreviewDays.toFloat())
    }
    var isStrictLock by remember(selectedPlan.versionId) {
        mutableStateOf(selectedPlan.pacingMode == "STRICT_DAILY_LOCK")
    }
    var isMandatoryReflection by remember(selectedPlan.versionId) {
        mutableStateOf(selectedPlan.isMandatoryReflectionEnabled)
    }

    var expandedDropdown by remember { mutableStateOf(false) }
    var showPatchDialog by remember { mutableStateOf(false) }
    var showForkDialog by remember { mutableStateOf(false) }
    var patchTitleInput by remember { mutableStateOf("") }
    var forkTitleInput by remember { mutableStateOf("") }

    val parsedCredits = creditPriceInput.toLongOrNull() ?: 0L
    // Fixed $0.0050 peg at 70% net distribution:
    val takeHomeUsd = (parsedCredits * 0.0050) * 0.70

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monetization & Plan Governance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Version ${selectedPlan.versionTag} · ${selectedPlan.durationDays} Days Duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF3B4CB8).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "v${selectedPlan.versionTag}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B4CB8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Plan Selector Dropdown
            Text(
                text = "Target Plan",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(6.dp))
            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = !expandedDropdown }
            ) {
                OutlinedTextField(
                    value = selectedPlan.title,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B4CB8),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    plans.forEachIndexed { index, plan ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(plan.title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "v${plan.versionTag} · ${plan.creditPrice} cr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            },
                            onClick = {
                                selectedPlanIndex = index
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(20.dp))

            // Credit Price Input
            Text(
                text = "Credit Unlock Price",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = creditPriceInput,
                onValueChange = { creditPriceInput = it.filter { ch -> ch.isDigit() } },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Paid,
                        contentDescription = null,
                        tint = Color(0xFF3B4CB8)
                    )
                },
                trailingIcon = {
                    Text(
                        text = "Credits",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B4CB8),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Real-Time Conversion Label
            val formattedTakeHome = String.format(Locale.US, "$%.2f", takeHomeUsd)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Calculate,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Users pay $parsedCredits credits -> You earn $formattedTakeHome USD (Fixed $0.0050/cr peg at 70%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Freemium Teaser Days Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Freemium Teaser Days",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = if (previewDaysValue.toInt() == 0) "No preview (Day 1 Locked)" else "Days 1 to ${previewDaysValue.toInt()} Free",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B4CB8)
                )
            }
            Slider(
                value = previewDaysValue,
                onValueChange = { previewDaysValue = it },
                valueRange = 0f..7f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF3B4CB8),
                    activeTrackColor = Color(0xFF3B4CB8)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(16.dp))

            // Strict Midnight Lock Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Strict Midnight Lock",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Enforce participant local midnight lock to prevent bingeing and client clock tampering.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = isStrictLock,
                    onCheckedChange = {
                        isStrictLock = it
                        onTogglePacing(selectedPlan.planId, it)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B4CB8))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mandatory Daily Reflection Note Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mandatory Daily Reflection",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Require participants to complete a reflection note before marking the day complete.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = isMandatoryReflection,
                    onCheckedChange = {
                        isMandatoryReflection = it
                        onToggleReflection(selectedPlan.planId, it)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B4CB8))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Settings Button
            Button(
                onClick = {
                    onUpdatePricing(selectedPlan.planId, parsedCredits.toInt(), previewDaysValue.toInt())
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B4CB8))
            ) {
                Icon(imageVector = Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Monetization Settings", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(16.dp))

            // Version Management Section
            Text(
                text = "Semantic Version Governance",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        patchTitleInput = selectedPlan.title
                        showPatchDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Icon(imageVector = Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Patch (v1.0.x)", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = {
                        forkTitleInput = "${selectedPlan.title} (v2)"
                        showForkDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Icon(imageVector = Icons.Outlined.ForkRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fork (v2.0)", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    // Dialog: Patch Update
    if (showPatchDialog) {
        AlertDialog(
            onDismissRequest = { showPatchDialog = false },
            title = { Text("Publish Patch Update", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "A patch update fixes descriptions or minor task notes without disrupting active student streaks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = patchTitleInput,
                        onValueChange = { patchTitleInput = it },
                        label = { Text("Release Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateVersion(selectedPlan.planId, false, patchTitleInput)
                        showPatchDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B4CB8))
                ) {
                    Text("Publish Patch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPatchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Breaking Fork
    if (showForkDialog) {
        AlertDialog(
            onDismissRequest = { showForkDialog = false },
            title = { Text("Fork Breaking Change (v2.0)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Creating a major fork archives existing participants on their current snapshot while all new enrollments receive v2.0.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = forkTitleInput,
                        onValueChange = { forkTitleInput = it },
                        label = { Text("New Major Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateVersion(selectedPlan.planId, true, forkTitleInput)
                        showForkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B4CB8))
                ) {
                    Text("Confirm Fork")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
