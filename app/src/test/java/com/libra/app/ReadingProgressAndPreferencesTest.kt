package com.libra.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.ReadingProgress
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.feature.reader.ReaderPreferences
import com.libra.app.feature.reader.ReaderPreferencesStore
import com.libra.app.feature.reader.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReadingProgressAndPreferencesTest {

    @Test
    fun readerPreferences_defaults_and_clamping() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = ReaderPreferencesStore(context)

        val defaults = store.read()
        assertEquals(ReaderTheme.LIGHT, defaults.theme)
        assertEquals(19f, defaults.fontSizeSp)
        assertEquals(1.55f, defaults.lineSpacing)
        assertEquals(720, defaults.contentWidthDp)
        assertTrue(defaults.showProgress)

        // Test clamping of out-of-bound values
        store.save(
            ReaderPreferences(
                theme = ReaderTheme.DARK,
                fontSizeSp = 50f,      // max is 30f
                lineSpacing = 5.0f,    // max is 2.1f
                contentWidthDp = 2000, // max is 900
                showProgress = false
            )
        )

        val clamped = store.read()
        assertEquals(ReaderTheme.DARK, clamped.theme)
        assertEquals(30f, clamped.fontSizeSp)
        assertEquals(2.1f, clamped.lineSpacing)
        assertEquals(900, clamped.contentWidthDp)
        assertEquals(false, clamped.showProgress)
    }

    @Test
    fun readingProgress_save_and_restore_via_store() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = ReaderPreferencesStore(context)

        val progress = ReadingProgress(
            userId = "user_123",
            bookId = "book_456",
            chapterId = "ch_789",
            chapterNumber = 3,
            progressPercent = 65,
            position = 1420,
            updatedAt = 1700000000000L
        )

        store.saveProgress(progress)

        val restored = store.readProgress("user_123", "book_456")
        assertNotNull(restored)
        assertEquals("user_123", restored!!.userId)
        assertEquals("book_456", restored.bookId)
        assertEquals("ch_789", restored.chapterId)
        assertEquals(3, restored.chapterNumber)
        assertEquals(65, restored.progressPercent)
        assertEquals(1420, restored.position)
        assertEquals(1700000000000L, restored.updatedAt)

        // Reading non-existent progress returns null
        val nonExistent = store.readProgress("user_unknown", "book_456")
        assertNull(nonExistent)
    }

    @Test
    fun userShelfItem_progress_and_shelf_properties() {
        val book = Book(id = "book_1", title = "Test Book", authorName = "Author")
        val item = UserShelfItem(
            id = "shelf_1",
            userId = "user_1",
            book = book,
            shelfType = ShelfType.READING,
            progressPercent = 45,
            addedAt = 1000L
        )

        assertEquals("book_1", item.book.id)
        assertEquals(ShelfType.READING, item.shelfType)
        assertEquals(45, item.progressPercent)
        assertEquals("Okuduklarım", item.shelfType.titleTr)
    }
}
