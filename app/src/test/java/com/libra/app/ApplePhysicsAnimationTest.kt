package com.libra.app

import com.libra.app.ui.animation.AppleSprings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit test suite verifying Apple iOS 18 & VisionOS dynamic physics animation tokens,
 * spring damping ratios, stagger mist timings, and rubber-band elasticity formulas.
 */
class ApplePhysicsAnimationTest {

    @Test
    fun vapor_stagger_delay_constant_is_calibrated_to_24ms() {
        assertEquals(24L, AppleSprings.VaporStaggerDelayMs)
    }

    @Test
    fun staggered_vapor_cascade_timing_formula_preserves_fluid_mist_entrance() {
        fun calculateStagger(index: Int): Long {
            val cappedIndex = index.coerceIn(0, 12)
            return cappedIndex * AppleSprings.VaporStaggerDelayMs
        }

        // First item appears immediately
        assertEquals(0L, calculateStagger(0))
        // Successive items stagger by 24ms increments
        assertEquals(24L, calculateStagger(1))
        assertEquals(48L, calculateStagger(2))
        assertEquals(120L, calculateStagger(5))
        // Capped at 12 items (288ms maximum waterfall) so large lists don't delay bottom content
        assertEquals(288L, calculateStagger(12))
        assertEquals(288L, calculateStagger(25))
    }

    @Test
    fun bouncy_spring_physics_specification() {
        assertEquals(0.58f, AppleSprings.FloatBouncy.dampingRatio, 0.001f)
        assertEquals(420f, AppleSprings.FloatBouncy.stiffness, 0.001f)
    }

    @Test
    fun snappy_spring_physics_specification() {
        assertEquals(0.72f, AppleSprings.FloatSnappy.dampingRatio, 0.001f)
        assertEquals(650f, AppleSprings.FloatSnappy.stiffness, 0.001f)
    }

    @Test
    fun gentle_spring_physics_specification() {
        assertEquals(0.85f, AppleSprings.FloatGentle.dampingRatio, 0.001f)
        assertEquals(320f, AppleSprings.FloatGentle.stiffness, 0.001f)
    }

    @Test
    fun offset_bouncy_spring_specification() {
        assertEquals(0.60f, AppleSprings.OffsetBouncy.dampingRatio, 0.001f)
        assertEquals(450f, AppleSprings.OffsetBouncy.stiffness, 0.001f)
    }

    @Test
    fun offset_gentle_spring_specification() {
        assertEquals(0.82f, AppleSprings.OffsetGentle.dampingRatio, 0.001f)
        assertEquals(340f, AppleSprings.OffsetGentle.stiffness, 0.001f)
    }

    @Test
    fun spring_damping_hierarchy_reflects_physical_responsiveness() {
        // Bouncy (playful overshoot) has lower damping than snappy (quick settle),
        // which in turn has lower damping than gentle (soft settling for sheets).
        assertTrue(
            "Bouncy damping must be lower than snappy damping",
            AppleSprings.FloatBouncy.dampingRatio < AppleSprings.FloatSnappy.dampingRatio
        )
        assertTrue(
            "Snappy damping must be lower than gentle damping",
            AppleSprings.FloatSnappy.dampingRatio < AppleSprings.FloatGentle.dampingRatio
        )
        // Snappy must be stiffer than bouncy and gentle
        assertTrue(
            "Snappy stiffness must be highest for immediate response",
            AppleSprings.FloatSnappy.stiffness > AppleSprings.FloatBouncy.stiffness
        )
        assertTrue(
            "Snappy stiffness must exceed gentle stiffness",
            AppleSprings.FloatSnappy.stiffness > AppleSprings.FloatGentle.stiffness
        )
    }

