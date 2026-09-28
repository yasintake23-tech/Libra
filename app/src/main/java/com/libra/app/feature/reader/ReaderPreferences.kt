package com.libra.app.feature.reader

import android.content.Context
import android.content.SharedPreferences
import com.libra.app.domain.model.ReadingProgress

enum class ReaderTheme { LIGHT, DARK, SEPIA }

data class ReaderPreferences(
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val fontSizeSp: Float = 19f,
    val lineSpacing: Float = 1.55f,
    val contentWidthDp: Int = 720,
    val showProgress: Boolean = true
)

class ReaderPreferencesStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("libra_reader_preferences", Context.MODE_PRIVATE)

    fun read(): ReaderPreferences = ReaderPreferences(
        theme = runCatching {
            ReaderTheme.valueOf(prefs.getString("theme", ReaderTheme.LIGHT.name) ?: ReaderTheme.LIGHT.name)
        }.getOrDefault(ReaderTheme.LIGHT),
        fontSizeSp = prefs.getFloat("fontSizeSp", 19f).coerceIn(14f, 30f),
        lineSpacing = prefs.getFloat("lineSpacing", 1.55f).coerceIn(1.15f, 2.1f),
        contentWidthDp = prefs.getInt("contentWidthDp", 720).coerceIn(320, 900),
        showProgress = prefs.getBoolean("showProgress", true)
    )

    fun save(value: ReaderPreferences) {
        prefs.edit()
            .putString("theme", value.theme.name)
            .putFloat("fontSizeSp", value.fontSizeSp.coerceIn(14f, 30f))
            .putFloat("lineSpacing", value.lineSpacing.coerceIn(1.15f, 2.1f))
            .putInt("contentWidthDp", value.contentWidthDp.coerceIn(320, 900))
            .putBoolean("showProgress", value.showProgress)
            .apply()
    }

    fun readProgress(userId: String, bookId: String): ReadingProgress? {
        if (userId.isBlank() || bookId.isBlank()) return null
        val raw = prefs.getString("progress_${userId}_${bookId}", null) ?: return null
        val parts = raw.split("|")
        if (parts.size != 5) return null
        return runCatching {
            ReadingProgress(userId, bookId, parts[0], parts[1].toInt(), parts[2].toInt(), parts[3].toInt(), parts[4].toLong())
        }.getOrNull()
    }

    fun saveProgress(progress: ReadingProgress) {
        if (progress.userId.isBlank() || progress.bookId.isBlank()) return
        prefs.edit().putString(
            "progress_${progress.userId}_${progress.bookId}",
            listOf(progress.chapterId, progress.chapterNumber, progress.progressPercent, progress.position, progress.updatedAt).joinToString("|")
        ).apply()
    }
}
