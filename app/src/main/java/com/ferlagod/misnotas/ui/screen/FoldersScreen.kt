// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.ui.screen

import com.ferlagod.misnotas.R
import com.ferlagod.misnotas.util.AppLanguage
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Note
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferlagod.misnotas.data.local.entity.FolderEntity
import com.ferlagod.misnotas.data.local.entity.NoteEntity
import com.ferlagod.misnotas.data.local.pref.NextcloudCredentials
import com.ferlagod.misnotas.data.sync.SyncState
import com.ferlagod.misnotas.ui.theme.AccentGreen
import com.ferlagod.misnotas.ui.theme.AccentOrange
import com.ferlagod.misnotas.ui.theme.AccentRed
import com.ferlagod.misnotas.ui.theme.NoteColor
import com.ferlagod.misnotas.ui.theme.RoyalBlueDark
import com.ferlagod.misnotas.ui.theme.RoyalBlueLight
import com.ferlagod.misnotas.ui.theme.getAccentColor
import com.ferlagod.misnotas.ui.viewmodel.NotesFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    isConfigured: Boolean,
    folders: List<FolderEntity>,
    notes: List<NoteEntity>,
    totalNotesCount: Int,
    pinnedNotesCount: Int,
    trashNotesCount: Int,
    syncState: SyncState,
    onSelectFilter: (NotesFilter) -> Unit,
    onCreateFolder: (name: String, color: NoteColor, parentId: String?) -> Unit,
    onUpdateFolder: (id: String, name: String, color: NoteColor, parentId: String?) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onTriggerSync: () -> Unit,
    onLockApp: () -> Unit,
    onConfigureNextcloud: (serverUrl: String, user: String, appPass: String, masterPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onDisconnectNextcloud: () -> Unit,
    getCredentials: () -> NextcloudCredentials?,
    getLastSync: () -> Long? = { null },
    currentLanguage: AppLanguage = AppLanguage.SYSTEM,
    onLanguageSelected: (AppLanguage) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val primaryBlue = if (isDark) RoyalBlueDark else RoyalBlueLight

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var parentForNewFolder by remember { mutableStateOf<String?>(null) }
    var folderToEdit by remember { mutableStateOf<FolderEntity?>(null) }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.folders_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    // Botón/Indicador de sincronización con estado cromático
                    SyncStatusIconButton(
                        isConfigured = isConfigured,
                        syncState = syncState,
                        onClick = {
                            if (isConfigured) {
                                onTriggerSync()
                            } else {
                                showSettingsDialog = true
                            }
                        }
                    )

                    // Ajustes de sincronización y cifrado
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Bloquear bóveda (solo visible si Nextcloud / clave maestra están configuradas)
                    if (isConfigured) {
                        IconButton(onClick = onLockApp) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = stringResource(R.string.cd_lock),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            parentForNewFolder = null
                            showCreateFolderDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = primaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.new_folder),
                            color = primaryBlue,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ACCESOS RÁPIDOS SUPERIORES (Estilo Inset Grouped List de Apple Notes)
            Text(
                text = stringResource(R.string.quick_views),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp, top = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    QuickAccessRow(
                        title = stringResource(R.string.all_notes),
                        icon = Icons.AutoMirrored.Rounded.Note,
                        iconTint = primaryBlue,
                        count = totalNotesCount,
                        onClick = { onSelectFilter(NotesFilter.All) }
                    )

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(start = 52.dp)
                    )

                    QuickAccessRow(
                        title = stringResource(R.string.pinned),
                        icon = Icons.Rounded.PushPin,
                        iconTint = AccentOrange,
                        count = pinnedNotesCount,
                        onClick = { onSelectFilter(NotesFilter.Pinned) }
                    )

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(start = 52.dp)
                    )

                    QuickAccessRow(
                        title = stringResource(R.string.trash),
                        icon = Icons.Rounded.Delete,
                        iconTint = AccentRed,
                        count = trashNotesCount,
                        onClick = { onSelectFilter(NotesFilter.Trash) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // LISTA DE CARPETAS JERÁRQUICAS (Joplin / Notesnook organization)
            Text(
                text = stringResource(R.string.my_folders),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )

            if (folders.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_folders),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Construir árbol de carpetas con jerarquía y sangrado
                        val rootFolders = folders.filter { it.parentId == null }
                        val childMap = folders.filter { it.parentId != null }.groupBy { it.parentId }

                        val flattenedTree = mutableListOf<Pair<FolderEntity, Int>>()
                        fun addNode(folder: FolderEntity, level: Int) {
                            flattenedTree.add(folder to level)
                            childMap[folder.id]?.forEach { child ->
                                addNode(child, level + 1)
                            }
                        }
                        rootFolders.forEach { addNode(it, 0) }

                        // Carpetas huérfanas si existieran
                        val addedIds = flattenedTree.map { it.first.id }.toSet()
                        folders.filterNot { addedIds.contains(it.id) }.forEach {
                            flattenedTree.add(it to 0)
                        }

                        flattenedTree.forEachIndexed { index, (folder, level) ->
                            val folderNotesCount = notes.count { it.folderId == folder.id && !it.isDeleted }
                            val noteColor = NoteColor.fromKey(folder.color)
                            val tint = noteColor.getAccentColor(isDark)

                            FolderRow(
                                name = folder.name,
                                tint = tint,
                                count = folderNotesCount,
                                indentationLevel = level,
                                onClick = {
                                    onSelectFilter(NotesFilter.Folder(folder.id, folder.name))
                                },
                                onAddSubfolder = {
                                    parentForNewFolder = folder.id
                                    showCreateFolderDialog = true
                                },
                                onEdit = {
                                    folderToEdit = folder
                                },
                                onDelete = {
                                    folderToDelete = folder
                                }
                            )

                            if (index < flattenedTree.lastIndex) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(start = (52 + level * 16).dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // DIÁLOGO PARA CREAR CARPETA / SUBCARPETA
    if (showCreateFolderDialog) {
        FolderDialog(
            title = if (parentForNewFolder != null) stringResource(R.string.new_subfolder) else stringResource(R.string.new_folder),
            initialName = "",
            initialColor = NoteColor.DEFAULT,
            initialParentId = parentForNewFolder,
            existingFolders = folders,
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name, color, parentId ->
                onCreateFolder(name, color, parentId)
                showCreateFolderDialog = false
            }
        )
    }

    // DIÁLOGO PARA EDITAR CARPETA
    folderToEdit?.let { folder ->
        FolderDialog(
            title = stringResource(R.string.edit_folder),
            initialName = folder.name,
            initialColor = NoteColor.fromKey(folder.color),
            initialParentId = folder.parentId,
            existingFolders = folders,
            currentFolderId = folder.id,
            onDismiss = { folderToEdit = null },
            onConfirm = { name, color, parentId ->
                onUpdateFolder(folder.id, name, color, parentId)
                folderToEdit = null
            }
        )
    }

    // DIÁLOGO PARA CONFIRMAR ELIMINACIÓN DE CARPETA
    folderToDelete?.let { folder ->
        DeleteFolderConfirmDialog(
            folderName = folder.name,
            onDismiss = { folderToDelete = null },
            onConfirm = {
                onDeleteFolder(folder.id)
                folderToDelete = null
            }
        )
    }

    // DIÁLOGO DE AJUSTES (SINCRONIZACIÓN Y CIFRADO)
    if (showSettingsDialog) {
        SettingsDialog(
            isConfigured = isConfigured,
            currentCredentials = getCredentials(),
            syncState = syncState,
            onDismiss = { showSettingsDialog = false },
            onSaveNextcloud = onConfigureNextcloud,
            onDisconnectNextcloud = onDisconnectNextcloud,
            onTriggerSync = onTriggerSync,
            lastSync = getLastSync(),
            currentLanguage = currentLanguage,
            onLanguageSelected = onLanguageSelected
        )
    }
}

@Composable
private fun QuickAccessRow(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FolderRow(
    name: String,
    tint: Color,
    count: Int,
    indentationLevel: Int,
    onClick: () -> Unit,
    onAddSubfolder: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = (16 + indentationLevel * 18).dp,
                top = 10.dp,
                bottom = 10.dp,
                end = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Folder,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.cd_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.new_subfolder)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        showMenu = false
                        onAddSubfolder()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_folder)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        showMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_folder), color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun SyncStatusIconButton(
    isConfigured: Boolean,
    syncState: SyncState,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotate")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val (icon, tint, isRotating, tooltip) = if (!isConfigured) {
        SyncIconData(Icons.Rounded.CloudOff, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), false, stringResource(R.string.sync_configure))
    } else {
        when (syncState) {
            is SyncState.Idle -> SyncIconData(Icons.Rounded.Sync, MaterialTheme.colorScheme.onSurfaceVariant, false, stringResource(R.string.sync_now))
            is SyncState.Syncing -> SyncIconData(Icons.Rounded.Sync, AccentOrange, true, stringResource(R.string.sync_in_progress))
            is SyncState.Success -> SyncIconData(Icons.Rounded.CheckCircle, AccentGreen, false, stringResource(R.string.sync_done))
            is SyncState.Error -> SyncIconData(Icons.Rounded.ErrorOutline, AccentRed, false, stringResource(R.string.sync_error))
        }
    }

    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = tint,
            modifier = Modifier
                .size(22.dp)
                .rotate(if (isRotating) angle else 0f)
        )
    }
}

