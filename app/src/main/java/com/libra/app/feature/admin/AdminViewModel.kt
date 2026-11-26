package com.libra.app.feature.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AdminAudienceCategory
import com.libra.app.domain.model.AdminNotificationCampaignLog
import com.libra.app.domain.model.AdminNotificationDraft
import com.libra.app.domain.model.AdminRole
import com.libra.app.domain.model.AppNotification
import com.libra.app.domain.model.AppReleaseInfo
import com.libra.app.domain.model.AppUpdate
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.GlobalChatMessage
import com.libra.app.domain.model.LibraNotificationTypes
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.PostComment
import com.libra.app.domain.model.ServerBan
import com.libra.app.domain.model.ServerCategory
import com.libra.app.domain.model.ServerChannel
import com.libra.app.domain.model.ServerMember
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.model.Story
import com.libra.app.domain.model.UserModeration
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.AdminRepository
import com.libra.app.domain.repository.AppReleaseStorageRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.ChatRepository
import com.libra.app.domain.repository.CommunityRepository
import com.libra.app.domain.repository.NotificationRepository
import com.libra.app.domain.repository.PostRepository
import com.libra.app.domain.repository.StoryRepository
import com.libra.app.domain.repository.UpdateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class AdminStoryFilter(val label: String) {
    ALL("Tümü"),
    ACTIVE("Aktif (24s)"),
    EXPIRED("Süresi Dolmuş"),
    WITH_MEDIA("Görselli"),
    TEXT_ONLY("Metin")
}

enum class AdminStorySortOption(val label: String) {
    NEWEST("En Yeni"),
    EXPIRING_SOON("Süresi Yaklaşan"),
    MOST_LIKED("En Çok Beğenilen")
}

fun filterAndSortAdminStories(
    stories: List<Story>,
    query: String = "",
    ownerFilterUid: String? = null,
    filter: AdminStoryFilter = AdminStoryFilter.ALL,
    sortOption: AdminStorySortOption = AdminStorySortOption.NEWEST,
    nowMs: Long = System.currentTimeMillis()
): List<Story> {
    val trLocale = java.util.Locale.forLanguageTag("tr")
    val q = query.trim().lowercase(trLocale)
    val filtered = stories.filter { story ->
        val matchesOwner = ownerFilterUid.isNullOrBlank() || story.authorId == ownerFilterUid
        val matchesQuery = q.isBlank() ||
            story.text.lowercase(trLocale).contains(q) ||
            story.authorName.lowercase(trLocale).contains(q) ||
            story.authorId.lowercase(trLocale).contains(q) ||
            story.id.lowercase(trLocale).contains(q)
        val matchesFilter = when (filter) {
            AdminStoryFilter.ALL -> true
            AdminStoryFilter.ACTIVE -> story.expiresAt > nowMs
            AdminStoryFilter.EXPIRED -> story.expiresAt <= nowMs
            AdminStoryFilter.WITH_MEDIA -> story.mediaUrl.isNotBlank()
            AdminStoryFilter.TEXT_ONLY -> story.mediaUrl.isBlank()
        }
        matchesOwner && matchesQuery && matchesFilter
    }
    return when (sortOption) {
        AdminStorySortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
        AdminStorySortOption.EXPIRING_SOON -> filtered.sortedWith(
            compareBy<Story> { it.expiresAt <= nowMs }.thenBy { it.expiresAt }
        )
        AdminStorySortOption.MOST_LIKED -> filtered.sortedWith(
            compareByDescending<Story> { it.likesCount }.thenByDescending { it.createdAt }
        )
    }
}

enum class AdminServerFilter(val label: String) {
    ALL("Tümü"),
    WITH_MEDIA("Görselli"),
    WITH_INVITE("Davet Anahtarlı")
}

enum class AdminServerSortOption(val label: String) {
    NEWEST("En Yeni"),
    OLDEST("En Eski"),
    NAME_ASC("Ad (A-Z)")
}

fun filterAndSortAdminServers(
    servers: List<CommunityServer>,
    query: String = "",
    ownerFilterUid: String? = null,
    filter: AdminServerFilter = AdminServerFilter.ALL,
    sortOption: AdminServerSortOption = AdminServerSortOption.NEWEST
): List<CommunityServer> {
    val trLocale = java.util.Locale.forLanguageTag("tr")
    val q = query.trim().lowercase(trLocale)
    val filtered = servers.filter { server ->
        val matchesOwner = ownerFilterUid.isNullOrBlank() || server.ownerId == ownerFilterUid
        val matchesQuery = q.isBlank() ||
            server.name.lowercase(trLocale).contains(q) ||
            server.description.lowercase(trLocale).contains(q) ||
            server.inviteKey.lowercase(trLocale).contains(q) ||
            server.ownerId.lowercase(trLocale).contains(q) ||
            server.id.lowercase(trLocale).contains(q)
        val matchesFilter = when (filter) {
            AdminServerFilter.ALL -> true
            AdminServerFilter.WITH_MEDIA -> server.avatarUrl.isNotBlank() || server.bannerUrl.isNotBlank()
            AdminServerFilter.WITH_INVITE -> server.inviteKey.isNotBlank()
        }
        matchesOwner && matchesQuery && matchesFilter
    }
    return when (sortOption) {
        AdminServerSortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
        AdminServerSortOption.OLDEST -> filtered.sortedBy { it.createdAt }
        AdminServerSortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase(trLocale) }
    }
}

enum class AdminGlobalChatFilter(val label: String) {
    ALL("Tümü"),
    WITH_MEDIA("Görselli"),
    WITH_SHARED_CONTENT("Paylaşımlı"),
    EDITED("Düzenlenen"),
    WITH_REACTIONS("Tepkili")
}

enum class AdminGlobalChatSortOption(val label: String) {
    NEWEST("En Yeni"),
    OLDEST("En Eski"),
    MOST_REACTED("En Çok Tepkili")
}

fun filterAndSortAdminGlobalMessages(
    messages: List<GlobalChatMessage>,
    query: String = "",
    ownerFilterUid: String? = null,
    filter: AdminGlobalChatFilter = AdminGlobalChatFilter.ALL,
    sortOption: AdminGlobalChatSortOption = AdminGlobalChatSortOption.NEWEST
): List<GlobalChatMessage> {
    val trLocale = java.util.Locale.forLanguageTag("tr")
    val q = query.trim().lowercase(trLocale)
    val filtered = messages.filter { msg ->
        val matchesOwner = ownerFilterUid.isNullOrBlank() || msg.senderId == ownerFilterUid
        val matchesQuery = q.isBlank() ||
            msg.text.lowercase(trLocale).contains(q) ||
            msg.senderName.lowercase(trLocale).contains(q) ||
            msg.senderUsername.lowercase(trLocale).contains(q) ||
            msg.senderId.lowercase(trLocale).contains(q) ||
            msg.id.lowercase(trLocale).contains(q) ||
            msg.sharedContent?.title.orEmpty().lowercase(trLocale).contains(q)
        val matchesFilter = when (filter) {
            AdminGlobalChatFilter.ALL -> true
            AdminGlobalChatFilter.WITH_MEDIA -> msg.mediaUrl.isNotBlank()
            AdminGlobalChatFilter.WITH_SHARED_CONTENT -> msg.sharedContent != null
            AdminGlobalChatFilter.EDITED -> msg.editedAt != null
            AdminGlobalChatFilter.WITH_REACTIONS -> msg.reactions.isNotEmpty()
        }
        matchesOwner && matchesQuery && matchesFilter
    }
    return when (sortOption) {
        AdminGlobalChatSortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
        AdminGlobalChatSortOption.OLDEST -> filtered.sortedBy { it.createdAt }
        AdminGlobalChatSortOption.MOST_REACTED -> filtered.sortedWith(
            compareByDescending<GlobalChatMessage> { it.reactions.size }.thenByDescending { it.createdAt }
        )
    }
}

enum class AdminBookStatusFilter(val label: String) {
    ALL("Tümü"),
    PUBLISHED("Yayında"),
    DRAFT("Taslak"),
    ARCHIVED("Arşiv"),
    RESTRICTED_VISIBILITY("Gizli / Liste Dışı")
}

enum class AdminBookVisibilityFilter(val label: String) {
    ALL("Tüm Görünürlükler"),
    PUBLIC("Herkese Açık"),
    UNLISTED("Liste Dışı"),
    PRIVATE("Gizli")
}

enum class AdminBookAudienceFilter(val label: String) {
    ALL("Tüm Kitleler"),
    GENERAL("Genel Okur"),
    YA("Genç Yetişkin"),
    ADULT("Yetişkin (18+)")
}

enum class AdminBookSortOption(val label: String) {
    RECENTLY_UPDATED("Son Güncellenen"),
    MOST_READ("En Çok Okunan"),
    MOST_LIKED("En Çok Beğenilen"),
    MOST_CHAPTERS("En Çok Bölüm"),
    TITLE_ASC("Başlık (A-Z)")
}

data class AdminBookCategorySummary(
    val category: BookCategory,
    val totalCount: Int,
    val publishedCount: Int,
    val draftCount: Int,
    val archivedCount: Int,
    val totalReads: Int
)

enum class AdminPostFilter(val label: String) {
    ALL("Tümü"),
    WITH_MEDIA("Görselli"),
    TEXT_ONLY("Metin"),
    TAGGED("Etiketli")
}

