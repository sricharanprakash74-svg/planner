package com.example.plannerapp.data.social

import com.example.plannerapp.data.template.AuthorDto
import com.example.plannerapp.data.template.PlanTemplateDto
import com.example.plannerapp.data.template.TaskTemplateDto
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

enum class FeedFilter {
    TRENDING,
    RECENT,
    MOST_DOWNLOADED
}

interface SocialRepository {
    fun getPosts(filter: FeedFilter = FeedFilter.TRENDING, query: String = ""): Flow<List<CommunityPost>>
    fun getPostById(postId: String): Flow<CommunityPost?>
    fun getPostsByCreator(userId: String): Flow<List<CommunityPost>>
    suspend fun createPost(
        author: CloudUser,
        title: String,
        description: String,
        planTemplateJson: String,
        durationDays: Int,
        tags: List<String>,
        category: String
    ): Result<CommunityPost>
    suspend fun votePost(postId: String, voteType: VoteType): Result<CommunityPost>
    suspend fun toggleSavePost(postId: String): Result<Boolean>
    fun getComments(postId: String): Flow<List<PostComment>>
    suspend fun addComment(
        postId: String,
        author: CloudUser,
        content: String,
        parentCommentId: String? = null
    ): Result<PostComment>
    suspend fun incrementJoinCount(postId: String): Result<Int>
    fun getUserProfile(userId: String): Flow<CloudUser?>
    fun isFollowingCreator(userId: String): Flow<Boolean>
    suspend fun followCreator(userId: String): Result<Boolean>
    suspend fun unfollowCreator(userId: String): Result<Boolean>
    suspend fun upsertCloudUser(user: CloudUser): Result<CloudUser>
}