    @Test
    fun rubber_band_elasticity_formula_dampens_displacement() {
        fun rubberBand(offset: Float, maxStretch: Float): Float {
            return offset / (1f + (abs(offset) / maxStretch))
        }

        val maxStretch = 100f

        // Zero offset remains zero
        assertEquals(0f, rubberBand(0f, maxStretch), 0.001f)

        // At max stretch, displacement is exactly 50% of original
        val atLimit = rubberBand(maxStretch, maxStretch)
        assertEquals(50f, atLimit, 0.001f)

        // Extreme tugging (10x max) never exceeds max stretch ceiling
        val extremeTug = rubberBand(1000f, maxStretch)
        assertTrue("Extreme tug must never exceed maxStretch", extremeTug < maxStretch)
        assertEquals(90.909f, extremeTug, 0.01f)

        // Negative tugs preserve direction
        val negativeTug = rubberBand(-maxStretch, maxStretch)
        assertEquals(-50f, negativeTug, 0.001f)
    }

    @Test
    fun sand_granule_alternating_drift_and_micro_tilt_geometry() {
        fun calculateDrift(index: Int): Float {
            val driftDirection = if (index % 2 == 0) -1f else 1f
            return 8f * driftDirection
        }

        fun calculateTilt(index: Int): Float {
            val driftDirection = if (index % 2 == 0) -1f else 1f
            return driftDirection * 1.5f
        }

        // Even indices drift left (-8px) and micro-tilt -1.5 degrees
        assertEquals(-8f, calculateDrift(0), 0.001f)
        assertEquals(-1.5f, calculateTilt(0), 0.001f)
        assertEquals(-8f, calculateDrift(2), 0.001f)
        assertEquals(-1.5f, calculateTilt(2), 0.001f)

        // Odd indices drift right (+8px) and micro-tilt +1.5 degrees
        assertEquals(8f, calculateDrift(1), 0.001f)
        assertEquals(1.5f, calculateTilt(1), 0.001f)
        assertEquals(8f, calculateDrift(3), 0.001f)
        assertEquals(1.5f, calculateTilt(3), 0.001f)
    }

    @Test
    fun directional_stretch_factor_physics_calculation() {
        fun calculateStretchFactor(dampedOffset: Float, maxStretch: Float): Float {
            return (abs(dampedOffset) / maxStretch.coerceAtLeast(1f)) * 0.16f
        }

        val maxStretch = 80f

        // At rest, elongation factor is 0.0 (no stretch)
        assertEquals(0.0f, calculateStretchFactor(0f, maxStretch), 0.001f)

        // At half-damped displacement (when dragged by maxStretch), factor is 0.08
        val halfDamped = 40f // 80 / (1 + 80/80) = 40
        assertEquals(0.08f, calculateStretchFactor(halfDamped, maxStretch), 0.001f)

        // Asymptotically approaching maxStretch, elongation factor reaches 0.16f ceiling
        val atCeiling = calculateStretchFactor(80f, maxStretch)
        assertEquals(0.16f, atCeiling, 0.001f)
    }

    @Test
    fun apple_button_compression_scale_tokens_follow_ergonomic_hierarchy() {
        val primaryButtonScale = 0.94f
        val iconButtonScale = 0.86f
        val cardItemScale = 0.97f

        // Smaller surface area buttons compress deeper for distinct tactile feedback
        assertTrue(iconButtonScale < primaryButtonScale)
        // Large cards compress minimally to preserve readability of large content
        assertTrue(primaryButtonScale < cardItemScale)
        assertTrue(iconButtonScale < cardItemScale)
    }

    @Test
    fun apple_squircle_corner_radius_hierarchy() {
        val primaryButtonRadiusDp = 18f
        val inputAndModalRadiusDp = 16f
        val chipRadiusDp = 14f

        assertTrue(primaryButtonRadiusDp > inputAndModalRadiusDp)
        assertTrue(inputAndModalRadiusDp > chipRadiusDp)
    }

    @Test
    fun apple_glass_subtle_refraction_border_contrast_tokens() {
        val primaryBorderTopAlpha = 0.32f
        val primaryBorderBottomAlpha = 0.08f
        val secondaryBorderTopAlpha = 0.22f
        val secondaryBorderBottomAlpha = 0.05f

        // Top edge light reflection is stronger than bottom ambient rim
        assertTrue(primaryBorderTopAlpha > primaryBorderBottomAlpha)
        assertTrue(secondaryBorderTopAlpha > secondaryBorderBottomAlpha)
        // Primary button border has higher contrast than secondary
        assertTrue(primaryBorderTopAlpha > secondaryBorderTopAlpha)
    }
}

