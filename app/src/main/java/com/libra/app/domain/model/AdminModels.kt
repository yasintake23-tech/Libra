package com.libra.app.domain.model

import com.google.firebase.firestore.Exclude

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
    val manageUpdates: Boolean = false,
    val manageNotifications: Boolean = false
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

enum class AdminAudienceCategory(
    val key: String,
    val label: String,
    val shortLabel: String,
    val description: String
) {
    ALL(
        key = "ALL",
        label = "Tüm Üyeler",
        shortLabel = "Herkes",
        description = "Uygulamadaki tüm kayıtlı üyelere gönderir."
    ),
    AUTHORS(
        key = "AUTHORS",
        label = "Kitap Yazarları",
        shortLabel = "Yazarlar",
        description = "En az 1 kitabı bulunan yazar üyeler."
    ),
    CREATORS(
        key = "CREATORS",
        label = "Aktif Paylaşımcılar",
        shortLabel = "Paylaşımcılar",
        description = "Gönderi veya hikâye paylaşan üyeler."
    ),
    NEW_MEMBERS(
        key = "NEW_MEMBERS",
        label = "Yeni Katılanlar",
        shortLabel = "Yeni Üyeler",
        description = "Son 14 gün içinde Libra'ya katılan üyeler."
    ),
    COSMETIC_ROLE(
        key = "COSMETIC_ROLE",
        label = "Kozmetik Rozet Grubu",
        shortLabel = "Rozete Göre",
        description = "Seçilen gerçek kozmetik rozete sahip üyeler."
    ),
    BOOK_CATEGORY_AUTHORS(
        key = "BOOK_CATEGORY_AUTHORS",
        label = "Kitap Türü Yazarları",
        shortLabel = "Kitap Türü",
        description = "Seçilen kitap kategorisinde eseri bulunan yazarlar."
    ),
    ADMINS(
        key = "ADMINS",
        label = "Yönetim Ekibi",
        shortLabel = "Yöneticiler",
        description = "Yetkili yönetici rolüne sahip ekip üyeleri."
    ),
    CUSTOM_USERS(
        key = "CUSTOM_USERS",
        label = "Seçili Kişiler",
        shortLabel = "Kişi Seç",
        description = "Listeden tek tek seçtiğin özel alıcı grubu."
    );

    companion object {
        fun fromKey(key: String?): AdminAudienceCategory =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: ALL
    }
}

data class AdminNotificationDraft(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val categoryType: String = AdminAudienceCategory.ALL.key,
    val categorySubId: String = "",
    val customUserIds: List<String> = emptyList(),
    val tagLabel: String = "Duyuru",
    val sendCount: Int = 0,
    val lastSentAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val createdBy: String = ""
)

data class AdminNotificationCampaignLog(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val categoryType: String = AdminAudienceCategory.ALL.key,
    val categoryLabel: String = "",
    val tagLabel: String = "Duyuru",
    val recipientCount: Int = 0,
    val senderUid: String = "",
    val senderName: String = "",
    val draftId: String = "",
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

    @get:Exclude
    val isBanned: Boolean get() = active(banned, banUntil)

    @get:Exclude
    val canPost: Boolean get() = !active(postingDisabled, postingDisabledUntil)

    @get:Exclude
    val canMessage: Boolean get() = !active(messagingDisabled, messagingDisabledUntil)

    @get:Exclude
    val canStory: Boolean get() = !active(storyDisabled, storyDisabledUntil)

    @get:Exclude
    val canComment: Boolean get() = !active(commentingDisabled, commentingDisabledUntil)

    @get:Exclude
    val canPublishBook: Boolean get() = !active(bookPublishingDisabled, bookPublishingDisabledUntil)

    @get:Exclude
    val canCreateServer: Boolean get() = !active(serverCreationDisabled, serverCreationDisabledUntil)

    @get:Exclude
    val canMessageInServer: Boolean get() = !active(serverMessagingDisabled, serverMessagingDisabledUntil)
}