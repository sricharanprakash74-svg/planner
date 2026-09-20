package com.example.plannerapp.data.social

import com.example.plannerapp.auth.SupabaseConfig
import com.example.plannerapp.data.template.PlanTemplateDto
import com.example.plannerapp.data.template.TaskTemplateDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import java.util.UUID

class SupabaseSocialRepository(
    private val client: SupabaseClient = SupabaseConfig.client,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
) : SocialRepository {

    private val postgrest: Postgrest
        get() = SupabaseConfig.postgrest

    private val _activeRealtimeChannels = mutableSetOf<String>()

    private val currentUserId: String?
        get() = SupabaseConfig.auth.currentUserOrNull()?.id

    // In-memory cache for fast responsive offline/hybrid operations
    private val _cachedPublicPlans = MutableStateFlow<List<PublicPlan>>(emptyList())
    val cachedPublicPlans: Flow<List<PublicPlan>> = _cachedPublicPlans.asStateFlow()

    private val _cachedComments = MutableStateFlow<Map<String, List<SocialComment>>>(emptyMap())
    private val _cachedProfiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    private val _followingUserIds = MutableStateFlow<Set<String>>(emptySet())
    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    private val _savedPlanIds = MutableStateFlow<Set<String>>(emptySet())
    private val _followedPlanIds = MutableStateFlow<Set<String>>(emptySet())
    private val _likedPlanIds = MutableStateFlow<Set<String>>(emptySet())
    private val _cachedConversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val _cachedMessages = MutableStateFlow<Map<String, List<DirectMessage>>>(emptyMap())
    private val _cachedNotifications = MutableStateFlow<List<SocialNotification>>(emptyList())

    // ISSUE-19: Tracks whether we have loaded saved/liked/followed/blocked state for the current session
    private var _userStateInitialized = false

    /**
     * ISSUE-19: Load all user-specific interaction state (saved/liked/followed plans, following/blocked users)
     * from Supabase at the start of a session. This ensures returning users see correct UI state immediately.
     * Safe to call multiple times — only runs once per session instance.
     */
    suspend fun initializeUserState() = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext
        if (_userStateInitialized || !SupabaseConfig.isConfigured) return@withContext
        _userStateInitialized = true
        try {
            // Load saved plan IDs
            val savedIds = postgrest.from("plan_saves").select {
                filter { eq("user_id", uid) }
            }.decodeList<PlanSave>().map { it.planId }.toSet()
            _savedPlanIds.value = savedIds

            // Load liked plan IDs
            val likedIds = postgrest.from("plan_likes").select {
                filter { eq("user_id", uid) }
            }.decodeList<PlanLike>().map { it.planId }.toSet()
            _likedPlanIds.value = likedIds

            // Load followed plan IDs
            val followedIds = postgrest.from("plan_follows").select {
                filter { eq("user_id", uid) }
            }.decodeList<PlanFollow>().map { it.planId }.toSet()
            _followedPlanIds.value = followedIds

            // Load following user IDs
            val followingIds = postgrest.from("user_follows").select {
                filter { eq("follower_id", uid) }
            }.decodeList<UserFollowRelationship>().map { it.followingId }.toSet()
            _followingUserIds.value = followingIds

            // Load blocked user IDs
            val blockedIds = postgrest.from("user_blocks").select {
                filter { eq("blocker_id", uid) }
            }.decodeList<UserBlock>().map { it.blockedId }.toSet()
            _blockedUserIds.value = blockedIds
        } catch (e: Exception) {
            // Non-fatal: if this fails, states remain empty and are populated lazily via interactions
            _userStateInitialized = false // allow retry on next call
        }
    }

    // ── 1. PUBLIC PLAN DISCOVERY & BROWSING ──────────────────────────────────

    suspend fun fetchPublicPlans(
        category: String? = null,
        tag: String? = null,
        filter: FeedFilter = FeedFilter.TRENDING,
        query: String = "",
        limit: Long = 30,
        userCategories: List<String> = emptyList(),
        userInterests: List<String> = emptyList()
    ): Result<List<PublicPlan>> = withContext(Dispatchers.IO) {
        // ISSUE-19: Initialize user state on the first real fetch so saves/likes/follows are correct
        if (!_userStateInitialized) {
            try { initializeUserState() } catch (_: Exception) {}
        }

        try {
            if (!SupabaseConfig.isConfigured) {
                val cached = _cachedPublicPlans.value.filter { it.creatorId !in _blockedUserIds.value }
                val filtered = if (filter == FeedFilter.FOLLOWING) {
                    cached.filter { it.creatorId in _followingUserIds.value }
                } else if (filter == FeedFilter.FOR_YOU) {
                    cached.sortedByDescending { post ->
                        val tagMatchCount = post.tags.count { it in userInterests }
                        val categoryBonus = if (post.category in userCategories) 5 else 0
                        tagMatchCount * 10 + categoryBonus + post.likesCount
                    }
                } else cached
                return@withContext Result.success(filtered)
            }

            val uid = currentUserId
            val followingIds = if (filter == FeedFilter.FOLLOWING) {
                if (uid != null) {
                    try {
                        postgrest.from("user_follows").select {
                            filter { eq("follower_id", uid) }
                        }.decodeList<UserFollowRelationship>().map { it.followingId }
                    } catch (e: Exception) {
                        _followingUserIds.value.toList()
                    }
                } else {
                    _followingUserIds.value.toList()
                }
            } else emptyList()

            if (filter == FeedFilter.FOLLOWING && followingIds.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val plansRaw = try {
                postgrest.from("public_plans").select(
                    Columns.raw("*, creator:profiles(*)")
                ) {
                    filter {
                        if (filter == FeedFilter.FOLLOWING) {
                            isIn("creator_id", followingIds)
                            isIn("visibility", listOf("PUBLIC", "public", "FOLLOWERS_ONLY", "followers_only"))
                        } else {
                            isIn("visibility", listOf("PUBLIC", "public"))
                        }
                        if (!category.isNullOrBlank() && category != "All" && category != "General") {
                            eq("category", category)
                        }
                        if (query.isNotBlank()) {
                            or {
                                ilike("title", "%$query%")
                                ilike("description", "%$query%")
                                ilike("category", "%$query%")
                            }
                        }
                    }
                    when (filter) {
                        FeedFilter.TRENDING -> order("likes_count", Order.DESCENDING)
                        FeedFilter.MOST_DOWNLOADED -> order("uses_count", Order.DESCENDING)
                        FeedFilter.RECENT -> order("created_at", Order.DESCENDING)
                        FeedFilter.FOLLOWING -> order("created_at", Order.DESCENDING)
                        FeedFilter.FOR_YOU -> order("likes_count", Order.DESCENDING)
                    }
                    limit(if (filter == FeedFilter.FOR_YOU) 100 else limit)
                }.decodeList<PublicPlan>()
            } catch (_: Exception) {
                try {
                    postgrest.from("public_plans").select {
                        filter {
                            if (filter == FeedFilter.FOLLOWING) {
                                isIn("creator_id", followingIds)
                                isIn("visibility", listOf("PUBLIC", "public", "FOLLOWERS_ONLY", "followers_only"))
                            } else {
                                isIn("visibility", listOf("PUBLIC", "public"))
                            }
                            if (!category.isNullOrBlank() && category != "All" && category != "General") {
                                eq("category", category)
                            }
                            if (query.isNotBlank()) {
                                or {
                                    ilike("title", "%$query%")
                                    ilike("description", "%$query%")
                                    ilike("category", "%$query%")
                                }
                            }
                        }
                        when (filter) {
                            FeedFilter.TRENDING -> order("likes_count", Order.DESCENDING)
                            FeedFilter.MOST_DOWNLOADED -> order("uses_count", Order.DESCENDING)
                            FeedFilter.RECENT -> order("created_at", Order.DESCENDING)
                            FeedFilter.FOLLOWING -> order("created_at", Order.DESCENDING)
                            FeedFilter.FOR_YOU -> order("likes_count", Order.DESCENDING)
                        }
                        limit(if (filter == FeedFilter.FOR_YOU) 100 else limit)
                    }.decodeList<PublicPlan>()
                } catch (_: Exception) { emptyList() }
            }

            // Populate creator profiles if not joined
            val creatorIdsToFetch = plansRaw.filter { it.creator == null && it.creatorId.isNotBlank() }.map { it.creatorId }.distinct()
            val profilesMap: Map<String, UserProfile> = if (creatorIdsToFetch.isNotEmpty()) {
                try {
                    postgrest.from("profiles").select {
                        filter { isIn("id", creatorIdsToFetch) }
                    }.decodeList<UserProfile>().associateBy { it.id }
                } catch (_: Exception) { emptyMap() }
            } else emptyMap()

            val resolvedPlans = mutableListOf<PublicPlan>()
            for (p in plansRaw) {
                if (p.creator == null) {
                    val prof: UserProfile? = profilesMap[p.creatorId]
                    if (prof != null) {
                        resolvedPlans.add(p.copy(creator = prof))
                        continue
                    }
                }
                resolvedPlans.add(p)
            }
            var plans: List<PublicPlan> = resolvedPlans
            
            // Post-query sorting for FOR_YOU based on tags
            if (filter == FeedFilter.FOR_YOU) {
                plans = plans.sortedByDescending { post ->
                    val tagMatchCount = post.tags.count { it in userInterests }
                    val categoryBonus = if (post.category in userCategories) 5 else 0
                    tagMatchCount * 10 + categoryBonus + post.likesCount
                }.take(limit.toInt())
            }

            val enriched = enrichPlansWithUserState(plans)
            _cachedPublicPlans.value = enriched
            Result.success(enriched)
        } catch (e: Exception) {
            val cached = _cachedPublicPlans.value.filter { it.creatorId !in _blockedUserIds.value }
            val filtered = if (filter == FeedFilter.FOLLOWING) {
                cached.filter { it.creatorId in _followingUserIds.value }
            } else cached
            Result.success(filtered)
        }
    }

    suspend fun getPublicPlanByIdDirect(planId: String): Result<PublicPlan?> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseConfig.isConfigured) {
                return@withContext Result.success(_cachedPublicPlans.value.find { it.id == planId })
            }

            val plan = postgrest.from("public_plans").select(
                Columns.raw("*, creator:profiles(*)")
            ) {
                filter { eq("id", planId) }
                single()
            }.decodeSingleOrNull<PublicPlan>()

            val enriched = plan?.let { enrichPlansWithUserState(listOf(it)).firstOrNull() }
            Result.success(enriched)
        } catch (e: Exception) {
            Result.success(_cachedPublicPlans.value.find { it.id == planId })
        }
    }

    suspend fun getPlanVersions(planId: String): Result<List<PlanVersion>> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseConfig.isConfigured) return@withContext Result.success(emptyList())

            val versions = postgrest.from("plan_versions").select {
                filter { eq("plan_id", planId) }
                order("published_at", Order.DESCENDING)
            }.decodeList<PlanVersion>()

            Result.success(versions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── 2. PLAN PUBLISHING & VERSIONING ──────────────────────────────────────

    suspend fun publishPublicPlan(
        title: String,
        description: String,
        category: String,
        tags: List<String>,
        durationDays: Int,
        templateJson: String,
        versionTag: String = "1.0.0",
        changelog: String = "Initial release",
        creatorId: String? = null,
        visibility: String = "PUBLIC"
    ): Result<PublicPlan> = withContext(Dispatchers.IO) {
        val rawUid = currentUserId ?: creatorId
        if (rawUid.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Must be signed in to publish plans online"))
        }

        val validUuid = try {
            UUID.fromString(rawUid).toString()
        } catch (e: Exception) {
            UUID.nameUUIDFromBytes(rawUid.toByteArray()).toString()
        }

        try {
            if (SupabaseConfig.isConfigured) {
                // Ensure profile row exists to prevent FK violation
                try {
                    postgrest.from("profiles").upsert(buildJsonObject {
                        put("id", validUuid)
                        put("username", "user_${validUuid.take(8)}")
                        put("display_name", "Planner User")
                    })
                } catch (_: Exception) { }

                val newPlanPayload = buildJsonObject {
                    put("creator_id", validUuid)
                    put("title", title)
                    put("description", description)
                    put("category", category)
                    put("duration_days", durationDays)
                    put("current_version", versionTag)
                    put("visibility", visibility.uppercase())
                    put("tags", kotlinx.serialization.json.buildJsonArray { 
                        tags.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } 
                    })
                }

                val createdPlan = postgrest.from("public_plans").insert(newPlanPayload) {
                    select()
                    single()
                }.decodeSingle<PublicPlan>()

                // BUG-09: plan_versions is the authoritative source for templateJson.
                // Failure here means the plan has no cloneable template, so we propagate the error.
                val versionPayload = buildJsonObject {
                    put("plan_id", createdPlan.id)
                    put("version_tag", versionTag)
                    put("changelog", changelog)
                    put("template_json", templateJson)
                }
                postgrest.from("plan_versions").insert(versionPayload)

                _cachedPublicPlans.value = listOf(createdPlan) + _cachedPublicPlans.value
                Result.success(createdPlan)
            } else {
                // Supabase not configured — offline/demo mode only
                val offlinePlan = PublicPlan(
                    id = "plan_${UUID.randomUUID().toString().take(8)}",
                    creatorId = validUuid,
                    title = title,
                    description = description,
                    category = category,
                    tags = tags,
                    durationDays = durationDays,
                    currentVersion = versionTag,
                    visibility = visibility.uppercase()
                )
                _cachedPublicPlans.value = listOf(offlinePlan) + _cachedPublicPlans.value
                Result.success(offlinePlan)
            }
        } catch (e: Exception) {
            // BUG-08: Return failure so the caller (ProfileViewModel, PublishPlanViewModel) can
            // show an error to the user. Do NOT create a fake local plan and return success —
            // that would mislead the user into thinking their post went live when it didn't.
            Result.failure(e)
        }
    }

    // ── 3. DISTINCT RELATIONSHIPS (SAVE, USE, FOLLOW PLAN, LIKE) ─────────────

    suspend fun toggleSavePublicPlan(planId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        try {
            val isCurrentlySaved = _savedPlanIds.value.contains(planId) || isPlanSavedDirect(planId)
            if (isCurrentlySaved) {
                postgrest.from("plan_saves").delete {
                    filter {
                        eq("user_id", uid)
                        eq("plan_id", planId)
                    }
                }
                _savedPlanIds.value = _savedPlanIds.value - planId
                updatePlanInCache(planId) { it.copy(isSaved = false, savesCount = (it.savesCount - 1).coerceAtLeast(0)) }
                Result.success(false)
            } else {
                val payload = buildJsonObject {
                    put("user_id", uid)
                    put("plan_id", planId)
                }
                postgrest.from("plan_saves").insert(payload)
                _savedPlanIds.value = _savedPlanIds.value + planId
                updatePlanInCache(planId) { it.copy(isSaved = true, savesCount = it.savesCount + 1) }
                Result.success(true)
            }
        } catch (e: Exception) {
            val isCurrentlySaved = _savedPlanIds.value.contains(planId)
            val newSaved = !isCurrentlySaved
            _savedPlanIds.value = if (newSaved) _savedPlanIds.value + planId else _savedPlanIds.value - planId
            updatePlanInCache(planId) { it.copy(isSaved = newSaved) }
            Result.success(newSaved)
        }
    }

    private suspend fun isPlanSavedDirect(planId: String): Boolean {
        val uid = currentUserId ?: return false
        return try {
            val count = postgrest.from("plan_saves").select {
                filter {
                    eq("user_id", uid)
                    eq("plan_id", planId)
                }
            }.decodeList<PlanSave>().size
            count > 0
        } catch (e: Exception) {
            _savedPlanIds.value.contains(planId)
        }
    }

    suspend fun recordPlanUsageDirect(planId: String, versionId: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        try {
            val payload = buildJsonObject {
                put("user_id", uid)
                put("plan_id", planId)
                if (versionId != null) put("version_id", versionId)
            }
            postgrest.from("plan_uses").insert(payload)
            updatePlanInCache(planId) { it.copy(isUsed = true, usesCount = it.usesCount + 1) }
            val currentUses = _cachedPublicPlans.value.find { it.id == planId }?.usesCount ?: 1
            Result.success(currentUses)
        } catch (e: Exception) {
            updatePlanInCache(planId) { it.copy(isUsed = true, usesCount = it.usesCount + 1) }
            Result.success(1)
        }
    }

    suspend fun toggleFollowPlanDirect(planId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        try {
            val isFollowed = _followedPlanIds.value.contains(planId)
            if (isFollowed) {
                postgrest.from("plan_follows").delete {
                    filter {
                        eq("user_id", uid)
                        eq("plan_id", planId)
                    }
                }
                _followedPlanIds.value = _followedPlanIds.value - planId
                updatePlanInCache(planId) { it.copy(isFollowing = false, followsCount = (it.followsCount - 1).coerceAtLeast(0)) }
                Result.success(false)
            } else {
                val payload = buildJsonObject {
                    put("user_id", uid)
                    put("plan_id", planId)
                }
                postgrest.from("plan_follows").insert(payload)
                _followedPlanIds.value = _followedPlanIds.value + planId
                updatePlanInCache(planId) { it.copy(isFollowing = true, followsCount = it.followsCount + 1) }
                Result.success(true)
            }
        } catch (e: Exception) {
            val isFollowed = _followedPlanIds.value.contains(planId)
            val newFollow = !isFollowed
            _followedPlanIds.value = if (newFollow) _followedPlanIds.value + planId else _followedPlanIds.value - planId
            updatePlanInCache(planId) { it.copy(isFollowing = newFollow) }
            Result.success(newFollow)
        }
    }

    suspend fun toggleLikePublicPlan(planId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        try {
            val isLiked = _likedPlanIds.value.contains(planId)
            if (isLiked) {
                postgrest.from("plan_likes").delete {
                    filter {
                        eq("user_id", uid)
                        eq("plan_id", planId)
                    }
                }
                _likedPlanIds.value = _likedPlanIds.value - planId
                updatePlanInCache(planId) { it.copy(isLiked = false, likesCount = (it.likesCount - 1).coerceAtLeast(0)) }
                Result.success(false)
            } else {
                val payload = buildJsonObject {
                    put("user_id", uid)
                    put("plan_id", planId)
                }
                postgrest.from("plan_likes").insert(payload)
                _likedPlanIds.value = _likedPlanIds.value + planId
                updatePlanInCache(planId) { it.copy(isLiked = true, likesCount = it.likesCount + 1) }
                Result.success(true)
            }
        } catch (e: Exception) {
            val isLiked = _likedPlanIds.value.contains(planId)
            val newLiked = !isLiked
            _likedPlanIds.value = if (newLiked) _likedPlanIds.value + planId else _likedPlanIds.value - planId
            updatePlanInCache(planId) { it.copy(isLiked = newLiked) }
            Result.success(newLiked)
        }
    }

    // ── 4. SOCIALREPOSITORY INTERFACE IMPLEMENTATION ────────────────────────

    override fun getPosts(
        filter: FeedFilter, 
        query: String,
        userCategories: List<String>,
        userInterests: List<String>
    ): Flow<List<CommunityPost>> = flow {
        fetchPublicPlans(
            filter = filter, 
            query = query,
            userCategories = userCategories,
            userInterests = userInterests
        )
        emit(_cachedPublicPlans.value.map { planToCommunityPost(it) })
    }.flowOn(Dispatchers.IO)

    override fun getPostById(postId: String): Flow<CommunityPost?> = flow {
        val plan = _cachedPublicPlans.value.find { it.id == postId } 
            ?: getPublicPlanByIdDirect(postId).getOrNull()
        if (plan != null) {
            val templateJson = getPlanVersions(plan.id).getOrNull()?.firstOrNull()?.templateJson
            emit(planToCommunityPost(plan, templateJson = templateJson))
        } else {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    override fun getPostsByCreator(userId: String): Flow<List<CommunityPost>> = flow {
        if (_blockedUserIds.value.contains(userId)) {
            emit(emptyList())
            return@flow
        }
        if (!SupabaseConfig.isConfigured) {
            emit(_cachedPublicPlans.value.filter { it.creatorId == userId && it.creatorId !in _blockedUserIds.value }.map { planToCommunityPost(it) })
            return@flow
        }
        try {
            val uid = currentUserId
            val isFollowing = if (uid != null) {
                try {
                    postgrest.from("user_follows").select {
                        filter {
                            eq("follower_id", uid)
                            eq("following_id", userId)
                        }
                    }.decodeList<UserFollowRelationship>().isNotEmpty()
                } catch (e: Exception) { _followingUserIds.value.contains(userId) }
            } else false

            val allowedVisibilities = if (isFollowing || (uid != null && uid == userId)) {
                listOf("PUBLIC", "FOLLOWERS_ONLY")
            } else {
                listOf("PUBLIC")
            }

            val plans = postgrest.from("public_plans").select(
                Columns.raw("*, creator:profiles(*)")
            ) {
                filter {
                    eq("creator_id", userId)
                    isIn("visibility", allowedVisibilities)
                }
                order("created_at", Order.DESCENDING)
            }.decodeList<PublicPlan>()
            val enriched = enrichPlansWithUserState(plans)
            emit(enriched.map { planToCommunityPost(it) })
        } catch (e: Exception) {
            emit(_cachedPublicPlans.value.filter { it.creatorId == userId && it.creatorId !in _blockedUserIds.value }.map { planToCommunityPost(it) })
        }
    }.flowOn(Dispatchers.IO)

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
        val planResult = publishPublicPlan(
            title = title,
            description = description,
            category = category,
            tags = tags,
            durationDays = durationDays,
            templateJson = planTemplateJson,
            versionTag = "1.0.0",
            changelog = "Initial community publish",
            creatorId = author.userId,
            visibility = visibility
        )
        return planResult.map { planToCommunityPost(it, templateJson = planTemplateJson) }
    }

    override suspend fun deletePost(postId: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            postgrest.from("public_plans").delete {
                filter {
                    eq("id", postId)
                    eq("creator_id", userId)
                }
            }
            _cachedPublicPlans.value = _cachedPublicPlans.value.filter { it.id != postId }
            Result.success(true)
        } catch (e: Exception) {
            _cachedPublicPlans.value = _cachedPublicPlans.value.filter { it.id != postId }
            Result.success(true)
        }
    }

    override suspend fun votePost(postId: String, voteType: VoteType): Result<CommunityPost> {
        val result = toggleLikePublicPlan(postId)
        val plan = _cachedPublicPlans.value.find { it.id == postId }
            ?: PublicPlan(id = postId, creatorId = "", title = "")
        return Result.success(planToCommunityPost(plan))
    }

    override suspend fun toggleSavePost(postId: String): Result<Boolean> {
        return toggleSavePublicPlan(postId)
    }

    override fun getSavedPosts(): Flow<List<CommunityPost>> = flow {
        val uid = currentUserId
        if (uid == null || !SupabaseConfig.isConfigured) {
            emit(_cachedPublicPlans.value.filter { it.isSaved || _savedPlanIds.value.contains(it.id) }.map { planToCommunityPost(it) })
            return@flow
        }
        try {
            val savedRecords = postgrest.from("plan_saves").select(
                Columns.raw("plan:public_plans(*, creator:profiles(*))")
            ) {
                filter { eq("user_id", uid) }
            }.decodeList<JsonObject>()

            val posts = savedRecords.mapNotNull { record ->
                val planObj = record["plan"]?.toString()
                if (planObj != null) {
                    try {
                        val plan = json.decodeFromString<PublicPlan>(planObj)
                        planToCommunityPost(plan.copy(isSaved = true))
                    } catch (e: Exception) { null }
                } else null
            }
            emit(posts)
        } catch (e: Exception) {
            emit(_cachedPublicPlans.value.filter { it.isSaved || _savedPlanIds.value.contains(it.id) }.map { planToCommunityPost(it) })
        }
    }.flowOn(Dispatchers.IO)

    override fun getComments(postId: String): Flow<List<PostComment>> = flow {
        if (!SupabaseConfig.isConfigured) {
            val list = _cachedComments.value[postId] ?: emptyList()
            emit(list.map { socialCommentToPostComment(it) })
            return@flow
        }
        try {
            val comments = postgrest.from("comments").select(
                Columns.raw("*, author:profiles(*)")
            ) {
                filter { eq("plan_id", postId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<SocialComment>()

            _cachedComments.value = _cachedComments.value + (postId to comments)
            emit(comments.map { socialCommentToPostComment(it) })
        } catch (e: Exception) {
            val list = _cachedComments.value[postId] ?: emptyList()
            emit(list.map { socialCommentToPostComment(it) })
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun addComment(
        postId: String,
        author: CloudUser,
        content: String,
        parentCommentId: String?
    ): Result<PostComment> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: author.userId
        try {
            val payload = buildJsonObject {
                put("user_id", uid)
                put("plan_id", postId)
                put("content", content)
                if (parentCommentId != null) put("parent_comment_id", parentCommentId)
            }
            val inserted = postgrest.from("comments").insert(payload) {
                select(Columns.raw("*, author:profiles(*)"))
                single()
            }.decodeSingle<SocialComment>()
            Result.success(socialCommentToPostComment(inserted))
        } catch (e: Exception) {
            val localComment = PostComment(
                commentId = "comm_${UUID.randomUUID().toString().take(8)}",
                postId = postId,
                author = author,
                content = content,
                parentCommentId = parentCommentId,
                upvoteCount = 0,
                createdAt = System.currentTimeMillis()
            )
            Result.success(localComment)
        }
    }

    override suspend fun deleteComment(postId: String, commentId: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            postgrest.from("comments").delete {
                filter {
                    eq("id", commentId)
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    override suspend fun toggleCommentLike(postId: String, commentId: String): Result<PostComment> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: "guest"
        try {
            if (!SupabaseConfig.isConfigured) {
                val currentList = _cachedComments.value[postId] ?: emptyList()
                val targetComment = currentList.find { it.id == commentId }
                val newLiked = !(targetComment?.isLiked ?: false)
                val currentLikes = targetComment?.likesCount ?: 0
                val newCount = if (newLiked) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0)
                val updated = currentList.map {
                    if (it.id == commentId) it.copy(isLiked = newLiked, likesCount = newCount) else it
                }
                _cachedComments.value = _cachedComments.value + (postId to updated)
                val updatedTarget = updated.find { it.id == commentId }
                return@withContext Result.success(
                    updatedTarget?.let { socialCommentToPostComment(it) } ?: PostComment(
                        commentId = commentId,
                        postId = postId,
                        author = CloudUser(userId = uid, username = "", displayName = "User"),
                        content = "",
                        upvoteCount = newCount,
                        isLiked = newLiked
                    )
                )
            }

            val existing = postgrest.from("comment_likes").select {
                filter {
                    eq("comment_id", commentId)
                    eq("user_id", uid)
                }
            }.decodeList<CommentLike>()

            val wasLiked = existing.isNotEmpty()
            if (wasLiked) {
                postgrest.from("comment_likes").delete {
                    filter {
                        eq("comment_id", commentId)
                        eq("user_id", uid)
                    }
                }
            } else {
                postgrest.from("comment_likes").insert(buildJsonObject {
                    put("comment_id", commentId)
                    put("user_id", uid)
                })
            }

            val refreshed = postgrest.from("comments").select(Columns.raw("*, author:profiles(*)")) {
                filter { eq("id", commentId) }
                single()
            }.decodeSingle<SocialComment>()

            Result.success(socialCommentToPostComment(refreshed.copy(isLiked = !wasLiked)))
        } catch (e: Exception) {
            val currentList = _cachedComments.value[postId] ?: emptyList()
            val targetComment = currentList.find { it.id == commentId }
            val newLiked = !(targetComment?.isLiked ?: false)
            val currentLikes = targetComment?.likesCount ?: 0
            val newCount = if (newLiked) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0)
            val fallback = PostComment(
                commentId = commentId,
                postId = postId,
                author = CloudUser(userId = uid, username = "", displayName = "User"),
                content = targetComment?.content ?: "",
                upvoteCount = newCount,
                isLiked = newLiked
            )
            Result.success(fallback)
        }
    }

    override suspend fun incrementJoinCount(postId: String): Result<Int> {
        return recordPlanUsageDirect(postId)
    }

    override fun getUserProfile(userId: String): Flow<CloudUser?> = flow {
        if (!SupabaseConfig.isConfigured) {
            val cached = _cachedProfiles.value[userId]
            emit(cached?.let { profileToCloudUser(it) })
            return@flow
        }
        try {
            val profile = postgrest.from("profiles").select {
                filter { eq("id", userId) }
                single()
            }.decodeSingleOrNull<UserProfile>()

            if (profile != null) {
                _cachedProfiles.value = _cachedProfiles.value + (userId to profile)
                emit(profileToCloudUser(profile))
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            val cached = _cachedProfiles.value[userId]
            emit(cached?.let { profileToCloudUser(it) })
        }
    }.flowOn(Dispatchers.IO)

    override fun isFollowingCreator(userId: String): Flow<Boolean> = flow {
        val uid = currentUserId
        if (uid == null) {
            emit(_followingUserIds.value.contains(userId))
            return@flow
        }
        try {
            val count = postgrest.from("user_follows").select {
                filter {
                    eq("follower_id", uid)
                    eq("following_id", userId)
                }
            }.decodeList<UserFollowRelationship>().size
            emit(count > 0)
        } catch (e: Exception) {
            emit(_followingUserIds.value.contains(userId))
        }
    }.flowOn(Dispatchers.IO)

    override fun getFollowingList(): Flow<Set<String>> = _followingUserIds.asStateFlow()

    override suspend fun followCreator(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        try {
            val payload = buildJsonObject {
                put("follower_id", uid)
                put("following_id", userId)
            }
            postgrest.from("user_follows").insert(payload)
            _followingUserIds.value = _followingUserIds.value + userId
            Result.success(true)
        } catch (e: Exception) {
            _followingUserIds.value = _followingUserIds.value + userId
            Result.success(true)
        }
    }

    override suspend fun unfollowCreator(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        try {
            postgrest.from("user_follows").delete {
                filter {
                    eq("follower_id", uid)
                    eq("following_id", userId)
                }
            }
            _followingUserIds.value = _followingUserIds.value - userId
            Result.success(false)
        } catch (e: Exception) {
            _followingUserIds.value = _followingUserIds.value - userId
            Result.success(false)
        }
    }

    override suspend fun upsertCloudUser(user: CloudUser): Result<CloudUser> = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonObject {
                put("username", user.username)
                put("display_name", user.displayName)
                if (user.avatarUrl != null) put("avatar_url", user.avatarUrl)
                put("bio", user.bio)
                put("is_creator", user.isCreator)
            }
            postgrest.from("profiles").upsert(payload)
            Result.success(user)
        } catch (e: Exception) {
            Result.success(user)
        }
    }

    override fun search(query: String): Flow<SocialSearchResult> = flow {
        val clean = query.trim()
        if (clean.isBlank()) {
            emit(SocialSearchResult())
            return@flow
        }
        try {
            val blocked = _blockedUserIds.value
            val plansRaw = try {
                postgrest.from("public_plans").select(
                    Columns.raw("*, creator:profiles(*)")
                ) {
                    filter {
                        isIn("visibility", listOf("PUBLIC", "public"))
                        or {
                            ilike("title", "%$clean%")
                            ilike("description", "%$clean%")
                            ilike("category", "%$clean%")
                        }
                    }
                    limit(30)
                }.decodeList<PublicPlan>().filter { it.creatorId !in blocked }
            } catch (_: Exception) {
                try {
                    postgrest.from("public_plans").select {
                        filter {
                            isIn("visibility", listOf("PUBLIC", "public"))
                            or {
                                ilike("title", "%$clean%")
                                ilike("description", "%$clean%")
                                ilike("category", "%$clean%")
                            }
                        }
                        limit(30)
                    }.decodeList<PublicPlan>().filter { it.creatorId !in blocked }
                } catch (_: Exception) { emptyList() }
            }

            val creatorIds = plansRaw.filter { it.creator == null && it.creatorId.isNotBlank() }.map { it.creatorId }.distinct()
            val profilesMap: Map<String, UserProfile> = if (creatorIds.isNotEmpty()) {
                try {
                    postgrest.from("profiles").select {
                        filter { isIn("id", creatorIds) }
                    }.decodeList<UserProfile>().associateBy { it.id }
                } catch (_: Exception) { emptyMap() }
            } else emptyMap()

            val resolvedPlans = mutableListOf<PublicPlan>()
            for (p in plansRaw) {
                if (p.creator == null) {
                    val prof: UserProfile? = profilesMap[p.creatorId]
                    if (prof != null) {
                        resolvedPlans.add(p.copy(creator = prof))
                        continue
                    }
                }
                resolvedPlans.add(p)
            }
            val plans: List<PublicPlan> = resolvedPlans

            val people = try {
                postgrest.from("profiles").select {
                    filter {
                        or {
                            ilike("username", "%$clean%")
                            ilike("display_name", "%$clean%")
                        }
                    }
                    limit(20)
                }.decodeList<UserProfile>().filter { it.id !in blocked }
            } catch (_: Exception) { emptyList() }

            emit(SocialSearchResult(
                plans = plans.map { planToCommunityPost(it) },
                creators = people.map { profileToCloudUser(it) }
            ))
        } catch (e: Exception) {
            val blocked = _blockedUserIds.value
            val matchedPlans = _cachedPublicPlans.value.filter {
                it.creatorId !in blocked && (
                    it.title.contains(clean, ignoreCase = true) || 
                    it.description.contains(clean, ignoreCase = true) ||
                    it.category.contains(clean, ignoreCase = true)
                )
            }
            emit(SocialSearchResult(plans = matchedPlans.map { planToCommunityPost(it) }))
        }
    }.flowOn(Dispatchers.IO)

    override fun searchCreators(query: String): Flow<List<CloudUser>> = flow<List<CloudUser>> {
        val clean = query.trim()
        if (clean.isBlank()) {
            emit(emptyList<CloudUser>())
            return@flow
        }
        try {
            val blocked = _blockedUserIds.value
            val people = postgrest.from("profiles").select {
                filter {
                    or {
                        ilike("username", "%$clean%")
                        ilike("display_name", "%$clean%")
                    }
                }
                limit(20)
            }.decodeList<UserProfile>().filter { it.id !in blocked }
            emit(people.map { profileToCloudUser(it) })
        } catch (e: Exception) {
            emit(emptyList<CloudUser>())
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun reportContent(
        targetId: String,
        targetType: String,
        reason: String,
        reporterUserId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: reporterUserId
        try {
            val payload = buildJsonObject {
                put("reporter_id", uid)
                put("target_id", targetId)
                put("target_type", targetType)
                put("reason", reason)
            }
            postgrest.from("content_reports").insert(payload)
            Result.success(true)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    override suspend fun blockUser(targetUserId: String, currentUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId.ifBlank { this@SupabaseSocialRepository.currentUserId ?: "" }
        try {
            val payload = buildJsonObject {
                put("blocker_id", uid)
                put("blocked_id", targetUserId)
            }
            postgrest.from("user_blocks").insert(payload)
            _blockedUserIds.value = _blockedUserIds.value + targetUserId
            unfollowCreator(targetUserId)
            Result.success(true)
        } catch (e: Exception) {
            _blockedUserIds.value = _blockedUserIds.value + targetUserId
            Result.success(true)
        }
    }

    override suspend fun unblockUser(targetUserId: String, currentUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId.ifBlank { this@SupabaseSocialRepository.currentUserId ?: "" }
        try {
            postgrest.from("user_blocks").delete {
                filter {
                    eq("blocker_id", uid)
                    eq("blocked_id", targetUserId)
                }
            }
            _blockedUserIds.value = _blockedUserIds.value - targetUserId
            Result.success(true)
        } catch (e: Exception) {
            _blockedUserIds.value = _blockedUserIds.value - targetUserId
            Result.success(true)
        }
    }

    override fun getBlockedUsers(currentUserId: String): Flow<Set<String>> = _blockedUserIds.asStateFlow()

    override suspend fun usePlan(planId: String, userId: String): Result<Int> {
        return recordPlanUsageDirect(planId)
    }

    override suspend fun toggleFollowPlan(planId: String, userId: String): Result<Boolean> {
        return toggleFollowPlanDirect(planId)
    }

    override fun isFollowingPlan(planId: String, userId: String): Flow<Boolean> = flow {
        val isFollowed = _followedPlanIds.value.contains(planId)
        emit(isFollowed)
    }.flowOn(Dispatchers.IO)

    override fun checkForPlanUpdate(sourcePlanId: String, currentLocalVersion: String): Flow<PlanVersionUpdate?> = flow {
        if (!SupabaseConfig.isConfigured || sourcePlanId.isBlank()) {
            emit(null)
            return@flow
        }
        try {
            val versions = postgrest.from("plan_versions").select {
                filter { eq("plan_id", sourcePlanId) }
                order("published_at", Order.DESCENDING)
            }.decodeList<PlanVersion>()

            if (versions.isEmpty()) {
                emit(null)
                return@flow
            }

            val latest = versions.first()
            if (latest.versionTag.trim() == currentLocalVersion.trim()) {
                emit(null)
                return@flow
            }

            val latestDto = try {
                json.decodeFromString<PlanTemplateDto>(latest.templateJson)
            } catch (e: Exception) {
                try {
                    com.google.gson.Gson().fromJson(latest.templateJson, PlanTemplateDto::class.java)
                } catch (e2: Exception) { null }
            }

            if (latestDto == null) {
                emit(null)
                return@flow
            }

            val currentVersionDto = versions.find { it.versionTag.trim() == currentLocalVersion.trim() }?.let { ver ->
                try {
                    json.decodeFromString<PlanTemplateDto>(ver.templateJson)
                } catch (e: Exception) {
                    try {
                        com.google.gson.Gson().fromJson(ver.templateJson, PlanTemplateDto::class.java)
                    } catch (e2: Exception) { null }
                }
            }

            val currentTaskDescriptions = currentVersionDto?.tasks?.map { it.taskDescription.trim().lowercase() }?.toSet() ?: emptySet()
            val newTasks = if (currentTaskDescriptions.isEmpty()) {
                latestDto.tasks
            } else {
                latestDto.tasks.filter { it.taskDescription.trim().lowercase() !in currentTaskDescriptions }
            }

            val update = PlanVersionUpdate(
                planId = sourcePlanId,
                latestVersionTag = latest.versionTag,
                currentLocalVersion = currentLocalVersion,
                changelog = latest.changelog,
                publishedAt = latest.publishedAt,
                newTasks = newTasks,
                totalTasksInLatest = latestDto.tasks.size
            )
            emit(update)
        } catch (e: Exception) {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    override fun getPrivacySettings(userId: String): Flow<PrivacySettings?> = flow {
        if (!SupabaseConfig.isConfigured || userId.isBlank()) {
            emit(PrivacySettings(userId = userId))
            return@flow
        }
        try {
            val settings = postgrest.from("privacy_settings").select {
                filter { eq("user_id", userId) }
                single()
            }.decodeSingleOrNull<PrivacySettings>()
            emit(settings ?: PrivacySettings(userId = userId))
        } catch (e: Exception) {
            emit(PrivacySettings(userId = userId))
        }
    }.flowOn(Dispatchers.IO)

    override fun canMessageUser(targetUserId: String): Flow<Boolean> = flow {
        val uid = currentUserId
        if (uid == null || uid == targetUserId) {
            emit(false)
            return@flow
        }
        if (_blockedUserIds.value.contains(targetUserId)) {
            emit(false)
            return@flow
        }
        if (!SupabaseConfig.isConfigured) {
            emit(true)
            return@flow
        }
        try {
            val blockedByTarget = postgrest.from("user_blocks").select {
                filter {
                    eq("blocker_id", targetUserId)
                    eq("blocked_id", uid)
                }
            }.decodeList<UserBlock>().isNotEmpty()
            if (blockedByTarget) {
                emit(false)
                return@flow
            }

            val settings = postgrest.from("privacy_settings").select {
                filter { eq("user_id", targetUserId) }
                single()
            }.decodeSingleOrNull<PrivacySettings>() ?: PrivacySettings(userId = targetUserId)

            when (settings.allowMessagesFrom.uppercase()) {
                "NONE" -> emit(false)
                "FOLLOWED", "FOLLOWED_ONLY" -> {
                    val targetFollowsViewer = postgrest.from("user_follows").select {
                        filter {
                            eq("follower_id", targetUserId)
                            eq("following_id", uid)
                        }
                    }.decodeList<UserFollowRelationship>().isNotEmpty()
                    emit(targetFollowsViewer)
                }
                else -> emit(true)
            }
        } catch (e: Exception) {
            emit(true)
        }
    }.flowOn(Dispatchers.IO)

    override fun getViewerRelationship(targetUserId: String): Flow<RelationshipStatus> = flow {
        val uid = currentUserId
        if (uid == null) {
            emit(RelationshipStatus.STRANGER)
            return@flow
        }
        if (uid == targetUserId) {
            emit(RelationshipStatus.OWNER)
            return@flow
        }
        if (_blockedUserIds.value.contains(targetUserId)) {
            emit(RelationshipStatus.BLOCKED)
            return@flow
        }
        if (!SupabaseConfig.isConfigured) {
            val iFollow = _followingUserIds.value.contains(targetUserId)
            emit(if (iFollow) RelationshipStatus.FOLLOWING else RelationshipStatus.STRANGER)
            return@flow
        }
        try {
            val isBlocked = postgrest.from("user_blocks").select {
                filter {
                    or {
                        and {
                            eq("blocker_id", uid)
                            eq("blocked_id", targetUserId)
                        }
                        and {
                            eq("blocker_id", targetUserId)
                            eq("blocked_id", uid)
                        }
                    }
                }
            }.decodeList<UserBlock>().isNotEmpty()
            if (isBlocked) {
                emit(RelationshipStatus.BLOCKED)
                return@flow
            }

            val iFollow = postgrest.from("user_follows").select {
                filter {
                    eq("follower_id", uid)
                    eq("following_id", targetUserId)
                }
            }.decodeList<UserFollowRelationship>().isNotEmpty()

            val theyFollow = postgrest.from("user_follows").select {
                filter {
                    eq("follower_id", targetUserId)
                    eq("following_id", uid)
                }
            }.decodeList<UserFollowRelationship>().isNotEmpty()

            when {
                iFollow && theyFollow -> emit(RelationshipStatus.MUTUAL)
                iFollow -> emit(RelationshipStatus.FOLLOWING)
                else -> emit(RelationshipStatus.STRANGER)
            }
        } catch (e: Exception) {
            val iFollow = _followingUserIds.value.contains(targetUserId)
            emit(if (iFollow) RelationshipStatus.FOLLOWING else RelationshipStatus.STRANGER)
        }
    }.flowOn(Dispatchers.IO)

    // Direct Messaging
    override fun getConversations(userId: String): Flow<List<Conversation>> = flow {
        val uid = currentUserId ?: userId
        if (!SupabaseConfig.isConfigured || uid.isBlank()) {
            emit(_cachedConversations.value)
            return@flow
        }
        try {
            val memberships = postgrest.from("conversation_members").select {
                filter { eq("user_id", uid) }
            }.decodeList<ConversationMember>()

            val convIds = memberships.map { it.conversationId }
            if (convIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            val otherMembers = postgrest.from("conversation_members").select(
                Columns.raw("*, profile:profiles(*)")
            ) {
                filter {
                    isIn("conversation_id", convIds)
                    neq("user_id", uid)
                }
            }.decodeList<JsonObject>()

            val conversations = convIds.map { cId ->
                val otherMem = otherMembers.find { it["conversation_id"]?.toString()?.trim('"') == cId }
                val profileObj = otherMem?.get("profile")?.toString()
                val profile = profileObj?.let {
                    try { json.decodeFromString<UserProfile>(it) } catch (e: Exception) { null }
                }
                Conversation(
                    id = cId,
                    participant = profile,
                    unreadCount = 0
                )
            }
            _cachedConversations.value = conversations
            emit(conversations)
        } catch (e: Exception) {
            emit(_cachedConversations.value)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getOrCreateConversation(currentUserId: String, otherUserId: String): Result<Conversation> = withContext(Dispatchers.IO) {
        val uid = this@SupabaseSocialRepository.currentUserId ?: currentUserId
        try {
            if (!SupabaseConfig.isConfigured) {
                val existing = _cachedConversations.value.find { it.participant?.id == otherUserId }
                if (existing != null) return@withContext Result.success(existing)
                val newC = Conversation(id = "conv_${UUID.randomUUID().toString().take(8)}", participant = UserProfile(id = otherUserId, username = "user", displayName = "User"))
                _cachedConversations.value = listOf(newC) + _cachedConversations.value
                return@withContext Result.success(newC)
            }

            // BUG-07: Check for an existing conversation shared between both users before creating a new one
            val myMemberships = postgrest.from("conversation_members").select {
                filter { eq("user_id", uid) }
            }.decodeList<ConversationMember>().map { it.conversationId }

            if (myMemberships.isNotEmpty()) {
                val sharedConvId = try {
                    postgrest.from("conversation_members").select {
                        filter {
                            isIn("conversation_id", myMemberships)
                            eq("user_id", otherUserId)
                        }
                    }.decodeList<ConversationMember>().firstOrNull()?.conversationId
                } catch (_: Exception) { null }

                if (sharedConvId != null) {
                    val cachedMatch = _cachedConversations.value.find { it.id == sharedConvId }
                    if (cachedMatch != null) return@withContext Result.success(cachedMatch)

                    val otherProfile = try {
                        postgrest.from("profiles").select {
                            filter { eq("id", otherUserId) }
                            single()
                        }.decodeSingleOrNull<UserProfile>()
                    } catch (_: Exception) { null }

                    val existing = Conversation(id = sharedConvId, participant = otherProfile)
                    _cachedConversations.value = listOf(existing) + _cachedConversations.value.filter { it.id != sharedConvId }
                    return@withContext Result.success(existing)
                }
            }

            // No existing conversation found — create a new one
            val conv = postgrest.from("conversations").insert(buildJsonObject {}) {
                select()
                single()
            }.decodeSingle<JsonObject>()
            val convId = conv["id"]?.toString()?.trim('"') ?: UUID.randomUUID().toString()

            // Insert current user first (passes auth.uid() = user_id), then insert other user
            postgrest.from("conversation_members").insert(
                buildJsonObject { put("conversation_id", convId); put("user_id", uid) }
            )
            try {
                postgrest.from("conversation_members").insert(
                    buildJsonObject { put("conversation_id", convId); put("user_id", otherUserId) }
                )
            } catch (_: Exception) {
                // If other user profile doesn't exist, ensure profile placeholder exists
                try {
                    postgrest.from("profiles").upsert(buildJsonObject {
                        put("id", otherUserId)
                        put("username", "user_${otherUserId.take(8)}")
                        put("display_name", "Planner User")
                    })
                    postgrest.from("conversation_members").insert(
                        buildJsonObject { put("conversation_id", convId); put("user_id", otherUserId) }
                    )
                } catch (_: Exception) {}
            }

            val otherProfile = postgrest.from("profiles").select {
                filter { eq("id", otherUserId) }
                single()
            }.decodeSingleOrNull<UserProfile>()

            val resultConv = Conversation(
                id = convId,
                participant = otherProfile
            )
            _cachedConversations.value = listOf(resultConv) + _cachedConversations.value
            Result.success(resultConv)
        } catch (e: Exception) {
            val fallback = Conversation(
                id = "conv_${UUID.randomUUID().toString().take(8)}",
                participant = UserProfile(id = otherUserId, username = "user", displayName = "User")
            )
            _cachedConversations.value = listOf(fallback) + _cachedConversations.value
            Result.success(fallback)
        }
    }

    private fun subscribeToMessagesRealtime(conversationId: String) {
        val channelName = "messages_$conversationId"
        if (!SupabaseConfig.isConfigured || _activeRealtimeChannels.contains(channelName)) return
        _activeRealtimeChannels.add(channelName)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channel = client.channel(channelName)
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                    filter("conversation_id", FilterOperator.EQ, conversationId)
                }
                channel.subscribe()
                changeFlow.collect { action ->
                    if (action is PostgresAction.Insert) {
                        try {
                            val newMsg = json.decodeFromJsonElement<DirectMessage>(action.record)
                            val current = _cachedMessages.value[conversationId] ?: emptyList()
                            if (current.none { it.id == newMsg.id }) {
                                _cachedMessages.value = _cachedMessages.value + (conversationId to (current + newMsg))
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {
                _activeRealtimeChannels.remove(channelName)
            }
        }
    }

    private fun subscribeToNotificationsRealtime(userId: String) {
        val channelName = "notifications_$userId"
        if (!SupabaseConfig.isConfigured || _activeRealtimeChannels.contains(channelName)) return
        _activeRealtimeChannels.add(channelName)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channel = client.channel(channelName)
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notifications"
                    filter("recipient_id", FilterOperator.EQ, userId)
                }
                channel.subscribe()
                changeFlow.collect { action ->
                    if (action is PostgresAction.Insert) {
                        try {
                            val newNotif = json.decodeFromJsonElement<SocialNotification>(action.record)
                            val current = _cachedNotifications.value
                            if (current.none { it.id == newNotif.id }) {
                                _cachedNotifications.value = listOf(newNotif) + current
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {
                _activeRealtimeChannels.remove(channelName)
            }
        }
    }

    override fun getMessages(conversationId: String): Flow<List<DirectMessage>> = flow {
        emit(_cachedMessages.value[conversationId] ?: emptyList())
        if (SupabaseConfig.isConfigured) {
            try {
                val msgs = postgrest.from("messages").select {
                    filter { eq("conversation_id", conversationId) }
                    order("created_at", Order.ASCENDING)
                }.decodeList<DirectMessage>()
                _cachedMessages.value = _cachedMessages.value + (conversationId to msgs)
                subscribeToMessagesRealtime(conversationId)
            } catch (_: Exception) {}
        }
        emitAll(_cachedMessages.map { it[conversationId] ?: emptyList() })
    }.flowOn(Dispatchers.IO)

    override suspend fun sendMessage(conversationId: String, senderId: String, content: String): Result<DirectMessage> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: senderId
        try {
            val payload = buildJsonObject {
                put("conversation_id", conversationId)
                put("sender_id", uid)
                put("content", content)
            }
            val inserted = postgrest.from("messages").insert(payload) {
                select()
                single()
            }.decodeSingle<DirectMessage>()

            val current = _cachedMessages.value[conversationId] ?: emptyList()
            _cachedMessages.value = _cachedMessages.value + (conversationId to (current + inserted))
            Result.success(inserted)
        } catch (e: Exception) {
            val fallbackMsg = DirectMessage(
                id = "msg_${UUID.randomUUID().toString().take(8)}",
                conversationId = conversationId,
                senderId = uid,
                content = content,
                createdAt = java.time.Instant.now().toString()
            )
            val current = _cachedMessages.value[conversationId] ?: emptyList()
            _cachedMessages.value = _cachedMessages.value + (conversationId to (current + fallbackMsg))
            Result.success(fallbackMsg)
        }
    }

    // Notifications
    override fun getNotifications(userId: String): Flow<List<SocialNotification>> = flow {
        val uid = currentUserId ?: userId
        emit(_cachedNotifications.value)
        if (SupabaseConfig.isConfigured && uid.isNotBlank()) {
            try {
                val notifs = postgrest.from("notifications").select(
                    Columns.raw("*, actor:profiles(*)")
                ) {
                    filter { eq("recipient_id", uid) }
                    order("created_at", Order.DESCENDING)
                    limit(50)
                }.decodeList<SocialNotification>()

                _cachedNotifications.value = notifs
                subscribeToNotificationsRealtime(uid)
            } catch (_: Exception) {}
        }
        emitAll(_cachedNotifications.asStateFlow())
    }.flowOn(Dispatchers.IO)

    override suspend fun markNotificationAsRead(notificationId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonObject { put("is_read", true) }
            postgrest.from("notifications").update(payload) {
                filter { eq("id", notificationId) }
            }
            _cachedNotifications.value = _cachedNotifications.value.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            Result.success(true)
        } catch (e: Exception) {
            _cachedNotifications.value = _cachedNotifications.value.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            Result.success(true)
        }
    }

    override suspend fun markAllNotificationsAsRead(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: userId
        try {
            val payload = buildJsonObject { put("is_read", true) }
            postgrest.from("notifications").update(payload) {
                filter { eq("recipient_id", uid) }
            }
            _cachedNotifications.value = _cachedNotifications.value.map { it.copy(isRead = true) }
            Result.success(true)
        } catch (e: Exception) {
            _cachedNotifications.value = _cachedNotifications.value.map { it.copy(isRead = true) }
            Result.success(true)
        }
    }

    // ── HELPERS & CONVERTERS ────────────────────────────────────────────────

    private suspend fun enrichPlansWithUserState(plans: List<PublicPlan>): List<PublicPlan> {
        val uid = currentUserId ?: return plans
        return plans.map { plan ->
            val isSaved = _savedPlanIds.value.contains(plan.id) || (try { isPlanSavedDirect(plan.id) } catch (e: Exception) { false })
            val isLiked = _likedPlanIds.value.contains(plan.id)
            val isFollowing = _followedPlanIds.value.contains(plan.id)
            plan.copy(isSaved = isSaved, isLiked = isLiked, isFollowing = isFollowing)
        }
    }

    private fun updatePlanInCache(planId: String, transform: (PublicPlan) -> PublicPlan) {
        val list = _cachedPublicPlans.value.toMutableList()
        val index = list.indexOfFirst { it.id == planId }
        if (index != -1) {
            list[index] = transform(list[index])
            _cachedPublicPlans.value = list
        }
    }

    private fun planToCommunityPost(plan: PublicPlan, templateJson: String? = null): CommunityPost {
        val authorUser = plan.creator?.let { profileToCloudUser(it) } ?: CloudUser(
            userId = plan.creatorId,
            username = "creator",
            displayName = "Creator",
            isCreator = true
        )
        val serverTime = plan.createdAt?.let { dateStr ->
            try {
                java.time.Instant.parse(dateStr).toEpochMilli()
            } catch (_: Exception) {
                try {
                    java.time.OffsetDateTime.parse(dateStr).toInstant().toEpochMilli()
                } catch (_: Exception) {
                    null
                }
            }
        } ?: System.currentTimeMillis()

        return CommunityPost(
            postId = plan.id,
            author = authorUser,
            title = plan.title,
            description = plan.description,
            planTemplateJson = templateJson ?: "{}",
            durationDays = plan.durationDays,
            tags = plan.tags,
            category = plan.category,
            upvoteCount = plan.likesCount,
            joinCount = plan.usesCount,
            commentCount = plan.commentsCount,
            userVote = if (plan.isLiked) VoteType.UP else null,
            isSaved = plan.isSaved,
            createdAt = serverTime,
            visibility = plan.visibility
        )
    }

    private fun profileToCloudUser(profile: UserProfile): CloudUser {
        return CloudUser(
            userId = profile.id,
            username = profile.username,
            displayName = profile.displayName,
            avatarUrl = profile.avatarUrl,
            isCreator = profile.isCreator,
            bio = profile.bio,
            followerCount = profile.followersCount,
            followingCount = profile.followingCount,
            publicPlansCount = profile.publicPlansCount
        )
    }

    private fun socialCommentToPostComment(c: SocialComment): PostComment {
        val author = c.author?.let { profileToCloudUser(it) } ?: CloudUser(
            userId = c.userId,
            username = "user",
            displayName = "User"
        )
        return PostComment(
            commentId = c.id,
            postId = c.planId ?: c.postId ?: "",
            author = author,
            content = c.content,
            parentCommentId = c.parentCommentId,
            upvoteCount = c.likesCount,
            replies = c.replies.map { socialCommentToPostComment(it) },
            isLiked = c.isLiked
        )
    }

    override fun getFollowersUsers(userId: String): Flow<List<CloudUser>> = flow<List<CloudUser>> {
        if (!SupabaseConfig.isConfigured) {
            emit(emptyList<CloudUser>())
            return@flow
        }
        try {
            val follows = postgrest.from("user_follows").select {
                filter {
                    eq("following_id", userId)
                }
            }.decodeList<UserFollowRelationship>()
            val followerIds = follows.map { it.followerId }
            if (followerIds.isEmpty()) {
                emit(emptyList<CloudUser>())
                return@flow
            }
            val blocked = _blockedUserIds.value
            val profiles = postgrest.from("profiles").select {
                filter {
                    isIn("id", followerIds)
                }
            }.decodeList<UserProfile>().filter { it.id !in blocked }
            emit(profiles.map { profileToCloudUser(it) })
        } catch (e: Exception) {
            emit(emptyList<CloudUser>())
        }
    }.flowOn(Dispatchers.IO)

    override fun getFollowingUsers(userId: String): Flow<List<CloudUser>> = flow<List<CloudUser>> {
        if (!SupabaseConfig.isConfigured) {
            emit(emptyList<CloudUser>())
            return@flow
        }
        try {
            val follows = postgrest.from("user_follows").select {
                filter {
                    eq("follower_id", userId)
                }
            }.decodeList<UserFollowRelationship>()
            val followingIds = follows.map { it.followingId }
            if (followingIds.isEmpty()) {
                emit(emptyList<CloudUser>())
                return@flow
            }
            val blocked = _blockedUserIds.value
            val profiles = postgrest.from("profiles").select {
                filter {
                    isIn("id", followingIds)
                }
            }.decodeList<UserProfile>().filter { it.id !in blocked }
            emit(profiles.map { profileToCloudUser(it) })
        } catch (e: Exception) {
            emit(emptyList<CloudUser>())
        }
    }.flowOn(Dispatchers.IO)
}
