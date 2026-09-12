package com.example.plannerapp.ui.settings

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.template.PlanExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ExportStatus {
    object Idle : ExportStatus()
    object Loading : ExportStatus()
    data class Success(val filePath: String) : ExportStatus()
    data class Error(val message: String) : ExportStatus()
}

class ExportDataViewModel(
    private val repository: PlannerRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus

    fun exportData(
        context: Context,
        format: String,
        includeHistory: Boolean,
        includeBadges: Boolean
    ) {
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.Loading
            try {
                val user = userDao.getActiveUserOnce() ?: throw IllegalStateException("No active user")
                val plans = repository.getPlansForUser(user.userId).first()

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val ext = when (format) { "CSV" -> "csv"; "Markdown" -> "md"; else -> "json" }
                val fileName = "planner_export_$timestamp.$ext"
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    ?: context.filesDir
                dir.mkdirs()
                val file = File(dir, fileName)

                val content = when (format) {
                    "CSV" -> buildCsv(plans.map { plan ->
                        val templates = repository.getTemplatesForPlan(plan.planId)
                        Pair(plan, templates)
                    })
                    "Markdown" -> buildMarkdown(plans.map { plan ->
                        val templates = repository.getTemplatesForPlan(plan.planId)
                        Pair(plan, templates)
                    }, user.displayName)
                    else -> buildJson(plans.map { plan ->
                        val templates = repository.getTemplatesForPlan(plan.planId)
                        val exporter = PlanExporter()
                        exporter.exportPlanToJson(plan, templates, user)
                    })
                }

                file.writeText(content)
                _exportStatus.value = ExportStatus.Success(file.absolutePath)
            } catch (e: Exception) {
                _exportStatus.value = ExportStatus.Error(e.message ?: "Export failed")
            }
        }
    }

    fun resetStatus() { _exportStatus.value = ExportStatus.Idle }

    private fun buildJson(planJsonList: List<String>): String {
        return "{\"plans\":[${planJsonList.joinToString(",")}]}"
    }

    private fun buildCsv(plansWithTemplates: List<Pair<com.example.plannerapp.data.PlanEntity, List<com.example.plannerapp.data.TaskTemplateEntity>>>): String {
        val sb = StringBuilder()
        sb.appendLine("Plan Name,Start Date,End Date,Task,Days")
        plansWithTemplates.forEach { (plan, templates) ->
            if (templates.isEmpty()) {
                sb.appendLine("\"${plan.heading}\",${plan.startDate},${plan.endDate},,")
            } else {
                templates.forEach { t ->
                    sb.appendLine("\"${plan.heading}\",${plan.startDate},${plan.endDate},\"${t.taskDescription}\",${t.selectedDays}")
                }
            }
        }
        return sb.toString()
    }

    private fun buildMarkdown(plansWithTemplates: List<Pair<com.example.plannerapp.data.PlanEntity, List<com.example.plannerapp.data.TaskTemplateEntity>>>, displayName: String): String {
        val sb = StringBuilder()
        sb.appendLine("# PlannerApp Export")
        sb.appendLine("**User:** $displayName")
        sb.appendLine("**Exported:** ${SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())}")
        sb.appendLine()
        plansWithTemplates.forEach { (plan, templates) ->
            sb.appendLine("## ${plan.heading}")
            sb.appendLine("- **Start:** ${plan.startDate}")
            sb.appendLine("- **End:** ${plan.endDate}")
            if (plan.description.isNotBlank()) sb.appendLine("- **Description:** ${plan.description}")
            if (templates.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("### Tasks")
                templates.forEach { t ->
                    sb.appendLine("- ${t.taskDescription} *(days: ${t.selectedDays})*")
                }
            }
            sb.appendLine()
        }
        return sb.toString()
    }
}

class ExportDataViewModelFactory(
    private val repository: PlannerRepository,
    private val userDao: UserDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExportDataViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExportDataViewModel(repository, userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
