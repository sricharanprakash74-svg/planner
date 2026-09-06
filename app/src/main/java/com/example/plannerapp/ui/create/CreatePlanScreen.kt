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
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanScreen(
    viewModel: CreatePlanViewModel,
    onClose: () -> Unit,
    onPlanCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    var planName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tasks by remember { mutableStateOf(listOf(TaskInput())) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(30)) }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf("08:00") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Create New Plan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
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
                                reminderTime = if (reminderEnabled) reminderTime else null
                            )
                            onPlanCreated()
                            onClose()
                        },
                        enabled = planName.isNotBlank() && tasks.any { it.description.isNotBlank() } && !startDate.isAfter(endDate)
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
                onReminderEnabledChange = { reminderEnabled = it },
                reminderTime = reminderTime,
                onReminderTimeChange = { reminderTime = it }
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
}
