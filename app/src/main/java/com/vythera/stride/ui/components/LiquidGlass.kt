package com.vythera.stride.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Ambient liquid glass background that provides luminous color gradients
 * for [hazeEffect] to blur across screens without infinite frame redraw overhead.
 */
@Composable
fun LiquidGlassBackground(
    hazeState: HazeState?,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    val tertiary = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f)

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

            if (enabled) {
                val w = size.width
                val h = size.height

                // Fluid light orbs rendered statically without infinite loop recomposition/redraw
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary, Color.Transparent),
                        center = Offset(w * 0.35f, h * 0.25f),
                        radius = w * 0.75f
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tertiary, Color.Transparent),
                        center = Offset(w * 0.75f, h * 0.60f),
                        radius = w * 0.85f
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondary, Color.Transparent),
                        center = Offset(w * 0.40f, h * 0.85f),
                        radius = w * 0.70f
                    )
                )
            }
        }
    }
}

/**
 * Applies a liquid glass blur effect and subtle glass border when enabled.
 * Uses HazeInputScale.Auto and capped blur radius for high frame-rate performance.
 */
fun Modifier.liquidGlassEffect(
    hazeState: HazeState?,
    enabled: Boolean,
    shape: Shape,
    blurRadius: Dp = 12.dp,
    borderAlpha: Float = 0.18f
): Modifier = if (enabled && hazeState != null) {
    this
        .hazeEffect(state = hazeState) {
            this.blurRadius = blurRadius.coerceIn(0.dp, 20.dp)
            this.blurEnabled = true
            this.inputScale = HazeInputScale.Auto
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

