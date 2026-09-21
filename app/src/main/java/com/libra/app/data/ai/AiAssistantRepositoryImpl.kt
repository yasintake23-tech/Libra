package com.libra.app.data.ai

import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.repository.AiAssistantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Extension point for a future AI provider. It never pretends an AI request succeeded. */
class AiAssistantRepositoryImpl : AiAssistantRepository {
    override fun generateBookSummary(title: String, draftOutline: String): Flow<AppResult<String>> = flow {
        emit(AppResult.Error(AppError.AiService("AI yazma servisi henüz yapılandırılmadı.")))
    }

    override fun suggestPlotIdeas(genre: String, premise: String): Flow<AppResult<List<String>>> = flow {
        emit(AppResult.Error(AppError.AiService("AI yazma servisi henüz yapılandırılmadı.")))
    }

    override fun improveWritingStyle(draftText: String): Flow<AppResult<String>> = flow {
        emit(AppResult.Error(AppError.AiService("AI yazma servisi henüz yapılandırılmadı.")))
    }
}
