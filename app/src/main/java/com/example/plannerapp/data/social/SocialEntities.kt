package com.example.plannerapp.data.social

import kotlinx.serialization.Serializable

@Serializable
enum class VoteType {
    UP,
    DOWN
}

@Serializable
data class CloudUser(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isCreator: Boolean = false,
    val bio: String = "",
    val followerCount: Int = 0,
    val totalMembersJoined: Int = 0
)

@Serializable
data class CommunityPost(
    val postId: String,
    val author: CloudUser,
    val title: String,
    val description: String,
    val planTemplateJson: String,
    val durationDays: Int = 7,
    val tags: List<String> = emptyList(),
    val category: String = "General",
    val upvoteCount: Int = 0,
    val joinCount: Int = 0,
    val commentCount: Int = 0,
    val userVote: VoteType? = null,
    val isSaved: Boolean = false,
    val isPaid: Boolean = false,
    val skuId: String? = null,
    val creditCost: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val score: Int
        get() = upvoteCount
}

@Serializable
data class PostComment(
    val commentId: String,
    val postId: String,
    val author: CloudUser,
    val content: String,
    val parentCommentId: String? = null,
    val upvoteCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val replies: List<PostComment> = emptyList(),
    val isLiked: Boolean = false
)

@Serializable
data class PostVote(
    val userId: String,
    val postId: String,
    val voteType: VoteType
)
