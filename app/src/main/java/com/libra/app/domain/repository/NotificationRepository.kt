package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(uid: String, limit: Long = 50): Flow<AppResult<List<AppNotification>>>
    suspend fun create(notification: AppNotification): AppResult<Unit>
    suspend fun markRead(notificationId: String): AppResult<Unit>
    suspend fun markAllRead(uid: String): AppResult<Unit>

    suspend fun createBatch(notifications: List<AppNotification>): AppResult<Int> {
        var sentCount = 0
        var lastError: AppResult.Error? = null
        notifications.forEach { item ->
            when (val res = create(item)) {
                is AppResult.Success -> sentCount++
                is AppResult.Error -> lastError = res
            }
        }
        return if (sentCount == 0 && notifications.isNotEmpty() && lastError != null) {
            lastError!!
        } else {
            AppResult.Success(sentCount)
        }
    }

    suspend fun fetchUnreadNotifications(uid: String, limit: Long = 25): AppResult<List<AppNotification>> =
        AppResult.Success(emptyList())
}
