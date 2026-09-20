package com.example.plannerapp.data.social

import com.example.plannerapp.data.template.AuthorDto
import com.example.plannerapp.data.template.PlanTemplateDto
import com.example.plannerapp.data.template.TaskTemplateDto
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

enum class FeedFilter {
    TRENDING,
    FOLLOWING,
    RECENT,
    MOST_DOWNLOADED,
    FOR_YOU
}

interface SocialRepository {
    fun getPosts(
        filter: FeedFilter = FeedFilter.TRENDING, 
        query: String = "",
        userCategories: List<String> = emptyList(),
        userInterests: List<String> = emptyList()
    ): Flow<List<CommunityPost>>
    fun getPostById(postId: String): Flow<CommunityPost?>
    fun getPostsByCreator(userId: String): Flow<List<CommunityPost>>
    suspend fun createPost(
        author: CloudUser,
        title: String,
        description: String,
        planTemplateJson: String,
        durationDays: Int,
        tags: List<String>,
        category: String,
        isPaid: Boolean = false,
        creditCost: Int = 0,
        visibility: String = "public"
    ): Result<CommunityPost>
    suspend fun deletePost(postId: String, userId: String): Result<Boolean>
    suspend fun votePost(postId: String, voteType: VoteType): Result<CommunityPost>
    suspend fun toggleSavePost(postId: String): Result<Boolean>
    fun getSavedPosts(): Flow<List<CommunityPost>>
    fun getComments(postId: String): Flow<List<PostComment>>
    suspend fun addComment(
        postId: String,
        author: CloudUser,
        content: String,
        parentCommentId: String? = null
    ): Result<PostComment>
    suspend fun deleteComment(postId: String, commentId: String, userId: String): Result<Boolean>
    suspend fun toggleCommentLike(postId: String, commentId: String): Result<PostComment>
    suspend fun incrementJoinCount(postId: String): Result<Int>
    fun getUserProfile(userId: String): Flow<CloudUser?>
    fun isFollowingCreator(userId: String): Flow<Boolean>
    fun getFollowingList(): Flow<Set<String>>
    suspend fun followCreator(userId: String): Result<Boolean>
    suspend fun unfollowCreator(userId: String): Result<Boolean>
    suspend fun upsertCloudUser(user: CloudUser): Result<CloudUser>
    fun search(query: String): Flow<SocialSearchResult>
    fun searchCreators(query: String): Flow<List<CloudUser>>
    // Distinct Plan Relations
    suspend fun usePlan(planId: String, userId: String): Result<Int>
    suspend fun toggleFollowPlan(planId: String, userId: String): Result<Boolean>
    fun isFollowingPlan(planId: String, userId: String): Flow<Boolean>

    // Direct Messaging
    fun getConversations(userId: String): Flow<List<Conversation>>
    suspend fun getOrCreateConversation(currentUserId: String, otherUserId: String): Result<Conversation>
    fun getMessages(conversationId: String): Flow<List<DirectMessage>>
    suspend fun sendMessage(conversationId: String, senderId: String, content: String): Result<DirectMessage>

    // Notifications
    fun getNotifications(userId: String): Flow<List<SocialNotification>>
    suspend fun markNotificationAsRead(notificationId: String): Result<Boolean>
    suspend fun markAllNotificationsAsRead(userId: String): Result<Boolean>

    // Safety & Moderation
    suspend fun reportContent(targetId: String, targetType: String, reason: String, reporterUserId: String): Result<Boolean>
    suspend fun blockUser(targetUserId: String, currentUserId: String): Result<Boolean>
    suspend fun unblockUser(targetUserId: String, currentUserId: String): Result<Boolean>
    fun getBlockedUsers(currentUserId: String): Flow<Set<String>>

    // Plan Versioning & Evolution
    fun checkForPlanUpdate(sourcePlanId: String, currentLocalVersion: String): Flow<PlanVersionUpdate?>

    // Privacy & Relationship Matrix
    fun getPrivacySettings(userId: String): Flow<PrivacySettings?>
    fun canMessageUser(targetUserId: String): Flow<Boolean>
    fun getViewerRelationship(targetUserId: String): Flow<RelationshipStatus>

    // Connects: Followers & Following
    fun getFollowersUsers(userId: String): Flow<List<CloudUser>>
    fun getFollowingUsers(userId: String): Flow<List<CloudUser>>
}

