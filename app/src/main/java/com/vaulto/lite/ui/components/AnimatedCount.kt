package com.vaulto.lite.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/**
 * Animates [targetValue] from its previous value to the new one whenever it
 * changes, returning the interpolated value to render. Used for hero amounts,
 * analytics totals, and any number that should "count up/down" smoothly.
 */
@Composable
fun animatedCount(targetValue: Double, durationMillis: Int = 600): Double {
    val animated by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = tween(durationMillis = durationMillis),
        label = "animatedCount"
    )
    return animated.toDouble()
}