class InMemorySocialRepository(
    private val gson: Gson = Gson()
) : SocialRepository {

    private val _posts = MutableStateFlow<List<CommunityPost>>(emptyList())
    private val _comments = MutableStateFlow<Map<String, List<PostComment>>>(emptyMap())
    private val _users = MutableStateFlow<Map<String, CloudUser>>(emptyMap())
    private val _following = MutableStateFlow<Set<String>>(emptySet())

    init {
        seedInitialCommunityData()
    }

    override fun getPosts(filter: FeedFilter, query: String): Flow<List<CommunityPost>> {
        return _posts.asStateFlow().map { list ->
            val filtered = if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true) ||
                        it.author.displayName.contains(query, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
                }
            }
            when (filter) {
                FeedFilter.TRENDING -> filtered.sortedByDescending { it.upvoteCount + it.joinCount * 2 }
                FeedFilter.RECENT -> filtered.sortedByDescending { it.createdAt }
                FeedFilter.MOST_DOWNLOADED -> filtered.sortedByDescending { it.joinCount }
            }
        }
    }

    override fun getPostById(postId: String): Flow<CommunityPost?> {
        return _posts.asStateFlow().map { list -> list.find { it.postId == postId } }
    }

    override suspend fun createPost(
        author: CloudUser,
        title: String,
        description: String,
        planTemplateJson: String,
        durationDays: Int,
        tags: List<String>,
        category: String
    ): Result<CommunityPost> {
        val newPost = CommunityPost(
            postId = "post_${UUID.randomUUID().toString().take(8)}",
            author = author,
            title = title,
            description = description,
            planTemplateJson = planTemplateJson,
            durationDays = durationDays,
            tags = tags,
            category = category,
            upvoteCount = 1,
            joinCount = 0,
            commentCount = 0,
            userVote = VoteType.UP,
            createdAt = System.currentTimeMillis()
        )
        _posts.value = listOf(newPost) + _posts.value
        return Result.success(newPost)
    }

    override suspend fun votePost(postId: String, voteType: VoteType): Result<CommunityPost> {
        val currentList = _posts.value
        val targetIndex = currentList.indexOfFirst { it.postId == postId }
        if (targetIndex == -1) return Result.failure(IllegalArgumentException("Post not found"))

        val post = currentList[targetIndex]
        val updatedPost = when (post.userVote) {
            voteType -> {
                // Cancel vote
                val diff = if (voteType == VoteType.UP) -1 else 1
                post.copy(upvoteCount = (post.upvoteCount + diff).coerceAtLeast(0), userVote = null)
            }
            null -> {
                // New vote
                val diff = if (voteType == VoteType.UP) 1 else -1
                post.copy(upvoteCount = (post.upvoteCount + diff).coerceAtLeast(0), userVote = voteType)
            }
            else -> {
                // Switch vote (e.g. from Down to Up = +2)
                val diff = if (voteType == VoteType.UP) 2 else -2
                post.copy(upvoteCount = (post.upvoteCount + diff).coerceAtLeast(0), userVote = voteType)
            }
        }

        val mutable = currentList.toMutableList()
        mutable[targetIndex] = updatedPost
        _posts.value = mutable
        return Result.success(updatedPost)
    }

    override suspend fun toggleSavePost(postId: String): Result<Boolean> {
        val currentList = _posts.value
        val targetIndex = currentList.indexOfFirst { it.postId == postId }
        if (targetIndex == -1) return Result.failure(IllegalArgumentException("Post not found"))

        val post = currentList[targetIndex]
        val newSaved = !post.isSaved
        val mutable = currentList.toMutableList()
        mutable[targetIndex] = post.copy(isSaved = newSaved)
        _posts.value = mutable
        return Result.success(newSaved)
    }

    override fun getComments(postId: String): Flow<List<PostComment>> {
        return _comments.asStateFlow().map { map ->
            val list = map[postId] ?: emptyList()
            buildCommentTree(list)
        }
    }

    override suspend fun addComment(
        postId: String,
        author: CloudUser,
        content: String,
        parentCommentId: String?
    ): Result<PostComment> {
        val newComment = PostComment(
            commentId = "comm_${UUID.randomUUID().toString().take(8)}",
            postId = postId,
            author = author,
            content = content,
            parentCommentId = parentCommentId,
            upvoteCount = 1,
            createdAt = System.currentTimeMillis()
        )

        val currentMap = _comments.value.toMutableMap()
        val postComments = currentMap[postId]?.toMutableList() ?: mutableListOf()
        postComments.add(newComment)
        currentMap[postId] = postComments
        _comments.value = currentMap

        // Update comment count on post
        val currentPosts = _posts.value.toMutableList()
        val postIndex = currentPosts.indexOfFirst { it.postId == postId }
        if (postIndex != -1) {
            val p = currentPosts[postIndex]
            currentPosts[postIndex] = p.copy(commentCount = p.commentCount + 1)
            _posts.value = currentPosts
        }

        return Result.success(newComment)
    }

    override suspend fun incrementJoinCount(postId: String): Result<Int> {
        val currentList = _posts.value
        val targetIndex = currentList.indexOfFirst { it.postId == postId }
        if (targetIndex == -1) return Result.failure(IllegalArgumentException("Post not found"))

        val post = currentList[targetIndex]
        val newCount = post.joinCount + 1
        val mutable = currentList.toMutableList()
        mutable[targetIndex] = post.copy(joinCount = newCount)
        _posts.value = mutable
        return Result.success(newCount)
    }

    override fun getUserProfile(userId: String): Flow<CloudUser?> {
        return _users.asStateFlow().map { it[userId] }
    }

    override fun getPostsByCreator(userId: String): Flow<List<CommunityPost>> {
        return _posts.asStateFlow().map { posts ->
            posts.filter { it.author.userId == userId }
        }
    }

    override fun isFollowingCreator(userId: String): Flow<Boolean> {
        return _following.asStateFlow().map { it.contains(userId) }
    }

    override suspend fun followCreator(userId: String): Result<Boolean> {
        val current = _following.value
        if (!current.contains(userId)) {
            _following.value = current + userId
            val user = _users.value[userId]
            if (user != null) {
                _users.value = _users.value + (userId to user.copy(followerCount = user.followerCount + 1))
            }
        }
        return Result.success(true)
    }

    override suspend fun unfollowCreator(userId: String): Result<Boolean> {
        val current = _following.value
        if (current.contains(userId)) {
            _following.value = current - userId
            val user = _users.value[userId]
            if (user != null) {
                _users.value = _users.value + (userId to user.copy(followerCount = (user.followerCount - 1).coerceAtLeast(0)))
            }
        }
        return Result.success(false)
    }

    override suspend fun upsertCloudUser(user: CloudUser): Result<CloudUser> {
        _users.value = _users.value + (user.userId to user)
        return Result.success(user)
    }

    private fun buildCommentTree(flatList: List<PostComment>): List<PostComment> {
        val rootComments = flatList.filter { it.parentCommentId == null }
        val repliesByParent = flatList.filter { it.parentCommentId != null }.groupBy { it.parentCommentId }

        fun attachReplies(comment: PostComment): PostComment {
            val directReplies = repliesByParent[comment.commentId] ?: emptyList()
            return comment.copy(replies = directReplies.map { attachReplies(it) })
        }

        return rootComments.map { attachReplies(it) }
    }

    private fun seedInitialCommunityData() {
        val sarah = CloudUser(
            userId = "user_sarah_fit",
            username = "sarah_fit",
            displayName = "Sarah Jenkins",
            avatarUrl = null,
            isCreator = true,
            bio = "Certified strength & mindset coach. Helping 50k+ build daily morning rituals.",
            followerCount = 1420,
            totalMembersJoined = 5820
        )

        val cal = CloudUser(
            userId = "user_deep_work",
            username = "cal_protocols",
            displayName = "Cal Newport Fanclub",
            avatarUrl = null,
            isCreator = true,
            bio = "Productivity researcher studying deep work, focus blocks, and digital minimalism.",
            followerCount = 3890,
            totalMembersJoined = 12400
        )

        val zenMind = CloudUser(
            userId = "user_zen_mind",
            username = "zen_mind",
            displayName = "Elena Rostova",
            avatarUrl = null,
            isCreator = false,
            bio = "Mindfulness practitioner and breathwork guide.",
            followerCount = 890,
            totalMembersJoined = 2150
        )

        _users.value = mapOf(
            sarah.userId to sarah,
            cal.userId to cal,
            zenMind.userId to zenMind
        )

        // Seed templates
        val sarahTemplate = PlanTemplateDto(
            title = "30-Day Morning Routine",
            description = "Wake up at 5AM, hydrate, meditate for 10 minutes, and write your daily focus goals before touching your phone.",
            targetDurationDays = 30,
            defaultTaskDurationDays = 1,
            tags = listOf("morning", "discipline", "mindset"),
            category = "Health & Fitness",
            author = AuthorDto(sarah.userId, sarah.displayName, sarah.avatarUrl, sarah.isCreator),
            tasks = listOf(
                TaskTemplateDto("Drink 500ml Water + Electrolytes", "1,2,3,4,5,6,7", 1, listOf("Room temp water", "No sugar added")),
                TaskTemplateDto("10 Min Mindfulness Meditation", "1,2,3,4,5,6,7", 1, listOf("Focus on breath", "Box breathing")),
                TaskTemplateDto("Write Top 3 Priority Outcomes", "1,2,3,4,5,6,7", 1, listOf("1 Main Lever", "2 Secondary Tasks")),
                TaskTemplateDto("20 Min Movement / Mobility", "1,2,3,4,5,6,7", 1, listOf("Dynamic stretching", "Light walk or yoga"))
            )
        )

        val calTemplate = PlanTemplateDto(
            title = "Deep Work Protocol",
            description = "4 hours of distraction-free focused work every weekday. No social media, no email during core blocks.",
            targetDurationDays = 21,
            defaultTaskDurationDays = 1,
            tags = listOf("productivity", "focus", "career"),
            category = "Productivity",
            author = AuthorDto(cal.userId, cal.displayName, cal.avatarUrl, cal.isCreator),
            tasks = listOf(
                TaskTemplateDto("Deep Work Block 1 (90m)", "1,2,3,4,5", 1, listOf("Phone in other room", "Close browser tabs", "Work on hard problem")),
                TaskTemplateDto("Mind Reset Break (20m)", "1,2,3,4,5", 1, listOf("Walk outside", "No screens")),
                TaskTemplateDto("Deep Work Block 2 (90m)", "1,2,3,4,5", 1, listOf("Execute primary deliverable")),
                TaskTemplateDto("Daily Shutdown Ritual", "1,2,3,4,5", 1, listOf("Check calendar for tomorrow", "Clear task inbox", "Say 'Schedule shutdown complete'"))
            )
        )

        val zenTemplate = PlanTemplateDto(
            title = "Mindfulness Month",
            description = "Reset your nervous system with daily guided breathwork, gratitude reflections, and zero screen time 1 hour before bed.",
            targetDurationDays = 30,
            defaultTaskDurationDays = 1,
            tags = listOf("mentalhealth", "meditation", "sleep"),
            category = "Wellness",
            author = AuthorDto(zenMind.userId, zenMind.displayName, zenMind.avatarUrl, zenMind.isCreator),
            tasks = listOf(
                TaskTemplateDto("Morning Sun & Gratitude (10m)", "1,2,3,4,5,6,7", 1, listOf("Get direct sunlight", "Name 3 things you are grateful for")),
                TaskTemplateDto("Midday Digital Detox (30m)", "1,2,3,4,5,6,7", 1, listOf("Eat lunch screen-free")),
                TaskTemplateDto("Evening Wind Down & Read", "1,2,3,4,5,6,7", 1, listOf("Screens off at 9:30 PM", "Read 15 pages of physical book"))
            )
        )

        val post1 = CommunityPost(
            postId = "post_sarah_01",
            author = sarah,
            title = sarahTemplate.title,
            description = sarahTemplate.description,
            planTemplateJson = gson.toJson(sarahTemplate),
            durationDays = sarahTemplate.targetDurationDays,
            tags = sarahTemplate.tags,
            category = sarahTemplate.category,
            upvoteCount = 248,
            joinCount = 1420,
            commentCount = 3,
            userVote = null,
            createdAt = System.currentTimeMillis() - 86400000L * 2
        )

        val post2 = CommunityPost(
            postId = "post_cal_02",
            author = cal,
            title = calTemplate.title,
            description = calTemplate.description,
            planTemplateJson = gson.toJson(calTemplate),
            durationDays = calTemplate.targetDurationDays,
            tags = calTemplate.tags,
            category = calTemplate.category,
            upvoteCount = 189,
            joinCount = 890,
            commentCount = 2,
            userVote = null,
            createdAt = System.currentTimeMillis() - 86400000L * 4
        )

        val post3 = CommunityPost(
            postId = "post_zen_03",
            author = zenMind,
            title = zenTemplate.title,
            description = zenTemplate.description,
            planTemplateJson = gson.toJson(zenTemplate),
            durationDays = zenTemplate.targetDurationDays,
            tags = zenTemplate.tags,
            category = zenTemplate.category,
            upvoteCount = 312,
            joinCount = 2100,
            commentCount = 1,
            userVote = null,
            createdAt = System.currentTimeMillis() - 86400000L * 1
        )

        val sarahTemplate2 = PlanTemplateDto(
            title = "Couch to 5K Sprint",
            description = "Progressive interval running program. Alternate between walking and jogging to build cardiovascular endurance safely.",
            targetDurationDays = 30,
            tags = listOf("running", "fitness", "cardio"),
            category = "Fitness",
            author = AuthorDto(sarah.userId, sarah.displayName, sarah.avatarUrl, sarah.isCreator),
            tasks = listOf(
                TaskTemplateDto("Interval Jog / Walk (25m)", "1,3,5", 1, listOf("5m warm-up walk", "6x (60s jog, 90s walk)", "5m cool-down")),
                TaskTemplateDto("Leg Mobility & Stretching", "2,4,6", 1, listOf("Hamstring stretch", "Calf raises", "Foam roll quads"))
            )
        )

        val post4 = CommunityPost(
            postId = "post_sarah_04",
            author = sarah,
            title = sarahTemplate2.title,
            description = sarahTemplate2.description,
            planTemplateJson = gson.toJson(sarahTemplate2),
            durationDays = sarahTemplate2.targetDurationDays,
            tags = sarahTemplate2.tags,
            category = sarahTemplate2.category,
            upvoteCount = 175,
            joinCount = 980,
            commentCount = 0,
            userVote = null,
            createdAt = System.currentTimeMillis() - 86400000L * 5
        )

        val calTemplate2 = PlanTemplateDto(
            title = "Evening Shutdown Protocol",
            description = "A structured end-of-day shutdown to completely detach from work and guarantee restorative sleep.",
            targetDurationDays = 14,
            tags = listOf("productivity", "sleep", "habits"),
            category = "Productivity",
            author = AuthorDto(cal.userId, cal.displayName, cal.avatarUrl, cal.isCreator),
            tasks = listOf(
                TaskTemplateDto("Review Tomorrow's Agenda", "1,2,3,4,5", 1, listOf("Check calendar", "Pick top 3 objectives")),
                TaskTemplateDto("Complete Shutdown Phrase", "1,2,3,4,5", 1, listOf("Close email and Slack", "Say: Schedule shutdown complete"))
            )
        )

        val post5 = CommunityPost(
            postId = "post_cal_05",
            author = cal,
            title = calTemplate2.title,
            description = calTemplate2.description,
            planTemplateJson = gson.toJson(calTemplate2),
            durationDays = calTemplate2.targetDurationDays,
            tags = calTemplate2.tags,
            category = calTemplate2.category,
            upvoteCount = 142,
            joinCount = 760,
            commentCount = 0,
            userVote = null,
            createdAt = System.currentTimeMillis() - 86400000L * 6
        )

        _posts.value = listOf(post1, post2, post3, post4, post5)

        // Seed initial comments
        val comment1 = PostComment(
            commentId = "comm_1",
            postId = post1.postId,
            author = cal,
            content = "This morning routine changed my life. Pairing the 500ml water with electrolytes completely eliminates morning brain fog.",
            upvoteCount = 34,
            createdAt = System.currentTimeMillis() - 3600000L * 12
        )

        val reply1 = PostComment(
            commentId = "comm_1_reply_1",
            postId = post1.postId,
            author = sarah,
            content = "Spot on! That hydration boost is non-negotiable for mental clarity.",
            parentCommentId = "comm_1",
            upvoteCount = 18,
            createdAt = System.currentTimeMillis() - 3600000L * 10
        )

        val comment2 = PostComment(
            commentId = "comm_2",
            postId = post1.postId,
            author = zenMind,
            content = "Just joined this today! Day 1 complete. Anyone else starting today?",
            upvoteCount = 9,
            createdAt = System.currentTimeMillis() - 3600000L * 4
        )

        _comments.value = mapOf(
            post1.postId to listOf(comment1, reply1, comment2)
        )
    }
}
