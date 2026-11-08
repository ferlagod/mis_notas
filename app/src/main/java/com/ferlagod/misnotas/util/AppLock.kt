// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

/**
 * Bloqueo de la app con la huella, la cara o el PIN/patrón del dispositivo.
 *
 * Usa directamente las APIs del sistema (sin dependencias extra):
 * - Android 10+: `BiometricPrompt` con el PIN del dispositivo como alternativa.
 * - Android 8-9: pantalla de confirmación de credenciales del sistema.
 */
object AppLock {

    /** Tiempo en segundo plano tras el que se vuelve a pedir el desbloqueo. */
    const val GRACE_PERIOD_MS = 60_000L

    /** El dispositivo tiene PIN, patrón o contraseña (requisito para el bloqueo). */
    fun isAvailable(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    /**
     * Pide autenticación. En Android 8-9 devuelve un [Intent] que la actividad debe lanzar
     * esperando RESULT_OK; en el resto, el resultado llega por [onResult].
     */
    fun authenticate(
        activity: Activity,
        title: String,
        subtitle: String,
        onResult: (success: Boolean) -> Unit
    ): Intent? {
        if (!isAvailable(activity)) {
            onResult(true)
            return null
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            return activity.getSystemService(KeyguardManager::class.java)
                ?.createConfirmDeviceCredentialIntent(title, subtitle)
                ?: run { onResult(true); null }
        }

        val builder = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }

        builder.build().authenticate(
            CancellationSignal(),
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = onResult(true)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) = onResult(false)
            }
        )
        return null
    }
}
