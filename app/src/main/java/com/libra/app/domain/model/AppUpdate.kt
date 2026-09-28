package com.libra.app.domain.model

data class AppUpdate(
    val releaseId: String = "",
    val versionCode: Long = 0L,
    val versionName: String = "",
    val apkObjectKey: String = "",
    val apkSize: Long = 0L,
    val changelog: List<String> = emptyList(),
    val forceUpdate: Boolean = false,
    val createdAt: Long = 0L
)
