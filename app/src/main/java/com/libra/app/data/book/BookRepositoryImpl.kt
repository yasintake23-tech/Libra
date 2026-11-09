package com.libra.app.data.book

import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.BookComment
import com.libra.app.domain.model.BookEngagement
import com.libra.app.domain.model.ReadingProgress
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Firebase Realtime Database implementation.
 *
 * Schema:
 * /books/{bookId}
 * /chapters/{bookId}/{chapterId}
 * /libraries/{uid}/{shelfType}/{bookId} -> { progressPercent, addedAt, updatedAt }
 */
class BookRepositoryImpl : BookRepository {

    private val database: FirebaseDatabase? by lazy {
        // This project uses the europe-west1 regional RTDB. Firebase's
        // Android SDK requires an explicit database URL for non-us-central1
        // instances. Read it from google-services.json via FirebaseOptions.
        runCatching {
            val databaseUrl = FirebaseApp.getInstance().options.databaseUrl
            if (databaseUrl.isNullOrBlank()) {
                FirebaseDatabase.getInstance("https://libra-3bfb9-default-rtdb.europe-west1.firebasedatabase.app")
            } else {
                FirebaseDatabase.getInstance(databaseUrl)
            }
        }.getOrNull()
    }

    private val booksRef: DatabaseReference? by lazy {
        database?.getReference("books")
    }

    private val chaptersRef: DatabaseReference? by lazy {
        database?.getReference("chapters")
    }

    private val librariesRef: DatabaseReference? by lazy {
        database?.getReference("libraries")
    }