enum class AdminPostSortOption(val label: String) {
    NEWEST("En Yeni"),
    OLDEST("En Eski"),
    MOST_LIKED("En Çok Beğenilen"),
    MOST_COMMENTED("En Çok Yorumlanan")
}

data class AdminPostTagSummary(
    val tag: String,
    val postCount: Int,
    val mediaPostCount: Int,
    val totalLikes: Int,
    val totalComments: Int
)

fun filterAndSortAdminPosts(
    posts: List<Post>,
    query: String = "",
    ownerFilterUid: String? = null,
    filter: AdminPostFilter = AdminPostFilter.ALL,
    tagFilter: String? = null,
    sortOption: AdminPostSortOption = AdminPostSortOption.NEWEST
): List<Post> {
    val trLocale = java.util.Locale.forLanguageTag("tr")
    val q = query.trim().lowercase(trLocale)
    val cleanTagFilter = tagFilter?.trim()?.removePrefix("#").orEmpty()
    val filtered = posts.filter { post ->
        val matchesOwner = ownerFilterUid.isNullOrBlank() || post.authorId == ownerFilterUid
        val matchesQuery = q.isBlank() ||
            post.title.lowercase(trLocale).contains(q) ||
            post.text.lowercase(trLocale).contains(q) ||
            post.authorName.lowercase(trLocale).contains(q) ||
            post.authorUsername.lowercase(trLocale).contains(q) ||
            post.id.lowercase(trLocale).contains(q) ||
            post.tags.any { it.removePrefix("#").lowercase(trLocale).contains(q) }
        val matchesFilter = when (filter) {
            AdminPostFilter.ALL -> true
            AdminPostFilter.WITH_MEDIA -> post.mediaUrl.isNotBlank()
            AdminPostFilter.TEXT_ONLY -> post.mediaUrl.isBlank()
            AdminPostFilter.TAGGED -> post.tags.any { it.removePrefix("#").isNotBlank() }
        }
        val matchesTag = cleanTagFilter.isBlank() ||
            post.tags.any { it.removePrefix("#").lowercase(trLocale) == cleanTagFilter.lowercase(trLocale) }

        matchesOwner && matchesQuery && matchesFilter && matchesTag
    }

    return when (sortOption) {
        AdminPostSortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
        AdminPostSortOption.OLDEST -> filtered.sortedBy { it.createdAt }
        AdminPostSortOption.MOST_LIKED -> filtered.sortedWith(
            compareByDescending<Post> { it.likesCount }.thenByDescending { it.createdAt }
        )
        AdminPostSortOption.MOST_COMMENTED -> filtered.sortedWith(
            compareByDescending<Post> { it.commentsCount }.thenByDescending { it.createdAt }
        )
    }
}

fun summarizeAdminPostTags(posts: List<Post>): List<AdminPostTagSummary> {
    val trLocale = java.util.Locale.forLanguageTag("tr")
    val buckets = linkedMapOf<String, MutableList<Post>>()
    posts.forEach { post ->
        post.tags
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(trLocale) }
            .forEach { cleanTag ->
                val key = cleanTag.lowercase(trLocale)
                buckets.getOrPut(key) { mutableListOf() }.add(post)
            }
    }
    return buckets.entries
        .map { (tagKey, items) ->
            AdminPostTagSummary(
                tag = tagKey,
                postCount = items.size,
                mediaPostCount = items.count { it.mediaUrl.isNotBlank() },
                totalLikes = items.sumOf { it.likesCount },
                totalComments = items.sumOf { it.commentsCount }
            )
        }
        .sortedWith(
            compareByDescending<AdminPostTagSummary> { it.postCount }
                .thenByDescending { it.totalLikes + it.totalComments }
                .thenBy { it.tag }
        )
}

fun filterAndSortAdminBooks(
    books: List<Book>,
    query: String = "",
    ownerFilterUid: String? = null,
    statusFilter: AdminBookStatusFilter = AdminBookStatusFilter.ALL,
    categoryFilter: BookCategory? = null,
    visibilityFilter: AdminBookVisibilityFilter = AdminBookVisibilityFilter.ALL,
    audienceFilter: AdminBookAudienceFilter = AdminBookAudienceFilter.ALL,
    sortOption: AdminBookSortOption = AdminBookSortOption.RECENTLY_UPDATED
): List<Book> {
    val q = query.trim().lowercase()
    val filtered = books.filter { book ->
        val matchesOwner = ownerFilterUid.isNullOrBlank() || book.ownerId == ownerFilterUid

        val matchesQuery = q.isBlank() ||
            book.title.lowercase().contains(q) ||
            book.authorName.lowercase().contains(q) ||
            book.id.lowercase().contains(q) ||
            book.genre.lowercase().contains(q) ||
            book.subGenre.lowercase().contains(q) ||
            book.seriesName.lowercase().contains(q) ||
            book.category.displayName.lowercase().contains(q) ||
            book.tags.any { it.lowercase().contains(q) }

        val matchesStatus = when (statusFilter) {
            AdminBookStatusFilter.ALL -> true
            AdminBookStatusFilter.PUBLISHED -> book.status == BookStatus.PUBLISHED
            AdminBookStatusFilter.DRAFT -> book.status == BookStatus.DRAFT
            AdminBookStatusFilter.ARCHIVED -> book.status == BookStatus.ARCHIVED
            AdminBookStatusFilter.RESTRICTED_VISIBILITY ->
                !book.visibility.equals("PUBLIC", ignoreCase = true)
        }

        val matchesCategory = categoryFilter == null || book.category == categoryFilter

        val matchesVisibility = when (visibilityFilter) {
            AdminBookVisibilityFilter.ALL -> true
            AdminBookVisibilityFilter.PUBLIC -> book.visibility.equals("PUBLIC", ignoreCase = true)
            AdminBookVisibilityFilter.UNLISTED -> book.visibility.equals("UNLISTED", ignoreCase = true)
            AdminBookVisibilityFilter.PRIVATE -> book.visibility.equals("PRIVATE", ignoreCase = true)
        }

        val matchesAudience = when (audienceFilter) {
            AdminBookAudienceFilter.ALL -> true
            AdminBookAudienceFilter.GENERAL ->
                book.targetAudience.isBlank() ||
                    book.targetAudience.equals("ALL", ignoreCase = true) ||
                    book.targetAudience.equals("Herkes İçin", ignoreCase = true) ||
                    book.targetAudience.equals("Genel Okur", ignoreCase = true)
            AdminBookAudienceFilter.YA ->
                book.targetAudience.equals("YA", ignoreCase = true) ||
                    book.targetAudience.equals("Genç Yetişkin (13+)", ignoreCase = true) ||
                    book.targetAudience.equals("Genç Yetişkin", ignoreCase = true)
            AdminBookAudienceFilter.ADULT ->
                book.targetAudience.equals("ADULT", ignoreCase = true) ||
                    book.targetAudience.equals("Yetişkin (18+)", ignoreCase = true) ||
                    book.targetAudience.equals("Yetişkin", ignoreCase = true)
        }

        matchesOwner && matchesQuery && matchesStatus && matchesCategory && matchesVisibility && matchesAudience
    }

    return when (sortOption) {
        AdminBookSortOption.RECENTLY_UPDATED -> filtered.sortedByDescending { it.updatedAt }
        AdminBookSortOption.MOST_READ -> filtered.sortedByDescending { it.readCount }
        AdminBookSortOption.MOST_LIKED -> filtered.sortedByDescending { it.likesCount }
        AdminBookSortOption.MOST_CHAPTERS -> filtered.sortedByDescending { it.chapterCount }
        AdminBookSortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
    }
}

fun summarizeAdminBookCategories(books: List<Book>): List<AdminBookCategorySummary> =
    BookCategory.entries.map { category ->
        val items = books.filter { it.category == category }
        AdminBookCategorySummary(
            category = category,
            totalCount = items.size,
            publishedCount = items.count { it.status == BookStatus.PUBLISHED },
            draftCount = items.count { it.status == BookStatus.DRAFT },
            archivedCount = items.count { it.status == BookStatus.ARCHIVED },
            totalReads = items.sumOf { it.readCount }
        )
    }

internal const val LIBRA_PRIMARY_ADMIN_UID = "ZBkz2js9plg07zrny2PzLW80X3i2"
private const val NEW_MEMBER_WINDOW_MILLIS = 14L * 24L * 60L * 60L * 1000L

