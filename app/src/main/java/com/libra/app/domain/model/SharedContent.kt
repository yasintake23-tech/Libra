package com.libra.app.domain.model

/**
 * Structured content attached to a chat message.
 *
 * The original media/text stays in the message for compatibility; this object
 * lets every chat surface render a rich preview without scraping a URL.
 */
data class SharedContent(
    val type: String = "",
    val id: String = "",
    val title: String = "",
    val text: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val mediaUrl: String = "",
    val url: String = ""
)