// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.ferlagod.misnotas.data.attachment.AttachmentStore
import com.ferlagod.misnotas.ui.markdown.markdownPreview
import com.ferlagod.misnotas.data.local.entity.FolderEntity
import com.ferlagod.misnotas.data.local.entity.NoteEntity
import com.ferlagod.misnotas.data.local.model.NoteContentParser
import com.ferlagod.misnotas.ui.theme.AccentOrange
import com.ferlagod.misnotas.ui.theme.AccentRed
import com.ferlagod.misnotas.ui.theme.NoteColor
import com.ferlagod.misnotas.ui.theme.RoyalBlueDark
import com.ferlagod.misnotas.ui.theme.RoyalBlueLight
import com.ferlagod.misnotas.ui.theme.getAccentColor
import com.ferlagod.misnotas.ui.theme.toCardBackground
import com.ferlagod.misnotas.ui.theme.toCardBorder
import com.ferlagod.misnotas.ui.viewmodel.NotesFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    notes: List<NoteEntity>,
    foldersMap: Map<String, FolderEntity>,
    currentFilter: NotesFilter,
    searchQuery: String,
    isGridLayout: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleLayout: () -> Unit,
    onBackToFolders: () -> Unit,
    onOpenNote: (noteId: String?) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit,
    onRestoreNote: (String) -> Unit,
    onDeletePermanently: (String) -> Unit,
    onEmptyTrash: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val primaryBlue = if (isDark) RoyalBlueDark else RoyalBlueLight
    val isTrashView = currentFilter is NotesFilter.Trash

    var showEmptyTrashConfirm by remember { mutableStateOf(false) }

    val filterTitle = when (currentFilter) {
        is NotesFilter.All -> "Todas las notas"
        is NotesFilter.Pinned -> "Fijadas"
        is NotesFilter.Trash -> "Papelera"
        is NotesFilter.Folder -> currentFilter.folderName
    }

    val pinnedNotes = notes.filter { it.isPinned }
    val otherNotes = notes.filter { !it.isPinned }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 12.dp, bottom = 8.dp, start = 12.dp, end = 12.dp)
            ) {
                // Barra superior de navegación estilo Apple Notes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackToFolders) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver a carpetas",
                            tint = primaryBlue
                        )
                    }

                    Text(
                        text = filterTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Alternar Cuadrícula / Lista estilo Google Keep
                    IconButton(onClick = onToggleLayout) {
                        Icon(
                            imageVector = if (isGridLayout) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                            contentDescription = "Cambiar vista",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Barra de búsqueda integrada estilo Google Keep (SurfaceVariant, sin bordes duros)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(primaryBlue),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = if (isTrashView) "Buscar en papelera..." else "Busca en tus notas...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Banner especial si estamos en la papelera
                if (isTrashView) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentRed.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${notes.size} nota(s) en papelera",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (notes.isNotEmpty()) {
                                TextButton(
                                    onClick = { showEmptyTrashConfirm = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = AccentRed)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteForever,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Vaciar",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isTrashView) {
                FloatingActionButton(
                    onClick = { onOpenNote(null) },
                    containerColor = primaryBlue,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Nueva nota",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when {
                            searchQuery.isNotBlank() -> "Sin resultados para \"$searchQuery\""
                            isTrashView -> "La papelera está vacía"
                            else -> "No hay notas en esta sección"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            if (isGridLayout) {
                // Vista en Cuadrícula Escalonada (Google Keep)
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp
                ) {
                    if (!isTrashView && pinnedNotes.isNotEmpty()) {
                        item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                            SectionHeader(title = "FIJADAS")
                        }
                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                folder = note.folderId?.let { foldersMap[it] },
                                isDark = isDark,
                                isTrashView = isTrashView,
                                onCardClick = { onOpenNote(note.id) },
                                onTogglePin = { onTogglePin(note) },
                                onDeleteNote = { onDeleteNote(note.id) },
                                onRestoreNote = { onRestoreNote(note.id) },
                                onDeletePermanently = { onDeletePermanently(note.id) }
                            )
                        }
                    }

                    val notesToShow = if (isTrashView) notes else otherNotes
                    if (notesToShow.isNotEmpty()) {
                        if (!isTrashView && pinnedNotes.isNotEmpty()) {
                            item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                SectionHeader(title = "OTRAS")
                            }
                        }
                        items(notesToShow, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                folder = note.folderId?.let { foldersMap[it] },
                                isDark = isDark,
                                isTrashView = isTrashView,
                                onCardClick = { if (!isTrashView) onOpenNote(note.id) },
                                onTogglePin = { onTogglePin(note) },
                                onDeleteNote = { onDeleteNote(note.id) },
                                onRestoreNote = { onRestoreNote(note.id) },
                                onDeletePermanently = { onDeletePermanently(note.id) }
                            )
                        }
                    }
                }
            } else {
                // Vista en Lista Vertical (Apple Notes)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isTrashView && pinnedNotes.isNotEmpty()) {
                        item { SectionHeader(title = "FIJADAS") }
                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                folder = note.folderId?.let { foldersMap[it] },
                                isDark = isDark,
                                isTrashView = isTrashView,
                                onCardClick = { onOpenNote(note.id) },
                                onTogglePin = { onTogglePin(note) },
                                onDeleteNote = { onDeleteNote(note.id) },
                                onRestoreNote = { onRestoreNote(note.id) },
                                onDeletePermanently = { onDeletePermanently(note.id) }
                            )
                        }
                    }

                    val notesToShow = if (isTrashView) notes else otherNotes
                    if (notesToShow.isNotEmpty()) {
                        if (!isTrashView && pinnedNotes.isNotEmpty()) {
                            item { SectionHeader(title = "OTRAS") }
                        }
                        items(notesToShow, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                folder = note.folderId?.let { foldersMap[it] },
                                isDark = isDark,
                                isTrashView = isTrashView,
                                onCardClick = { if (!isTrashView) onOpenNote(note.id) },
                                onTogglePin = { onTogglePin(note) },
                                onDeleteNote = { onDeleteNote(note.id) },
                                onRestoreNote = { onRestoreNote(note.id) },
                                onDeletePermanently = { onDeletePermanently(note.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = { Text("¿Vaciar papelera?", fontWeight = FontWeight.Bold) },
            text = { Text("Todas las notas de la papelera se eliminarán permanentemente. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        onEmptyTrash()
                        showEmptyTrashConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Vaciar papelera", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun NoteCard(
    note: NoteEntity,
    folder: FolderEntity?,
    isDark: Boolean,
    isTrashView: Boolean,
    onCardClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDeleteNote: () -> Unit,
    onRestoreNote: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val noteColor = NoteColor.fromKey(note.color)
    val cardBackground = noteColor.toCardBackground(isDark)
    val cardBorder = noteColor.toCardBorder(isDark)

    var showCardMenu by remember { mutableStateOf(false) }

    val formattedDate = remember(note.updatedAt) {
        val formatter = SimpleDateFormat("d MMM", Locale.forLanguageTag("es-ES"))
        formatter.format(Date(note.updatedAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Fila superior con título y acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (!isTrashView) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PushPin,
                                contentDescription = "Fijar",
                                tint = if (note.isPinned) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showCardMenu = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Más",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showCardMenu,
                                onDismissRequest = { showCardMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (note.isPinned) "Desfijar" else "Fijar") },
                                    leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showCardMenu = false
                                        onTogglePin()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar nota", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showCardMenu = false
                                        onDeleteNote()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Miniatura del primer adjunto de imagen
            val context = LocalContext.current
            val firstImage = remember(note.content) {
                AttachmentStore.firstReferencedId(note.content)?.let { AttachmentStore.fileFor(context, it) }
                    ?: extractFirstDataUri(note.content)
            }
            if (firstImage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = firstImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            // Limpieza y previsualización de texto
            val cleanContent = remember(note.content) { markdownPreview(note.content).trim() }

            if (cleanContent.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = cleanContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pie de tarjeta con Etiqueta de Carpeta (Joplin / Notesnook) y Fecha (Apple Notes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (folder != null) {
                    val folderTint = NoteColor.fromKey(folder.color).getAccentColor(isDark)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = folderTint.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = folderTint,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = folderTint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Botones de acción en papelera
            if (isTrashView) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onRestoreNote,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestoreFromTrash,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restaurar", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = onDeletePermanently,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun extractFirstDataUri(content: String): String? {
    val regex = Regex("data:image/[^;]+;base64,[A-Za-z0-9+/=]+")
    return regex.find(content)?.value
}
