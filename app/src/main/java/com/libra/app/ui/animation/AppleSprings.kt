package com.libra.app.ui.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset

/**
 * Standardized Apple iOS 18 & VisionOS inspired spring physics and timing tokens.
 * Calibrated for tactile responsiveness, organic overshoot, and 120Hz buttery smoothness.
 */
object AppleSprings {

    /**
     * Stagger delay between successive items in lists, menus, and sheets (milliseconds).
     * Creates an ethereal "dust / mist settling" cascading waterfall effect.
     */
    const val VaporStaggerDelayMs: Long = 24L

    /**
     * Apple iOS fluid easing curve for alpha and opacity transitions.
     */
    val AppleFluidEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    /**
     * Bouncy spring: moderate damping with playful overshoot.
     * Ideal for button presses, icon pops, heart like animations, and elastic releases.
     */
    fun <T> bouncy(stiffness: Float = 420f, dampingRatio: Float = 0.58f) = spring<T>(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )

    /**
     * Snappy spring: low bounce, highly responsive.
     * Ideal for tab indicators, segmented pill sliders, and switches.
     */
    fun <T> snappy(stiffness: Float = 650f, dampingRatio: Float = 0.72f) = spring<T>(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )

    /**
     * Gentle spring: minimal bounce, soft and elegant settling.
     * Ideal for modal sheets, full-screen dialogs, and card entrance cascades.
     */
    fun <T> gentle(stiffness: Float = 320f, dampingRatio: Float = 0.85f) = spring<T>(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )

    val FloatBouncy = spring<Float>(
        dampingRatio = 0.58f,
        stiffness = 420f
    )

    val FloatSnappy = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 650f
    )

    val FloatGentle = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 320f
    )

    val OffsetBouncy = spring<Offset>(
        dampingRatio = 0.60f,
        stiffness = 450f
    )

    val OffsetGentle = spring<Offset>(
        dampingRatio = 0.82f,
        stiffness = 340f
    )
}
