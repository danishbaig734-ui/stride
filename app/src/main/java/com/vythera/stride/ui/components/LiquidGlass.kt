package com.vythera.stride.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Animated liquid glass background that provides luminous color gradients
 * and fluid light orbs for [hazeEffect] to blur across screens.
 */
@Composable
fun LiquidGlassBackground(
    hazeState: HazeState?,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val transition = rememberInfiniteTransition(label = "liquidGlassAnim")
    val shift1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shift1"
    )
    val shift2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shift2"
    )

    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val tertiary = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    val bg = MaterialTheme.colorScheme.background

    val hazeModifier = if (hazeState != null) {
        Modifier.hazeSource(state = hazeState)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(hazeModifier)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = bg)

            val w = size.width
            val h = size.height

            // Fluid light orbs that drift slowly
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary, Color.Transparent),
                    center = Offset(w * (0.25f + 0.5f * shift1), h * (0.2f + 0.2f * shift2)),
                    radius = w * 0.75f
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tertiary, Color.Transparent),
                    center = Offset(w * (0.8f - 0.4f * shift2), h * (0.55f + 0.3f * shift1)),
                    radius = w * 0.85f
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondary, Color.Transparent),
                    center = Offset(w * (0.3f + 0.4f * shift2), h * (0.85f - 0.2f * shift1)),
                    radius = w * 0.7f
                )
            )
        }
    }
}

/**
 * Applies a liquid glass blur effect and subtle glass border when enabled.
 */
fun Modifier.liquidGlassEffect(
    hazeState: HazeState?,
    enabled: Boolean,
    shape: Shape,
    blurRadius: Dp = 16.dp,
    borderAlpha: Float = 0.18f
): Modifier = if (enabled && hazeState != null) {
    this
        .hazeEffect(state = hazeState) {
            this.blurRadius = blurRadius
            this.blurEnabled = true
        }
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderAlpha),
                    Color.White.copy(alpha = borderAlpha * 0.25f),
                    Color.White.copy(alpha = borderAlpha * 0.5f)
                )
            ),
            shape = shape
        )
} else {
    this
}

@Composable
fun liquidGlassContainerColor(
    enabled: Boolean,
    baseColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    glassAlpha: Float = 0.65f
): Color = if (enabled) baseColor.copy(alpha = glassAlpha) else baseColor

