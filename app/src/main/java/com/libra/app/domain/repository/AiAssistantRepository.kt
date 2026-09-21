package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Clean AI Service abstraction for future AI-powered writing, summarizing, and discovery.
 * Ensures AI capabilities are completely decoupled from UI code.
 */
interface AiAssistantRepository {
    fun generateBookSummary(title: String, draftOutline: String): Flow<AppResult<String>>
    fun suggestPlotIdeas(genre: String, premise: String): Flow<AppResult<List<String>>>
    fun improveWritingStyle(draftText: String): Flow<AppResult<String>>
}
