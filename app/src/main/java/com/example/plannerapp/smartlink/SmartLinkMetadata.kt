package com.example.plannerapp.smartlink

/**
 * Structured model representing metadata for a smart link / embedded resource.
 */
data class SmartLinkMetadata(
    val url: String,
    val provider: SmartLinkProvider,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val domain: String,
    val type: SmartLinkType = SmartLinkType.LINK,
    val mediaDuration: String? = null,
    val author: String? = null,
    val internalTargetId: String? = null
)

enum class SmartLinkProvider {
    YOUTUBE,
    IMAGE,
    PDF,
    WEBPAGE,
    INTERNAL,
    UNKNOWN
}

enum class SmartLinkType {
    VIDEO,
    IMAGE,
    DOCUMENT,
    ARTICLE,
    PLAN,
    LINK
}
