// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ferlagod.misnotas.data.remote.webdav.WebDavAuthException
import java.util.concurrent.TimeUnit

/**
 * Sincronización periódica en segundo plano (cada ~30 min con red disponible).
 * Solo actúa si hay una cuenta conectada y la bóveda está desbloqueada.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = SyncRepository.getInstance(applicationContext).synchronize()
        val error = result.exceptionOrNull() ?: return Result.success()
        // Errores que no se arreglan reintentando: credenciales, contraseña o bóveda. La próxima
        // ejecución periódica (o abrir la app) lo vuelve a intentar.
        return if (error is WebDavAuthException || error is WrongMasterPasswordException || error is VaultException) {
            Result.failure()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "misnotas-periodic-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
