package com.libra.app.domain.model

data class AdminPermissionSet(
    val manageMembers: Boolean = false,
    val manageBans: Boolean = false,
    val editProfiles: Boolean = false,
    val manageBooks: Boolean = false,
    val managePosts: Boolean = false,
    val manageStories: Boolean = false,
    val manageServers: Boolean = false,
    val manageGlobalChat: Boolean = false,
    val manageAdminRoles: Boolean = false,
    val manageCosmetics: Boolean = false,
    val sendFeedback: Boolean = false,
    val manageUpdates: Boolean = false
)

data class AdminRole(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val permissions: AdminPermissionSet = AdminPermissionSet(),
    val createdAt: Long = 0L
)

data class CosmeticRole(
    val id: String = "",
    val name: String = "",
    val icon: String = "✦",
    val imageUrl: String = "",
    val color: Long = 0xFF76AAFF,
    val description: String = "",
    val animationStyle: String = "LIGHTNING",
    val createdAt: Long = 0L
)

data class UserModeration(
    val banned: Boolean = false,
    val banUntil: Long = 0L,
    val banReason: String = "",
    val postingDisabled: Boolean = false,
    val postingDisabledUntil: Long = 0L,
    val messagingDisabled: Boolean = false,
    val messagingDisabledUntil: Long = 0L,
    val storyDisabled: Boolean = false,
    val storyDisabledUntil: Long = 0L,
    val commentingDisabled: Boolean = false,
    val commentingDisabledUntil: Long = 0L,
    val bookPublishingDisabled: Boolean = false,
    val bookPublishingDisabledUntil: Long = 0L,
    val serverCreationDisabled: Boolean = false,
    val serverCreationDisabledUntil: Long = 0L,
    val serverMessagingDisabled: Boolean = false,
    val serverMessagingDisabledUntil: Long = 0L
) {
    private fun active(enabled: Boolean, until: Long): Boolean =
        enabled && (until <= 0L || until > System.currentTimeMillis())

    val isBanned: Boolean get() = active(banned, banUntil)
    val canPost: Boolean get() = !active(postingDisabled, postingDisabledUntil)
    val canMessage: Boolean get() = !active(messagingDisabled, messagingDisabledUntil)
    val canStory: Boolean get() = !active(storyDisabled, storyDisabledUntil)
    val canComment: Boolean get() = !active(commentingDisabled, commentingDisabledUntil)
    val canPublishBook: Boolean get() = !active(bookPublishingDisabled, bookPublishingDisabledUntil)
    val canCreateServer: Boolean get() = !active(serverCreationDisabled, serverCreationDisabledUntil)
    val canMessageInServer: Boolean get() = !active(serverMessagingDisabled, serverMessagingDisabledUntil)
}