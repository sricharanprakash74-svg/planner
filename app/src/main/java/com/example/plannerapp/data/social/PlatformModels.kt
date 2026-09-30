package com.example.plannerapp.data.social

import com.example.plannerapp.data.template.TaskTemplateDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive

// ==============================================================================
// 0. RELATIONSHIP STATUS MATRIX
// ==============================================================================

enum class RelationshipStatus {
    OWNER,
    STRANGER,
    FOLLOWING,
    MUTUAL,
    BLOCKED,
    PENDING
}

// ==============================================================================
// 1. IDENTITY & PRIVACY MODELS
// ==============================================================================

@Serializable
data class UserProfile(
    val id: String = "",
    val username: String = "user",
    @SerialName("display_name") val displayName: String = "Planner User",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String = "",
    @SerialName("is_creator") val isCreator: Boolean = false,
    @SerialName("followers_count") val followersCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("public_plans_count") val publicPlansCount: Int = 0,
    @SerialName("onboarding_completed") val onboardingCompleted: Boolean = false,
    val categories: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    @SerialName("experience_level") val experienceLevel: String = "BEGINNER",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PrivacySettings(
    @SerialName("user_id") val userId: String,
    @SerialName("profile_visibility") val profileVisibility: String = "PUBLIC", // PUBLIC, FOLLOWERS_ONLY, PRIVATE
    @SerialName("allow_messages_from") val allowMessagesFrom: String = "EVERYONE", // EVERYONE, FOLLOWED, NONE
    @SerialName("allow_comments_from") val allowCommentsFrom: String = "EVERYONE", // EVERYONE, FOLLOWED, NONE
    @SerialName("show_activity_status") val showActivityStatus: Boolean = true,
    @SerialName("default_plan_visibility") val defaultPlanVisibility: String = "PUBLIC" // PUBLIC, UNLISTED, PRIVATE
)

// ==============================================================================
// 2. PUBLIC PLAN & VERSION MODELS (The Central Social Object)
// ==============================================================================

@Serializable
data class PublicPlan(
    val id: String,
    @SerialName("creator_id") val creatorId: String,
    val title: String,
    val description: String = "",
    val category: String = "General",
    val tags: List<String> = emptyList(),
    @SerialName("duration_days") val durationDays: Int = 7,
    @SerialName("current_version") val currentVersion: String = "1.0.0",
    @SerialName("saves_count") val savesCount: Int = 0,
    @SerialName("uses_count") val usesCount: Int = 0,
    @SerialName("follows_count") val followsCount: Int = 0,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    val visibility: String = "PUBLIC", // PUBLIC, UNLISTED, ARCHIVED
    @SerialName("created_at") val createdAt: String? = null,
    // Client-side enriched state
    val creator: UserProfile? = null,
    val isSaved: Boolean = false,
    val isLiked: Boolean = false,
    val isFollowing: Boolean = false,
    val isUsed: Boolean = false
)

object JsonElementAsStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("JsonElementAsString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeString()
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> element.content
            else -> element.toString()
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            val element = try {
                Json.parseToJsonElement(value)
            } catch (_: Exception) {
                JsonPrimitive(value)
            }
            jsonEncoder.encodeJsonElement(element)
        } else {
            encoder.encodeString(value)
        }
    }
}

@Serializable
data class PlanVersion(
    val id: String? = "",
    @SerialName("plan_id") val planId: String = "",
    @SerialName("version_tag") val versionTag: String? = "1.0.0",
    val changelog: String? = "Initial publication",
    @Serializable(with = JsonElementAsStringSerializer::class)
    @SerialName("template_json") val templateJson: String = "{}",
    @SerialName("published_at") val publishedAt: String? = null
)

@Serializable
data class PlanVersionUpdate(
    val planId: String,
    val latestVersionTag: String,
    val currentLocalVersion: String = "1.0.0",
    val changelog: String = "",
    val publishedAt: String? = null,
    val newTasks: List<TaskTemplateDto> = emptyList(),
    val totalTasksInLatest: Int = 0
)

// ==============================================================================
// 3. DISTINCT SOCIAL RELATIONSHIPS
// ==============================================================================

@Serializable
data class PlanSave(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("plan_id") val planId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PlanUse(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("plan_id") val planId: String,
    @SerialName("version_id") val versionId: String? = null,
    @SerialName("used_at") val usedAt: String? = null
)

@Serializable
data class PlanFollow(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("plan_id") val planId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PlanLike(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("plan_id") val planId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class UserFollowRelationship(
    val id: String = "",
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String,
    @SerialName("created_at") val createdAt: String? = null
)

// ==============================================================================
// 4. POSTS & COMMENTS (Secondary Content referencing plans)
// ==============================================================================

@Serializable
data class SocialPost(
    val id: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("referenced_plan_id") val referencedPlanId: String? = null,
    val title: String,
    val content: String,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    // Client-side enriched state
    val author: UserProfile? = null,
    val referencedPlan: PublicPlan? = null,
    val isLiked: Boolean = false
)

@Serializable
data class SocialComment(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("plan_id") val planId: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("parent_comment_id") val parentCommentId: String? = null,
    val content: String,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    // Client-side enriched state
    val author: UserProfile? = null,
    val replies: List<SocialComment> = emptyList(),
    val isLiked: Boolean = false
)

@Serializable
data class CommentLike(
    val id: String = "",
    @SerialName("comment_id") val commentId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String? = null
)

// ==============================================================================
// 5. MESSAGING & CONVERSATIONS
// ==============================================================================

@Serializable
data class Conversation(
    val id: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    // Client-side enriched state
    val participant: UserProfile? = null,
    val lastMessage: DirectMessage? = null,
    val unreadCount: Int = 0
)

@Serializable
data class ConversationMember(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("joined_at") val joinedAt: String? = null
)

@Serializable
data class DirectMessage(
    val id: String = "",
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// ==============================================================================
// 6. NOTIFICATIONS
// ==============================================================================

@Serializable
data class SocialNotification(
    val id: String,
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("actor_id") val actorId: String,
    @SerialName("event_type") val eventType: String, // USER_FOLLOW, PLAN_FOLLOW, PLAN_SAVE, PLAN_USE, PLAN_LIKE, PLAN_COMMENT, POST_LIKE, POST_COMMENT, MESSAGE
    @SerialName("entity_id") val entityId: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    // Client-side enriched state
    val actor: UserProfile? = null
)

// ==============================================================================
// 7. MODERATION & SAFETY
// ==============================================================================

@Serializable
data class UserBlock(
    val id: String = "",
    @SerialName("blocker_id") val blockerId: String,
    @SerialName("blocked_id") val blockedId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ContentReport(
    val id: String = "",
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("target_id") val targetId: String,
    @SerialName("target_type") val targetType: String, // PLAN, POST, COMMENT, USER, MESSAGE
    val reason: String,
    val status: String = "PENDING"
)

// ==============================================================================
// 8. UNIFIED SEARCH RESULT
// ==============================================================================

@Serializable
data class UnifiedSearchResult(
    val plans: List<PublicPlan> = emptyList(),
    val people: List<UserProfile> = emptyList()
)
