package com.libra.app.feature.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AdminRole
import com.libra.app.domain.model.AppNotification
import com.libra.app.domain.model.AppReleaseInfo
import com.libra.app.domain.model.AppUpdate
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.UserModeration
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.AdminRepository
import com.libra.app.domain.repository.AppReleaseStorageRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.NotificationRepository
import com.libra.app.domain.repository.UpdateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
            AdminBookAudienceFilter.GENERAL -> book.targetAudience.equals("ALL", ignoreCase = true) || book.targetAudience.isBlank()
            AdminBookAudienceFilter.YA -> book.targetAudience.equals("YA", ignoreCase = true)
            AdminBookAudienceFilter.ADULT -> book.targetAudience.equals("ADULT", ignoreCase = true)
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

class AdminViewModel(
    private val repo: AdminRepository,
    private val bookRepo: BookRepository,
    private val updateRepo: UpdateRepository,
    private val releaseStorageRepo: AppReleaseStorageRepository,
    private val notificationRepo: NotificationRepository
) : ViewModel() {

    constructor() : this(
        repo = ServiceLocator.adminRepository,
        bookRepo = ServiceLocator.bookRepository,
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
    private val _cosmeticRoles = MutableStateFlow<List<CosmeticRole>>(emptyList())
    val cosmeticRoles: StateFlow<List<CosmeticRole>> = _cosmeticRoles.asStateFlow()
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
}