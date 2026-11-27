package com.libra.app.domain.model

data class AppNotification(
    val id: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorUsername: String = "",
    val actorPhotoUrl: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val referenceId: String = "",
    val category: String = "",
    val createdAt: Long = 0L,
    val read: Boolean = false
)

object LibraNotificationTypes {
    const val MESSAGE = "MESSAGE"
    const val GROUP_MESSAGE = "GROUP_MESSAGE"
    const val SERVER_MENTION = "SERVER_MENTION"
    const val LIKE = "LIKE"
    const val COMMENT = "COMMENT"
    const val COMMENT_REPLY = "COMMENT_REPLY"
    const val BOOK_LIKE = "BOOK_LIKE"
    const val BOOK_COMMENT = "BOOK_COMMENT"
    const val BOOK_COMMENT_REPLY = "BOOK_COMMENT_REPLY"
    const val STORY_LIKE = "STORY_LIKE"
    const val FOLLOW = "FOLLOW"
    const val ADMIN_BROADCAST = "ADMIN_BROADCAST"
    const val ADMIN_FEEDBACK = "ADMIN_FEEDBACK"
}
