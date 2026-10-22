package com.vythera.stride.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.vythera.stride.model.StridePrefs
import com.vythera.stride.model.ThemeMode
import com.vythera.stride.model.ThemeStyle

/** Zenith-style remap: tinted window background, popping cards. */
private fun ColorScheme.expressiveSurfaces(dark: Boolean): ColorScheme =
    if (dark) copy(
        background = surfaceContainerLowest,
        surface = surfaceContainerLowest,
        surfaceContainer = surfaceContainerHigh,
        surfaceContainerLow = surfaceContainerLow
    ) else copy(
        background = surfaceContainerLow,
        surface = surfaceContainerLow,
        surfaceContainer = Color.White,
        surfaceContainerLow = Color.White,
        surfaceContainerHigh = Color.White
    )

@Composable
fun StrideTheme(
    prefs: StridePrefs,
    content: @Composable () -> Unit
) {
    val darkTheme = when (prefs.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val effectiveDark = darkTheme || prefs.themeStyle != ThemeStyle.CLASSIC

    val base = when (prefs.themeStyle) {
        ThemeStyle.RETRO -> RetroColorScheme
        ThemeStyle.VERDANT -> VerdantColorScheme
        ThemeStyle.CLASSIC -> {
            if (prefs.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                val dyn = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                dyn.expressiveSurfaces(darkTheme)
            } else {
                val palette = paletteById(prefs.paletteId)
                if (darkTheme) palette.dark else palette.light
            }
        }
    }

    val colorScheme = if (prefs.themeStyle == ThemeStyle.CLASSIC) {
        base
            .applyStyle(prefs.colorStyle)
            .let { if (prefs.amoled && darkTheme) it.applyAmoled() else it }
    } else {
        if (prefs.amoled) base.applyAmoled() else base
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !effectiveDark
            controller.isAppearanceLightNavigationBars = !effectiveDark
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = strideTypography(prefs),
        content = content
    )
}