private data class SyncIconData(
    val icon: ImageVector,
    val tint: Color,
    val isRotating: Boolean,
    val tooltip: String
)

@Composable
private fun FolderDialog(
    title: String,
    initialName: String = "",
    initialColor: NoteColor = NoteColor.DEFAULT,
    initialParentId: String? = null,
    existingFolders: List<FolderEntity>,
    currentFolderId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: NoteColor, parentId: String?) -> Unit
) {
    var folderName by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedParentId by remember { mutableStateOf(initialParentId) }
    val isDark = isSystemInDarkTheme()

    // Filtrar carpetas elegibles como padre (evitar auto-paternidad)
    val eligibleParents = existingFolders.filter { it.id != currentFolderId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text(stringResource(R.string.folder_name_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.folder_color),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NoteColor.entries.forEach { noteColor ->
                        val color = noteColor.getAccentColor(isDark)
                        val isSelected = selectedColor == noteColor

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = noteColor }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                if (eligibleParents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.parent_folder),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Opción Ninguna (Carpeta raíz)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedParentId = null }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.parent_none),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedParentId == null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedParentId == null) (if (isDark) RoyalBlueDark else RoyalBlueLight) else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            eligibleParents.forEach { parent ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedParentId = parent.id }
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "↳ ${parent.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selectedParentId == parent.id) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedParentId == parent.id) (if (isDark) RoyalBlueDark else RoyalBlueLight) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onConfirm(folderName, selectedColor, selectedParentId)
                    }
                },
                enabled = folderName.isNotBlank()
            ) {
                Text(
                    text = stringResource(R.string.save),
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) RoyalBlueDark else RoyalBlueLight
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun DeleteFolderConfirmDialog(
    folderName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.delete_folder_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_folder_message, folderName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.delete), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
