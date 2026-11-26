package com.libra.app

import android.graphics.Bitmap
import android.graphics.Color
import com.libra.app.feature.home.analyzeStoryBitmapPalette
import com.libra.app.feature.home.computeStoryHeaderIsLight
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StoryVisualPaletteTest {

    @Test
    fun analyzeStoryBitmapPalette_handlesWhiteBlackAndSplitStoryBitmaps() {
        val whiteBitmap = Bitmap.createBitmap(24, 36, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        val whitePalette = analyzeStoryBitmapPalette(whiteBitmap, sourceWidthPx = 1080, sourceHeightPx = 1920)
        assertTrue(whitePalette.topSliceLuminance > 0.95f)
        assertTrue(whitePalette.bottomSliceLuminance > 0.95f)
        assertTrue(
            computeStoryHeaderIsLight(
                topSliceLuminance = whitePalette.topSliceLuminance,
                cardWidthPx = 1040,
                cardHeightPx = 1860,
                imageWidthPx = whitePalette.imageWidthPx,
                imageHeightPx = whitePalette.imageHeightPx,
                headerHeightPx = 240f
            )
        )
        whiteBitmap.recycle()

        val darkBitmap = Bitmap.createBitmap(24, 36, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(16, 18, 28))
        }
        val darkPalette = analyzeStoryBitmapPalette(darkBitmap, sourceWidthPx = 1080, sourceHeightPx = 1920)
        assertTrue(darkPalette.topSliceLuminance < 0.2f)
        assertFalse(
            computeStoryHeaderIsLight(
                topSliceLuminance = darkPalette.topSliceLuminance,
                cardWidthPx = 1040,
                cardHeightPx = 1860,
                imageWidthPx = darkPalette.imageWidthPx,
                imageHeightPx = darkPalette.imageHeightPx,
                headerHeightPx = 240f
            )
        )
        darkBitmap.recycle()

        val splitBitmap = Bitmap.createBitmap(24, 36, Bitmap.Config.ARGB_8888)
        for (y in 0 until 36) {
            val rowColor = if (y < 14) Color.WHITE else Color.rgb(20, 22, 30)
            for (x in 0 until 24) {
                splitBitmap.setPixel(x, y, rowColor)
            }
        }
        val splitPalette = analyzeStoryBitmapPalette(splitBitmap, sourceWidthPx = 1080, sourceHeightPx = 1920)
        assertTrue(splitPalette.topSliceLuminance > 0.95f)
        assertTrue(splitPalette.bottomSliceLuminance < 0.2f)
        splitBitmap.recycle()
    }
}
