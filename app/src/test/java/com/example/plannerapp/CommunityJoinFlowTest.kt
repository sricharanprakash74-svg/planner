package com.example.plannerapp

import com.example.plannerapp.data.DailyCheckinEntity
import com.example.plannerapp.data.DailyTaskView
import com.example.plannerapp.data.JoinedCommunityEntity
import com.example.plannerapp.data.PlanDayCompletionEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerDao
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.TaskTemplateEntity
import com.example.plannerapp.data.social.InMemorySocialRepository
import com.example.plannerapp.data.template.AuthorDto
import com.example.plannerapp.data.template.PlanTemplateDto
import com.example.plannerapp.data.template.TaskTemplateDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CommunityJoinFlowTest {

    private lateinit var fakeDao: FakePlannerDao
    private lateinit var plannerRepository: PlannerRepository
    private lateinit var socialRepository: InMemorySocialRepository

    @Before
    fun setup() {
        fakeDao = FakePlannerDao()
        plannerRepository = PlannerRepository(fakeDao)
        socialRepository = InMemorySocialRepository()
    }

    @Test
    fun joinCommunityPlan_insertsPlanAndCreatesLinkAndIncrementsJoinCount() = runBlocking {
        val posts = socialRepository.getPosts().firstBlocking()
        val targetPost = posts.first()
        val initialJoinCount = targetPost.joinCount

        val sampleTemplate = PlanTemplateDto(
            title = targetPost.title,
            description = targetPost.description,
            targetDurationDays = targetPost.durationDays,
            defaultTaskDurationDays = 1,
            author = AuthorDto(
                userId = targetPost.author.userId,
                displayName = targetPost.author.displayName,
                isCreator = targetPost.author.isCreator
            ),
            tasks = listOf(
                TaskTemplateDto(
                    taskDescription = "Morning Walk",
                    selectedDays = "1,2,3,4,5,6,7",
                    durationDays = 1,
                    subtasks = emptyList()
                )
            )
        )

        val result = plannerRepository.joinCommunityPlan(
            postId = targetPost.postId,
            template = sampleTemplate,
            targetUserId = 42L,
            startDate = LocalDate.of(2026, 9, 1),
            socialRepository = socialRepository
        )

        assertTrue(result.isSuccess)
        val newPlanId = result.getOrThrow()
        assertTrue(newPlanId > 0)

        // Verify plan was created in DAO
        val insertedPlan = fakeDao.plans.find { it.planId == newPlanId }
        assertNotNull(insertedPlan)
        assertEquals("Morning Routine Protocol", insertedPlan!!.heading)
        assertEquals(42L, insertedPlan.userId)
        assertEquals("2026-09-01", insertedPlan.startDate)

        // Verify joined_communities record exists
        val joinedRecord = plannerRepository.getJoinedCommunityForPlanOnce(newPlanId)
        assertNotNull(joinedRecord)
        assertEquals(targetPost.postId, joinedRecord!!.postId)
        assertEquals(newPlanId, joinedRecord.localPlanId)

        // Verify lookup by postId returns this plan
        val lookupRecord = plannerRepository.getJoinedCommunityByPostId(targetPost.postId)
        assertNotNull(lookupRecord)
        assertEquals(newPlanId, lookupRecord!!.localPlanId)

        // Verify remote join count was incremented
        val updatedPost = socialRepository.getPostById(targetPost.postId).firstBlocking()
        assertEquals(initialJoinCount + 1, updatedPost!!.joinCount)
    }

    @Test
    fun joinCommunityPlan_rejectsInvalidTemplate() = runBlocking {
        val invalidTemplate = PlanTemplateDto(
            title = "", // invalid blank title
            targetDurationDays = 0, // invalid 0 duration
            author = AuthorDto("usr1", "User")
        )

        val result = plannerRepository.joinCommunityPlan(
            postId = "any_post",
            template = invalidTemplate,
            targetUserId = 1L,
            socialRepository = socialRepository
        )

        assertTrue(result.isFailure)
        assertTrue(fakeDao.plans.isEmpty())
        assertTrue(fakeDao.joinedCommunities.isEmpty())
    }

    // Helper extension to grab first item from Flow synchronously in test
    private fun <T> Flow<T>.firstBlocking(): T = runBlocking {
        var result: T? = null
        collect {
            result = it
            return@collect
        }
        result ?: throw NoSuchElementException("Empty flow")
    }

    private class FakePlannerDao : PlannerDao {
        val plans = mutableListOf<PlanEntity>()
        val taskTemplates = mutableListOf<TaskTemplateEntity>()
        val checkins = mutableListOf<DailyCheckinEntity>()
        val joinedCommunities = mutableListOf<JoinedCommunityEntity>()
        private var nextPlanId = 1L
        private var nextTemplateId = 1L
        private var nextCheckinId = 1L
        private var nextJoinedId = 1L

        override suspend fun insertPlan(plan: PlanEntity): Long {
            val id = if (plan.planId != 0L) plan.planId else nextPlanId++
            plans.removeAll { it.planId == id }
            plans.add(plan.copy(planId = id))
            return id
        }

        override suspend fun updatePlan(plan: PlanEntity) {
            plans.removeAll { it.planId == plan.planId }
            plans.add(plan)
        }

        override fun getPlansForUser(userId: Long): Flow<List<PlanEntity>> =
            flowOf(plans.filter { it.userId == userId })

        override fun getPlanFlow(planId: Long): Flow<PlanEntity?> =
            flowOf(plans.find { it.planId == planId })

        override suspend fun getPlanById(planId: Long): PlanEntity? =
            plans.find { it.planId == planId }

        override suspend fun updatePlanDetails(
            planId: Long,
            heading: String,
            description: String,
            startDate: String,
            endDate: String,
            defaultTaskDurationDays: Int,
            reminderEnabled: Boolean,
            reminderTime: String?,
            updatedAt: Long
        ) {}

        override suspend fun updatePlanPinStatus(planId: Long, isPinned: Boolean) {}
        override suspend fun updatePlansPinStatus(planIds: List<Long>, isPinned: Boolean) {}
        override suspend fun deletePlan(planId: Long) { plans.removeAll { it.planId == planId } }
        override suspend fun deletePlans(planIds: List<Long>) { plans.removeAll { it.planId in planIds } }
        override suspend fun getPlansWithRemindersEnabled(): List<PlanEntity> =
            plans.filter { it.reminderEnabled }

        override suspend fun insertTaskTemplate(template: TaskTemplateEntity): Long {
            val id = if (template.templateId != 0L) template.templateId else nextTemplateId++
            taskTemplates.removeAll { it.templateId == id }
            taskTemplates.add(template.copy(templateId = id))
            return id
        }

        override suspend fun insertTaskTemplates(templates: List<TaskTemplateEntity>): List<Long> =
            templates.map { insertTaskTemplate(it) }

        override suspend fun updateTaskTemplate(templateId: Long, taskDescription: String, durationDays: Int, subtasks: String) {}
        override suspend fun deleteTaskTemplate(templateId: Long) { taskTemplates.removeAll { it.templateId == templateId } }
        override suspend fun deleteCheckinsForTemplate(templateId: Long) { checkins.removeAll { it.templateId == templateId } }
        override suspend fun getTemplatesForPlan(planId: Long): List<TaskTemplateEntity> =
            taskTemplates.filter { it.planId == planId }

        override suspend fun insertDailyCheckins(checkins: List<DailyCheckinEntity>) {
            for (c in checkins) {
                val id = if (c.checkinId != 0L) c.checkinId else nextCheckinId++
                this.checkins.add(c.copy(checkinId = id))
            }
        }

        override fun getTasksForDate(todayDate: String): Flow<List<DailyTaskView>> = flowOf(emptyList())
        override fun getTasksForPlanAndDate(planId: Long, exactDate: String): Flow<List<DailyTaskView>> = flowOf(emptyList())
        override suspend fun updateCheckinStatus(checkinId: Long, isCompleted: Boolean, completedAt: Long?, timezoneOffset: String) {}
        override suspend fun updateCheckinAndSubtasksStatus(checkinId: Long, isCompleted: Boolean, completedSubtasks: String, completedAt: Long?, timezoneOffset: String) {}
        override suspend fun getPastCheckins(userId: Long, currentDate: String): List<DailyCheckinEntity> = emptyList()
        override fun getCheckinsBetweenDates(userId: Long, startDate: String, endDate: String): Flow<List<DailyCheckinEntity>> = flowOf(emptyList())
        override fun getAllCheckinsForPlan(planId: Long): Flow<List<DailyCheckinEntity>> = flowOf(emptyList())
        override suspend fun insertDayCompletion(completion: PlanDayCompletionEntity) {}
        override fun getPlanDayCompletions(planId: Long): Flow<List<PlanDayCompletionEntity>> = flowOf(emptyList())
        override fun getDayCompletion(planId: Long, exactDate: String): Flow<PlanDayCompletionEntity?> = flowOf(null)
        override suspend fun updateJournalNotes(planId: Long, exactDate: String, notes: String) {}
        override suspend fun getPendingSyncPlans(): List<PlanEntity> = emptyList()
        override suspend fun getPendingSyncTemplates(): List<TaskTemplateEntity> = emptyList()
        override suspend fun getPendingSyncCheckins(): List<DailyCheckinEntity> = emptyList()
        override suspend fun markCheckinsSynced(ids: List<Long>) {}
        override suspend fun markTemplatesSynced(ids: List<Long>) {}
        override suspend fun markPlansSynced(ids: List<Long>) {}
        override suspend fun migrateUserPlans(oldUserId: Long, newUserId: Long) {}

        override suspend fun insertJoinedCommunity(joined: JoinedCommunityEntity): Long {
            val id = if (joined.id != 0L) joined.id else nextJoinedId++
            joinedCommunities.removeAll { it.postId == joined.postId }
            joinedCommunities.add(joined.copy(id = id))
            return id
        }

        override fun getJoinedCommunityForPlan(planId: Long): Flow<JoinedCommunityEntity?> =
            flowOf(joinedCommunities.find { it.localPlanId == planId })

        override suspend fun getJoinedCommunityForPlanOnce(planId: Long): JoinedCommunityEntity? =
            joinedCommunities.find { it.localPlanId == planId }

        override suspend fun getJoinedCommunityByPostId(postId: String): JoinedCommunityEntity? =
            joinedCommunities.find { it.postId == postId }

        override fun getJoinedCommunityByPostIdFlow(postId: String): Flow<JoinedCommunityEntity?> =
            flowOf(joinedCommunities.find { it.postId == postId })

        override fun getAllJoinedCommunities(): Flow<List<JoinedCommunityEntity>> =
            flowOf(joinedCommunities)

        override suspend fun deleteJoinedCommunityForPlan(planId: Long) {
            joinedCommunities.removeAll { it.localPlanId == planId }
        }
    }
}
