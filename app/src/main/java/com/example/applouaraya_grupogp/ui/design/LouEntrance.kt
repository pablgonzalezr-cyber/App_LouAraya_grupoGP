package com.example.applouaraya_grupogp.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp

/** Finite draw-only entrance: preserves layout and clicks. Compose applies system duration scale.
 * No coroutine delays: disabling system animations also removes the stagger.
 * Save completion across recreation; data emissions do not restart the effect.
 */
@Composable
fun Modifier.louEntrance(delayMillis: Int = 0, rise: Boolean = false, scale: Boolean = false): Modifier {
    val preview = LocalInspectionMode.current
    var completed by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (completed || preview) 1f else 0f) }
    val distance = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(Unit) {
        if (!completed && !preview) {
            progress.animateTo(1f, tween(360, delayMillis, FastOutSlowInEasing))
            completed = true
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = if (rise) distance * (1f - progress.value) else 0f
        scaleX = if (scale) .92f + .08f * progress.value else 1f
        scaleY = scaleX
    }
}
