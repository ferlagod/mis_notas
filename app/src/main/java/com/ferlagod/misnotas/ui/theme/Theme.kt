// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlueLight,
    onPrimary = Color.White,
    primaryContainer = RoyalBlueLight.copy(alpha = 0.12f),
    onPrimaryContainer = RoyalBlueLight,

    secondary = IndigoSecondary,
    onSecondary = Color.White,
    secondaryContainer = IndigoSecondary.copy(alpha = 0.12f),
    onSecondaryContainer = IndigoSecondary,

    tertiary = AccentGreen,
    onTertiary = Color.White,

    background = BackgroundLight,
    onBackground = TextPrimaryLight,

    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,

    outline = OutlineLight,
    outlineVariant = TextTertiaryLight,

    error = AccentRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlueDark,
    onPrimary = Color.Black,
    primaryContainer = RoyalBlueDark.copy(alpha = 0.20f),
    onPrimaryContainer = RoyalBlueDark,

    secondary = IndigoSecondaryDark,
    onSecondary = Color.Black,
    secondaryContainer = IndigoSecondaryDark.copy(alpha = 0.20f),
    onSecondaryContainer = IndigoSecondaryDark,

    tertiary = AccentGreen,
    onTertiary = Color.Black,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,

    outline = OutlineDark,
    outlineVariant = TextTertiaryDark,

    error = AccentRed,
    onError = Color.White
)

/** Indica si la app está en modo oscuro (según el tema elegido en Ajustes, no solo el del sistema). */
val LocalDarkTheme = staticCompositionLocalOf { false }

/** Sustituto de `isSystemInDarkTheme()` que respeta el tema elegido por el usuario. */
@Composable
@ReadOnlyComposable
fun isAppInDarkTheme(): Boolean = LocalDarkTheme.current

@Composable
fun MisNotasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Desactivado estrictamente para respetar la paleta inspirada en iOS HIG y True OLED
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}