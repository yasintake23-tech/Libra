package com.libra.app.domain.model

data class GroupPermissions(
    val onlyAdminsCanMessage: Boolean = false,
    val membersCanAddOthers: Boolean = true,
    val membersCanEditInfo: Boolean = false,
    val membersCanSendMedia: Boolean = true
)

data class DirectConversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserUsername: String = "",
    val otherUserPhotoUrl: String = "",
    val lastMessage: String = "",
    val updatedAt: Long = 0L,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val groupName: String = "",
    val groupDescription: String = "",
    val groupPhotoUrl: String = "",
    val participantIds: List<String> = emptyList(),
    val ownerId: String = "",
    val adminIds: List<String> = emptyList(),
    val groupPermissions: GroupPermissions = GroupPermissions(),
    val createdAt: Long = 0L,
    val peerLastReadAt: Long = 0L,
    val myLastReadAt: Long = 0L
)

enum class GroupMemberRole(val label: String) {
    PRESIDENT("Grup Başkanı"),
    ADMIN("Yönetici"),
    MEMBER("Üye")
}

fun resolveGroupOwnerId(conversation: DirectConversation): String {
    if (!conversation.isGroup) return ""
    return conversation.ownerId.ifBlank { conversation.participantIds.lastOrNull().orEmpty() }
}

fun resolveGroupMemberRole(conversation: DirectConversation, uid: String): GroupMemberRole {
    if (!conversation.isGroup || uid.isBlank()) return GroupMemberRole.MEMBER
    val presidentUid = resolveGroupOwnerId(conversation)
    return when {
        uid == presidentUid -> GroupMemberRole.PRESIDENT
        uid in conversation.adminIds -> GroupMemberRole.ADMIN
        else -> GroupMemberRole.MEMBER
    }
}

fun canManageGroupSettings(conversation: DirectConversation, uid: String): Boolean {
    if (!conversation.isGroup || uid.isBlank()) return false
    val role = resolveGroupMemberRole(conversation, uid)
    return role == GroupMemberRole.PRESIDENT || role == GroupMemberRole.ADMIN
}

fun canEditGroupInfo(conversation: DirectConversation, uid: String): Boolean {
    if (!conversation.isGroup || uid.isBlank()) return false
    return canManageGroupSettings(conversation, uid) || conversation.groupPermissions.membersCanEditInfo
}

fun canAddGroupMembers(conversation: DirectConversation, uid: String): Boolean {
    if (!conversation.isGroup || uid.isBlank()) return false
    return canManageGroupSettings(conversation, uid) || conversation.groupPermissions.membersCanAddOthers
}

fun canSendInGroup(conversation: DirectConversation, uid: String): Boolean {
    if (!conversation.isGroup) return true
    if (uid.isBlank()) return false
    return !conversation.groupPermissions.onlyAdminsCanMessage || canManageGroupSettings(conversation, uid)
}

fun canSendMediaInGroup(conversation: DirectConversation, uid: String): Boolean {
    if (!canSendInGroup(conversation, uid)) return false
    if (!conversation.isGroup) return true
    return conversation.groupPermissions.membersCanSendMedia || canManageGroupSettings(conversation, uid)
}

data class DirectMessage(
    val id: String = "",
    val senderId: String = "",
    val senderPhotoUrl: String = "",
    val senderName: String = "",
    val senderUsername: String = "",
    val recipientId: String = "",
    val conversationId: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "",
    val createdAt: Long = 0L,
    val editedAt: Long? = null,
    val replyToMessageId: String = "",
    val replyToText: String = "",
    val replyToSenderId: String = "",
    val replyToSenderName: String = "",
    val reactions: Map<String, String> = emptyMap(),
    val sharedContent: SharedContent? = null
)
