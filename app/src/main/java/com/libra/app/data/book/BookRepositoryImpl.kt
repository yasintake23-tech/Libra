package com.libra.app.data.book

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.BookRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
        val ref = librariesRef?.child(userId)?.child(shelfType.name) ?: run { trySend(databaseError()); close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                launch {
                    try {
                        val shelfItems = mutableListOf<UserShelfItem>()
                        for (shelfSnapshot in snapshot.children) {
                            val bookId = shelfSnapshot.key ?: continue
                            val book = parseBook(booksRef!!.child(bookId).get().await()) ?: continue
                            shelfItems += UserShelfItem(
                                id = "${userId}_${shelfType.name}_$bookId",
                                userId = userId,
                                book = book,
                                shelfType = shelfType,
                                progressPercent = shelfSnapshot.child("progressPercent").getValue(Int::class.java) ?: 0,
                                addedAt = shelfSnapshot.child("addedAt").getValue(Long::class.java) ?: 0L
                            )
                        }
                        trySend(AppResult.Success(shelfItems.sortedByDescending { it.addedAt }))
                    } catch (e: Exception) {
                        trySend(AppResult.Error(AppError.Database("Kütüphane yüklenemedi: ${e.localizedMessage}", e)))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(AppResult.Error(AppError.Database(error.message, error.toException())))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
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