fun resolveAdminNotificationRecipients(
    members: List<UserProfile>,
    books: List<Book>,
    posts: List<Post>,
    stories: List<Story>,
    adminRoleUserIds: Set<String>,
    categoryType: AdminAudienceCategory,
    categorySubId: String = "",
    customUserIds: Collection<String> = emptyList(),
    excludeUid: String = "",
    nowMillis: Long = System.currentTimeMillis()
): List<UserProfile> {
    val eligibleMembers = members
        .asSequence()
        .filter { it.uid.isNotBlank() && it.uid != excludeUid && !it.moderation.isBanned }
        .distinctBy { it.uid }
        .toList()

    return when (categoryType) {
        AdminAudienceCategory.ALL -> eligibleMembers

        AdminAudienceCategory.AUTHORS -> {
            val authorIds = books.mapNotNull { it.ownerId.takeIf(String::isNotBlank) }.toSet()
            eligibleMembers.filter { it.uid in authorIds || it.booksWrittenCount > 0 }
        }

        AdminAudienceCategory.CREATORS -> {
            val creatorIds = buildSet {
                posts.forEach { if (it.authorId.isNotBlank()) add(it.authorId) }
                stories.forEach { if (it.authorId.isNotBlank()) add(it.authorId) }
            }
            eligibleMembers.filter { it.uid in creatorIds }
        }

        AdminAudienceCategory.NEW_MEMBERS -> {
            eligibleMembers.filter { member ->
                member.createdAt > 0L &&
                    (nowMillis - member.createdAt) in 0L..NEW_MEMBER_WINDOW_MILLIS
            }
        }

        AdminAudienceCategory.COSMETIC_ROLE -> {
            val targetRoleId = categorySubId.trim()
            if (targetRoleId.isBlank()) {
                emptyList()
            } else {
                eligibleMembers.filter { targetRoleId in it.cosmeticRoleIds }
            }
        }

        AdminAudienceCategory.BOOK_CATEGORY_AUTHORS -> {
            val targetCategory = BookCategory.entries.firstOrNull {
                it.name.equals(categorySubId.trim(), ignoreCase = true)
            }
            if (targetCategory == null) {
                emptyList()
            } else {
                val categoryAuthorIds = books
                    .asSequence()
                    .filter { it.category == targetCategory && it.ownerId.isNotBlank() }
                    .map { it.ownerId }
                    .toSet()
                eligibleMembers.filter { it.uid in categoryAuthorIds }
            }
        }

        AdminAudienceCategory.ADMINS -> {
            eligibleMembers.filter { it.uid in adminRoleUserIds || it.uid == LIBRA_PRIMARY_ADMIN_UID }
        }

        AdminAudienceCategory.CUSTOM_USERS -> {
            val selectedIds = customUserIds.map(String::trim).filter(String::isNotBlank).toSet()
            if (selectedIds.isEmpty()) {
                emptyList()
            } else {
                eligibleMembers.filter { it.uid in selectedIds }
            }
        }
    }
}

fun resolveAdminAudienceLabel(
    categoryType: AdminAudienceCategory,
    categorySubId: String = "",
    cosmeticRoles: List<CosmeticRole> = emptyList(),
    customCount: Int = 0
): String = when (categoryType) {
    AdminAudienceCategory.ALL -> categoryType.label
    AdminAudienceCategory.AUTHORS -> categoryType.label
    AdminAudienceCategory.CREATORS -> categoryType.label
    AdminAudienceCategory.NEW_MEMBERS -> "Yeni Katılanlar (14 Gün)"
    AdminAudienceCategory.COSMETIC_ROLE -> {
        val role = cosmeticRoles.firstOrNull { it.id == categorySubId }
        if (role != null) "Rozet: ${role.name}" else categoryType.label
    }
    AdminAudienceCategory.BOOK_CATEGORY_AUTHORS -> {
        val bookCategory = BookCategory.entries.firstOrNull {
            it.name.equals(categorySubId.trim(), ignoreCase = true)
        }
        if (bookCategory != null) "${bookCategory.displayName} Yazarları" else categoryType.label
    }
    AdminAudienceCategory.ADMINS -> categoryType.label
    AdminAudienceCategory.CUSTOM_USERS -> "Seçili Kişiler ($customCount)"
}

fun filterAndSortAdminNotificationDrafts(
    drafts: List<AdminNotificationDraft>,
    query: String = "",
    tagFilter: String = "Tümü"
): List<AdminNotificationDraft> {
    val cleanQuery = query.trim().lowercase()
    val cleanTag = tagFilter.trim()
    return drafts
        .filter { draft ->
            val matchesTag = cleanTag.isBlank() ||
                cleanTag.equals("Tümü", ignoreCase = true) ||
                draft.tagLabel.equals(cleanTag, ignoreCase = true)
            val categoryLabel = AdminAudienceCategory.fromKey(draft.categoryType).label
            val matchesQuery = cleanQuery.isBlank() ||
                draft.title.lowercase().contains(cleanQuery) ||
                draft.body.lowercase().contains(cleanQuery) ||
                draft.tagLabel.lowercase().contains(cleanQuery) ||
                categoryLabel.lowercase().contains(cleanQuery)
            matchesTag && matchesQuery
        }
        .sortedByDescending { maxOf(it.updatedAt, it.createdAt, it.lastSentAt) }
}

