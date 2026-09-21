package com.libra.app

import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun appResult_success_contains_data() {
        val result: AppResult<String> = AppResult.Success("Libra")
        assertTrue(result.isSuccess)
        assertEquals("Libra", result.getOrNull())
    }

    @Test
    fun appResult_error_does_not_expose_data() {
        val result: AppResult<String> = AppResult.Error(AppError.Validation("invalid"))
        assertTrue(result.isError)
        assertEquals(null, result.getOrNull())
    }

    @Test
    fun user_profile_builds_handle_and_initials() {
        val profile = UserProfile(displayName = "Ada Lovelace", username = "ada")
        assertEquals("@ada", profile.handle)
        assertEquals("AL", profile.initials)
    }
}
