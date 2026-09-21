package com.libra.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {
    @Test
    fun app_name_is_libra() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Libra", context.getString(R.string.app_name))
    }
}
