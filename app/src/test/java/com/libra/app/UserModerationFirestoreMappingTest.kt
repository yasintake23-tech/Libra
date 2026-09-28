package com.libra.app

import com.google.firebase.firestore.Exclude
import com.libra.app.domain.model.UserModeration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserModerationFirestoreMappingTest {

    @Test
    fun computedModerationGettersAreExcludedFromFirestore() {
        val moderation = UserModeration(
            banned = true,
            banUntil = 0L,
            postingDisabled = true,
            messagingDisabled = true
        )

        assertTrue(moderation.isBanned)
        assertFalse(moderation.canPost)
        assertFalse(moderation.canMessage)

        val excludedGetters = listOf(
            "isBanned",
            "getCanPost",
            "getCanMessage",
            "getCanStory",
            "getCanComment",
            "getCanPublishBook",
            "getCanCreateServer",
            "getCanMessageInServer"
        )

        excludedGetters.forEach { getterName ->
            val method = UserModeration::class.java.methods
                .firstOrNull { it.name == getterName }
                ?: error("Getter bulunamadı: $getterName")

            assertTrue(
                "$getterName Firestore'dan hariç tutulmalı",
                method.getAnnotation(Exclude::class.java) != null
            )
        }
    }
}
