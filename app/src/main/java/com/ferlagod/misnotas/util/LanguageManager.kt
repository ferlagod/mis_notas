// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/**
 * Idiomas disponibles en la app. [nativeName] se muestra siempre en el propio idioma.
 */
enum class AppLanguage(val tag: String, val nativeName: String) {
    SYSTEM("", ""),
    SPANISH("es", "Español"),
    ENGLISH("en", "English"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    ITALIAN("it", "Italiano");

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            if (tag.isNullOrBlank()) return SYSTEM
            val language = Locale.forLanguageTag(tag).language
            return entries.firstOrNull { it != SYSTEM && it.tag == language } ?: SYSTEM
        }
    }
}

/**
 * Idioma de la app elegido por el usuario.
 *
 * - Android 13+: usa la API de idioma por aplicación del sistema (también aparece en
 *   Ajustes del sistema → Apps → Mis Notas → Idioma).
 * - Android 8-12: guarda la preferencia y aplica el idioma al contexto de la actividad.
 */
object LanguageManager {

    private const val PREFS = "misnotas_settings"
    private const val KEY_LANGUAGE = "app_language"

    fun current(context: Context): AppLanguage {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            return if (locales.isEmpty) AppLanguage.SYSTEM else AppLanguage.fromTag(locales[0].toLanguageTag())
        }
        return AppLanguage.fromTag(prefs(context).getString(KEY_LANGUAGE, null))
    }

    fun set(activity: Activity, language: AppLanguage) {
        prefs(activity).edit { putString(KEY_LANGUAGE, language.tag) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // El sistema recrea la actividad por sí mismo.
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (language == AppLanguage.SYSTEM) LocaleList.getEmptyLocaleList()
                else LocaleList.forLanguageTags(language.tag)
        } else {
            activity.recreate()
        }
    }

    /**
     * Aplica el idioma guardado a [base] (solo necesario antes de Android 13).
     * Se usa en `attachBaseContext` de la actividad y para textos generados fuera de la UI.
     */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base

        val language = AppLanguage.fromTag(prefs(base).getString(KEY_LANGUAGE, null))
        val locale = if (language == AppLanguage.SYSTEM) {
            Resources.getSystem().configuration.locales[0]
        } else {
            Locale.forLanguageTag(language.tag)
        }
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocales(LocaleList(locale)) }
        return base.createConfigurationContext(config)
    }

    /** Contexto con el idioma de la app, para obtener textos desde capas sin UI. */
    fun localizedContext(context: Context): Context = wrap(context.applicationContext)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
