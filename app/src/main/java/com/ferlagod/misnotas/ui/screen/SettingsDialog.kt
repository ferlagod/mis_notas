// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ferlagod.misnotas.BuildConfig
import com.ferlagod.misnotas.data.local.pref.NextcloudCredentials
import com.ferlagod.misnotas.data.sync.SyncState
import androidx.compose.ui.platform.LocalUriHandler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.ferlagod.misnotas.ui.theme.AccentGreen
import com.ferlagod.misnotas.ui.theme.AccentOrange
import com.ferlagod.misnotas.ui.theme.AccentRed
import com.ferlagod.misnotas.ui.theme.RoyalBlueDark
import com.ferlagod.misnotas.ui.theme.RoyalBlueLight

@Composable
fun SettingsDialog(
    isConfigured: Boolean,
    currentCredentials: NextcloudCredentials?,
    syncState: SyncState,
    onDismiss: () -> Unit,
    onSaveNextcloud: (serverUrl: String, user: String, appPass: String, masterPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onDisconnectNextcloud: () -> Unit,
    onTriggerSync: () -> Unit,
    lastSync: Long? = null
) {
    val isDark = isSystemInDarkTheme()
    val primaryBlue = if (isDark) RoyalBlueDark else RoyalBlueLight
    val focusManager = LocalFocusManager.current

    var serverUrl by remember { mutableStateOf(currentCredentials?.serverUrl ?: "https://") }
    var username by remember { mutableStateOf(currentCredentials?.username ?: "") }
    var appPassword by remember { mutableStateOf(currentCredentials?.appPassword ?: "") }
    var masterPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var masterPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showDisconnectConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 700.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Cabecera del diálogo con título y botón cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(primaryBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                tint = primaryBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ajustes",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sincronización y Cifrado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contenido desplazable
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Tarjeta de estado de sincronización
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isConfigured) AccentGreen.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isConfigured) Icons.Rounded.CloudSync else Icons.Rounded.CloudOff,
                                    contentDescription = null,
                                    tint = if (isConfigured) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isConfigured) "Nextcloud Conectado" else "Modo Local (Sin sincronización)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isConfigured) {
                                        "${currentCredentials?.username ?: ""} • ${currentCredentials?.serverUrl ?: ""}"
                                    } else {
                                        "Tus notas se guardan solo en este dispositivo de forma privada."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isConfigured) {
                                    val statusText = when (syncState) {
                                        is SyncState.Syncing -> syncState.message
                                        is SyncState.Error -> "Error: ${syncState.message}"
                                        is SyncState.Success -> "Sincronizado: ${formatSyncTime(syncState.timestamp)}" +
                                            if (syncState.warnings > 0) " (${syncState.warnings} ficheros no se pudieron leer)" else ""
                                        SyncState.Idle -> lastSync?.let { "Última sincronización: ${formatSyncTime(it)}" } ?: "Pendiente de sincronizar"
                                    }
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (syncState is SyncState.Error) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sección: SERVIDOR WEBDAV / NEXTCLOUD
                    Text(
                        text = "CUENTA NEXTCLOUD",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = primaryBlue
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it; errorMessage = null; successMessage = null },
                        placeholder = "https://nube.tu-servidor.com",
                        leadingIcon = Icons.Rounded.Storage,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsTextField(
                        value = username,
                        onValueChange = { username = it; errorMessage = null; successMessage = null },
                        placeholder = "Usuario de Nextcloud",
                        leadingIcon = Icons.Rounded.Person,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsTextField(
                        value = appPassword,
                        onValueChange = { appPassword = it; errorMessage = null; successMessage = null },
                        placeholder = "Contraseña de aplicación",
                        leadingIcon = Icons.Rounded.Key,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sección: CIFRADO E2EE
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = primaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CIFRADO DE EXTREMO A EXTREMO (E2EE)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = primaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tus notas se cifran con AES-256 en este dispositivo antes de subirse. Usa la misma contraseña maestra en todos tus dispositivos. Si la olvidas, nadie podrá recuperar tus notas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsTextField(
                        value = masterPassword,
                        onValueChange = { masterPassword = it; errorMessage = null; successMessage = null },
                        placeholder = "Contraseña maestra",
                        leadingIcon = Icons.Rounded.Lock,
                        isPassword = true,
                        passwordVisible = masterPasswordVisible,
                        onTogglePassword = { masterPasswordVisible = !masterPasswordVisible },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )

                    // Mensajes de error o éxito
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (successMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = successMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Botón para guardar / conectar
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (serverUrl.isBlank() || username.isBlank() || appPassword.isBlank()) {
                                errorMessage = "Servidor, usuario y contraseña son obligatorios"
                                return@Button
                            }
                            if (masterPassword.isBlank()) {
                                errorMessage = "Introduce la contraseña maestra para el cifrado E2EE"
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null
                            successMessage = null

                            onSaveNextcloud(
                                serverUrl.trim(),
                                username.trim(),
                                appPassword.trim(),
                                masterPassword,
                                {
                                    isLoading = false
                                    successMessage = "Cuenta conectada. Sincronizando..."
                                },
                                { error ->
                                    isLoading = false
                                    errorMessage = error
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isConfigured) "Actualizar y Sincronizar" else "Conectar y Cifrar",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    // Botones si ya está configurado: Sincronizar ahora / Desconectar
                    if (isConfigured) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onTriggerSync()
                                    successMessage = "Sincronización iniciada"
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sincronizar")
                            }

                            OutlinedButton(
                                onClick = { showDisconnectConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AccentRed
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = AccentRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Desconectar")
                            }
                        }

                        AnimatedVisibility(visible = showDisconnectConfirm) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                                    .background(
                                        AccentRed.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "¿Desconectar de Nextcloud?",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AccentRed
                                )
                                Text(
                                    text = "Volverás a modo local. Las notas almacenadas en este dispositivo se conservarán.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { showDisconnectConfirm = false }) {
                                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            showDisconnectConfirm = false
                                            onDisconnectNextcloud()
                                            serverUrl = "https://"
                                            username = ""
                                            appPassword = ""
                                            masterPassword = ""
                                            successMessage = "Cuenta desconectada. Modo local activo."
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Sí, desconectar", color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    AboutSection(primaryBlue = primaryBlue)
                }
            }
        }
    }
}

private fun formatSyncTime(timestamp: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag("es-ES")).format(Date(timestamp))

@Composable
private fun AboutSection(primaryBlue: Color) {
    val uriHandler = LocalUriHandler.current
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "ACERCA DE",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Mis Notas ${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text = "Software libre bajo licencia GNU GPL v3 o posterior. Se distribuye SIN NINGUNA GARANTÍA.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row {
        TextButton(onClick = { uriHandler.openUri(REPO_URL) }) {
            Text("Código fuente", color = primaryBlue)
        }
        TextButton(onClick = { uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.html") }) {
            Text("Licencia", color = primaryBlue)
        }
    }
}

private const val REPO_URL = "https://github.com/ferlagod/mis_notas"

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = "Alternar contraseña",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = Modifier.fillMaxWidth()
    )
}
