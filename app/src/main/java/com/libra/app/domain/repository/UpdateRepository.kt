package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AppUpdate
import kotlinx.coroutines.flow.Flow

interface UpdateRepository {
    fun observeActiveRelease(): Flow<AppResult<AppUpdate?>>
    suspend fun getActiveRelease(): AppResult<AppUpdate?>
    suspend fun publishRelease(release: AppUpdate): AppResult<Unit>
}
