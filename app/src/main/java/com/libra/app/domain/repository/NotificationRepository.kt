package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(uid: String, limit: Long = 50): Flow<AppResult<List<AppNotification>>>
    suspend fun create(notification: AppNotification): AppResult<Unit>
    suspend fun markRead(notificationId: String): AppResult<Unit>
    suspend fun markAllRead(uid: String): AppResult<Unit>
}
