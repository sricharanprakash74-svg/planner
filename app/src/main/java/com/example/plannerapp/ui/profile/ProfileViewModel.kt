package com.example.plannerapp.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.DailyCheckinEntity
import com.example.plannerapp.data.JoinedCommunityEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.UserEntity
import com.example.plannerapp.data.social.CloudUser
import com.example.plannerapp.data.social.CommunityPost
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.social.VoteType
import com.example.plannerapp.ui.state.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.time.LocalDate

data class ProfileUiState(
    val user: UserEntity? = null,
    val userPlans: List<PlanEntity> = emptyList(),
    val weeklyCheckins: List<DailyCheckinEntity> = emptyList(),
    val userPosts: List<CommunityPost> = emptyList(),
    val streak: Int = 0,
    val consistencyPercentage: Int = 0,
    // planId → completion fraction [0.0, 1.0].
    // Sourced exclusively from Room (daily_checkins JOIN task_templates).
    // Because this uses a Room Flow the offline and online paths are identical:
    //  - Offline: local task completions write to Room → Flow emits → fluid rises.
    //  - Online: sync writes to the same Room tables → Flow emits → fluid rises.
    // The UI never needs to distinguish between the two modes.
    val planCompletionMap: Map<Long, Float> = emptyMap(),
    // Set of localPlanIds that have a joined_communities record.
    // These are the "Following Plans" the user has joined from the community.
    // Populated from the joined_communities Room table — also offline-safe.
    val joinedCommunityPlanIds: Set<Long> = emptySet(),
    val followingUsers: List<CloudUser> = emptyList(),
    val followersUsers: List<CloudUser> = emptyList()
)

