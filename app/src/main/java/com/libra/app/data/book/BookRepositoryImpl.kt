package com.libra.app.data.book

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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Firebase Realtime Database implementation.
 *
 * Schema:
 * /books/{bookId}
 * /chapters/{bookId}/{chapterId}
 * /libraries/{uid}/{shelfType}/{bookId} -> { progressPercent, addedAt }
 */
class BookRepositoryImpl : BookRepository {

    private val database: FirebaseDatabase? by lazy {
        // Use the database URL from google-services.json. Hard-coding a
        // firebaseio.com host can point the app at a different region/database.
        runCatching { FirebaseDatabase.getInstance() }.getOrNull()
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
        val libraries = librariesRef?.child(userId)?.child(shelfType.name)
            ?: run { trySend(databaseError()); close(); return@callbackFlow }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                launch {
                    try {
                        val shelfSnapshots = snapshot.children.toList()
                        if (shelfSnapshots.isEmpty()) {
                            trySend(AppResult.Success(emptyList()))
                            return@launch
                        }

                        // /books is protected by a query-based read rule.
                        // Load published books with the exact permitted query instead
                        // of issuing direct /books/{id} reads for every shelf item.
                        val publishedSnapshot = booksRef
                            ?.orderByChild("status")
                            ?.equalTo(BookStatus.PUBLISHED.name)
                            ?.get()
                            ?.await()
                            ?: run {
                                trySend(databaseError())
                                return@launch
                            }

                        val booksById = publishedSnapshot.children
                            .mapNotNull { parseBook(it)?.let { book -> book.id to book } }
                            .toMap()

                        val shelfItems = shelfSnapshots.mapNotNull { shelfSnapshot ->
                            val bookId = shelfSnapshot.key ?: return@mapNotNull null
                            val book = booksById[bookId] ?: return@mapNotNull null
                            UserShelfItem(
                                id = userId + "_" + shelfType.name + "_" + bookId,
                                userId = userId,
                                book = book,
                                shelfType = shelfType,
                                progressPercent = shelfSnapshot.child("progressPercent").getValue(Int::class.java) ?: 0,
                                addedAt = shelfSnapshot.child("addedAt").getValue(Long::class.java) ?: 0L
                            )
                        }.sortedByDescending { it.addedAt }

                        trySend(AppResult.Success(shelfItems))
                    } catch (e: Exception) {
                        trySend(
                            AppResult.Error(
                                AppError.Database(
                                    "Kütüphane kitapları yüklenemedi: " + (e.localizedMessage ?: "Bilinmeyen Firebase hatası."),
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
        val newBook = book.copy(id = id, createdAt = now, updatedAt = now, status = BookStatus.DRAFT)
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
            val updated = book.copy(
                ownerId = authUid,
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
        val books = booksRef ?: return databaseError()
        val libraries = librariesRef ?: return databaseError()
        if (userId.isBlank()) return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        return try {
            if (!books.child(bookId).get().await().exists()) return AppResult.Error(AppError.NotFound("Kitap bulunamadı."))
            val now = System.currentTimeMillis()
            val updates = mutableMapOf<String, Any?>()
            ShelfType.values().forEach { shelf ->
                updates["/libraries/$userId/${shelf.name}/$bookId"] = if (shelf == shelfType) mapOf("progressPercent" to 0, "addedAt" to now) else null
            }
            db.reference.updateChildren(updates).await()
            AppResult.Success(Unit)
        } catch (e: Exception) { AppResult.Error(AppError.Database("Kitap kütüphaneye eklenemedi: ${e.localizedMessage}", e)) }
    }

    override fun getBookChapters(bookId: String): Flow<AppResult<List<Chapter>>> = callbackFlow {
        val ref = chaptersRef?.child(bookId) ?: run { trySend(databaseError()); close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chapters = snapshot.children.mapNotNull { child ->
                    runCatching { child.getValue(Chapter::class.java)?.let { it.copy(id = child.key ?: it.id, bookId = bookId) } }.getOrNull()
                }.sortedBy { it.chapterNumber }
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
        val saved = chapter.copy(id = id, wordCount = wordCount, updatedAt = now, createdAt = if (chapter.createdAt == 0L) now else chapter.createdAt)
        return try { chapters.child(saved.bookId).child(saved.id).setValue(saved).await(); AppResult.Success(saved) }
        catch (e: Exception) { AppResult.Error(AppError.Database("Bölüm kaydedilemedi: ${e.localizedMessage}", e)) }
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
            if (!booksRef!!.child(progress.bookId).get().await().exists()) return AppResult.Error(AppError.NotFound("Kitap bulunamadı."))
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

    private fun parseBook(snapshot: DataSnapshot): Book? = if (!snapshot.exists()) null else runCatching {
        snapshot.getValue(Book::class.java)?.let { it.copy(id = snapshot.key ?: it.id) }
    }.getOrNull()
}
