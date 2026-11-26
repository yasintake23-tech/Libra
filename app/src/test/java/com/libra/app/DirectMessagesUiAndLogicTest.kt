package com.libra.app

import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.SharedContent
import com.libra.app.feature.messages.buildForwardSharedContent
import com.libra.app.feature.messages.computeDirectMessageGrouping
import com.libra.app.feature.messages.formatChatTimeHeader
import com.libra.app.feature.messages.formatConversationRelativeTime
import com.libra.app.feature.messages.formatMessageHour
import com.libra.app.feature.messages.formatReadableByteSize
import com.libra.app.feature.messages.isMessageSeenByPeer
import com.libra.app.feature.messages.resolveShareableMessageText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectMessagesUiAndLogicTest {

    // Fixed anchor: 2026-11-26 14:30:00 local-neutral epoch (test uses relative deltas on same day)
    private val baseMs = 1_795_703_400_000L

    @Test
    fun computeDirectMessageGrouping_groupsConsecutiveSameSenderAndShowsDividerOnGap() {
        val messages = listOf(
            DirectMessage(id = "m1", senderId = "peer_1", text = "Selam", createdAt = baseMs),
            DirectMessage(id = "m2", senderId = "peer_1", text = "Nasılsın?", createdAt = baseMs + 60_000L),
            DirectMessage(id = "m3", senderId = "me", text = "İyiyim!", createdAt = baseMs + 90_000L),
            DirectMessage(id = "m4", senderId = "me", text = "Uzun ara sonrası", createdAt = baseMs + 45 * 60_000L)
        )

        val g0 = computeDirectMessageGrouping(messages, 0, currentUid = "me", isGroup = true)
        assertTrue(g0.showTimestampDivider)
        assertTrue(g0.isFirstInGroup)
        assertFalse(g0.isLastInGroup)
        assertTrue(g0.showSenderName)
        assertFalse(g0.showAvatar)

        val g1 = computeDirectMessageGrouping(messages, 1, currentUid = "me", isGroup = true)
        assertFalse(g1.showTimestampDivider)
        assertFalse(g1.isFirstInGroup)
        assertTrue(g1.isLastInGroup)
        assertFalse(g1.showSenderName)
        assertTrue(g1.showAvatar)

        val g2 = computeDirectMessageGrouping(messages, 2, currentUid = "me", isGroup = true)
        assertFalse(g2.showTimestampDivider)
        assertTrue(g2.isFirstInGroup)
        assertTrue(g2.isLastInGroup)
        assertFalse(g2.showSenderName)
        assertFalse(g2.showAvatar)

        val g3 = computeDirectMessageGrouping(messages, 3, currentUid = "me", isGroup = true)
        assertTrue(g3.showTimestampDivider)
        assertTrue(g3.isFirstInGroup)
        assertTrue(g3.isLastInGroup)
    }

    @Test
    fun formatters_produceCleanTurkishLabelsAndByteSizes() {
        assertEquals("Şimdi", formatConversationRelativeTime(baseMs - 10_000L, nowMs = baseMs))
        assertEquals("5d", formatConversationRelativeTime(baseMs - 5 * 60_000L, nowMs = baseMs))
        assertEquals("Dün", formatConversationRelativeTime(baseMs - 24L * 60 * 60_000L, nowMs = baseMs))

        assertTrue(formatChatTimeHeader(baseMs, nowMs = baseMs).startsWith("Bugün "))
        assertTrue(formatChatTimeHeader(baseMs - 24L * 60 * 60_000L, nowMs = baseMs).startsWith("Dün "))
        assertTrue(formatMessageHour(baseMs).contains(":"))

        assertEquals("512 B", formatReadableByteSize(512))
        assertEquals("250 KB", formatReadableByteSize(250 * 1024))
        assertEquals("1.5 MB", formatReadableByteSize((1.5 * 1024 * 1024).toInt()))
    }

    @Test
    fun buildForwardSharedContent_andResolveShareableText_preserveMediaAndMetadata() {
        val photoMsg = DirectMessage(
            id = "dm_img_1",
            senderId = "peer_9",
            senderName = "Nisa",
            text = "Kitaplık köşem",
            mediaUrl = "users/peer_9/shelf.jpg",
            createdAt = baseMs
        )
        val forwarded = buildForwardSharedContent(photoMsg, fallbackAuthorName = "Nisa")
        assertEquals("dm_photo", forwarded.type)
        assertEquals("users/peer_9/shelf.jpg", forwarded.mediaUrl)
        assertEquals("Nisa tarafından iletildi", forwarded.title)
        assertEquals("Kitaplık köşem", forwarded.text)
        assertEquals("Kitaplık köşem", resolveShareableMessageText(photoMsg))

        val alreadyShared = SharedContent(
            type = "book",
            id = "bk_1",
            title = "Kürk Mantolu Madonna",
            text = "Kesin oku",
            url = "https://libra-3bfb9.web.app/share/book/bk_1"
        )
        val sharedMsg = DirectMessage(
            id = "dm_shared_1",
            senderId = "peer_9",
            text = "Harika bir kitap",
            sharedContent = alreadyShared
        )
        assertEquals(alreadyShared, buildForwardSharedContent(sharedMsg))
        val shareText = resolveShareableMessageText(sharedMsg)
        assertTrue(shareText.contains("Harika bir kitap"))
        assertTrue(shareText.contains("Kürk Mantolu Madonna"))
        assertTrue(shareText.contains("https://libra-3bfb9.web.app/share/book/bk_1"))
    }

    @Test
    fun isMessageSeenByPeer_respectsSenderOwnershipPendingStateAndTimestamps() {
        val sentMsg = DirectMessage(
            id = "srv_msg_1",
            senderId = "me",
            recipientId = "peer",
            text = "Selam",
            createdAt = baseMs
        )
        val pendingMsg = sentMsg.copy(id = "local-12345")
        val peerMsg = sentMsg.copy(senderId = "peer", recipientId = "me")

        assertTrue(isMessageSeenByPeer(sentMsg, peerLastReadAt = baseMs, currentUid = "me"))
        assertTrue(isMessageSeenByPeer(sentMsg, peerLastReadAt = baseMs + 5_000L, currentUid = "me"))
        assertFalse(isMessageSeenByPeer(sentMsg, peerLastReadAt = baseMs - 1L, currentUid = "me"))
        assertFalse(isMessageSeenByPeer(sentMsg, peerLastReadAt = 0L, currentUid = "me"))
        assertFalse(isMessageSeenByPeer(pendingMsg, peerLastReadAt = baseMs + 5_000L, currentUid = "me"))
        assertFalse(isMessageSeenByPeer(peerMsg, peerLastReadAt = baseMs + 5_000L, currentUid = "me"))
    }
}
