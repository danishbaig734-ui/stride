package com.vythera.stride.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState

/**
 * Ambient background layer for liquid glass UI effect.
 */
@Composable
fun LiquidGlassBackground(
    hazeState: HazeState? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (enabled) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                        )
                    )
                )
        )
    }
}

/**
 * Returns translucent container color when liquid glass mode is active, otherwise default base color.
 */
fun liquidGlassContainerColor(
    enabled: Boolean,
    baseColor: Color,
    alpha: Float = 0.70f
): Color {
    return if (enabled) baseColor.copy(alpha = alpha) else baseColor
}

/**
 * Modifier to apply glass-like border and clipping when enabled.
 */
fun Modifier.liquidGlassEffect(
    hazeState: HazeState? = null,
    enabled: Boolean = true,
    shape: Shape? = null
): Modifier {
    if (!enabled) return this
    var mod = this
    if (shape != null) {
        mod = mod
            .clip(shape)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = shape
            )
    }
    return mod
}
