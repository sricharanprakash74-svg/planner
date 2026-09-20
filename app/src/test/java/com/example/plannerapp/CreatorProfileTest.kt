package com.example.plannerapp

import com.example.plannerapp.data.social.CloudUser
import com.example.plannerapp.data.social.InMemorySocialRepository
import com.example.plannerapp.data.social.PlanVersionUpdate
import com.example.plannerapp.data.social.RelationshipStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreatorProfileTest {

    private lateinit var socialRepository: InMemorySocialRepository

    @Before
    fun setup() {
        socialRepository = InMemorySocialRepository()
    }

    @Test
    fun getUserProfile_returnsVerifiedCreatorDetails() = runTest {
        val sarah = socialRepository.getUserProfile("user_sarah_fit").first()
        assertNotNull(sarah)
        assertEquals("Sarah Jenkins", sarah!!.displayName)
        assertEquals("sarah_fit", sarah.username)
        assertTrue(sarah.isCreator)
        assertTrue(sarah.bio.contains("strength & mindset"))
        assertTrue(sarah.followerCount > 0)
    }

    @Test
    fun getPostsByCreator_filtersPostsExclusivelyForCreator() = runTest {
        val sarahPosts = socialRepository.getPostsByCreator("user_sarah_fit").first()
        val calPosts = socialRepository.getPostsByCreator("user_deep_work").first()

        assertTrue(sarahPosts.isNotEmpty())
        assertTrue(sarahPosts.all { it.author.userId == "user_sarah_fit" })
        assertTrue(sarahPosts.any { it.title.contains("Morning Routine") })

        assertTrue(calPosts.isNotEmpty())
        assertTrue(calPosts.all { it.author.userId == "user_deep_work" })
        assertTrue(calPosts.any { it.title.contains("Deep Work") })
    }

    @Test
    fun followAndUnfollowCreator_updatesStateAndFollowerCounts() = runTest {
        val targetCreatorId = "user_sarah_fit"
        val initialProfile = socialRepository.getUserProfile(targetCreatorId).first()!!
        val initialFollowers = initialProfile.followerCount

        // Initially not following
        assertFalse(socialRepository.isFollowingCreator(targetCreatorId).first())

        // Follow creator
        val followResult = socialRepository.followCreator(targetCreatorId)
        assertTrue(followResult.isSuccess)
        assertTrue(socialRepository.isFollowingCreator(targetCreatorId).first())

        val profileAfterFollow = socialRepository.getUserProfile(targetCreatorId).first()!!
        assertEquals(initialFollowers + 1, profileAfterFollow.followerCount)

        // Idempotent follow
        socialRepository.followCreator(targetCreatorId)
        assertEquals(initialFollowers + 1, socialRepository.getUserProfile(targetCreatorId).first()!!.followerCount)

        // Unfollow creator
        val unfollowResult = socialRepository.unfollowCreator(targetCreatorId)
        assertTrue(unfollowResult.isSuccess)
        assertFalse(socialRepository.isFollowingCreator(targetCreatorId).first())

        val profileAfterUnfollow = socialRepository.getUserProfile(targetCreatorId).first()!!
        assertEquals(initialFollowers, profileAfterUnfollow.followerCount)
    }

    @Test
    fun upsertCloudUser_registersNewCreatorAndSupportsPlanCreation() = runTest {
        val newCreator = CloudUser(
            userId = "user_custom_dev",
            username = "android_lead",
            displayName = "Dev Architect",
            isCreator = true,
            bio = "Crafting high-performance offline-first apps."
        )

        socialRepository.upsertCloudUser(newCreator)

        val retrieved = socialRepository.getUserProfile("user_custom_dev").first()
        assertNotNull(retrieved)
        assertEquals("Dev Architect", retrieved!!.displayName)
        assertTrue(retrieved.isCreator)

        // Create post for this new creator
        val postResult = socialRepository.createPost(
            author = newCreator,
            title = "Clean Architecture Blueprint",
            description = "Learn how to build rock-solid offline-first apps.",
            planTemplateJson = "{}",
            durationDays = 21,
            tags = listOf("architecture", "android"),
            category = "Education"
        )
        assertTrue(postResult.isSuccess)

        val creatorPosts = socialRepository.getPostsByCreator("user_custom_dev").first()
        assertEquals(1, creatorPosts.size)
        assertEquals("Clean Architecture Blueprint", creatorPosts.first().title)
    }

    @Test
    fun viewerRelationship_transitionsAccurately() = runTest {
        val targetId = "user_sarah_fit"

        // Initially Stranger
        val initialRelation = socialRepository.getViewerRelationship(targetId).first()
        assertEquals(RelationshipStatus.STRANGER, initialRelation)

        // Follow -> Following
        socialRepository.followCreator(targetId)
        val followingRelation = socialRepository.getViewerRelationship(targetId).first()
        assertEquals(RelationshipStatus.FOLLOWING, followingRelation)

        // Block -> Blocked
        socialRepository.blockUser(targetId, "current_user")
        val blockedRelation = socialRepository.getViewerRelationship(targetId).first()
        assertEquals(RelationshipStatus.BLOCKED, blockedRelation)

        // Unblock -> Stranger
        socialRepository.unblockUser(targetId, "current_user")
        val unblockedRelation = socialRepository.getViewerRelationship(targetId).first()
        assertEquals(RelationshipStatus.STRANGER, unblockedRelation)
    }

    @Test
    fun canMessageUser_blockedUserCannotBeMessaged() = runTest {
        val targetId = "user_cal_protocols"

        assertTrue(socialRepository.canMessageUser(targetId).first())

        socialRepository.blockUser(targetId, "current_user")
        assertFalse(socialRepository.canMessageUser(targetId).first())

        socialRepository.unblockUser(targetId, "current_user")
        assertTrue(socialRepository.canMessageUser(targetId).first())
    }

    @Test
    fun planVersionUpdate_modelHoldsDeltaTasksAndChangelog() {
        val update = PlanVersionUpdate(
            planId = "plan_123",
            latestVersionTag = "1.2.0",
            currentLocalVersion = "1.0.0",
            changelog = "Added sprint endurance intervals",
            totalTasksInLatest = 5
        )

        assertEquals("plan_123", update.planId)
        assertEquals("1.2.0", update.latestVersionTag)
        assertEquals("1.0.0", update.currentLocalVersion)
        assertEquals("Added sprint endurance intervals", update.changelog)
        assertEquals(5, update.totalTasksInLatest)
    }
}