class InMemorySocialRepository(
    private val gson: Gson = Gson()
) : SocialRepository {

    override fun getFollowersUsers(userId: String): Flow<List<CloudUser>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override fun getFollowingUsers(userId: String): Flow<List<CloudUser>> = kotlinx.coroutines.flow.flowOf(emptyList())

    private val _posts = MutableStateFlow<List<CommunityPost>>(emptyList())
    private val _comments = MutableStateFlow<Map<String, List<PostComment>>>(emptyMap())
    private val _users = MutableStateFlow<Map<String, CloudUser>>(emptyMap())
    private val _following = MutableStateFlow<Set<String>>(emptySet())
    private val _followedPlans = MutableStateFlow<Set<String>>(emptySet())
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val _messages = MutableStateFlow<Map<String, List<DirectMessage>>>(emptyMap())
    private val _notifications = MutableStateFlow<List<SocialNotification>>(emptyList())
    private val _blockedUsers = MutableStateFlow<Set<String>>(emptySet())
    private val _reports = MutableStateFlow<List<Report>>(emptyList())

    init {
        seedInitialCommunityData()
    }

    override fun getPosts(
        filter: FeedFilter, 
        query: String,
        userCategories: List<String>,
        userInterests: List<String>
    ): Flow<List<CommunityPost>> {
        return combine(_posts, _blockedUsers, _following) { list, blocked, following ->
            val visible = list.filter {
                it.visibility.equals("public", ignoreCase = true) &&
                    it.author.userId !in blocked
            }
            val filtered = if (query.isBlank()) {
                visible
            } else {
                visible.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true) ||
                        it.author.displayName.contains(query, ignoreCase = true) ||
                        it.author.username.contains(query, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
                }
            }
            when (filter) {
                FeedFilter.TRENDING     -> filtered.sortedByDescending { it.upvoteCount + it.joinCount * 2 }
                FeedFilter.FOLLOWING    -> filtered.filter { it.author.userId in following }.sortedByDescending { it.createdAt }
                FeedFilter.RECENT       -> filtered.sortedByDescending { it.createdAt }
                FeedFilter.MOST_DOWNLOADED -> filtered.sortedByDescending { it.joinCount }
                FeedFilter.FOR_YOU      -> filtered.sortedByDescending { post ->
                    val tagMatchCount = post.tags.count { it in userInterests }
                    val categoryBonus = if (post.category in userCategories) 5 else 0
                    tagMatchCount * 10 + categoryBonus + post.upvoteCount
                }
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
        category: String,
        isPaid: Boolean,
        creditCost: Int,
        visibility: String
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
            isPaid = isPaid,
            creditCost = creditCost,
            createdAt = System.currentTimeMillis(),
            visibility = visibility
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

    override suspend fun toggleCommentLike(postId: String, commentId: String): Result<PostComment> {
        val currentMap = _comments.value.toMutableMap()
        val postComments = currentMap[postId]?.toMutableList() ?: return Result.failure(IllegalArgumentException("Post not found"))
        val targetIndex = postComments.indexOfFirst { it.commentId == commentId }
        if (targetIndex == -1) return Result.failure(IllegalArgumentException("Comment not found"))

        val target = postComments[targetIndex]
        val newLiked = !target.isLiked
        val newCount = if (newLiked) target.upvoteCount + 1 else (target.upvoteCount - 1).coerceAtLeast(0)
        val updated = target.copy(isLiked = newLiked, upvoteCount = newCount)
        postComments[targetIndex] = updated
        currentMap[postId] = postComments
        _comments.value = currentMap
        return Result.success(updated)
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

    override suspend fun deletePost(postId: String, userId: String): Result<Boolean> {
        val currentList = _posts.value
        val post = currentList.find { it.postId == postId } ?: return Result.failure(IllegalArgumentException("Post not found"))
        if (post.author.userId != userId && userId != "system_admin") {
            return Result.failure(IllegalAccessException("Only author can delete post"))
        }
        _posts.value = currentList.filter { it.postId != postId }
        val author = _users.value[post.author.userId]
        if (author != null) {
            _users.value = _users.value + (author.userId to author.copy(publicPlansCount = (author.publicPlansCount - 1).coerceAtLeast(0)))
        }
        return Result.success(true)
    }

    override suspend fun deleteComment(postId: String, commentId: String, userId: String): Result<Boolean> {
        val currentMap = _comments.value.toMutableMap()
        val postComments = currentMap[postId]?.toMutableList() ?: return Result.failure(IllegalArgumentException("Post not found"))
        val target = postComments.find { it.commentId == commentId } ?: return Result.failure(IllegalArgumentException("Comment not found"))
        if (target.author.userId != userId && userId != "system_admin") {
            return Result.failure(IllegalAccessException("Only author can delete comment"))
        }
        postComments.removeAll { it.commentId == commentId || it.parentCommentId == commentId }
        currentMap[postId] = postComments
        _comments.value = currentMap

        val currentPosts = _posts.value.toMutableList()
        val postIdx = currentPosts.indexOfFirst { it.postId == postId }
        if (postIdx != -1) {
            val p = currentPosts[postIdx]
            currentPosts[postIdx] = p.copy(commentCount = postComments.size)
            _posts.value = currentPosts
        }
        return Result.success(true)
    }

    override fun getSavedPosts(): Flow<List<CommunityPost>> {
        return _posts.asStateFlow().map { list -> list.filter { it.isSaved } }
    }

    override fun getFollowingList(): Flow<Set<String>> {
        return _following.asStateFlow()
    }

    override fun search(query: String): Flow<SocialSearchResult> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(SocialSearchResult())
        }
        return combine(_posts, _users, _blockedUsers) { posts, usersMap, blocked ->
            val matchingPlans = posts.filter {
                it.visibility.equals("public", ignoreCase = true) &&
                    it.author.userId !in blocked &&
                    (it.title.contains(cleanQuery, ignoreCase = true) ||
                        it.description.contains(cleanQuery, ignoreCase = true) ||
                        it.category.contains(cleanQuery, ignoreCase = true) ||
                        it.author.displayName.contains(cleanQuery, ignoreCase = true) ||
                        it.author.username.contains(cleanQuery, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(cleanQuery, ignoreCase = true) })
            }
            val matchingCreators = usersMap.values.filter {
                it.userId !in blocked &&
                    (it.username.contains(cleanQuery, ignoreCase = true) ||
                        it.displayName.contains(cleanQuery, ignoreCase = true) ||
                        it.bio.contains(cleanQuery, ignoreCase = true))
            }
            SocialSearchResult(plans = matchingPlans, creators = matchingCreators)
        }
    }

    override fun searchCreators(query: String): Flow<List<CloudUser>> {
        val cleanQuery = query.trim()
        return combine(_users, _blockedUsers) { usersMap, blocked ->
            if (cleanQuery.isBlank()) emptyList()
            else usersMap.values.filter {
                it.userId !in blocked &&
                    (it.username.contains(cleanQuery, ignoreCase = true) ||
                        it.displayName.contains(cleanQuery, ignoreCase = true) ||
                        it.bio.contains(cleanQuery, ignoreCase = true))
            }
        }
    }

    override suspend fun reportContent(
        targetId: String,
        targetType: String,
        reason: String,
        reporterUserId: String
    ): Result<Boolean> {
        val report = Report(
            reportId = "rep_${UUID.randomUUID().toString().take(8)}",
            targetId = targetId,
            targetType = targetType,
            reason = reason,
            reporterUserId = reporterUserId,
            createdAt = System.currentTimeMillis()
        )
        _reports.value = _reports.value + report
        return Result.success(true)
    }

    override suspend fun blockUser(targetUserId: String, currentUserId: String): Result<Boolean> {
        _blockedUsers.value = _blockedUsers.value + targetUserId
        if (_following.value.contains(targetUserId)) {
            unfollowCreator(targetUserId)
        }
        return Result.success(true)
    }

    override suspend fun unblockUser(targetUserId: String, currentUserId: String): Result<Boolean> {
        _blockedUsers.value = _blockedUsers.value - targetUserId
        return Result.success(true)
    }

    override fun getBlockedUsers(currentUserId: String): Flow<Set<String>> {
        return _blockedUsers.asStateFlow()
    }

    override suspend fun usePlan(planId: String, userId: String): Result<Int> {
        return incrementJoinCount(planId)
    }

    override suspend fun toggleFollowPlan(planId: String, userId: String): Result<Boolean> {
        val current = _followedPlans.value
        val isFollowed = current.contains(planId)
        val newSet = if (isFollowed) current - planId else current + planId
        _followedPlans.value = newSet
        return Result.success(!isFollowed)
    }

    override fun isFollowingPlan(planId: String, userId: String): Flow<Boolean> {
        return _followedPlans.asStateFlow().map { it.contains(planId) }
    }

    override fun getConversations(userId: String): Flow<List<Conversation>> {
        return _conversations.asStateFlow()
    }

    override suspend fun getOrCreateConversation(currentUserId: String, otherUserId: String): Result<Conversation> {
        val existing = _conversations.value.find { it.participant?.id == otherUserId }
        if (existing != null) return Result.success(existing)

        val otherUser = _users.value[otherUserId]
        val participantProfile = UserProfile(
            id = otherUserId,
            username = otherUser?.username ?: "user",
            displayName = otherUser?.displayName ?: "User",
            avatarUrl = otherUser?.avatarUrl,
            bio = otherUser?.bio ?: "",
            isCreator = otherUser?.isCreator ?: false
        )
        val newConv = Conversation(
            id = "conv_${UUID.randomUUID().toString().take(8)}",
            participant = participantProfile,
            unreadCount = 0
        )
        _conversations.value = listOf(newConv) + _conversations.value
        return Result.success(newConv)
    }

    override fun getMessages(conversationId: String): Flow<List<DirectMessage>> {
        return _messages.asStateFlow().map { it[conversationId] ?: emptyList() }
    }

    override suspend fun sendMessage(conversationId: String, senderId: String, content: String): Result<DirectMessage> {
        val newMsg = DirectMessage(
            id = "msg_${UUID.randomUUID().toString().take(8)}",
            conversationId = conversationId,
            senderId = senderId,
            content = content,
            createdAt = java.time.Instant.now().toString()
        )
        val map = _messages.value.toMutableMap()
        val list = (map[conversationId] ?: emptyList()) + newMsg
        map[conversationId] = list
        _messages.value = map

        // Update last message in conversation
        val convs = _conversations.value.toMutableList()
        val idx = convs.indexOfFirst { it.id == conversationId }
        if (idx != -1) {
            convs[idx] = convs[idx].copy(lastMessage = newMsg)
            _conversations.value = convs
        }
        return Result.success(newMsg)
    }

    override fun getNotifications(userId: String): Flow<List<SocialNotification>> {
        return _notifications.asStateFlow().map { list -> list.filter { it.recipientId == userId } }
    }

    override suspend fun markNotificationAsRead(notificationId: String): Result<Boolean> {
        val list = _notifications.value.map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }
        _notifications.value = list
        return Result.success(true)
    }

    override suspend fun markAllNotificationsAsRead(userId: String): Result<Boolean> {
        val list = _notifications.value.map {
            if (it.recipientId == userId) it.copy(isRead = true) else it
        }
        _notifications.value = list
        return Result.success(true)
    }

    override fun checkForPlanUpdate(sourcePlanId: String, currentLocalVersion: String): Flow<PlanVersionUpdate?> {
        return kotlinx.coroutines.flow.flowOf(null)
    }

    override fun getPrivacySettings(userId: String): Flow<PrivacySettings?> {
        return kotlinx.coroutines.flow.flowOf(PrivacySettings(userId = userId))
    }

    override fun canMessageUser(targetUserId: String): Flow<Boolean> {
        return _blockedUsers.asStateFlow().map { blocked ->
            targetUserId !in blocked
        }
    }

    override fun getViewerRelationship(targetUserId: String): Flow<RelationshipStatus> {
        return combine(_following, _blockedUsers) { following, blocked ->
            if (targetUserId in blocked) RelationshipStatus.BLOCKED
            else if (targetUserId in following) RelationshipStatus.FOLLOWING
            else RelationshipStatus.STRANGER
        }
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

        val alex = CloudUser(
            userId = "user_alex",
            username = "alex",
            displayName = "Alex Rivera",
            avatarUrl = null,
            isCreator = true,
            bio = "Senior Python engineer & educator. Building practical coding curriculums.",
            followerCount = 4200,
            followingCount = 120,
            publicPlansCount = 1,
            totalMembersJoined = 8900
        )

        val rahul = CloudUser(
            userId = "user_rahul",
            username = "rahul",
            displayName = "Rahul Sharma",
            avatarUrl = null,
            isCreator = true,
            bio = "Tech mentor, ex-FAANG interviewer. Helping developers ace technical interviews.",
            followerCount = 6100,
            followingCount = 85,
            publicPlansCount = 1,
            totalMembersJoined = 15300
        )

        _users.value = mapOf(
            sarah.userId to sarah.copy(followingCount = 45, publicPlansCount = 2),
            cal.userId to cal.copy(followingCount = 12, publicPlansCount = 2),
            zenMind.userId to zenMind.copy(followingCount = 67, publicPlansCount = 1),
            alex.userId to alex,
            rahul.userId to rahul
        )

        // Seed templates
        val alexTemplate = PlanTemplateDto(
            title = "Python in 30 Days",
            description = "Master core Python syntax, OOP, modules, and build 4 real-world projects from scratch in 30 days.",
            targetDurationDays = 30,
            defaultTaskDurationDays = 1,
            tags = listOf("python", "coding", "programming", "beginners"),
            category = "Technology",
            author = AuthorDto(alex.userId, alex.displayName, alex.avatarUrl, alex.isCreator),
            tasks = listOf(
                TaskTemplateDto("Variables, Loops & Control Flow", "1,2,3,4,5,6,7", 1, listOf("Syntax review", "Writing condition logic", "10 practice problems")),
                TaskTemplateDto("Functions & Modules", "1,2,3,4,5,6,7", 1, listOf("Lambda functions", "Importing packages", "Building a math helper module")),
                TaskTemplateDto("Data Structures: Lists, Dicts & Sets", "1,2,3,4,5,6,7", 1, listOf("List comprehensions", "Dictionary lookups", "Time complexity analysis")),
                TaskTemplateDto("Building a CLI Weather Application", "1,2,3,4,5,6,7", 1, listOf("Fetch JSON from API", "Parse responses", "Terminal UI formatting"))
            )
        )

        val rahulTemplate = PlanTemplateDto(
            title = "Python Interview Preparation",
            description = "Comprehensive Python DSA and coding interview roadmap: sliding window, graphs, dynamic programming, and mock interviews.",
            targetDurationDays = 21,
            defaultTaskDurationDays = 1,
            tags = listOf("python", "interview", "leetcode", "algorithms"),
            category = "Career",
            author = AuthorDto(rahul.userId, rahul.displayName, rahul.avatarUrl, rahul.isCreator),
            tasks = listOf(
                TaskTemplateDto("Two Pointers & Sliding Window", "1,2,3,4,5", 1, listOf("Trapping Rain Water", "Longest Substring Without Repeating Characters")),
                TaskTemplateDto("Trees & Graph Traversal", "1,2,3,4,5", 1, listOf("BFS/DFS implementations", "Lowest Common Ancestor", "Cycle Detection")),
                TaskTemplateDto("Dynamic Programming Fundamentals", "1,2,3,4,5", 1, listOf("Memoization vs Tabulation", "0/1 Knapsack", "Coin Change")),
                TaskTemplateDto("Mock Technical Interview & Retrospective", "6", 1, listOf("45-min timed coding", "Big-O verbal walkthrough", "Self-critique log"))
            )
        )

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

        val postAlex = CommunityPost(
            postId = "post_alex_01",
            author = alex,
            title = alexTemplate.title,
            description = alexTemplate.description,
            planTemplateJson = gson.toJson(alexTemplate),
            durationDays = alexTemplate.targetDurationDays,
            tags = alexTemplate.tags,
            category = alexTemplate.category,
            upvoteCount = 485,
            joinCount = 2930,
            commentCount = 1,
            userVote = null,
            createdAt = System.currentTimeMillis() - 86400000L * 1
        )

        val postRahul = CommunityPost(
            postId = "post_rahul_01",
            author = rahul,
            title = rahulTemplate.title,
            description = rahulTemplate.description,
            planTemplateJson = gson.toJson(rahulTemplate),
            durationDays = rahulTemplate.targetDurationDays,
            tags = rahulTemplate.tags,
            category = rahulTemplate.category,
            upvoteCount = 612,
            joinCount = 4120,
            commentCount = 0,
            userVote = null,
            createdAt = System.currentTimeMillis() - 86400000L * 2
        )

        _posts.value = listOf(postAlex, postRahul, post1, post2, post3, post4, post5)

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

        val reply2 = PostComment(
            commentId = "comm_1_reply_2",
            postId = post1.postId,
            author = zenMind,
            content = "Do you take the electrolytes before or after the 10 min meditation?",
            parentCommentId = "comm_1",
            upvoteCount = 5,
            createdAt = System.currentTimeMillis() - 3600000L * 8
        )

        val reply3 = PostComment(
            commentId = "comm_1_reply_3",
            postId = post1.postId,
            author = cal,
            content = "Right when waking up, before meditation. Helps prime the nervous system for focus.",
            parentCommentId = "comm_1",
            upvoteCount = 12,
            createdAt = System.currentTimeMillis() - 3600000L * 6
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
            post1.postId to listOf(comment1, reply1, reply2, reply3, comment2)
        )
    }
}
