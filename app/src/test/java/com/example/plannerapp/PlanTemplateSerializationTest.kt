package com.example.plannerapp

import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.TaskTemplateEntity
import com.example.plannerapp.data.UserEntity
import com.example.plannerapp.data.template.AuthorDto
import com.example.plannerapp.data.template.PlanExporter
import com.example.plannerapp.data.template.PlanImporter
import com.example.plannerapp.data.template.PlanTemplateDto
import com.example.plannerapp.data.template.TaskTemplateDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanTemplateSerializationTest {

    private val exporter = PlanExporter()
    private val importer = PlanImporter()

    @Test
    fun exportPlan_createsValidPlanTemplateDto() {
        val plan = PlanEntity(
            planId = 10,
            userId = 1,
            heading = "30-Day Morning Routine",
            description = "Wake up at 5AM and conquer the day.",
            startDate = "2026-09-01",
            endDate = "2026-09-30",
            defaultTaskDurationDays = 1
        )

        val taskTemplates = listOf(
            TaskTemplateEntity(
                templateId = 101,
                planId = 10,
                taskDescription = "Drink 500ml Water",
                selectedDays = "1,2,3,4,5,6,7",
                durationDays = 1,
                subtasks = "[\"Add lemon\",\"Drink cold\"]"
            ),
            TaskTemplateEntity(
                templateId = 102,
                planId = 10,
                taskDescription = "20 Min Meditation",
                selectedDays = "1,3,5",
                durationDays = 1,
                subtasks = "[]"
            )
        )

        val author = UserEntity(
            userId = 1,
            cloudUserId = "usr_abc123",
            displayName = "Alex Creator",
            isCreator = true
        )

        val dto = exporter.exportPlan(
            plan = plan,
            templates = taskTemplates,
            author = author,
            tags = listOf("morning", "habits", "wellness"),
            category = "Health & Fitness"
        )

        assertEquals("30-Day Morning Routine", dto.title)
        assertEquals("Wake up at 5AM and conquer the day.", dto.description)
        assertEquals(30, dto.targetDurationDays)
        assertEquals("Alex Creator", dto.author.displayName)
        assertEquals("usr_abc123", dto.author.userId)
        assertTrue(dto.author.isCreator)
        assertEquals(2, dto.tasks.size)
        assertEquals("Drink 500ml Water", dto.tasks[0].taskDescription)
        assertEquals(listOf("Add lemon", "Drink cold"), dto.tasks[0].subtasks)
        assertEquals(listOf("morning", "habits", "wellness"), dto.tags)
    }

    @Test
    fun roundtrip_exportAndImportJson_preservesAllData() {
        val originalDto = PlanTemplateDto(
            title = "Deep Work Sprint",
            description = "Focus for 4 hours daily without distraction.",
            targetDurationDays = 14,
            defaultTaskDurationDays = 1,
            tags = listOf("productivity", "focus"),
            category = "Productivity",
            author = AuthorDto(
                userId = "usr_999",
                displayName = "Sam Flow",
                isCreator = true
            ),
            tasks = listOf(
                TaskTemplateDto(
                    taskDescription = "Block 1: Creative Work",
                    selectedDays = "1,2,3,4,5",
                    durationDays = 1,
                    subtasks = listOf("Turn off phone notifications", "Set 90m timer")
                ),
                TaskTemplateDto(
                    taskDescription = "Block 2: Review & Plan",
                    selectedDays = "1,2,3,4,5",
                    durationDays = 1,
                    subtasks = emptyList()
                )
            )
        )

        val json = exporter.toJson(originalDto)
        assertNotNull(json)

        val parseResult = importer.parseJson(json)
        assertTrue(parseResult.isSuccess)

        val parsedDto = parseResult.getOrThrow()
        assertEquals(originalDto.title, parsedDto.title)
        assertEquals(originalDto.description, parsedDto.description)
        assertEquals(originalDto.targetDurationDays, parsedDto.targetDurationDays)
        assertEquals(originalDto.author.displayName, parsedDto.author.displayName)
        assertEquals(2, parsedDto.tasks.size)
        assertEquals(listOf("Turn off phone notifications", "Set 90m timer"), parsedDto.tasks[0].subtasks)
    }

    @Test
    fun validate_rejectsInvalidTemplates() {
        val blankTitleDto = PlanTemplateDto(
            title = "",
            targetDurationDays = 7,
            author = AuthorDto("1", "User")
        )
        val zeroDurationDto = PlanTemplateDto(
            title = "Valid Title",
            targetDurationDays = 0,
            author = AuthorDto("1", "User")
        )

        assertTrue(importer.validate(blankTitleDto).isFailure)
        assertTrue(importer.validate(zeroDurationDto).isFailure)
    }
}
