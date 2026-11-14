package com.vinay.ledger.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDecay

// Same curves the original Ledger used
val EaseStd = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)      // tabs, months, charts
val EaseSheet = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)    // sheets sliding up
val EaseSheetOut = CubicBezierEasing(0.4f, 0f, 1f, 1f)     // sheets sliding away

/**
 * Calmer scrolling: a flick carries the list about half as far as Android's default,
 * and it slows down sooner. Dragging with your finger is unchanged.
 */
@androidx.compose.runtime.Composable
fun rememberCalmFling(): androidx.compose.foundation.gestures.FlingBehavior {
    val decay = androidx.compose.animation.rememberSplineBasedDecay<Float>()
    return androidx.compose.runtime.remember(decay) {
        object : androidx.compose.foundation.gestures.FlingBehavior {
            override suspend fun androidx.compose.foundation.gestures.ScrollScope.performFling(initialVelocity: Float): Float {
                val v = initialVelocity * 0.5f
                if (kotlin.math.abs(v) < 50f) return initialVelocity
                var left = v
                var last = 0f
                androidx.compose.animation.core.AnimationState(0f, v).animateDecay(decay) {
                    val delta = value - last
                    val used = scrollBy(delta)
                    last = value
                    left = velocity
                    if (kotlin.math.abs(delta - used) > 0.5f) cancelAnimation()
                }
                return left
            }
        }
    }
}
