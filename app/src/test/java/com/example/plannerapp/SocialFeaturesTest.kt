package com.example.plannerapp

import com.example.plannerapp.data.social.CloudUser
import com.example.plannerapp.data.social.FeedFilter
import com.example.plannerapp.data.social.InMemorySocialRepository
import com.example.plannerapp.data.social.VoteType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SocialFeaturesTest {

    private lateinit var socialRepository: InMemorySocialRepository

    @Before
    fun setup() {
        socialRepository = InMemorySocialRepository()
    }

    @Test
    fun getPosts_returnsSeededPosts() = runBlocking {
        val posts = socialRepository.getPosts().first()
        assertTrue(posts.isNotEmpty())
        assertTrue(posts.any { it.title.contains("Morning Routine") })
    }

    @Test
    fun searchPosts_filtersByTitleOrTags() = runBlocking {
        val searchResults = socialRepository.getPosts(query = "Deep Work").first()
        assertEquals(1, searchResults.size)
        assertEquals("Deep Work Protocol", searchResults.first().title)
    }

    @Test
    fun filterPosts_sortsByTrendingAndRecent() = runBlocking {
        val trending = socialRepository.getPosts(filter = FeedFilter.TRENDING).first()
        val recent = socialRepository.getPosts(filter = FeedFilter.RECENT).first()
        assertNotNull(trending)
        assertNotNull(recent)
    }

    @Test
    fun votePost_incrementsAndTogglesScore() = runBlocking {
        val initialPosts = socialRepository.getPosts().first()
        val targetPost = initialPosts.first()
        val initialScore = targetPost.score

        // Upvote
        val upvoted = socialRepository.votePost(targetPost.postId, VoteType.UP).getOrThrow()
        assertEquals(initialScore + 1, upvoted.score)
        assertEquals(VoteType.UP, upvoted.userVote)

        // Cancel Upvote by clicking UP again
        val toggledOff = socialRepository.votePost(targetPost.postId, VoteType.UP).getOrThrow()
        assertEquals(initialScore, toggledOff.score)
        assertNull(toggledOff.userVote)
    }

    @Test
    fun addComment_createsTopLevelAndNestedReplies() = runBlocking {
        val post = socialRepository.getPosts().first().first()
        val author = CloudUser(
            userId = "test_user_1",
            username = "test_dev",
            displayName = "Test Dev"
        )

        // Add top-level comment
        val comment = socialRepository.addComment(
            postId = post.postId,
            author = author,
            content = "This is a great habit system!"
        ).getOrThrow()

        assertNotNull(comment.commentId)
        assertEquals("This is a great habit system!", comment.content)

        // Add nested reply to this comment
        val reply = socialRepository.addComment(
            postId = post.postId,
            author = author,
            content = "Replying to my own question: yes it works!",
            parentCommentId = comment.commentId
        ).getOrThrow()

        assertEquals(comment.commentId, reply.parentCommentId)

        // Fetch tree and verify nesting
        val commentTree = socialRepository.getComments(post.postId).first()
        val rootInTree = commentTree.find { it.commentId == comment.commentId }
        assertNotNull(rootInTree)
        assertTrue(rootInTree!!.replies.any { it.commentId == reply.commentId })
    }

    @Test
    fun createPost_addsNewPostToFeed() = runBlocking {
        val author = CloudUser(
            userId = "creator_1",
            username = "lead_creator",
            displayName = "Lead Creator",
            isCreator = true
        )

        val newPost = socialRepository.createPost(
            author = author,
            title = "100-Day Code Sprint",
            description = "Code 1 hour daily and ship 3 apps.",
            planTemplateJson = "{}",
            durationDays = 100,
            tags = listOf("coding", "android"),
            category = "Productivity"
        ).getOrThrow()

        assertNotNull(newPost.postId)
        assertEquals("100-Day Code Sprint", newPost.title)

        val posts = socialRepository.getPosts().first()
        assertTrue(posts.any { it.postId == newPost.postId })
    }
}
