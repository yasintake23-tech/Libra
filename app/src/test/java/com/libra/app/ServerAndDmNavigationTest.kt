package com.libra.app

import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.GroupPermissions
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.model.countUnreadPrivateConversations
import com.libra.app.feature.messages.DmHubMode
import com.libra.app.feature.messages.computeUnreadDividerIndex
import com.libra.app.feature.messages.countUnreadServerChannels
import com.libra.app.feature.messages.deterministicSenderAccent
import com.libra.app.feature.messages.isServerChannelUnread
import com.libra.app.feature.messages.parseDmHubMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAndDmNavigationTest {

    @Test
    fun countUnreadPrivateConversations_countsDistinctPeopleNotTotalMessages() {
        // 1 person sends 20 messages -> badge must show 1
        val singlePersonTwentyMsgs = listOf(
            DirectConversation(
                id = "a_b",
                isGroup = false,
                otherUserId = "user_b",
                otherUserName = "Ayşe",
                unreadCount = 20
            )
        )
        assertEquals(1, countUnreadPrivateConversations(singlePersonTwentyMsgs))

        // 3 distinct people send 50 messages total -> badge must show 3
        val threePeopleFiftyMsgs = listOf(
            DirectConversation(id = "a_b", isGroup = false, otherUserId = "b", unreadCount = 10),
            DirectConversation(id = "a_c", isGroup = false, otherUserId = "c", unreadCount = 15),
            DirectConversation(id = "a_d", isGroup = false, otherUserId = "d", unreadCount = 25)
        )
        assertEquals(3, countUnreadPrivateConversations(threePeopleFiftyMsgs))
    }

    @Test
    fun countUnreadPrivateConversations_excludesGroupsAndUpdatesWhenRead() {
        val mixed = listOf(
            DirectConversation(id = "dm_1", isGroup = false, otherUserId = "u1", unreadCount = 4),
            DirectConversation(id = "dm_2", isGroup = false, otherUserId = "u2", unreadCount = 0),
            DirectConversation(id = "grp_1", isGroup = true, groupName = "Kitap Kulübü", unreadCount = 18),
            DirectConversation(id = "dm_3", isGroup = false, otherUserId = "u3", unreadCount = 7)
        )
        assertEquals(2, countUnreadPrivateConversations(mixed))

        val afterReadingOne = mixed.map { if (it.id == "dm_1") it.copy(unreadCount = 0) else it }
        assertEquals(1, countUnreadPrivateConversations(afterReadingOne))

        val afterReadingAllPrivate = afterReadingOne.map { if (!it.isGroup) it.copy(unreadCount = 0) else it }
        assertEquals(0, countUnreadPrivateConversations(afterReadingAllPrivate))
    }

    @Test
    fun parseDmHubMode_persistsAndRecoversAllThreeModesSafely() {
        assertEquals(DmHubMode.MESSAGES, parseDmHubMode(null))
        assertEquals(DmHubMode.MESSAGES, parseDmHubMode(""))
        assertEquals(DmHubMode.MESSAGES, parseDmHubMode("UNKNOWN"))
        assertEquals(DmHubMode.MESSAGES, parseDmHubMode(DmHubMode.MESSAGES.name))
        assertEquals(DmHubMode.GLOBAL_CHAT, parseDmHubMode(DmHubMode.GLOBAL_CHAT.name))
        assertEquals(DmHubMode.GLOBAL_CHAT, parseDmHubMode("global"))
        assertEquals(DmHubMode.SERVERS, parseDmHubMode(DmHubMode.SERVERS.name))
        assertEquals(DmHubMode.SERVERS, parseDmHubMode("servers"))
    }

    @Test
    fun computeUnreadDividerIndex_placesDividerBeforeFirstUnreadIncomingMessage() {
        val me = "uid_me"
        val peer = "uid_peer"
        val messages = listOf(
            DirectMessage(id = "1", senderId = me, text = "Selam"),
            DirectMessage(id = "2", senderId = peer, text = "Merhaba"),
            DirectMessage(id = "3", senderId = peer, text = "Nasılsın?"),
            DirectMessage(id = "4", senderId = peer, text = "Kitabı okudun mu?")
        )

        assertEquals(-1, computeUnreadDividerIndex(messages, initialUnreadCount = 0, currentUid = me))
        assertEquals(-1, computeUnreadDividerIndex(emptyList(), initialUnreadCount = 2, currentUid = me))
        assertEquals(3, computeUnreadDividerIndex(messages, initialUnreadCount = 1, currentUid = me))
        assertEquals(2, computeUnreadDividerIndex(messages, initialUnreadCount = 2, currentUid = me))
        assertEquals(1, computeUnreadDividerIndex(messages, initialUnreadCount = 5, currentUid = me))
    }

    @Test
    fun serverChannelUnreadLogic_handlesOwnMessagesAndReadWatermarks() {
        val myUid = "me_123"
        val peerMsg = ServerMessage(
            id = "msg_1",
            senderId = "peer_456",
            senderName = "Kaan",
            text = "Yeni bölüm yayında",
            createdAt = 1_000_000L
        )
        val myMsg = ServerMessage(
            id = "msg_2",
            senderId = myUid,
            senderName = "Ben",
            text = "Harika!",
            createdAt = 2_000_000L
        )

        assertFalse(isServerChannelUnread(null, lastReadAtMs = 0L, currentUid = myUid))
        assertFalse(isServerChannelUnread(myMsg, lastReadAtMs = 0L, currentUid = myUid))
        assertTrue(isServerChannelUnread(peerMsg, lastReadAtMs = 0L, currentUid = myUid))
        assertTrue(isServerChannelUnread(peerMsg, lastReadAtMs = 999_999L, currentUid = myUid))
        assertFalse(isServerChannelUnread(peerMsg, lastReadAtMs = 1_000_000L, currentUid = myUid))
        assertFalse(isServerChannelUnread(peerMsg, lastReadAtMs = 1_500_000L, currentUid = myUid))

        val latestByChannel = mapOf(
            "ch_general" to peerMsg,
            "ch_books" to myMsg,
            "ch_art" to peerMsg.copy(id = "msg_3", createdAt = 3_000_000L),
            "ch_empty" to null
        )
        val readByChannel = mapOf(
            "ch_general" to 1_000_000L, // already read
            "ch_art" to 2_500_000L      // newer peer msg -> unread
        )
        assertEquals(1, countUnreadServerChannels(latestByChannel, readByChannel, myUid))
    }

    @Test
    fun deterministicSenderAccent_isStableAndGroupBannerFieldPreserved() {
        val light1 = deterministicSenderAccent("uid_alpha", darkTheme = false)
        val light2 = deterministicSenderAccent("uid_alpha", darkTheme = false)
        val dark1 = deterministicSenderAccent("uid_alpha", darkTheme = true)
        assertEquals(light1, light2)
        assertTrue(light1.alpha > 0.9f)
        assertTrue(dark1.alpha > 0.9f)

        val group = DirectConversation(
            id = "grp_42",
            isGroup = true,
            groupName = "Gece Okurları",
            groupPhotoUrl = "users/u1/grp_avatar.jpg",
            groupBannerUrl = "users/u1/grp_banner.jpg",
            groupPermissions = GroupPermissions(membersCanAddOthers = false)
        )
        assertEquals("users/u1/grp_banner.jpg", group.groupBannerUrl)
        assertFalse(group.groupPermissions.membersCanAddOthers)
    }
}
