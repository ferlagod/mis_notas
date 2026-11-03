// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

// ==========================================
// Identidad de Marca
// ==========================================
val RoyalBlueLight = Color(0xFF007AFF)
val RoyalBlueDark = Color(0xFF0A84FF)
val IndigoSecondary = Color(0xFF5856D6)
val IndigoSecondaryDark = Color(0xFF5E5CE6)

// ==========================================
// Tema Claro (Inspirado en iOS HIG)
// ==========================================
val BackgroundLight = Color(0xFFF2F2F7)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE5E5EA)
val TextPrimaryLight = Color(0xFF000000)
val TextSecondaryLight = Color(0xFF8E8E93)
val TextTertiaryLight = Color(0xFFC7C7CC)
val OutlineLight = Color(0xFFD1D1D6)

// ==========================================
// Tema Oscuro (True OLED)
// ==========================================
val BackgroundDark = Color(0xFF000000)
val SurfaceDark = Color(0xFF1C1C1E)
val SurfaceVariantDark = Color(0xFF2C2C2E)
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFF98989D)
val TextTertiaryDark = Color(0xFF48484A)
val OutlineDark = Color(0xFF38383A)

// ==========================================
// Acentos Semánticos
// ==========================================
val AccentGreen = Color(0xFF34C759)
val AccentOrange = Color(0xFFFF9500)
val AccentRed = Color(0xFFFF3B30)

// ==========================================
// Paleta de Notas & Carpetas (Estilo Google Keep + Apple Notes)
// ==========================================
val AccentYellow = Color(0xFFFFCC00)
val AccentYellowDark = Color(0xFFFFD60A)
val AccentTeal = Color(0xFF30B0C7)
val AccentTealDark = Color(0xFF40C8E0)
val AccentPurple = Color(0xFFAF52DE)
val AccentPurpleDark = Color(0xFFBF5AF2)

enum class NoteColor(val key: String) {
    DEFAULT("DEFAULT"),
    YELLOW("YELLOW"),
    BLUE("BLUE"),
    GREEN("GREEN"),
    ORANGE("ORANGE"),
    RED("RED"),
    PURPLE("PURPLE"),
    TEAL("TEAL"),
    INDIGO("INDIGO");

    companion object {
        fun fromKey(key: String?): NoteColor {
            if (key == null) return DEFAULT
            return entries.firstOrNull {
                it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true)
            } ?: DEFAULT
        }
    }
}

/**
 * Obtiene el color de acento correspondiente a la nota según el tema activo.
 */
fun NoteColor.getAccentColor(isDark: Boolean): Color = when (this) {
    NoteColor.DEFAULT -> if (isDark) SurfaceDark else SurfaceLight
    NoteColor.YELLOW -> if (isDark) AccentYellowDark else AccentYellow
    NoteColor.BLUE -> if (isDark) RoyalBlueDark else RoyalBlueLight
    NoteColor.GREEN -> AccentGreen
    NoteColor.ORANGE -> AccentOrange
    NoteColor.RED -> AccentRed
    NoteColor.PURPLE -> if (isDark) AccentPurpleDark else AccentPurple
    NoteColor.TEAL -> if (isDark) AccentTealDark else AccentTeal
    NoteColor.INDIGO -> if (isDark) IndigoSecondaryDark else IndigoSecondary
}

/**
 * Fondo para tarjetas de notas:
 * Devuelve SurfaceLight/SurfaceDark para DEFAULT.
 * Para el resto, combina el color de acento al 12% en claro y 18% en oscuro sobre la superficie base.
 */
@Composable
fun NoteColor.toCardBackground(isDark: Boolean = isSystemInDarkTheme()): Color {
    val surfaceBase = if (isDark) SurfaceDark else SurfaceLight
    if (this == NoteColor.DEFAULT) return surfaceBase
    val accent = getAccentColor(isDark)
    val opacity = if (isDark) 0.18f else 0.12f
    return accent.copy(alpha = opacity).compositeOver(surfaceBase)
}

/**
 * Borde para tarjetas de notas:
 * Devuelve OutlineLight/OutlineDark para DEFAULT.
 * Para notas con color, devuelve el acento al 35% de opacidad.
 */
@Composable
fun NoteColor.toCardBorder(isDark: Boolean = isSystemInDarkTheme()): Color {
    if (this == NoteColor.DEFAULT) {
        return if (isDark) OutlineDark else OutlineLight
    }
    val accent = getAccentColor(isDark)
    return accent.copy(alpha = 0.35f)
}

/**
 * Fondo sutil para la pantalla del editor de notas:
 * Devuelve BackgroundLight/BackgroundDark para DEFAULT.
 * Para colores con acento, aplica un tinte muy sutil (6% en claro, 9% en oscuro) sobre el fondo base.
 */
@Composable
fun NoteColor.toEditorBackground(isDark: Boolean = isSystemInDarkTheme()): Color {
    val bgBase = if (isDark) BackgroundDark else BackgroundLight
    if (this == NoteColor.DEFAULT) return bgBase
    val accent = getAccentColor(isDark)
    val opacity = if (isDark) 0.09f else 0.06f
    return accent.copy(alpha = opacity).compositeOver(bgBase)
}