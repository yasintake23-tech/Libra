package com.libra.app.domain.model

data class AppNotification(
    val id: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorUsername: String = "",
    val actorPhotoUrl: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val referenceId: String = "",
    val createdAt: Long = 0L,
    val read: Boolean = false
)
