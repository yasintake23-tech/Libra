package com.libra.app.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val bio: String = "",
    val booksWrittenCount: Int = 0,
    val booksReadCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val profileCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    @ServerTimestamp
    val usernameLastChangedAt: Date? = null,
    @ServerTimestamp
    val displayNameChangeWindowStart: Date? = null,
    val displayNameChangesInWindow: Int = 0,
    val moderation: UserModeration = UserModeration(),
    val cosmeticRoleIds: List<String> = emptyList()
) {
    val handle: String
        get() = if (username.isNotBlank()) "@$username" else "@user_" + uid.take(6)

    val initials: String
        get() = displayName.trim()
            .split(Regex("\s+"))
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
            .ifEmpty { "U" }
}