class AdminViewModel(
    private val repo: AdminRepository,
    private val bookRepo: BookRepository,
    private val postRepo: PostRepository,
    private val storyRepo: StoryRepository,
    private val communityRepo: CommunityRepository,
    private val chatRepo: ChatRepository,
    private val updateRepo: UpdateRepository,
    private val releaseStorageRepo: AppReleaseStorageRepository,
    private val notificationRepo: NotificationRepository
) : ViewModel() {

    constructor() : this(
        repo = ServiceLocator.adminRepository,
        bookRepo = ServiceLocator.bookRepository,
        postRepo = ServiceLocator.postRepository,
        storyRepo = ServiceLocator.storyRepository,
        communityRepo = ServiceLocator.communityRepository,
        chatRepo = ServiceLocator.chatRepository,
        updateRepo = ServiceLocator.updateRepository,
        releaseStorageRepo = ServiceLocator.appReleaseStorageRepository,
        notificationRepo = ServiceLocator.notificationRepository
    )

    private val _members = MutableStateFlow<List<UserProfile>>(emptyList())
    val members: StateFlow<List<UserProfile>> = _members.asStateFlow()
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()
    private val _booksLoading = MutableStateFlow(true)
    val booksLoading: StateFlow<Boolean> = _booksLoading.asStateFlow()
    private val _bookActionBusy = MutableStateFlow(false)
    val bookActionBusy: StateFlow<Boolean> = _bookActionBusy.asStateFlow()
    private val _bookActionMessage = MutableStateFlow<String?>(null)
    val bookActionMessage: StateFlow<String?> = _bookActionMessage.asStateFlow()
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()
    private val _postsLoading = MutableStateFlow(true)
    val postsLoading: StateFlow<Boolean> = _postsLoading.asStateFlow()
    private val _postActionBusy = MutableStateFlow(false)
    val postActionBusy: StateFlow<Boolean> = _postActionBusy.asStateFlow()
    private val _postActionMessage = MutableStateFlow<String?>(null)
    val postActionMessage: StateFlow<String?> = _postActionMessage.asStateFlow()
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()
    private val _storiesLoading = MutableStateFlow(true)
    val storiesLoading: StateFlow<Boolean> = _storiesLoading.asStateFlow()
    private val _storyActionBusy = MutableStateFlow(false)
    val storyActionBusy: StateFlow<Boolean> = _storyActionBusy.asStateFlow()
    private val _storyActionMessage = MutableStateFlow<String?>(null)
    val storyActionMessage: StateFlow<String?> = _storyActionMessage.asStateFlow()
    private val _servers = MutableStateFlow<List<CommunityServer>>(emptyList())
    val servers: StateFlow<List<CommunityServer>> = _servers.asStateFlow()
    private val _serversLoading = MutableStateFlow(true)
    val serversLoading: StateFlow<Boolean> = _serversLoading.asStateFlow()
    private val _serverActionBusy = MutableStateFlow(false)
    val serverActionBusy: StateFlow<Boolean> = _serverActionBusy.asStateFlow()
    private val _serverActionMessage = MutableStateFlow<String?>(null)
    val serverActionMessage: StateFlow<String?> = _serverActionMessage.asStateFlow()
    private val _globalMessages = MutableStateFlow<List<GlobalChatMessage>>(emptyList())
    val globalMessages: StateFlow<List<GlobalChatMessage>> = _globalMessages.asStateFlow()
    private val _globalMessagesLoading = MutableStateFlow(true)
    val globalMessagesLoading: StateFlow<Boolean> = _globalMessagesLoading.asStateFlow()
    private val _globalChatActionBusy = MutableStateFlow(false)
    val globalChatActionBusy: StateFlow<Boolean> = _globalChatActionBusy.asStateFlow()
    private val _globalChatActionMessage = MutableStateFlow<String?>(null)
    val globalChatActionMessage: StateFlow<String?> = _globalChatActionMessage.asStateFlow()
    private val _cosmeticRoles = MutableStateFlow<List<CosmeticRole>>(emptyList())
    val cosmeticRoles: StateFlow<List<CosmeticRole>> = _cosmeticRoles.asStateFlow()
    private val _notificationDrafts = MutableStateFlow<List<AdminNotificationDraft>>(emptyList())
    val notificationDrafts: StateFlow<List<AdminNotificationDraft>> = _notificationDrafts.asStateFlow()
    private val _notificationLogs = MutableStateFlow<List<AdminNotificationCampaignLog>>(emptyList())
    val notificationLogs: StateFlow<List<AdminNotificationCampaignLog>> = _notificationLogs.asStateFlow()
    private val _adminRoleUserIds = MutableStateFlow<Set<String>>(emptySet())
    val adminRoleUserIds: StateFlow<Set<String>> = _adminRoleUserIds.asStateFlow()
    private val _notificationActionBusy = MutableStateFlow(false)
    val notificationActionBusy: StateFlow<Boolean> = _notificationActionBusy.asStateFlow()
    private val _notificationActionMessage = MutableStateFlow<String?>(null)
    val notificationActionMessage: StateFlow<String?> = _notificationActionMessage.asStateFlow()
    private val _activeRelease = MutableStateFlow<AppUpdate?>(null)
    val activeRelease: StateFlow<AppUpdate?> = _activeRelease.asStateFlow()
    private val _releaseProgress = MutableStateFlow(0)
    val releaseProgress: StateFlow<Int> = _releaseProgress.asStateFlow()
    private val _releaseBusy = MutableStateFlow(false)
    val releaseBusy: StateFlow<Boolean> = _releaseBusy.asStateFlow()
    private val _releaseStatus = MutableStateFlow<String?>(null)
    val releaseStatus: StateFlow<String?> = _releaseStatus.asStateFlow()
    private val _selectedReleaseInfo = MutableStateFlow<AppReleaseInfo?>(null)
    val selectedReleaseInfo: StateFlow<AppReleaseInfo?> = _selectedReleaseInfo.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var booksJob: Job? = null
    private var postsJob: Job? = null
    private var storiesJob: Job? = null
    private var serversJob: Job? = null
    private var globalMessagesJob: Job? = null
    private var notificationDraftsJob: Job? = null
    private var notificationLogsJob: Job? = null
    private var adminRoleIdsJob: Job? = null

    init {
        viewModelScope.launch {
            when (val result = updateRepo.getActiveRelease()) {
                is AppResult.Success -> _activeRelease.value = result.data
                is AppResult.Error -> _error.value = result.error.message
            }
        }
        viewModelScope.launch {
            repo.observeMembers().collect {
                if (it is AppResult.Success) _members.value = it.data
                else if (it is AppResult.Error) _error.value = it.error.message
            }
        }
        viewModelScope.launch {
            repo.observeCosmeticRoles().collect {
                if (it is AppResult.Success) _cosmeticRoles.value = it.data
                else if (it is AppResult.Error) _error.value = it.error.message
            }
        }
        observeBooks()
        observePosts()
        observeStories()
        observeServers()
        observeGlobalMessages()
        observeNotificationHub()
    }

    private fun observeNotificationHub() {
        notificationDraftsJob?.cancel()
        notificationDraftsJob = viewModelScope.launch {
            repo.observeAdminNotificationDrafts().collect { res ->
                if (res is AppResult.Success) _notificationDrafts.value = res.data
                else if (res is AppResult.Error) _error.value = res.error.message
            }
        }
        notificationLogsJob?.cancel()
        notificationLogsJob = viewModelScope.launch {
            repo.observeAdminNotificationLogs().collect { res ->
                if (res is AppResult.Success) _notificationLogs.value = res.data
                else if (res is AppResult.Error) _error.value = res.error.message
            }
        }
        adminRoleIdsJob?.cancel()
        adminRoleIdsJob = viewModelScope.launch {
            repo.observeAdminRoleUserIds().collect { res ->
                if (res is AppResult.Success) _adminRoleUserIds.value = res.data
            }
        }
    }

    fun clearNotificationActionMessage() {
        _notificationActionMessage.value = null
    }

    fun observeBooks() {
        booksJob?.cancel()
        _booksLoading.value = true
        booksJob = viewModelScope.launch {
            bookRepo.observeAllBooksForAdmin().collect { result ->
                _booksLoading.value = false
                when (result) {
                    is AppResult.Success -> _books.value = result.data
                    is AppResult.Error -> _error.value = result.error.message
                }
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearReleaseStatus() { _releaseStatus.value = null }
    fun clearBookActionMessage() { _bookActionMessage.value = null }

    fun inspectAppRelease(uri: Uri) = viewModelScope.launch {
        _selectedReleaseInfo.value = null
        _releaseStatus.value = "APK sürümü okunuyor..."
        when (val result = releaseStorageRepo.inspectApk(uri)) {
            is AppResult.Success -> {
                _selectedReleaseInfo.value = result.data
                _releaseStatus.value = "APK sürümü otomatik algılandı."
            }
            is AppResult.Error -> _releaseStatus.value = result.error.message
        }
    }

    fun clearSelectedRelease() {
        _selectedReleaseInfo.value = null
    }

    fun publishAppRelease(
        uri: Uri,
        fileName: String,
        fileSize: Long,
        releaseId: String,
        versionCode: Long,
        versionName: String,
        changelog: List<String>,
        forceUpdate: Boolean
    ) = viewModelScope.launch {
        _releaseBusy.value = true
        _releaseProgress.value = 0
        _releaseStatus.value = null
        val oldRelease = _activeRelease.value
        val releaseInfo = _selectedReleaseInfo.value
        if (releaseInfo == null) {
            _releaseStatus.value = "Önce geçerli bir Libra APK seçin."
            _releaseBusy.value = false
            return@launch
        }
        if (releaseId.trim() != releaseInfo.releaseId) {
            _releaseStatus.value = "Release ID, seçilen APK'nın kimliğiyle eşleşmiyor."
            _releaseBusy.value = false
            return@launch
        }
        if (versionCode <= maxOf(com.libra.app.BuildConfig.VERSION_CODE.toLong(), oldRelease?.versionCode ?: 0L)) {
            _releaseStatus.value = "Version code mevcut sürümden daha büyük olmalı."
            _releaseBusy.value = false
            return@launch
        }
        if (versionName.trim().isBlank()) {
            _releaseStatus.value = "Sürüm adı boş olamaz."
            _releaseBusy.value = false
            return@launch
        }
        try {
            val uploaded = releaseStorageRepo.uploadApk(
                uri = uri, fileName = fileName, fileSize = fileSize,
                onProgress = { _releaseProgress.value = it }
            ).first()
            if (uploaded !is AppResult.Success) {
                _releaseStatus.value = (uploaded as AppResult.Error).error.message
                return@launch
            }

            val release = AppUpdate(
                releaseId = releaseInfo.releaseId, versionCode = versionCode, versionName = versionName.trim(),
                apkObjectKey = uploaded.data, apkSize = fileSize, changelog = changelog,
                forceUpdate = forceUpdate, createdAt = System.currentTimeMillis()
            )

            when (val published = updateRepo.publishRelease(release)) {
                is AppResult.Error -> {
                    releaseStorageRepo.deleteApk(uploaded.data)
                    _releaseStatus.value = published.error.message
                    return@launch
                }
                is AppResult.Success -> Unit
            }

            when (val mirrored = releaseStorageRepo.publishPublicRelease(release)) {
                is AppResult.Error -> {
                    _activeRelease.value = release
                    _releaseStatus.value = "Yayınlandı, ancak genel güncelleme bilgisi yenilenemedi: ${mirrored.error.message}"
                    return@launch
                }
                is AppResult.Success -> Unit
            }

            _activeRelease.value = release
            oldRelease?.apkObjectKey
                ?.takeIf { it.isNotBlank() && it != release.apkObjectKey }
                ?.let { oldKey ->
                    when (val deleted = releaseStorageRepo.deleteApk(oldKey)) {
                        is AppResult.Error -> {
                            _releaseStatus.value = "Yayınlandı. Eski APK silinemedi: " + deleted.error.message
                            return@launch
                        }
                        is AppResult.Success -> Unit
                    }
                }

            _releaseProgress.value = 100
            _releaseStatus.value = "Yeni sürüm başarıyla yayınlandı."
        } catch (e: Exception) {
            _releaseStatus.value = e.localizedMessage ?: "Güncelleme yayınlanamadı."
        } finally {
            _releaseBusy.value = false
        }
    }

    // --- Book Administration & Categorization ---

    fun updateBookCategorization(
        book: Book,
        category: BookCategory,
        genre: String = book.genre,
        subGenre: String = book.subGenre,
        tags: List<String> = book.tags,
        targetAudience: String = book.targetAudience,
        language: String = book.language,
        isSeries: Boolean = book.isSeries,
        seriesName: String = book.seriesName,
        seriesOrder: Int = book.seriesOrder,
        onDone: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        _bookActionBusy.value = true
        val cleanTags = tags.map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }.distinct().take(12)
        val updated = book.copy(
            category = category,
            genre = genre.trim(),
            subGenre = subGenre.trim(),
            tags = cleanTags,
            targetAudience = targetAudience.ifBlank { "ALL" },
            language = language.ifBlank { "tr" },
            isSeries = isSeries,
            seriesName = if (isSeries) seriesName.trim() else "",
            seriesOrder = if (isSeries) seriesOrder.coerceAtLeast(1) else 0,
            updatedAt = System.currentTimeMillis()
        )
        when (val res = bookRepo.updateBook(updated)) {
            is AppResult.Success -> {
                replaceBookInList(res.data)
                _bookActionMessage.value = "\"${res.data.title}\" kategorisi (${category.displayName}) güncellendi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _bookActionBusy.value = false
    }

    fun publishBookAsAdmin(bookId: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _bookActionBusy.value = true
        when (val res = bookRepo.publishBook(bookId)) {
            is AppResult.Success -> {
                replaceBookInList(res.data)
                _bookActionMessage.value = "\"${res.data.title}\" yayınlandı."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _bookActionBusy.value = false
    }

    fun unpublishBookAsAdmin(bookId: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _bookActionBusy.value = true
        when (val res = bookRepo.unpublishBook(bookId)) {
            is AppResult.Success -> {
                replaceBookInList(res.data)
                _bookActionMessage.value = "\"${res.data.title}\" taslağa alındı."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _bookActionBusy.value = false
    }

    fun archiveBookAsAdmin(bookId: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _bookActionBusy.value = true
        when (val res = bookRepo.archiveBook(bookId)) {
            is AppResult.Success -> {
                replaceBookInList(res.data)
                _bookActionMessage.value = "\"${res.data.title}\" arşivlendi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _bookActionBusy.value = false
    }

    fun updateBookVisibilityAsAdmin(
        book: Book,
        visibility: String,
        contentWarnings: List<String> = book.contentWarnings,
        onDone: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        _bookActionBusy.value = true
        val updated = book.copy(
            visibility = visibility,
            contentWarnings = contentWarnings,
            updatedAt = System.currentTimeMillis()
        )
        when (val res = bookRepo.updateBook(updated)) {
            is AppResult.Success -> {
                replaceBookInList(res.data)
                _bookActionMessage.value = "\"${res.data.title}\" görünürlüğü güncellendi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _bookActionBusy.value = false
    }

    fun deleteBookAsAdmin(bookId: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _bookActionBusy.value = true
        when (val res = bookRepo.deleteBook(bookId)) {
            is AppResult.Success -> {
                _books.value = _books.value.filterNot { it.id == bookId }
                _bookActionMessage.value = "Kitap kalıcı olarak silindi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _bookActionBusy.value = false
    }

    fun batchCategorizeBooks(bookIds: Set<String>, category: BookCategory, onDone: (Int) -> Unit = {}) = viewModelScope.launch {
        if (bookIds.isEmpty()) return@launch
        _bookActionBusy.value = true
        var updatedCount = 0
        val targets = _books.value.filter { it.id in bookIds }
        targets.forEach { book ->
            val updated = book.copy(category = category, updatedAt = System.currentTimeMillis())
            if (bookRepo.updateBook(updated) is AppResult.Success) {
                replaceBookInList(updated)
                updatedCount++
            }
        }
        _bookActionMessage.value = "$updatedCount kitap \"${category.displayName}\" kategorisine taşındı."
        _bookActionBusy.value = false
        onDone(updatedCount)
    }

    fun batchSetBookStatus(bookIds: Set<String>, targetStatus: BookStatus, onDone: (Int) -> Unit = {}) = viewModelScope.launch {
        if (bookIds.isEmpty()) return@launch
        _bookActionBusy.value = true
        var updatedCount = 0
        var lastError: String? = null
        val targets = _books.value.filter { it.id in bookIds }
        targets.forEach { book ->
            val res = when (targetStatus) {
                BookStatus.PUBLISHED -> bookRepo.publishBook(book.id)
                BookStatus.DRAFT -> bookRepo.unpublishBook(book.id)
                BookStatus.ARCHIVED -> bookRepo.archiveBook(book.id)
            }
            when (res) {
                is AppResult.Success -> {
                    replaceBookInList(res.data)
                    updatedCount++
                }
                is AppResult.Error -> lastError = res.error.message
            }
        }
        if (updatedCount > 0) {
            val statusLabel = when (targetStatus) {
                BookStatus.PUBLISHED -> "yayınlandı"
                BookStatus.DRAFT -> "taslağa alındı"
                BookStatus.ARCHIVED -> "arşivlendi"
            }
            _bookActionMessage.value = "$updatedCount kitap $statusLabel."
        } else if (lastError != null) {
            _error.value = lastError
        }
        _bookActionBusy.value = false
        onDone(updatedCount)
    }

    fun batchSetBookVisibility(bookIds: Set<String>, visibility: String, onDone: (Int) -> Unit = {}) = viewModelScope.launch {
        if (bookIds.isEmpty()) return@launch
        _bookActionBusy.value = true
        var updatedCount = 0
        _books.value.filter { it.id in bookIds }.forEach { book ->
            val updated = book.copy(visibility = visibility, updatedAt = System.currentTimeMillis())
            if (bookRepo.updateBook(updated) is AppResult.Success) {
                replaceBookInList(updated)
                updatedCount++
            }
        }
        _bookActionMessage.value = "$updatedCount kitabın görünürlüğü güncellendi."
        _bookActionBusy.value = false
        onDone(updatedCount)
    }

    fun toggleAuthorBookPublishing(uid: String, disabled: Boolean, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val member = _members.value.firstOrNull { it.uid == uid } ?: return@launch
        val updatedMod = member.moderation.copy(
            bookPublishingDisabled = disabled,
            bookPublishingDisabledUntil = 0L
        )
        when (val res = repo.updateModeration(uid, updatedMod)) {
            is AppResult.Success -> {
                _members.value = _members.value.map {
                    if (it.uid == uid) it.copy(moderation = updatedMod) else it
                }
                _bookActionMessage.value = if (disabled) {
                    "${member.displayName} için kitap paylaşma kısıtlandı."
                } else {
                    "${member.displayName} için kitap paylaşma yetkisi açıldı."
                }
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
    }

    fun sendBookModerationNotification(
        recipientUid: String,
        actorUid: String,
        title: String,
        body: String,
        bookId: String = "",
        onDone: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        val res = notificationRepo.create(
            AppNotification(
                recipientId = recipientUid,
                actorId = actorUid,
                actorName = "Libra Yönetim",
                type = "ADMIN_BOOK_NOTICE",
                title = title.trim().ifBlank { "Kitap Yönetimi Bildirimi" },
                body = body.trim(),
                referenceId = bookId,
                createdAt = System.currentTimeMillis()
            )
        )
        if (res is AppResult.Success) {
            _bookActionMessage.value = "Yazara bildirim gönderildi."
            onDone(true)
        } else {
            _error.value = (res as AppResult.Error).error.message
            onDone(false)
        }
    }

    private fun replaceBookInList(updated: Book) {
        val exists = _books.value.any { it.id == updated.id }
        _books.value = if (exists) {
            _books.value.map { if (it.id == updated.id) updated else it }
        } else {
            listOf(updated) + _books.value
        }
    }

    // --- Post Administration & Moderation ---

    fun observePosts() {
        postsJob?.cancel()
        _postsLoading.value = true
        postsJob = viewModelScope.launch {
            postRepo.observeAllPostsForAdmin(250).collect { res ->
                when (res) {
                    is AppResult.Success -> _posts.value = res.data
                    is AppResult.Error -> _error.value = res.error.message
                }
                _postsLoading.value = false
            }
        }
    }

    fun clearPostActionMessage() {
        _postActionMessage.value = null
    }

    fun observePostComments(postId: String): Flow<AppResult<List<PostComment>>> =
        postRepo.observeComments(postId)

    fun updatePostCategorization(
        post: Post,
        title: String = post.title,
        tags: List<String> = post.tags,
        onDone: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        _postActionBusy.value = true
        val trLocale = java.util.Locale.forLanguageTag("tr")
        val cleanTitle = title.trim().take(120)
        val cleanTags = tags
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(trLocale) }
            .take(10)
        when (val res = postRepo.updatePostCategorization(post.id, cleanTitle, cleanTags)) {
            is AppResult.Success -> {
                replacePostInList(res.data)
                _postActionMessage.value = "Gönderi başlık ve etiketleri güncellendi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _postActionBusy.value = false
    }

    fun deletePostAsAdmin(
        post: Post,
        actorUid: String,
        onDone: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        _postActionBusy.value = true
        when (val res = postRepo.deletePost(post.id, actorUid)) {
            is AppResult.Success -> {
                _posts.value = _posts.value.filterNot { it.id == post.id }
                _postActionMessage.value = "Gönderi kalıcı olarak silindi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _postActionBusy.value = false
    }

    fun batchDeletePosts(
        postIds: Set<String>,
        actorUid: String,
        onDone: (Int) -> Unit = {}
    ) = viewModelScope.launch {
        if (postIds.isEmpty()) return@launch
        _postActionBusy.value = true
        var deletedCount = 0
        var lastError: String? = null
        val targets = _posts.value.filter { it.id in postIds }
        targets.forEach { post ->
            when (val res = postRepo.deletePost(post.id, actorUid)) {
                is AppResult.Success -> deletedCount++
                is AppResult.Error -> lastError = res.error.message
            }
        }
        if (deletedCount > 0) {
            val removedIds = targets.map { it.id }.toSet()
            _posts.value = _posts.value.filterNot { it.id in removedIds }
            _postActionMessage.value = "$deletedCount gönderi silindi."
        } else if (lastError != null) {
            _error.value = lastError
        }
        _postActionBusy.value = false
        onDone(deletedCount)
    }

    fun batchAddTagToPosts(
        postIds: Set<String>,
        tag: String,
        onDone: (Int) -> Unit = {}
    ) = viewModelScope.launch {
        val cleanTag = tag.trim().removePrefix("#")
        if (postIds.isEmpty() || cleanTag.isBlank()) return@launch
        val trLocale = java.util.Locale.forLanguageTag("tr")
        _postActionBusy.value = true
        var updatedCount = 0
        var lastError: String? = null
        _posts.value.filter { it.id in postIds }.forEach { post ->
            val mergedTags = (post.tags + cleanTag)
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase(trLocale) }
                .take(10)
            when (val res = postRepo.updatePostCategorization(post.id, post.title, mergedTags)) {
                is AppResult.Success -> {
                    replacePostInList(res.data)
                    updatedCount++
                }
                is AppResult.Error -> lastError = res.error.message
            }
        }
        if (updatedCount > 0) {
            _postActionMessage.value = "$updatedCount gönderiye #$cleanTag etiketi eklendi."
        } else if (lastError != null) {
            _error.value = lastError
        }
        _postActionBusy.value = false
        onDone(updatedCount)
    }

    fun deletePostCommentAsAdmin(
        postId: String,
        commentId: String,
        actorUid: String,
        onDone: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        _postActionBusy.value = true
        val previousCount = _posts.value.firstOrNull { it.id == postId }?.commentsCount
        when (val res = postRepo.deleteComment(postId, commentId, actorUid)) {
            is AppResult.Success -> {
                val currentCount = _posts.value.firstOrNull { it.id == postId }?.commentsCount
                if (previousCount != null && currentCount == previousCount) {
                    _posts.value = _posts.value.map { post ->
                        if (post.id == postId) {
                            post.copy(commentsCount = (post.commentsCount - 1).coerceAtLeast(0))
                        } else {
                            post
                        }
                    }
                }
                _postActionMessage.value = "Yorum silindi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _postActionBusy.value = false
    }

    fun toggleAuthorPostingPermission(uid: String, disabled: Boolean, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val member = _members.value.firstOrNull { it.uid == uid } ?: return@launch
        val updatedMod = member.moderation.copy(
            postingDisabled = disabled,
            postingDisabledUntil = 0L
        )
        when (val res = repo.updateModeration(uid, updatedMod)) {
            is AppResult.Success -> {
                _members.value = _members.value.map {
                    if (it.uid == uid) it.copy(moderation = updatedMod) else it
                }
                _postActionMessage.value = if (disabled) {
                    "${member.displayName} için gönderi paylaşma kısıtlandı."
                } else {
                    "${member.displayName} için gönderi paylaşma yetkisi açıldı."
                }
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
    }

    fun toggleAuthorCommentingPermission(uid: String, disabled: Boolean, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val member = _members.value.firstOrNull { it.uid == uid } ?: return@launch
        val updatedMod = member.moderation.copy(
            commentingDisabled = disabled,
            commentingDisabledUntil = 0L
        )
        when (val res = repo.updateModeration(uid, updatedMod)) {
            is AppResult.Success -> {
                _members.value = _members.value.map {
                    if (it.uid == uid) it.copy(moderation = updatedMod) else it
                }
                _postActionMessage.value = if (disabled) {
                    "${member.displayName} için yorum yapma kısıtlandı."
                } else {
                    "${member.displayName} için yorum yapma yetkisi açıldı."
                }
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
    }

    fun sendPostModerationNotification(
        recipientUid: String,
        actorUid: String,
        title: String,
        body: String,
        postId: String = "",
        onDone: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        val res = notificationRepo.create(
            AppNotification(
                recipientId = recipientUid,
                actorId = actorUid,
                actorName = "Libra Yönetim",
                type = "ADMIN_FEEDBACK",
                title = title.trim().ifBlank { "Gönderi Yönetimi Bildirimi" },
                body = body.trim(),
                referenceId = postId,
                createdAt = System.currentTimeMillis()
            )
        )
        if (res is AppResult.Success) {
            _postActionMessage.value = "Üyeye bildirim gönderildi."
            onDone(true)
        } else {
            _error.value = (res as AppResult.Error).error.message
            onDone(false)
        }
    }

    private fun replacePostInList(updated: Post) {
        val exists = _posts.value.any { it.id == updated.id }
        _posts.value = if (exists) {
            _posts.value.map { if (it.id == updated.id) updated else it }
        } else {
            listOf(updated) + _posts.value
        }
    }

    // --- Story Administration & Moderation ---

    fun observeStories() {
        storiesJob?.cancel()
        _storiesLoading.value = true
        storiesJob = viewModelScope.launch {
            storyRepo.observeAllStoriesForAdmin(250).collect { res ->
                when (res) {
                    is AppResult.Success -> _stories.value = res.data
                    is AppResult.Error -> _error.value = res.error.message
                }
                _storiesLoading.value = false
            }
        }
    }

    fun clearStoryActionMessage() {
        _storyActionMessage.value = null
    }

    fun deleteStoryAsAdmin(story: Story, actorUid: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _storyActionBusy.value = true
        when (val res = storyRepo.deleteStory(story.id, actorUid)) {
            is AppResult.Success -> {
                _stories.value = _stories.value.filterNot { it.id == story.id }
                _storyActionMessage.value = "Hikâye kalıcı olarak silindi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _storyActionBusy.value = false
    }

    fun batchDeleteStories(storyIds: Set<String>, actorUid: String, onDone: (Int) -> Unit = {}) = viewModelScope.launch {
        if (storyIds.isEmpty()) return@launch
        _storyActionBusy.value = true
        var deletedCount = 0
        var lastError: String? = null
        val targets = _stories.value.filter { it.id in storyIds }
        targets.forEach { story ->
            when (val res = storyRepo.deleteStory(story.id, actorUid)) {
                is AppResult.Success -> deletedCount++
                is AppResult.Error -> lastError = res.error.message
            }
        }
        if (deletedCount > 0) {
            val removedIds = targets.map { it.id }.toSet()
            _stories.value = _stories.value.filterNot { it.id in removedIds }
            _storyActionMessage.value = "$deletedCount hikâye silindi."
        } else if (lastError != null) {
            _error.value = lastError
        }
        _storyActionBusy.value = false
        onDone(deletedCount)
    }

    fun toggleAuthorStoryPermission(uid: String, disabled: Boolean, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val member = _members.value.firstOrNull { it.uid == uid } ?: return@launch
        val updatedMod = member.moderation.copy(
            storyDisabled = disabled,
            storyDisabledUntil = 0L
        )
        when (val res = repo.updateModeration(uid, updatedMod)) {
            is AppResult.Success -> {
                _members.value = _members.value.map {
                    if (it.uid == uid) it.copy(moderation = updatedMod) else it
                }
                _storyActionMessage.value = if (disabled) {
                    "${member.displayName} için hikâye paylaşma kısıtlandı."
                } else {
                    "${member.displayName} için hikâye paylaşma yetkisi açıldı."
                }
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
    }

    fun sendStoryModerationNotification(
        recipientUid: String,
        actorUid: String,
        title: String,
        body: String,
        storyId: String = "",
        onDone: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        val res = notificationRepo.create(
            AppNotification(
                recipientId = recipientUid,
                actorId = actorUid,
                actorName = "Libra Yönetim",
                type = "ADMIN_FEEDBACK",
                title = title.trim().ifBlank { "Hikâye Yönetimi Bildirimi" },
                body = body.trim(),
                referenceId = storyId,
                createdAt = System.currentTimeMillis()
            )
        )
        if (res is AppResult.Success) {
            _storyActionMessage.value = "Üyeye bildirim gönderildi."
            onDone(true)
        } else {
            _error.value = (res as AppResult.Error).error.message
            onDone(false)
        }
    }

    // --- Community Server Administration & Moderation ---

    fun observeServers() {
        serversJob?.cancel()
        _serversLoading.value = true
        serversJob = viewModelScope.launch {
            communityRepo.observeCommunityServers().collect { res ->
                when (res) {
                    is AppResult.Success -> _servers.value = res.data
                    is AppResult.Error -> _error.value = res.error.message
                }
                _serversLoading.value = false
            }
        }
    }

    fun clearServerActionMessage() {
        _serverActionMessage.value = null
    }

    fun observeServerMembers(serverId: String): Flow<AppResult<List<ServerMember>>> =
        communityRepo.observeServerMembers(serverId)

    fun observeServerCategories(serverId: String): Flow<AppResult<List<ServerCategory>>> =
        communityRepo.observeServerCategories(serverId)

    fun observeServerChannels(serverId: String): Flow<AppResult<List<ServerChannel>>> =
        communityRepo.observeServerChannels(serverId)

    fun observeServerBans(serverId: String): Flow<AppResult<List<ServerBan>>> =
        communityRepo.observeServerBans(serverId)

    fun observeServerMessages(serverId: String, channelId: String): Flow<AppResult<List<ServerMessage>>> =
        chatRepo.observeServerMessages(serverId, limit = 150, channelId = channelId)

    fun updateServerMetadataAsAdmin(
        serverId: String,
        name: String,
        description: String,
        onDone: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        _serverActionBusy.value = true
        val cleanName = name.trim()
        val cleanDesc = description.trim()
        when (val res = communityRepo.updateCommunityServer(serverId, cleanName, cleanDesc)) {
            is AppResult.Success -> {
                _servers.value = _servers.value.map {
                    if (it.id == serverId) it.copy(name = cleanName, description = cleanDesc) else it
                }
                _serverActionMessage.value = "Sunucu bilgileri güncellendi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _serverActionBusy.value = false
    }

    fun clearServerMediaAsAdmin(
        server: CommunityServer,
        clearAvatar: Boolean,
        clearBanner: Boolean,
        onDone: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        _serverActionBusy.value = true
        val nextAvatar = if (clearAvatar) "" else server.avatarUrl
        val nextBanner = if (clearBanner) "" else server.bannerUrl
        when (val res = communityRepo.updateCommunityServerMedia(server.id, nextAvatar, nextBanner)) {
            is AppResult.Success -> {
                if (clearAvatar && server.avatarUrl.isNotBlank()) {
                    communityRepo.deleteServerMedia(server.id, server.avatarUrl)
                }
                if (clearBanner && server.bannerUrl.isNotBlank()) {
                    communityRepo.deleteServerMedia(server.id, server.bannerUrl)
                }
                _servers.value = _servers.value.map {
                    if (it.id == server.id) it.copy(avatarUrl = nextAvatar, bannerUrl = nextBanner) else it
                }
                _serverActionMessage.value = "Sunucu görseli kaldırıldı."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _serverActionBusy.value = false
    }

    fun deleteServerAsAdmin(server: CommunityServer, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _serverActionBusy.value = true
        when (val res = communityRepo.deleteCommunityServer(server.id)) {
            is AppResult.Success -> {
                _servers.value = _servers.value.filterNot { it.id == server.id }
                _serverActionMessage.value = "\"${server.name}\" sunucusu silindi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _serverActionBusy.value = false
    }

    fun createServerCategoryAsAdmin(serverId: String, name: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _serverActionBusy.value = true
        when (val res = communityRepo.createServerCategory(serverId, name)) {
            is AppResult.Success -> {
                _serverActionMessage.value = "Kategori oluşturuldu."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _serverActionBusy.value = false
    }

    fun deleteServerCategoryAsAdmin(serverId: String, categoryId: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _serverActionBusy.value = true
        when (val res = communityRepo.deleteServerCategory(serverId, categoryId)) {
            is AppResult.Success -> {
                _serverActionMessage.value = "Kategori silindi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _serverActionBusy.value = false
    }

    fun createServerChannelAsAdmin(serverId: String, categoryId: String, name: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _serverActionBusy.value = true
        when (val res = communityRepo.createServerChannel(serverId, categoryId, name)) {
            is AppResult.Success -> {
                _serverActionMessage.value = "Kanal oluşturuldu."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _serverActionBusy.value = false
    }

    fun deleteServerChannelAsAdmin(serverId: String, channelId: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _serverActionBusy.value = true
        when (val res = communityRepo.deleteServerChannel(serverId, channelId)) {
            is AppResult.Success -> {
                _serverActionMessage.value = "Kanal silindi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _serverActionBusy.value = false
    }

    fun setServerMemberRoleAsAdmin(serverId: String, memberId: String, role: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _serverActionBusy.value = true
        when (val res = communityRepo.setServerMemberRole(serverId, memberId, role)) {
            is AppResult.Success -> {
                _serverActionMessage.value = "Sunucu üye rolü güncellendi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _serverActionBusy.value = false
    }

    fun removeServerMemberAsAdmin(serverId: String, memberId: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _serverActionBusy.value = true
        when (val res = communityRepo.removeServerMember(serverId, memberId)) {
            is AppResult.Success -> {
                _serverActionMessage.value = "Üye sunucudan çıkarıldı."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _serverActionBusy.value = false
    }

    fun banServerMemberAsAdmin(serverId: String, memberId: String, reason: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _serverActionBusy.value = true
        when (val res = communityRepo.banServerMember(serverId, memberId, reason)) {
            is AppResult.Success -> {
                _serverActionMessage.value = "Üye sunucudan yasaklandı."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _serverActionBusy.value = false
    }

    fun unbanServerMemberAsAdmin(serverId: String, memberId: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _serverActionBusy.value = true
        when (val res = communityRepo.unbanServerMember(serverId, memberId)) {
            is AppResult.Success -> {
                _serverActionMessage.value = "Sunucu yasağı kaldırıldı."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _serverActionBusy.value = false
    }

    fun deleteServerMessageAsAdmin(serverId: String, messageId: String, channelId: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _serverActionBusy.value = true
        when (val res = chatRepo.deleteServerMessage(serverId, messageId, channelId)) {
            is AppResult.Success -> {
                _serverActionMessage.value = "Kanal mesajı silindi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _serverActionBusy.value = false
    }

    fun toggleOwnerServerCreationPermission(uid: String, disabled: Boolean, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val member = _members.value.firstOrNull { it.uid == uid } ?: return@launch
        val updatedMod = member.moderation.copy(
            serverCreationDisabled = disabled,
            serverCreationDisabledUntil = 0L
        )
        when (val res = repo.updateModeration(uid, updatedMod)) {
            is AppResult.Success -> {
                _members.value = _members.value.map { if (it.uid == uid) it.copy(moderation = updatedMod) else it }
                _serverActionMessage.value = if (disabled) {
                    "${member.displayName} için sunucu oluşturma kısıtlandı."
                } else {
                    "${member.displayName} için sunucu oluşturma yetkisi açıldı."
                }
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
    }

    fun toggleUserServerMessagingPermission(uid: String, disabled: Boolean, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val member = _members.value.firstOrNull { it.uid == uid } ?: return@launch
        val updatedMod = member.moderation.copy(
            serverMessagingDisabled = disabled,
            serverMessagingDisabledUntil = 0L
        )
        when (val res = repo.updateModeration(uid, updatedMod)) {
            is AppResult.Success -> {
                _members.value = _members.value.map { if (it.uid == uid) it.copy(moderation = updatedMod) else it }
                val msg = if (disabled) {
                    "${member.displayName} için sunucu/genel chat mesajları kısıtlandı."
                } else {
                    "${member.displayName} için sunucu/genel chat mesaj yetkisi açıldı."
                }
                _serverActionMessage.value = msg
                _globalChatActionMessage.value = msg
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
    }

    fun sendServerModerationNotification(
        recipientUid: String,
        actorUid: String,
        title: String,
        body: String,
        serverId: String = "",
        onDone: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        val res = notificationRepo.create(
            AppNotification(
                recipientId = recipientUid,
                actorId = actorUid,
                actorName = "Libra Yönetim",
                type = "ADMIN_FEEDBACK",
                title = title.trim().ifBlank { "Sunucu Yönetimi Bildirimi" },
                body = body.trim(),
                referenceId = serverId,
                createdAt = System.currentTimeMillis()
            )
        )
        if (res is AppResult.Success) {
            _serverActionMessage.value = "Sunucu sahibine bildirim gönderildi."
            onDone(true)
        } else {
            _error.value = (res as AppResult.Error).error.message
            onDone(false)
        }
    }

    // --- Global Chat Administration & Moderation ---

    fun observeGlobalMessages() {
        globalMessagesJob?.cancel()
        _globalMessagesLoading.value = true
        globalMessagesJob = viewModelScope.launch {
            chatRepo.observeGlobalMessages(250).collect { res ->
                when (res) {
                    is AppResult.Success -> _globalMessages.value = res.data
                    is AppResult.Error -> _error.value = res.error.message
                }
                _globalMessagesLoading.value = false
            }
        }
    }

    fun clearGlobalChatActionMessage() {
        _globalChatActionMessage.value = null
    }

    fun deleteGlobalMessageAsAdmin(messageId: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _globalChatActionBusy.value = true
        when (val res = chatRepo.deleteGlobalMessage(messageId)) {
            is AppResult.Success -> {
                _globalMessages.value = _globalMessages.value.filterNot { it.id == messageId }
                _globalChatActionMessage.value = "Mesaj genel chatten silindi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _globalChatActionBusy.value = false
    }

    fun batchDeleteGlobalMessages(messageIds: Set<String>, onDone: (Int) -> Unit = {}) = viewModelScope.launch {
        if (messageIds.isEmpty()) return@launch
        _globalChatActionBusy.value = true
        var deletedCount = 0
        var lastError: String? = null
        val targets = _globalMessages.value.filter { it.id in messageIds }
        targets.forEach { msg ->
            when (val res = chatRepo.deleteGlobalMessage(msg.id)) {
                is AppResult.Success -> deletedCount++
                is AppResult.Error -> lastError = res.error.message
            }
        }
        if (deletedCount > 0) {
            val removedIds = targets.map { it.id }.toSet()
            _globalMessages.value = _globalMessages.value.filterNot { it.id in removedIds }
            _globalChatActionMessage.value = "$deletedCount mesaj silindi."
        } else if (lastError != null) {
            _error.value = lastError
        }
        _globalChatActionBusy.value = false
        onDone(deletedCount)
    }

    fun sendGlobalAnnouncementAsAdmin(text: String, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        _globalChatActionBusy.value = true
        when (val res = chatRepo.sendGlobalMessage(text.trim())) {
            is AppResult.Success -> {
                _globalChatActionMessage.value = "Mesaj genel chate gönderildi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _globalChatActionBusy.value = false
    }

    fun toggleUserGlobalMessagingPermission(uid: String, disabled: Boolean, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val member = _members.value.firstOrNull { it.uid == uid } ?: return@launch
        val updatedMod = member.moderation.copy(
            messagingDisabled = disabled,
            messagingDisabledUntil = 0L
        )
        when (val res = repo.updateModeration(uid, updatedMod)) {
            is AppResult.Success -> {
                _members.value = _members.value.map { if (it.uid == uid) it.copy(moderation = updatedMod) else it }
                _globalChatActionMessage.value = if (disabled) {
                    "${member.displayName} için mesaj atma kısıtlandı."
                } else {
                    "${member.displayName} için mesaj atma yetkisi açıldı."
                }
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
    }

    fun sendGlobalChatModerationNotification(
        recipientUid: String,
        actorUid: String,
        title: String,
        body: String,
        onDone: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        val res = notificationRepo.create(
            AppNotification(
                recipientId = recipientUid,
                actorId = actorUid,
                actorName = "Libra Yönetim",
                type = "ADMIN_FEEDBACK",
                title = title.trim().ifBlank { "Genel Chat Bildirimi" },
                body = body.trim(),
                createdAt = System.currentTimeMillis()
            )
        )
        if (res is AppResult.Success) {
            _globalChatActionMessage.value = "Üyeye bildirim gönderildi."
            onDone(true)
        } else {
            _error.value = (res as AppResult.Error).error.message
            onDone(false)
        }
    }

    // --- Member Moderation & Roles ---

    fun saveModeration(uid: String, value: UserModeration, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val r = repo.updateModeration(uid, value)
        if (r is AppResult.Error) _error.value = r.error.message
        onDone(r is AppResult.Success)
    }

    fun saveProfile(profile: UserProfile, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val r = repo.updateMemberProfile(profile)
        if (r is AppResult.Error) _error.value = r.error.message
        onDone(r is AppResult.Success)
    }

    suspend fun loadAdminRole(uid: String): AdminRole? =
        (repo.getAdminRole(uid) as? AppResult.Success)?.data

    fun saveAdminRole(uid: String, role: AdminRole?, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val r = repo.setAdminRole(uid, role)
        if (r is AppResult.Error) _error.value = r.error.message
        onDone(r is AppResult.Success)
    }

    fun saveCosmetic(role: CosmeticRole, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val r = repo.saveCosmeticRole(role)
        if (r is AppResult.Error) _error.value = r.error.message
        onDone(r is AppResult.Success)
    }

    fun deleteCosmetic(id: String) = viewModelScope.launch {
        val r = repo.deleteCosmeticRole(id)
        if (r is AppResult.Error) _error.value = r.error.message
    }

    fun assignCosmetic(uid: String, roleId: String, assigned: Boolean, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val r = repo.assignCosmeticRole(uid, roleId, assigned)
        if (r is AppResult.Error) _error.value = r.error.message
        onDone(r is AppResult.Success)
    }

    // --- Admin Notification Broadcasts & Drafts ---

    fun resolveNotificationRecipients(
        categoryType: AdminAudienceCategory,
        categorySubId: String = "",
        customUserIds: Collection<String> = emptyList()
    ): List<UserProfile> = resolveAdminNotificationRecipients(
        members = _members.value,
        books = _books.value,
        posts = _posts.value,
        stories = _stories.value,
        adminRoleUserIds = _adminRoleUserIds.value,
        categoryType = categoryType,
        categorySubId = categorySubId,
        customUserIds = customUserIds
    )

    fun saveNotificationDraft(
        draft: AdminNotificationDraft,
        onDone: (AdminNotificationDraft?) -> Unit = {}
    ) = viewModelScope.launch {
        val cleanTitle = draft.title.trim()
        val cleanBody = draft.body.trim()
        if (cleanTitle.isBlank() || cleanBody.isBlank()) {
            _error.value = "Taslak için başlık ve bildirim mesajı zorunludur."
            onDone(null)
            return@launch
        }
        _notificationActionBusy.value = true
        val now = System.currentTimeMillis()
        val normalized = draft.copy(
            title = cleanTitle,
            body = cleanBody,
            tagLabel = draft.tagLabel.trim().ifBlank { "Duyuru" },
            createdAt = if (draft.createdAt > 0L) draft.createdAt else now,
            updatedAt = now
        )
        when (val res = repo.saveAdminNotificationDraft(normalized)) {
            is AppResult.Success -> {
                val saved = res.data
                if (saved.id.isNotBlank()) {
                    val current = _notificationDrafts.value.filterNot { it.id == saved.id }
                    _notificationDrafts.value = listOf(saved) + current
                }
                _notificationActionMessage.value = if (draft.id.isBlank()) {
                    "Bildirim taslağı kaydedildi."
                } else {
                    "Bildirim taslağı güncellendi."
                }
                onDone(saved)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(null)
            }
        }
        _notificationActionBusy.value = false
    }

    fun deleteNotificationDraft(
        draftId: String,
        onDone: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        if (draftId.isBlank()) {
            onDone(false)
            return@launch
        }
        _notificationActionBusy.value = true
        when (val res = repo.deleteAdminNotificationDraft(draftId)) {
            is AppResult.Success -> {
                _notificationDrafts.value = _notificationDrafts.value.filterNot { it.id == draftId }
                _notificationActionMessage.value = "Bildirim taslağı silindi."
                onDone(true)
            }
            is AppResult.Error -> {
                _error.value = res.error.message
                onDone(false)
            }
        }
        _notificationActionBusy.value = false
    }

    fun sendAdminNotificationBroadcast(
        title: String,
        body: String,
        categoryType: AdminAudienceCategory,
        categorySubId: String = "",
        customUserIds: List<String> = emptyList(),
        tagLabel: String = "Duyuru",
        senderUid: String,
        senderName: String = "Libra Yönetim",
        draftId: String = "",
        overrideAllMembers: Boolean = false,
        onDone: (Boolean, Int) -> Unit = { _, _ -> }
    ) = viewModelScope.launch {
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        if (cleanTitle.isBlank() || cleanBody.isBlank()) {
            _error.value = "Bildirim başlığı ve içeriği boş bırakılamaz."
            onDone(false, 0)
            return@launch
        }

        val effectiveAudience = if (overrideAllMembers) AdminAudienceCategory.ALL else categoryType
        val effectiveSubId = if (overrideAllMembers) "" else categorySubId
        val effectiveCustomIds = if (overrideAllMembers) emptyList() else customUserIds

        val recipients = resolveNotificationRecipients(
            categoryType = effectiveAudience,
            categorySubId = effectiveSubId,
            customUserIds = effectiveCustomIds
        )
        if (recipients.isEmpty()) {
            _error.value = "Seçilen kategoride bildirim gönderilecek uygun üye bulunamadı."
            onDone(false, 0)
            return@launch
        }

        _notificationActionBusy.value = true
        val now = System.currentTimeMillis()
        val effectiveActorUid = senderUid.trim().ifBlank { LIBRA_PRIMARY_ADMIN_UID }
        val effectiveSenderName = senderName.trim().ifBlank { "Libra Yönetim" }
        val effectiveTag = tagLabel.trim().ifBlank { "Duyuru" }
        val categoryLabel = resolveAdminAudienceLabel(
            categoryType = effectiveAudience,
            categorySubId = effectiveSubId,
            cosmeticRoles = _cosmeticRoles.value,
            customCount = recipients.size
        )

        val notifications = recipients.map { recipient ->
            AppNotification(
                recipientId = recipient.uid,
                actorId = effectiveActorUid,
                actorName = effectiveSenderName,
                type = LibraNotificationTypes.ADMIN_BROADCAST,
                title = cleanTitle,
                body = cleanBody,
                referenceId = draftId,
                category = effectiveTag,
                createdAt = now,
                read = false
            )
        }

        when (val sendRes = notificationRepo.createBatch(notifications)) {
            is AppResult.Success -> {
                val deliveredCount = sendRes.data
                val logEntry = AdminNotificationCampaignLog(
                    title = cleanTitle,
                    body = cleanBody,
                    categoryType = effectiveAudience.key,
                    categoryLabel = categoryLabel,
                    tagLabel = effectiveTag,
                    recipientCount = deliveredCount,
                    senderUid = effectiveActorUid,
                    senderName = effectiveSenderName,
                    draftId = draftId,
                    createdAt = now
                )
                repo.logAdminNotificationCampaign(logEntry)

                if (draftId.isNotBlank()) {
                    val existingDraft = _notificationDrafts.value.firstOrNull { it.id == draftId }
                    if (existingDraft != null) {
                        val updatedDraft = existingDraft.copy(
                            sendCount = existingDraft.sendCount + 1,
                            lastSentAt = now,
                            updatedAt = now
                        )
                        repo.saveAdminNotificationDraft(updatedDraft)
                        _notificationDrafts.value = _notificationDrafts.value.map {
                            if (it.id == draftId) updatedDraft else it
                        }
                    }
                }

                _notificationActionMessage.value =
                    "$deliveredCount üyeye bildirim gönderildi ($categoryLabel)."
                onDone(true, deliveredCount)
            }
            is AppResult.Error -> {
                _error.value = sendRes.error.message
                onDone(false, 0)
            }
        }
        _notificationActionBusy.value = false
    }

    fun sendAdminDraftNotification(
        draft: AdminNotificationDraft,
        senderUid: String,
        senderName: String = "Libra Yönetim",
        sendToEveryone: Boolean = false,
        onDone: (Boolean, Int) -> Unit = { _, _ -> }
    ) {
        sendAdminNotificationBroadcast(
            title = draft.title,
            body = draft.body,
            categoryType = AdminAudienceCategory.fromKey(draft.categoryType),
            categorySubId = draft.categorySubId,
            customUserIds = draft.customUserIds,
            tagLabel = draft.tagLabel,
            senderUid = senderUid,
            senderName = senderName,
            draftId = draft.id,
            overrideAllMembers = sendToEveryone,
            onDone = onDone
        )
    }
}