    private suspend fun DatabaseReference.runTransactionAwait(handler: Transaction.Handler) = suspendCancellableCoroutine<DataSnapshot> { continuation ->
        runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result = handler.doTransaction(currentData)
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (continuation.isCompleted) return
                when {
                    error != null -> continuation.resumeWithException(error.toException())
                    committed && currentData != null -> continuation.resume(currentData)
                    else -> continuation.resumeWithException(IllegalStateException("Firebase transaction tamamlanamadı."))
                }
            }
        })
        continuation.invokeOnCancellation { }
    }

    private fun databaseError(): AppResult.Error =
        AppResult.Error(
            AppError.Database(
                "Realtime Database yapılandırması bulunamadı. Firebase'den güncel google-services.json dosyasını ekleyin."
            )
        )

    override fun getFeaturedBooks(): Flow<AppResult<List<Book>>> = observePublishedBooks { books ->
        books.sortedWith(compareByDescending<Book> { it.rating }.thenByDescending { it.readCount }).take(20)
    }

    override fun getRecentBooks(): Flow<AppResult<List<Book>>> = observePublishedBooks { books ->
        books.sortedByDescending { it.createdAt }.take(50)
    }

    override fun getBooksByCategory(category: BookCategory): Flow<AppResult<List<Book>>> = observePublishedBooks { books ->
        books.filter { it.category == category }.sortedByDescending { it.createdAt }.take(50)
    }

    override fun getBookById(bookId: String): Flow<AppResult<Book?>> = callbackFlow {
        val ref = booksRef?.child(bookId) ?: run { trySend(databaseError()); close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(AppResult.Success(parseBook(snapshot)))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(AppResult.Error(AppError.Database(error.message, error.toException())))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun getUserLibrary(userId: String, shelfType: ShelfType): Flow<AppResult<List<UserShelfItem>>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(AppResult.Error(AppError.Auth("Kullanıcı kimliği boş olamaz.")))
            close()
            return@callbackFlow
        }

        val libraries = librariesRef?.child(userId)?.child(shelfType.name)
            ?: run { trySend(databaseError()); close(); return@callbackFlow }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                launch(Dispatchers.IO) {
                    try {
                        val shelfSnapshots = snapshot.children.toList()
                        if (shelfSnapshots.isEmpty()) {
                            trySend(AppResult.Success(emptyList()))
                            return@launch
                        }

                        // Concurrently fetch each referenced book with per-book error isolation.
                        // Missing, deleted, or unpublished books will not crash the shelf.
                        val deferredItems = shelfSnapshots.map { shelfSnapshot ->
                            async {
                                val bookId = shelfSnapshot.key?.trim() ?: return@async null
                                if (bookId.isBlank()) return@async null

                                val progressPercent = shelfSnapshot.child("progressPercent").getValue(Int::class.java) ?: 0
                                val addedAt = shelfSnapshot.child("addedAt").getValue(Long::class.java) ?: 0L
                                val updatedAt = shelfSnapshot.child("updatedAt").getValue(Long::class.java) ?: addedAt

                                val book = runCatching {
                                    val snap = withTimeoutOrNull(4_000L) {
                                        booksRef?.child(bookId)?.get()?.await()
                                    }
                                    snap?.let(::parseBook)
                                }.getOrNull()

                                if (book == null || (book.status != BookStatus.PUBLISHED && book.ownerId != userId)) {
                                    null
                                } else {
                                    UserShelfItem(
                                        id = "${userId}_${shelfType.name}_${bookId}",
                                        userId = userId,
                                        book = book,
                                        shelfType = shelfType,
                                        progressPercent = progressPercent.coerceIn(0, 100),
                                        addedAt = addedAt,
                                        updatedAt = updatedAt
                                    )
                                }
                            }
                        }

                        val shelfItems = deferredItems.awaitAll()
                            .filterNotNull()
                            .distinctBy { it.book.id }
                            .sortedByDescending { if (it.updatedAt > 0) it.updatedAt else it.addedAt }

                        trySend(AppResult.Success(shelfItems))
                    } catch (e: Exception) {
                        trySend(
                            AppResult.Error(
                                AppError.Database(
                                    "Kütüphane kitapları yüklenemedi: " +
                                        (e.localizedMessage ?: "Bilinmeyen Firebase hatası."),
                                    e
                                )
                            )
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(
                    AppResult.Error(
                        AppError.Database(
                            "Kütüphane Firebase okuması reddedildi: " + error.message,
                            error.toException()
                        )
                    )
                )
            }
        }

        libraries.addValueEventListener(listener)
        awaitClose { libraries.removeEventListener(listener) }
    }
    override fun getUserWrittenBooks(userId: String): Flow<AppResult<List<Book>>> = callbackFlow {
        val ref = booksRef ?: run { trySend(databaseError()); close(); return@callbackFlow }
        val query = ref.orderByChild("ownerId").equalTo(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(AppResult.Success(snapshot.children.mapNotNull(::parseBook).sortedByDescending { it.updatedAt }))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(AppResult.Error(AppError.Database(error.message, error.toException())))
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun createBook(book: Book): AppResult<Book> {
        val books = booksRef ?: return databaseError()
        if (book.ownerId.isBlank()) return AppResult.Error(AppError.Validation("Kitap sahibi belirlenemedi."))
        if (book.title.isBlank()) return AppResult.Error(AppError.Validation("Kitap başlığı boş olamaz."))
        val now = System.currentTimeMillis()
        val id = book.id.ifBlank { books.push().key ?: return AppResult.Error(AppError.Database("Kitap kimliği oluşturulamadı.")) }
        val finalCover = book.coverImageUrl.ifBlank { book.coverUrl }
        val newBook = book.copy(
            id = id,
            coverImageUrl = finalCover,
            coverUrl = finalCover,
            createdAt = now,
            updatedAt = now,
            status = BookStatus.DRAFT
        )
        return try { books.child(id).setValue(newBook).await(); AppResult.Success(newBook) }
        catch (e: Exception) { AppResult.Error(AppError.Database("Kitap oluşturulamadı: ${e.localizedMessage}", e)) }
    }

    override suspend fun updateBook(book: Book): AppResult<Book> {
        val books = booksRef ?: return databaseError()
        if (book.id.isBlank()) return AppResult.Error(AppError.Validation("Kitap kimliği boş olamaz."))

        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        if (book.ownerId != authUid) {
            return AppResult.Error(AppError.Auth("Bu kitabı güncelleme yetkin yok."))
        }

        return try {
            val finalCover = book.coverImageUrl.ifBlank { book.coverUrl }
            val updated = book.copy(
                ownerId = authUid,
                coverImageUrl = finalCover,
                coverUrl = finalCover,
                updatedAt = System.currentTimeMillis()
            )
            books.child(book.id).setValue(updated).await()
            AppResult.Success(updated)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kitap güncellenemedi: ${e.localizedMessage}", e))
        }
    }

    override suspend fun deleteBook(bookId: String): AppResult<Unit> {
        val db = database ?: return databaseError()
        return try {
            db.reference.updateChildren(
                mapOf("/books/$bookId" to null, "/chapters/$bookId" to null)
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kitap silinemedi: " + e.localizedMessage, e))
        }
    }

    override suspend fun addBookToShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> {
        val db = database ?: return databaseError()
        if (userId.isBlank()) return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        if (bookId.isBlank()) return AppResult.Error(AppError.Validation("Kitap kimliği boş olamaz."))
        return try {
            val now = System.currentTimeMillis()
            val shelfRef = db.reference.child("libraries").child(userId).child(shelfType.name).child(bookId)
            val data = if (shelfType == ShelfType.READING) {
                val existing = shelfRef.get().await()
                val existingProgress = existing.child("progressPercent").getValue(Int::class.java) ?: 0
                val existingChapterId = existing.child("chapterId").getValue(String::class.java) ?: ""
                val existingChapterNumber = existing.child("chapterNumber").getValue(Int::class.java) ?: 0
                val existingPosition = existing.child("position").getValue(Int::class.java) ?: 0
                val existingAddedAt = existing.child("addedAt").getValue(Long::class.java) ?: now
                mapOf(
                    "progressPercent" to existingProgress,
                    "chapterId" to existingChapterId,
                    "chapterNumber" to existingChapterNumber,
                    "position" to existingPosition,
                    "updatedAt" to now,
                    "addedAt" to existingAddedAt
                )
            } else {
                val existingAddedAt = shelfRef.child("addedAt").get().await().getValue(Long::class.java) ?: now
                mapOf("progressPercent" to 0, "addedAt" to existingAddedAt)
            }
            shelfRef.setValue(data).await()
            AppResult.Success(Unit)
        } catch (e: Exception) { AppResult.Error(AppError.Database("Kitap kütüphaneye eklenemedi: ${e.localizedMessage}", e)) }
    }

    override suspend fun removeBookFromShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> {
        val db = database ?: return databaseError()
        if (userId.isBlank()) return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        if (bookId.isBlank()) return AppResult.Error(AppError.Validation("Kitap kimliği boş olamaz."))
        return try {
            db.reference.child("libraries").child(userId).child(shelfType.name).child(bookId).removeValue().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kitap kütüphaneden kaldırılamadı: ${e.localizedMessage}", e))
        }
    }

    override fun getBookChapters(bookId: String): Flow<AppResult<List<Chapter>>> = callbackFlow {
        val ref = chaptersRef?.child(bookId) ?: run { trySend(databaseError()); close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chapters = snapshot.children.mapNotNull { parseChapter(it, bookId) }
                    .sortedBy { it.chapterNumber }
                trySend(AppResult.Success(chapters))
            }
            override fun onCancelled(error: DatabaseError) { trySend(AppResult.Error(AppError.Database(error.message, error.toException()))) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun deleteChapter(bookId: String, chapterId: String): AppResult<Unit> {
        val ref = chaptersRef?.child(bookId)?.child(chapterId) ?: return databaseError()
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        return try {
            val owner = booksRef?.child(bookId)?.child("ownerId")?.get()?.await()?.getValue(String::class.java)
            if (owner != uid) return AppResult.Error(AppError.Auth("Bu bölümü silme yetkin yok."))
            ref.removeValue().await()
            syncBookChapterCount(bookId)
            AppResult.Success(Unit)
        } catch (e: Exception) { AppResult.Error(AppError.Database("Bölüm silinemedi: ${e.localizedMessage}", e)) }
    }

    override suspend fun saveChapter(chapter: Chapter): AppResult<Chapter> {
        val chapters = chaptersRef ?: return databaseError()
        if (chapter.bookId.isBlank()) return AppResult.Error(AppError.Validation("Bölüm bir kitaba bağlı olmalı."))
        if (chapter.title.isBlank()) return AppResult.Error(AppError.Validation("Bölüm başlığı boş olamaz."))
        val now = System.currentTimeMillis()
        val id = chapter.id.ifBlank { chapters.child(chapter.bookId).push().key ?: return AppResult.Error(AppError.Database("Bölüm kimliği oluşturulamadı.")) }
        val wordCount = chapter.content.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        val saved = chapter.copy(
            id = id,
            wordCount = wordCount,
            updatedAt = now,
            createdAt = if (chapter.createdAt == 0L) now else chapter.createdAt
        )
        return try {
            chapters.child(saved.bookId).child(saved.id).setValue(saved).await()
            syncBookChapterCount(saved.bookId)
            AppResult.Success(saved)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Bölüm kaydedilemedi: ${e.localizedMessage}", e))
        }
    }

    override suspend fun reorderChapters(bookId: String, chapters: List<Chapter>): AppResult<Unit> {
        val ref = chaptersRef?.child(bookId) ?: return databaseError()
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        return try {
            val owner = booksRef?.child(bookId)?.child("ownerId")?.get()?.await()?.getValue(String::class.java)
            if (owner != uid) return AppResult.Error(AppError.Auth("Bu işlemi yapma yetkin yok."))
            val updates = mutableMapOf<String, Any>()
            val now = System.currentTimeMillis()
            chapters.forEachIndexed { index, chapter ->
                val newNumber = index + 1
                updates["${chapter.id}/chapterNumber"] = newNumber
                updates["${chapter.id}/updatedAt"] = now
            }
            ref.updateChildren(updates).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Bölümler sıralanamadı: ${e.localizedMessage}", e))
        }
    }

    override suspend fun duplicateChapter(bookId: String, chapterId: String): AppResult<Chapter> {
        val ref = chaptersRef?.child(bookId) ?: return databaseError()
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        return try {
            val owner = booksRef?.child(bookId)?.child("ownerId")?.get()?.await()?.getValue(String::class.java)
            if (owner != uid) return AppResult.Error(AppError.Auth("Bu işlemi yapma yetkin yok."))
            val snapshot = ref.child(chapterId).get().await()
            val original = parseChapter(snapshot, bookId) ?: return AppResult.Error(AppError.NotFound("Kopyalanacak bölüm bulunamadı."))
            val allChaptersSnap = ref.get().await()
            val totalCount = allChaptersSnap.childrenCount.toInt()
            val newId = ref.push().key ?: return AppResult.Error(AppError.Database("Yeni bölüm kimliği oluşturulamadı."))
            val now = System.currentTimeMillis()
            val duplicate = original.copy(
                id = newId,
                title = "${original.title} (Kopya)",
                chapterNumber = totalCount + 1,
                isPublished = false,
                createdAt = now,
                updatedAt = now
            )
            ref.child(newId).setValue(duplicate).await()
            syncBookChapterCount(bookId)
            AppResult.Success(duplicate)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Bölüm kopyalanamadı: ${e.localizedMessage}", e))
        }
    }

    override suspend fun publishBook(bookId: String): AppResult<Book> {
        val bRef = booksRef?.child(bookId) ?: return databaseError()
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        return try {
            val bookSnap = bRef.get().await()
            val currentBook = parseBook(bookSnap) ?: return AppResult.Error(AppError.NotFound("Kitap bulunamadı."))
            if (currentBook.ownerId != uid) return AppResult.Error(AppError.Auth("Bu kitabı yayınlama yetkin yok."))

            val chapSnap = chaptersRef?.child(bookId)?.get()?.await()
            val chapters = chapSnap?.children?.mapNotNull { parseChapter(it, bookId) } ?: emptyList()
            if (chapters.isEmpty()) {
                return AppResult.Error(AppError.Validation("Yayınlamak için en az bir bölüm bulunmalıdır."))
            }

            val now = System.currentTimeMillis()
            val chapUpdates = mutableMapOf<String, Any>()
            chapters.forEach { ch ->
                chapUpdates["${ch.id}/isPublished"] = true
                chapUpdates["${ch.id}/publishedAt"] = if (ch.publishedAt == 0L) now else ch.publishedAt
                chapUpdates["${ch.id}/updatedAt"] = now
            }
            chaptersRef?.child(bookId)?.updateChildren(chapUpdates)?.await()

            val publishedBook = currentBook.copy(
                status = BookStatus.PUBLISHED,
                publishedAt = if (currentBook.publishedAt == 0L) now else currentBook.publishedAt,
                updatedAt = now,
                chapterCount = chapters.size
            )
            bRef.setValue(publishedBook).await()
            AppResult.Success(publishedBook)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kitap yayınlanamadı: ${e.localizedMessage}", e))
        }
    }

    override suspend fun unpublishBook(bookId: String): AppResult<Book> {
        val bRef = booksRef?.child(bookId) ?: return databaseError()
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        return try {
            val bookSnap = bRef.get().await()
            val currentBook = parseBook(bookSnap) ?: return AppResult.Error(AppError.NotFound("Kitap bulunamadı."))
            if (currentBook.ownerId != uid) return AppResult.Error(AppError.Auth("Bu kitabı düzenleme yetkin yok."))
            val now = System.currentTimeMillis()
            val draftBook = currentBook.copy(
                status = BookStatus.DRAFT,
                updatedAt = now
            )
            bRef.setValue(draftBook).await()
            AppResult.Success(draftBook)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kitap taslağa alınamadı: ${e.localizedMessage}", e))
        }
    }

    override suspend fun archiveChapter(bookId: String, chapterId: String): AppResult<Unit> {
        val ref = chaptersRef?.child(bookId)?.child(chapterId) ?: return databaseError()
        return try {
            ref.updateChildren(mapOf(
                "isArchived" to true,
                "isPublished" to false,
                "updatedAt" to System.currentTimeMillis()
            )).await()
            syncBookChapterCount(bookId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Bölüm arşivlenemedi: ${e.localizedMessage}", e))
        }
    }

    override suspend fun unarchiveChapter(bookId: String, chapterId: String): AppResult<Unit> {
        val ref = chaptersRef?.child(bookId)?.child(chapterId) ?: return databaseError()
        return try {
            ref.updateChildren(mapOf(
                "isArchived" to false,
                "updatedAt" to System.currentTimeMillis()
            )).await()
            syncBookChapterCount(bookId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Bölüm arşivden çıkarılamadı: ${e.localizedMessage}", e))
        }
    }

    private suspend fun syncBookChapterCount(bookId: String) {
        runCatching {
            val snap = chaptersRef?.child(bookId)?.get()?.await()
            val activeCount = snap?.children?.count { child ->
                val isArchived = child.child("isArchived").getValue(Boolean::class.java) ?: false
                !isArchived
            } ?: 0
            booksRef?.child(bookId)?.child("chapterCount")?.setValue(activeCount)?.await()
        }
    }

    private fun parseChapter(child: DataSnapshot, defaultBookId: String): Chapter? {
        if (!child.exists()) return null
        val key = child.key.orEmpty()
        val id = child.child("id").getValue(String::class.java)?.ifBlank { key } ?: key
        if (id.isBlank()) return null
        val bookId = child.child("bookId").getValue(String::class.java)?.ifBlank { defaultBookId } ?: defaultBookId

        val chapterNumber = (child.child("chapterNumber").value as? Number)?.toInt()
            ?: (child.child("chapterNumber").value as? String)?.toIntOrNull()
            ?: 1

        val title = child.child("title").getValue(String::class.java).orEmpty()
        val content = child.child("content").getValue(String::class.java).orEmpty()

        val rawWordCount = (child.child("wordCount").value as? Number)?.toInt()
            ?: (child.child("wordCount").value as? String)?.toIntOrNull()
            ?: 0
        val wordCount = if (rawWordCount > 0) rawWordCount else {
            content.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        }

        val rawPublished = child.child("isPublished").value
        val isPublished = when (rawPublished) {
            is Boolean -> rawPublished
            is String -> rawPublished.equals("true", ignoreCase = true)
            is Number -> rawPublished.toInt() != 0
            else -> false
        }

        val rawArchived = child.child("isArchived").value
        val isArchived = when (rawArchived) {
            is Boolean -> rawArchived
            is String -> rawArchived.equals("true", ignoreCase = true)
            is Number -> rawArchived.toInt() != 0
            else -> false
        }

        val publishedAt = (child.child("publishedAt").value as? Number)?.toLong() ?: 0L
        val createdAt = (child.child("createdAt").value as? Number)?.toLong() ?: 0L
        val updatedAt = (child.child("updatedAt").value as? Number)?.toLong() ?: createdAt

        return Chapter(
            id = id,
            bookId = bookId,
            chapterNumber = chapterNumber,
            title = title,
            content = content,
            wordCount = wordCount,
            isPublished = isPublished,
            isArchived = isArchived,
            publishedAt = publishedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    override suspend fun getBookEngagement(bookId: String, userId: String): AppResult<BookEngagement> {
        if (bookId.isBlank() || userId.isBlank()) return AppResult.Error(AppError.Validation("Kitap ve kullanıcı bilgisi gerekli."))
        return try {
            val bookRef = booksRef?.child(bookId) ?: return databaseError()
            if (!bookRef.get().await().exists()) return AppResult.Error(AppError.NotFound("Kitap bulunamadı."))
            AppResult.Success(
                BookEngagement(
                    bookId = bookId,
                    liked = bookRef.child("likes").child(userId).get().await().exists(),
                    saved = bookRef.child("saves").child(userId).get().await().exists()
                )
            )
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kitap etkileşimi yüklenemedi: ${e.localizedMessage}", e))
        }
    }

    override suspend fun toggleBookLike(bookId: String, userId: String): AppResult<Boolean> =
        toggleBookReaction(bookId, userId, "likes", "likesCount")

    override suspend fun toggleBookSave(bookId: String, userId: String): AppResult<Boolean> =
        toggleBookReaction(bookId, userId, "saves", "savesCount")

    private suspend fun toggleBookReaction(bookId: String, userId: String, collection: String, counter: String): AppResult<Boolean> {
        if (bookId.isBlank() || userId.isBlank()) return AppResult.Error(AppError.Validation("Kitap ve kullanıcı bilgisi gerekli."))
        val bookRef = booksRef?.child(bookId) ?: return databaseError()
        return try {
            if (!bookRef.get().await().exists()) return AppResult.Error(AppError.NotFound("Kitap bulunamadı."))
            val memberRef = bookRef.child(collection).child(userId)
            val active = !memberRef.get().await().exists()
            memberRef.setValue(if (active) true else null).await()
            bookRef.child(counter).runTransactionAwait(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val current = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = (current + if (active) 1 else -1).coerceAtLeast(0)
                    return Transaction.success(currentData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
            })
            AppResult.Success(active)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kitap etkileşimi güncellenemedi: ${e.localizedMessage}", e))
        }
    }

    override fun getBookComments(bookId: String): Flow<AppResult<List<BookComment>>> = callbackFlow {
        val ref = booksRef?.child(bookId)?.child("comments") ?: run { trySend(databaseError()); close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = snapshot.children.mapNotNull { child ->
                    runCatching { child.getValue(BookComment::class.java)?.copy(id = child.key ?: "") }.getOrNull()
                }.sortedByDescending { it.createdAt }
                trySend(AppResult.Success(comments))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(AppResult.Error(AppError.Database(error.message, error.toException())))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun addBookComment(comment: BookComment): AppResult<BookComment> {
        val ref = booksRef?.child(comment.bookId)?.child("comments") ?: return databaseError()
        val text = comment.text.trim()
        if (comment.bookId.isBlank() || comment.userId.isBlank()) return AppResult.Error(AppError.Validation("Yorum sahibi ve kitap bilgisi gerekli."))
        if (text.isBlank()) return AppResult.Error(AppError.Validation("Yorum boş olamaz."))
        if (text.length > 1000) return AppResult.Error(AppError.Validation("Yorum en fazla 1000 karakter olabilir."))
        return try {
            if (!booksRef!!.child(comment.bookId).get().await().exists()) return AppResult.Error(AppError.NotFound("Kitap bulunamadı."))
            val id = comment.id.ifBlank { ref.push().key ?: return AppResult.Error(AppError.Database("Yorum kimliği oluşturulamadı.")) }
            val saved = comment.copy(id = id, text = text, createdAt = if (comment.createdAt == 0L) System.currentTimeMillis() else comment.createdAt)
            ref.child(id).setValue(saved).await()
            booksRef!!.child(comment.bookId).child("commentsCount").runTransactionAwait(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    currentData.value = (currentData.getValue(Int::class.java) ?: 0) + 1
                    return Transaction.success(currentData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
            })
            AppResult.Success(saved)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Yorum eklenemedi: ${e.localizedMessage}", e))
        }
    }

    override suspend fun deleteBookComment(bookId: String, commentId: String): AppResult<Unit> {
        val ref = booksRef?.child(bookId)?.child("comments")?.child(commentId) ?: return databaseError()
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        return try {
            val comment = ref.get().await()
            if (!comment.exists()) return AppResult.Success(Unit)
            val author = comment.child("userId").getValue(String::class.java)
            val bookOwner = booksRef!!.child(bookId).child("ownerId").get().await().getValue(String::class.java)
            if (author != uid && bookOwner != uid) return AppResult.Error(AppError.Auth("Bu yorumu silme yetkin yok."))
            ref.removeValue().await()
            booksRef!!.child(bookId).child("commentsCount").runTransactionAwait(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    currentData.value = ((currentData.getValue(Int::class.java) ?: 0) - 1).coerceAtLeast(0)
                    return Transaction.success(currentData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
            })
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Yorum silinemedi: ${e.localizedMessage}", e))
        }
    }

    override suspend fun saveReadingProgress(progress: ReadingProgress): AppResult<ReadingProgress> {
        if (progress.userId.isBlank() || progress.bookId.isBlank()) return AppResult.Error(AppError.Validation("Okuma ilerlemesi için kullanıcı ve kitap gerekli."))
        val ref = librariesRef?.child(progress.userId)?.child(ShelfType.READING.name)?.child(progress.bookId) ?: return databaseError()
        return try {
            val now = System.currentTimeMillis()
            val existingAddedAt = ref.child("addedAt").get().await().getValue(Long::class.java) ?: now
            val saved = progress.copy(
                progressPercent = progress.progressPercent.coerceIn(0, 100),
                updatedAt = now
            )
            ref.updateChildren(mapOf(
                "progressPercent" to saved.progressPercent,
                "chapterId" to saved.chapterId,
                "chapterNumber" to saved.chapterNumber,
                "position" to saved.position,
                "updatedAt" to saved.updatedAt,
                "addedAt" to existingAddedAt
            )).await()
            AppResult.Success(saved.copy(updatedAt = now))
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Okuma ilerlemesi kaydedilemedi: ${e.localizedMessage}", e))
        }
    }

    override fun getReadingProgress(userId: String, bookId: String): Flow<AppResult<ReadingProgress?>> = callbackFlow {
        val ref = librariesRef?.child(userId)?.child(ShelfType.READING.name)?.child(bookId) ?: run { trySend(databaseError()); close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(AppResult.Success(null))
                    return
                }
                trySend(AppResult.Success(ReadingProgress(
                    userId = userId,
                    bookId = bookId,
                    chapterId = snapshot.child("chapterId").getValue(String::class.java) ?: "",
                    chapterNumber = snapshot.child("chapterNumber").getValue(Int::class.java) ?: 0,
                    progressPercent = snapshot.child("progressPercent").getValue(Int::class.java) ?: 0,
                    position = snapshot.child("position").getValue(Int::class.java) ?: 0,
                    updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L
                )))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(AppResult.Error(AppError.Database(error.message, error.toException())))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun observePublishedBooks(transform: (List<Book>) -> List<Book>): Flow<AppResult<List<Book>>> = callbackFlow {
        val ref = booksRef ?: run { trySend(databaseError()); close(); return@callbackFlow }
        val query = ref.orderByChild("status").equalTo(BookStatus.PUBLISHED.name)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try { trySend(AppResult.Success(transform(snapshot.children.mapNotNull(::parseBook)))) }
                catch (e: Exception) { trySend(AppResult.Error(AppError.Database("Kitap verisi çözümlenemedi.", e))) }
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(AppResult.Error(AppError.Database(error.message, error.toException())))
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    private fun observeBooks(transform: (List<Book>) -> List<Book>): Flow<AppResult<List<Book>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try { trySend(AppResult.Success(transform(snapshot.children.mapNotNull(::parseBook)))) }
                catch (e: Exception) { trySend(AppResult.Error(AppError.Database("Kitap verisi çözümlenemedi.", e))) }
            }
            override fun onCancelled(error: DatabaseError) { trySend(AppResult.Error(AppError.Database(error.message, error.toException()))) }
        }
        val ref = booksRef ?: run { trySend(databaseError()); close(); return@callbackFlow }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun parseBook(snapshot: DataSnapshot): Book? {
        if (!snapshot.exists()) return null
        val rawCover = snapshot.child("coverImageUrl").getValue(String::class.java)?.ifBlank { null }
            ?: snapshot.child("coverUrl").getValue(String::class.java)?.ifBlank { null }
            ?: snapshot.child("cover").getValue(String::class.java)?.ifBlank { null }
            ?: snapshot.child("coverPath").getValue(String::class.java).orEmpty()

        val parsed = runCatching {
            snapshot.getValue(Book::class.java)?.let { book ->
                val id = snapshot.key ?: book.id
                val finalCover = book.coverImageUrl.ifBlank { book.coverUrl.ifBlank { rawCover } }
                book.copy(id = id, coverImageUrl = finalCover, coverUrl = finalCover)
            }
        }.getOrNull()

        if (parsed != null) return parsed

        return runCatching {
            val key = snapshot.key.orEmpty()
            val id = snapshot.child("id").getValue(String::class.java)?.ifBlank { key } ?: key
            val ownerId = snapshot.child("ownerId").getValue(String::class.java).orEmpty()
            val authorName = snapshot.child("authorName").getValue(String::class.java).orEmpty()
            val title = snapshot.child("title").getValue(String::class.java).orEmpty()
            val description = snapshot.child("description").getValue(String::class.java).orEmpty()
            val discoverySummary = snapshot.child("discoverySummary").getValue(String::class.java).orEmpty()
            val coverImageUrl = rawCover

            val catName = snapshot.child("category").getValue(String::class.java).orEmpty()
            val category = BookCategory.entries.firstOrNull { it.name.equals(catName, ignoreCase = true) }
                ?: BookCategory.FICTION

            val statusName = snapshot.child("status").getValue(String::class.java).orEmpty()
            val status = BookStatus.entries.firstOrNull { it.name.equals(statusName, ignoreCase = true) }
                ?: BookStatus.DRAFT

            val rating = (snapshot.child("rating").value as? Number)?.toFloat() ?: 0f
            val ratingCount = (snapshot.child("ratingCount").value as? Number)?.toInt() ?: 0
            val readCount = (snapshot.child("readCount").value as? Number)?.toInt() ?: 0
            val likesCount = (snapshot.child("likesCount").value as? Number)?.toInt() ?: 0
            val savesCount = (snapshot.child("savesCount").value as? Number)?.toInt() ?: 0
            val commentsCount = (snapshot.child("commentsCount").value as? Number)?.toInt() ?: 0
            val chapterCount = (snapshot.child("chapterCount").value as? Number)?.toInt() ?: 0
            val createdAt = (snapshot.child("createdAt").value as? Number)?.toLong() ?: 0L
            val updatedAt = (snapshot.child("updatedAt").value as? Number)?.toLong() ?: createdAt
            val publishedAt = (snapshot.child("publishedAt").value as? Number)?.toLong() ?: 0L

            val tags = snapshot.child("tags").children.mapNotNull { it.getValue(String::class.java) }
            val language = snapshot.child("language").getValue(String::class.java) ?: "tr"
            val genre = snapshot.child("genre").getValue(String::class.java).orEmpty()
            val subGenre = snapshot.child("subGenre").getValue(String::class.java).orEmpty()
            val targetAudience = snapshot.child("targetAudience").getValue(String::class.java) ?: "ALL"
            val contentWarnings = snapshot.child("contentWarnings").children.mapNotNull { it.getValue(String::class.java) }
            val isSeries = snapshot.child("isSeries").getValue(Boolean::class.java) ?: false
            val seriesName = snapshot.child("seriesName").getValue(String::class.java).orEmpty()
            val seriesOrder = (snapshot.child("seriesOrder").value as? Number)?.toInt() ?: 0
            val visibility = snapshot.child("visibility").getValue(String::class.java) ?: "PUBLIC"

            Book(
                id = id,
                ownerId = ownerId,
                authorName = authorName,
                title = title,
                description = description,
                discoverySummary = discoverySummary,
                coverImageUrl = coverImageUrl,
                coverUrl = coverImageUrl,
                category = category,
                status = status,
                rating = rating,
                ratingCount = ratingCount,
                readCount = readCount,
                likesCount = likesCount,
                savesCount = savesCount,
                commentsCount = commentsCount,
                chapterCount = chapterCount,
                createdAt = createdAt,
                updatedAt = updatedAt,
                publishedAt = publishedAt,
                tags = tags,
                language = language,
                genre = genre,
                subGenre = subGenre,
                targetAudience = targetAudience,
                contentWarnings = contentWarnings,
                isSeries = isSeries,
                seriesName = seriesName,
                seriesOrder = seriesOrder,
                visibility = visibility
            )
        }.getOrNull()
    }
}