class ProfileViewModel(
    private val repository: PlannerRepository,
    private val userDao: UserDao,
    private val socialRepository: SocialRepository? = null,
    private val creditRepository: com.example.plannerapp.credits.CreditRepository? = null
) : ViewModel() {

    private val _streak = MutableStateFlow(0)

    val currentUser: StateFlow<UserEntity?> = userDao.getActiveUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Joined communities — Room Flow, never requires network. Works offline first.
    val joinedCommunitiesFlow: StateFlow<List<JoinedCommunityEntity>> =
        repository.getAllJoinedCommunities()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Helper flows extracted so nested combines stay readable.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val plansFlow: Flow<List<PlanEntity>> =
        currentUser.flatMapLatest { user ->
            if (user != null) repository.getPlansForUser(user.userId)
            else MutableStateFlow(emptyList())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val checkinsFlow: Flow<List<DailyCheckinEntity>> =
        currentUser.flatMapLatest { user ->
            if (user != null) repository.getRecentCheckins(user.userId, 30)
            else MutableStateFlow(emptyList())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val postsFlow: Flow<List<CommunityPost>> =
        currentUser.flatMapLatest { user ->
            if (user != null && socialRepository != null) {
                val authorId = user.cloudUserId ?: user.userId.toString()
                socialRepository.getPostsByCreator(authorId)
            } else MutableStateFlow(emptyList())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val followingUsersFlow: Flow<List<CloudUser>> =
        currentUser.flatMapLatest { user ->
            if (user != null && socialRepository != null) {
                val authorId = user.cloudUserId ?: user.userId.toString()
                socialRepository.getFollowingUsers(authorId)
            } else MutableStateFlow(emptyList())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val followersUsersFlow: Flow<List<CloudUser>> =
        currentUser.flatMapLatest { user ->
            if (user != null && socialRepository != null) {
                val authorId = user.cloudUserId ?: user.userId.toString()
                socialRepository.getFollowersUsers(authorId)
            } else MutableStateFlow(emptyList())
        }

    // Combine 8 flows safely by nesting three branches.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<Resource<ProfileUiState>> = combine(
        // Left branch: user identity + her plans + her check-ins
        combine(currentUser, plansFlow, checkinsFlow) { user, plans, checkins ->
            Triple(user, plans, checkins)
        },
        // Middle branch: social posts + joined communities (Room-backed, offline-safe) + streak
        combine(postsFlow, joinedCommunitiesFlow, _streak) { posts, joined, streak ->
            Triple(posts, joined, streak)
        },
        // Right branch: connects (following & followers)
        combine(followingUsersFlow, followersUsersFlow) { following, followers ->
            Pair(following, followers)
        }
    ) { (user, plans, checkins), (posts, joined, streak), (following, followers) ->

        val weeklyCheckins: List<DailyCheckinEntity> = checkins

        // Overall consistency from recent check-ins (offline and online identical)
        val total     = weeklyCheckins.size
        val completed = weeklyCheckins.count { it.isCompleted }
        val percentage = if (total > 0) ((completed.toFloat() / total) * 100).toInt() else 0

        // Joined plan IDs — derived from joined_communities Room table.
        // Offline: local records written by joinCommunityPlan() populate this immediately.
        // Online/synced: sync worker writes to same table, same Flow emits, same result.
        val joinedPlanIds: Set<Long> = joined.map { it.localPlanId }.toSet()

        Resource.Success(
            ProfileUiState(
                user = user,
                userPlans = plans,
                weeklyCheckins = weeklyCheckins,
                userPosts = posts,
                streak = streak,
                consistencyPercentage = percentage,
                // planCompletionMap is populated per-plan in ProfileScreenWrapper
                // via individual Room Flows — one collectAsState per card.
                planCompletionMap = emptyMap(),
                joinedCommunityPlanIds = joinedPlanIds,
                followingUsers = following,
                followersUsers = followers
            )
        ) as Resource<ProfileUiState>
    }
    .catch { e -> emit(Resource.Error(e.message ?: "Failed to load profile data")) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Resource.Loading
    )

    fun toggleFollowUser(targetUserId: String, isCurrentlyFollowing: Boolean) {
        viewModelScope.launch {
            try {
                if (isCurrentlyFollowing) {
                    socialRepository?.unfollowCreator(targetUserId)
                } else {
                    socialRepository?.followCreator(targetUserId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val avatarVersion = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            try {
                val user = userDao.getActiveUserOnce()
                if (user != null) {
                    _streak.value = repository.getStreak(user.userId, LocalDate.now())
                }
            } catch (e: Exception) {
                // Ignore initial streak load error
            }
        }
    }

    fun updateProfilePicture(context: Context, imageUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDao.getActiveUserOnce() ?: return@launch
                val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@launch
                val avatarFile = File(context.filesDir, "avatar_${user.userId}.jpg")
                avatarFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                userDao.updateProfile(user.userId, user.displayName, avatarFile.absolutePath)
                avatarVersion.value = System.currentTimeMillis()

                // Upload to Supabase Storage if authenticated and configured
                val cloudUid = user.cloudUserId
                if (com.example.plannerapp.auth.SupabaseConfig.isConfigured && !cloudUid.isNullOrBlank()) {
                    try {
                        val bytes = avatarFile.readBytes()
                        val path = "avatars/${cloudUid}.jpg"
                        com.example.plannerapp.auth.SupabaseConfig.storage.from("avatars").upload(
                            path = path,
                            data = bytes,
                            options = { upsert = true }
                        )
                        val publicUrl = com.example.plannerapp.auth.SupabaseConfig.storage.from("avatars").publicUrl(path)
                        com.example.plannerapp.auth.SupabaseConfig.postgrest.from("profiles").update(
                            kotlinx.serialization.json.buildJsonObject {
                                put("avatar_url", publicUrl)
                            }
                        ) {
                            filter { eq("id", cloudUid) }
                        }
                    } catch (_: Exception) {
                        // Non-fatal: local avatar remains active
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDao.getActiveUserOnce() ?: return@launch
                userDao.updateProfile(user.userId, name, user.avatarUrl)
                val cloudUid = user.cloudUserId
                if (com.example.plannerapp.auth.SupabaseConfig.isConfigured && !cloudUid.isNullOrBlank()) {
                    try {
                        com.example.plannerapp.auth.SupabaseConfig.postgrest.from("profiles").update(
                            kotlinx.serialization.json.buildJsonObject {
                                put("display_name", name)
                            }
                        ) {
                            filter { eq("id", cloudUid) }
                        }
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createPost(
        title: String,
        content: String,
        visibility: String,
        category: String = "Habits",
        tags: List<String> = emptyList(),
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val user = userDao.getActiveUserOnce() ?: throw IllegalStateException("No active user")
                val author = CloudUser(
                    userId = user.cloudUserId ?: user.userId.toString(),
                    username = user.displayName.lowercase().replace(" ", "_"),
                    displayName = user.displayName,
                    avatarUrl = user.avatarUrl,
                    isCreator = user.isCreator,
                    bio = "",
                    followerCount = 0,
                    totalMembersJoined = 0
                )
                if (socialRepository != null) {
                    val template = com.example.plannerapp.data.template.PlanTemplateDto(
                        title = title,
                        description = content,
                        targetDurationDays = 7,
                        defaultTaskDurationDays = 1,
                        tags = tags.ifEmpty { listOf("routine", "insight") },
                        category = category,
                        author = com.example.plannerapp.data.template.AuthorDto(
                            userId = author.userId,
                            displayName = author.displayName,
                            avatarUrl = author.avatarUrl,
                            isCreator = author.isCreator
                        ),
                        tasks = listOf(
                            com.example.plannerapp.data.template.TaskTemplateDto(
                                taskDescription = title,
                                selectedDays = "1,2,3,4,5,6,7",
                                durationDays = 1,
                                subtasks = emptyList()
                            )
                        )
                    )
                    val templateJson = com.google.gson.Gson().toJson(template)

                    val result = socialRepository.createPost(
                        author = author,
                        title = title,
                        description = content,
                        planTemplateJson = templateJson,
                        durationDays = 7,
                        tags = tags.ifEmpty { listOf("routine", "insight") },
                        category = category,
                        isPaid = false,
                        creditCost = 0,
                        visibility = visibility
                    )
                    if (result.isSuccess) {
                        val createdPost = result.getOrNull()
                        if (createdPost != null) {
                            creditRepository?.awardPlanShare(user.userId, createdPost.postId)
                        }
                        onSuccess()
                    } else {
                        onError(result.exceptionOrNull()?.message ?: "Failed to publish post")
                    }
                } else {
                    onError("Social features not initialized")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to publish post")
            }
        }
    }

    fun onVote(postId: String, voteType: VoteType) {
        viewModelScope.launch {
            socialRepository?.votePost(postId, voteType)
        }
    }

    fun toggleSave(postId: String) {
        viewModelScope.launch {
            socialRepository?.toggleSavePost(postId)
        }
    }
}

class ProfileViewModelFactory(
    private val repository: PlannerRepository,
    private val userDao: UserDao,
    private val socialRepository: SocialRepository? = null,
    private val creditRepository: com.example.plannerapp.credits.CreditRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository, userDao, socialRepository, creditRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
