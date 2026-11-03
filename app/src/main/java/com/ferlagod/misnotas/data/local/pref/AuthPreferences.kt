// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

@file:Suppress("DEPRECATION") // security-crypto está deprecado pero no hay sustituto estable equivalente

package com.ferlagod.misnotas.data.local.pref

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ferlagod.misnotas.security.CryptoManager
import javax.crypto.SecretKey

/**
 * Credenciales de Nextcloud / ownCloud.
 */
data class NextcloudCredentials(
    val serverUrl: String,
    val username: String,
    val appPassword: String
)

/**
 * Almacén seguro (EncryptedSharedPreferences, clave en Android Keystore) para credenciales,
 * parámetros de derivación y la clave de cifrado desbloqueada.
 */
class AuthPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    companion object {
        private const val TAG = "AuthPreferences"
        private const val PREFS_FILENAME = "misnotas_secure_auth_prefs"
        private const val KEY_SERVER_URL = "key_server_url"
        private const val KEY_USERNAME = "key_username"
        private const val KEY_APP_PASSWORD = "key_app_password"
        private const val KEY_SALT_BASE64 = "key_salt_base64"
        private const val KEY_ITERATIONS = "key_kdf_iterations"
        private const val KEY_CHECK = "key_check"
        private const val KEY_DERIVED_KEY_BASE64 = "key_derived_key_base64"
        private const val KEY_LAST_SYNC = "key_last_sync"
        const val DEFAULT_FOLDER_NAME = "MisNotas"

        @Volatile
        private var INSTANCE: AuthPreferences? = null

        fun getInstance(context: Context): AuthPreferences =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthPreferences(context).also { INSTANCE = it }
            }

        /**
         * Si el Keystore se ha perdido (p. ej. tras restaurar una copia de seguridad en otro
         * dispositivo), el fichero cifrado es ilegible: se borra y se crea de nuevo.
         */
        private fun createPrefs(context: Context): SharedPreferences {
            fun build(): SharedPreferences {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                return EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILENAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }
            return try {
                build()
            } catch (e: Exception) {
                Log.w(TAG, "Preferencias seguras ilegibles; se reinician", e)
                context.deleteSharedPreferences(PREFS_FILENAME)
                build()
            }
        }
    }

    // ------------------------------------------------------------------
    // Credenciales
    // ------------------------------------------------------------------

    fun saveCredentials(serverUrl: String, username: String, appPassword: String) {
        prefs.edit {
            putString(KEY_SERVER_URL, serverUrl.trim().trimEnd('/'))
            putString(KEY_USERNAME, username.trim())
            putString(KEY_APP_PASSWORD, appPassword.trim())
        }
    }

    fun getCredentials(): NextcloudCredentials? {
        val server = prefs.getString(KEY_SERVER_URL, null)?.takeIf { it.isNotBlank() } ?: return null
        val user = prefs.getString(KEY_USERNAME, null)?.takeIf { it.isNotBlank() } ?: return null
        val pass = prefs.getString(KEY_APP_PASSWORD, null)?.takeIf { it.isNotBlank() } ?: return null
        return NextcloudCredentials(server, user, pass)
    }

    fun hasCredentials(): Boolean = getCredentials() != null

    fun clearCredentials() {
        prefs.edit {
            remove(KEY_SERVER_URL)
            remove(KEY_USERNAME)
            remove(KEY_APP_PASSWORD)
            remove(KEY_LAST_SYNC)
        }
    }

    // ------------------------------------------------------------------
    // Parámetros de la bóveda (salt, iteraciones, verificador)
    // ------------------------------------------------------------------

    fun getSalt(): ByteArray? {
        val saltBase64 = prefs.getString(KEY_SALT_BASE64, null) ?: return null
        return try {
            CryptoManager.saltFromBase64(saltBase64)
        } catch (_: Exception) {
            null
        }
    }

    fun getIterations(): Int = prefs.getInt(KEY_ITERATIONS, CryptoManager.LEGACY_PBKDF2_ITERATIONS)

    fun getKeyCheck(): String? = prefs.getString(KEY_CHECK, null)

    fun saveVaultParams(salt: ByteArray, iterations: Int, keyCheck: String?) {
        prefs.edit {
            putString(KEY_SALT_BASE64, CryptoManager.saltToBase64(salt))
            putInt(KEY_ITERATIONS, iterations)
            if (keyCheck != null) putString(KEY_CHECK, keyCheck) else remove(KEY_CHECK)
        }
    }

    fun clearVaultParams() {
        prefs.edit {
            remove(KEY_SALT_BASE64)
            remove(KEY_ITERATIONS)
            remove(KEY_CHECK)
        }
    }

    // ------------------------------------------------------------------
    // Clave desbloqueada
    // ------------------------------------------------------------------

    fun saveDerivedKey(secretKey: SecretKey) {
        prefs.edit { putString(KEY_DERIVED_KEY_BASE64, CryptoManager.keyToBase64(secretKey)) }
    }

    fun getDerivedKey(): SecretKey? {
        val keyBase64 = prefs.getString(KEY_DERIVED_KEY_BASE64, null) ?: return null
        return try {
            CryptoManager.keyFromBase64(keyBase64)
        } catch (_: Exception) {
            null
        }
    }

    fun clearDerivedKey() {
        prefs.edit { remove(KEY_DERIVED_KEY_BASE64) }
    }

    fun isKeyUnlocked(): Boolean = getDerivedKey() != null

    // ------------------------------------------------------------------
    // Estado
    // ------------------------------------------------------------------

    fun setLastSync(timestamp: Long) {
        prefs.edit { putLong(KEY_LAST_SYNC, timestamp) }
    }

    fun getLastSync(): Long? = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0L }

    /**
     * URL WebDAV de la carpeta de la app:
     * https://servidor/remote.php/dav/files/<usuario>/MisNotas/
     */
    fun getWebDavRootUrl(): String? = getCredentials()?.let { buildWebDavRootUrl(it) }

    fun buildWebDavRootUrl(creds: NextcloudCredentials): String {
        val rawServer = creds.serverUrl.trim().trimEnd('/')
        if (rawServer.contains("/remote.php/dav/files/")) return "$rawServer/"
        val user = java.net.URLEncoder.encode(creds.username, "UTF-8").replace("+", "%20")
        return "$rawServer/remote.php/dav/files/$user/$DEFAULT_FOLDER_NAME/"
    }

    fun clearAll() {
        prefs.edit { clear() }
    }
}
