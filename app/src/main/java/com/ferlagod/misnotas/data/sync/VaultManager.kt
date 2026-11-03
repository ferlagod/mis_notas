// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.data.sync

import com.ferlagod.misnotas.data.local.pref.AuthPreferences
import com.ferlagod.misnotas.data.remote.webdav.WebDavClient
import com.ferlagod.misnotas.data.remote.webdav.WebDavPreconditionFailedException
import com.ferlagod.misnotas.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey

/**
 * Gestiona la "bóveda" cifrada: derivación de la clave a partir de la contraseña maestra,
 * creación de `keyinfo.json` en el servidor y verificación de la contraseña.
 */
class VaultManager(private val prefs: AuthPreferences) {

    /**
     * Desbloquea (o crea) la bóveda remota con [masterPassword].
     *
     * - Si existe `keyinfo.json`: deriva la clave con su salt e iteraciones y verifica la contraseña.
     * - Si no existe y hay ficheros de la v1.0 de la app: usa el salt local antiguo y comprueba
     *   que se pueden descifrar.
     * - Si la carpeta está vacía: crea una bóveda nueva.
     *
     * @return la clave y si la bóveda se acaba de crear.
     */
    suspend fun unlockRemote(client: WebDavClient, masterPassword: String): Pair<SecretKey, Boolean> {
        val existing = client.downloadOrNull(KeyInfo.FILE_NAME)?.let { KeyInfo.parse(String(it, Charsets.UTF_8)) }
        if (existing != null) {
            val key = derive(masterPassword, existing.salt, existing.iterations)
            if (!existing.verify(key)) throw WrongMasterPasswordException()
            prefs.saveVaultParams(existing.salt, existing.iterations, existing.check)
            return key to false
        }

        val legacyFiles = client.listRemoteFiles().filter {
            (it.name.startsWith(SyncRepository.NOTE_PREFIX) || it.name.startsWith(SyncRepository.FOLDER_PREFIX)) &&
                it.name.endsWith(SyncRepository.JSON_EXT)
        }

        val salt: ByteArray
        val iterations: Int
        if (legacyFiles.isNotEmpty()) {
            salt = prefs.getSalt() ?: throw IllegalStateException(
                "Hay notas de una versión anterior en el servidor. Actualiza primero la app en el " +
                    "dispositivo donde las creaste y sincroniza desde allí."
            )
            iterations = CryptoManager.LEGACY_PBKDF2_ITERATIONS
        } else {
            salt = CryptoManager.generateSalt()
            iterations = CryptoManager.DEFAULT_PBKDF2_ITERATIONS
        }

        val key = derive(masterPassword, salt, iterations)

        if (legacyFiles.isNotEmpty()) {
            val sample = legacyFiles.first()
            val raw = client.downloadText(sample.name)
            val ok = try {
                if (sample.name.startsWith(SyncRepository.NOTE_PREFIX)) {
                    RemoteCodec.decodeNote(raw, idOf(sample.name, SyncRepository.NOTE_PREFIX), key)
                } else {
                    RemoteCodec.decodeFolder(raw, idOf(sample.name, SyncRepository.FOLDER_PREFIX), key)
                }
                true
            } catch (_: Exception) {
                false
            }
            if (!ok) throw WrongMasterPasswordException("La contraseña no coincide con las notas existentes en el servidor")
        }

        val info = KeyInfo.create(salt, iterations, key)
        try {
            client.uploadText(KeyInfo.FILE_NAME, info.toJson(), ifNoneMatch = true)
        } catch (_: WebDavPreconditionFailedException) {
            // Otro dispositivo creó la bóveda a la vez: usamos la suya.
            return unlockRemote(client, masterPassword)
        }
        prefs.saveVaultParams(salt, iterations, info.check)
        return key to true
    }

    /**
     * Desbloqueo sin conexión con los parámetros guardados en el dispositivo.
     */
    suspend fun unlockLocal(masterPassword: String): SecretKey {
        val salt = prefs.getSalt()
            ?: throw IllegalStateException("No hay datos de cifrado en este dispositivo. Vuelve a conectar la cuenta.")
        val key = derive(masterPassword, salt, prefs.getIterations())
        val check = prefs.getKeyCheck()
        if (check != null && !CryptoManager.verify(check, KeyInfo.CHECK_PLAINTEXT, key, KeyInfo.CHECK_AAD)) {
            throw WrongMasterPasswordException()
        }
        return key
    }

    /**
     * Comprueba en cada sincronización que la clave local sigue siendo la de la bóveda remota.
     */
    suspend fun verifyRemoteKey(client: WebDavClient, key: SecretKey) {
        val raw = client.downloadOrNull(KeyInfo.FILE_NAME)
            ?: throw IllegalStateException(
                "No se encuentra ${KeyInfo.FILE_NAME} en el servidor. Vuelve a conectar la cuenta en Ajustes."
            )
        val info = KeyInfo.parse(String(raw, Charsets.UTF_8))
        if (!info.verify(key)) {
            throw WrongMasterPasswordException(
                "La contraseña maestra del servidor no coincide con la de este dispositivo. Desbloquea de nuevo."
            )
        }
        if (prefs.getKeyCheck() == null) prefs.saveVaultParams(info.salt, info.iterations, info.check)
    }

    private suspend fun derive(password: String, salt: ByteArray, iterations: Int): SecretKey =
        withContext(Dispatchers.Default) { CryptoManager.deriveKey(password, salt, iterations) }

    private fun idOf(fileName: String, prefix: String) =
        fileName.removePrefix(prefix).removeSuffix(SyncRepository.JSON_EXT)
}
