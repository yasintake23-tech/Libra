package com.libra.app.feature.write.create

import android.content.Context
import android.content.SharedPreferences
import com.libra.app.domain.model.BookCategory
import org.json.JSONArray
import org.json.JSONObject

data class DraftChapter(
    val id: String = "",
    val chapterNumber: Int = 1,
    val title: String = "",
    val content: String = ""
) {
    val wordCount: Int
        get() = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
}

data class CreateBookDraft(
    val title: String = "",
    val description: String = "",
    val discoverySummary: String = "",
    val categoryName: String = BookCategory.FICTION.name,
    val coverUriString: String? = null,
    val uploadedCoverUrl: String = "",
    val chapters: List<DraftChapter> = emptyList(),
    val tags: List<String> = emptyList(),
    val language: String = "tr",
    val genre: String = "",
    val subGenre: String = "",
    val targetAudience: String = "ALL",
    val contentWarnings: List<String> = emptyList(),
    val isSeries: Boolean = false,
    val seriesName: String = "",
    val seriesOrder: Int = 1,
    val visibility: String = "PUBLIC",
    val currentStep: Int = 1,
    val lastSavedAt: Long = System.currentTimeMillis()
) {
    fun isNotEmpty(): Boolean =
        title.isNotBlank() ||
            description.isNotBlank() ||
            discoverySummary.isNotBlank() ||
            chapters.any { it.title.isNotBlank() || it.content.isNotBlank() } ||
            tags.isNotEmpty() ||
            genre.isNotBlank() ||
            subGenre.isNotBlank() ||
            seriesName.isNotBlank()
}

interface BookDraftStorage {
    fun loadDraft(): CreateBookDraft?
    fun saveDraft(draft: CreateBookDraft)
    fun clearDraft()
    fun hasDraft(): Boolean
}

class SharedPreferencesBookDraftStorage(context: Context) : BookDraftStorage {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("libra_book_drafts", Context.MODE_PRIVATE)

    override fun loadDraft(): CreateBookDraft? {
        val jsonString = prefs.getString(KEY_CURRENT_DRAFT, null) ?: return null
        return runCatching {
            val json = JSONObject(jsonString)
            val chaptersArray = json.optJSONArray("chapters") ?: JSONArray()
            val chapters = mutableListOf<DraftChapter>()
            for (i in 0 until chaptersArray.length()) {
                val item = chaptersArray.getJSONObject(i)
                chapters.add(
                    DraftChapter(
                        id = item.optString("id", ""),
                        chapterNumber = item.optInt("chapterNumber", i + 1),
                        title = item.optString("title", ""),
                        content = item.optString("content", "")
                    )
                )
            }

            val tagsArray = json.optJSONArray("tags") ?: JSONArray()
            val tags = mutableListOf<String>()
            for (i in 0 until tagsArray.length()) {
                tags.add(tagsArray.getString(i))
            }

            val warningsArray = json.optJSONArray("contentWarnings") ?: JSONArray()
            val warnings = mutableListOf<String>()
            for (i in 0 until warningsArray.length()) {
                warnings.add(warningsArray.getString(i))
            }

            CreateBookDraft(
                title = json.optString("title", ""),
                description = json.optString("description", ""),
                discoverySummary = json.optString("discoverySummary", ""),
                categoryName = json.optString("categoryName", BookCategory.FICTION.name),
                coverUriString = if (json.has("coverUriString")) json.getString("coverUriString") else null,
                uploadedCoverUrl = json.optString("uploadedCoverUrl", ""),
                chapters = chapters,
                tags = tags,
                language = json.optString("language", "tr"),
                genre = json.optString("genre", ""),
                subGenre = json.optString("subGenre", ""),
                targetAudience = json.optString("targetAudience", "ALL"),
                contentWarnings = warnings,
                isSeries = json.optBoolean("isSeries", false),
                seriesName = json.optString("seriesName", ""),
                seriesOrder = json.optInt("seriesOrder", 1),
                visibility = json.optString("visibility", "PUBLIC"),
                currentStep = json.optInt("currentStep", 1),
                lastSavedAt = json.optLong("lastSavedAt", 0L)
            )
        }.getOrNull()
    }

    override fun saveDraft(draft: CreateBookDraft) {
        runCatching {
            val json = JSONObject().apply {
                put("title", draft.title)
                put("description", draft.description)
                put("discoverySummary", draft.discoverySummary)
                put("categoryName", draft.categoryName)
                draft.coverUriString?.let { put("coverUriString", it) }
                put("uploadedCoverUrl", draft.uploadedCoverUrl)

                val chaptersArray = JSONArray()
                draft.chapters.forEach { chapter ->
                    val obj = JSONObject().apply {
                        put("id", chapter.id)
                        put("chapterNumber", chapter.chapterNumber)
                        put("title", chapter.title)
                        put("content", chapter.content)
                    }
                    chaptersArray.put(obj)
                }
                put("chapters", chaptersArray)

                val tagsArray = JSONArray()
                draft.tags.forEach { tagsArray.put(it) }
                put("tags", tagsArray)

                put("language", draft.language)
                put("genre", draft.genre)
                put("subGenre", draft.subGenre)
                put("targetAudience", draft.targetAudience)

                val warningsArray = JSONArray()
                draft.contentWarnings.forEach { warningsArray.put(it) }
                put("contentWarnings", warningsArray)

                put("isSeries", draft.isSeries)
                put("seriesName", draft.seriesName)
                put("seriesOrder", draft.seriesOrder)
                put("visibility", draft.visibility)
                put("currentStep", draft.currentStep)
                put("lastSavedAt", System.currentTimeMillis())
            }
            prefs.edit().putString(KEY_CURRENT_DRAFT, json.toString()).apply()
        }
    }

    override fun clearDraft() {
        prefs.edit().remove(KEY_CURRENT_DRAFT).apply()
    }

    override fun hasDraft(): Boolean = prefs.contains(KEY_CURRENT_DRAFT)

    companion object {
        private const val KEY_CURRENT_DRAFT = "current_book_draft"
    }
}

class InMemoryBookDraftStorage : BookDraftStorage {
    private var memoryDraft: CreateBookDraft? = null

    override fun loadDraft(): CreateBookDraft? = memoryDraft

    override fun saveDraft(draft: CreateBookDraft) {
        memoryDraft = draft
    }

    override fun clearDraft() {
        memoryDraft = null
    }

    override fun hasDraft(): Boolean = memoryDraft != null
}
