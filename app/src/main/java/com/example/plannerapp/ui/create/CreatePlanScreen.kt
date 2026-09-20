package com.example.plannerapp.ui.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.example.plannerapp.theme.PhysicsSpec
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.plannerapp.notifications.NotificationHelper
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanScreen(
    viewModel: CreatePlanViewModel,
    onClose: () -> Unit,
    onPlanCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var planName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tasks by remember { mutableStateOf(listOf(TaskInput())) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(30)) }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf("08:00") }
    var isPublic by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationHelper.createChannel(context)
            reminderEnabled = true
        } else {
            reminderEnabled = false
        }
    }

    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                NotificationHelper.createChannel(context)
                reminderEnabled = true
            } else {
                showPermissionRationaleDialog = true
            }
        } else {
            NotificationHelper.createChannel(context)
            reminderEnabled = true
        }
    }

    val closeInteraction = remember { MutableInteractionSource() }
    val isClosePressed by closeInteraction.collectIsPressedAsState()
    val closeScale by animateFloatAsState(
        targetValue = if (isClosePressed) 0.88f else 1f,
        animationSpec = if (isClosePressed) PhysicsSpec.PressDown else PhysicsSpec.PressRelease,
        label = "create_close_scale"
    )

    val doneInteraction = remember { MutableInteractionSource() }
    val isDonePressed by doneInteraction.collectIsPressedAsState()
    val doneScale by animateFloatAsState(
        targetValue = if (isDonePressed) 0.92f else 1f,
        animationSpec = if (isDonePressed) PhysicsSpec.PressDown else PhysicsSpec.PressRelease,
        label = "create_done_scale"
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Create New Plan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        interactionSource = closeInteraction,
                        modifier = Modifier.graphicsLayer {
                            scaleX = closeScale
                            scaleY = closeScale
                        }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val validTasks = tasks.filter { it.description.isNotBlank() && it.days.isNotEmpty() }
                            viewModel.createNewPlan(
                                name = planName,
                                description = description,
                                startDate = startDate,
                                endDate = endDate,
                                tasksInput = validTasks.map { it.description to it.days },
                                reminderEnabled = reminderEnabled,
                                reminderTime = if (reminderEnabled) reminderTime else null,
                                isPublic = isPublic
                            )
                            onPlanCreated()
                            onClose()
                        },
                        enabled = planName.isNotBlank() && tasks.any { it.description.isNotBlank() } && !startDate.isAfter(endDate),
                        interactionSource = doneInteraction,
                        modifier = Modifier.graphicsLayer {
                            scaleX = doneScale
                            scaleY = doneScale
                        }
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            PlanDetailsFormSection(
                planName = planName,
                onPlanNameChange = { planName = it },
                description = description,
                onDescriptionChange = { description = it },
                startDate = startDate,
                onStartDateChange = { startDate = it },
                endDate = endDate,
                onEndDateChange = { endDate = it },
                reminderEnabled = reminderEnabled,
                onReminderEnabledChange = { enabled ->
                    if (enabled) {
                        requestNotificationPermission()
                    } else {
                        reminderEnabled = false
                    }
                },
                reminderTime = reminderTime,
                onReminderTimeChange = { reminderTime = it },
                isPublic = isPublic,
                onIsPublicChange = { isPublic = it }
            )

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            TaskTemplatesListSection(
                tasks = tasks,
                onTasksChange = { tasks = it }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = {
                showPermissionRationaleDialog = false
                reminderEnabled = false
            },
            title = { Text("Enable Plan Reminders", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (planName.isNotBlank())
                        "Allow notifications so you don't miss scheduled daily habits for \"$planName\"."
                    else
                        "Allow notifications so you don't miss scheduled daily habits for this plan."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationaleDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) {
                    Text("Allow", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionRationaleDialog = false
                    reminderEnabled = false
                }) {
                    Text("Not Now")
                }
            }
        )
    }
}
