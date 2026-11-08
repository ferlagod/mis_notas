// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.data.local.pref

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tema de la interfaz. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Orden de las notas en las listas (las fijadas siempre van primero). */
enum class SortOrder { MODIFIED_DESC, MODIFIED_ASC, TITLE_ASC, TITLE_DESC }

/** Preferencias de la app que no son secretas (aspecto y comportamiento). */
data class AppSettingsState(
    val gridLayout: Boolean = true,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoPurgeTrash: Boolean = true,
    val appLock: Boolean = false
)

/**
 * Preferencias no sensibles en SharedPreferences normales, expuestas como [StateFlow]
 * para que la interfaz reaccione al instante. Comparte fichero con el idioma.
 */
class AppSettings private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettingsState> = _state.asStateFlow()

    private fun read() = AppSettingsState(
        gridLayout = prefs.getBoolean(KEY_GRID, true),
        sortOrder = enumOrDefault(prefs.getString(KEY_SORT, null), SortOrder.MODIFIED_DESC),
        themeMode = enumOrDefault(prefs.getString(KEY_THEME, null), ThemeMode.SYSTEM),
        autoPurgeTrash = prefs.getBoolean(KEY_AUTO_PURGE, true),
        appLock = prefs.getBoolean(KEY_APP_LOCK, false)
    )

    private fun update(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit(action = block)
        _state.value = read()
    }

    fun setGridLayout(grid: Boolean) = update { putBoolean(KEY_GRID, grid) }
    fun setSortOrder(order: SortOrder) = update { putString(KEY_SORT, order.name) }
    fun setThemeMode(mode: ThemeMode) = update { putString(KEY_THEME, mode.name) }
    fun setAutoPurgeTrash(enabled: Boolean) = update { putBoolean(KEY_AUTO_PURGE, enabled) }
    fun setAppLock(enabled: Boolean) = update { putBoolean(KEY_APP_LOCK, enabled) }

    companion object {
        private const val PREFS = "misnotas_settings"
        private const val KEY_GRID = "grid_layout"
        private const val KEY_SORT = "sort_order"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_AUTO_PURGE = "auto_purge_trash"
        private const val KEY_APP_LOCK = "app_lock"

        /** Días que una nota permanece en la papelera antes del vaciado automático. */
        const val TRASH_RETENTION_DAYS = 30

        @Volatile
        private var INSTANCE: AppSettings? = null

        fun getInstance(context: Context): AppSettings =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSettings(context).also { INSTANCE = it }
            }

        private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
            name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
    }
}
