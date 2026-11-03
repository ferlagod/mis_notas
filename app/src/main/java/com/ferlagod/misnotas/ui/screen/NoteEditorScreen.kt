// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ferlagod.misnotas.data.local.entity.FolderEntity
import com.ferlagod.misnotas.data.local.entity.NoteEntity
import com.ferlagod.misnotas.data.local.model.NoteContentParser
import com.ferlagod.misnotas.data.local.model.NotePayload
import com.ferlagod.misnotas.ui.theme.AccentGreen
import com.ferlagod.misnotas.ui.theme.AccentOrange
import com.ferlagod.misnotas.ui.theme.AccentRed
import com.ferlagod.misnotas.ui.theme.NoteColor
import com.ferlagod.misnotas.ui.theme.RoyalBlueDark
import com.ferlagod.misnotas.ui.theme.RoyalBlueLight
import com.ferlagod.misnotas.ui.theme.getAccentColor
import com.ferlagod.misnotas.ui.theme.toEditorBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.ferlagod.misnotas.ui.markdown.MarkdownReader
import com.ferlagod.misnotas.ui.markdown.toggleTaskLine
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    folderId: String?,
    existingNote: NoteEntity?,
    noteLoaded: Boolean,
    folders: List<FolderEntity>,
    onBack: () -> Unit,
    onSaveNote: (id: String, title: String, content: String, folderId: String?, color: NoteColor, isPinned: Boolean, sync: Boolean) -> Unit,
    onDeleteNote: (String) -> Unit,
    onAddImage: suspend (android.net.Uri) -> String
) {
    val isDark = isSystemInDarkTheme()
    val primaryBlue = if (isDark) RoyalBlueDark else RoyalBlueLight
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    // Id estable: una nota nueva conserva el mismo id en todos los guardados.
    val stableId = rememberSaveable { noteId ?: UUID.randomUUID().toString() }

    var title by rememberSaveable { mutableStateOf("") }
    var textValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var selectedColor by rememberSaveable { mutableStateOf(NoteColor.DEFAULT) }
    var isPinned by rememberSaveable { mutableStateOf(false) }
    var selectedFolderId by rememberSaveable { mutableStateOf(folderId) }
    var updatedAt by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }

    // true cuando el contenido inicial ya se ha cargado (para no guardar una nota vacía encima).
    var loaded by rememberSaveable { mutableStateOf(noteId == null) }
    var deleted by remember { mutableStateOf(false) }

    var isReadingMode by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Carga inicial (solo una vez: una sincronización posterior no pisa lo que se está escribiendo)
    LaunchedEffect(existingNote, noteLoaded) {
        if (loaded) return@LaunchedEffect
        if (existingNote != null) {
            title = existingNote.title
            selectedColor = NoteColor.fromKey(existingNote.color)
            isPinned = existingNote.isPinned
            selectedFolderId = existingNote.folderId
            updatedAt = existingNote.updatedAt
            textValue = TextFieldValue(existingNote.content)
            loaded = true
        } else if (noteLoaded) {
            loaded = true
        }
    }

    fun persistChanges(sync: Boolean) {
        if (!loaded || deleted) return
        onSaveNote(stableId, title, textValue.text, selectedFolderId, selectedColor, isPinned, sync)
    }

    // Autoguardado local mientras se escribe
    LaunchedEffect(title, textValue.text, selectedColor, isPinned, selectedFolderId, loaded) {
        if (!loaded) return@LaunchedEffect
        delay(1200)
        persistChanges(sync = false)
    }

    // Guardar al salir con el gesto/botón "atrás" del sistema y al pasar a segundo plano
    BackHandler {
        persistChanges(sync = true)
        onBack()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        persistChanges(sync = true)
    }

    // Inserción de tokens Markdown en la posición actual del cursor
    fun insertMarkdownToken(prefix: String, suffix: String = "") {
        val currentText = textValue.text
        val selection = textValue.selection
        val selectedText = currentText.substring(selection.start, selection.end)

        val newText = currentText.replaceRange(
            selection.start,
            selection.end,
            "$prefix$selectedText$suffix"
        )
        val newCursor = selection.start + prefix.length + selectedText.length

        textValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    // Selector de imágenes de galería
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val reference = onAddImage(uri)
                    insertMarkdownToken("\n![imagen]($reference)\n")
                } catch (e: Exception) {
                    Toast.makeText(context, "No se pudo añadir la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val editorBackground = selectedColor.toEditorBackground(isDark)

    // Estadísticas estilo Apple Notes
    val wordCount = remember(textValue.text) {
        textValue.text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val charCount = textValue.text.length

    val currentFolder = remember(selectedFolderId, folders) {
        folders.firstOrNull { it.id == selectedFolderId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val formattedDate = remember(updatedAt) {
                            val formatter = SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag("es-ES"))
                            formatter.format(Date(updatedAt))
                        }
                        Text(
                            text = "Editado $formattedDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            persistChanges(sync = true)
                            onBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver",
                            tint = primaryBlue
                        )
                    }
                },
                actions = {
                    // Alternar entre modo edición y modo lectura
                    IconButton(onClick = { isReadingMode = !isReadingMode }) {
                        Icon(
                            imageVector = if (isReadingMode) Icons.Rounded.Edit else Icons.Rounded.Visibility,
                            contentDescription = if (isReadingMode) "Modo Edición" else "Modo Lectura",
                            tint = if (isReadingMode) primaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Botón para fijar nota
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = "Fijar nota",
                            tint = if (isPinned) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    // Botón para eliminar nota si ya existe
                    if (existingNote != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Eliminar",
                                tint = AccentRed.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = editorBackground
                )
            )
        },
        bottomBar = {
            // BARRA FLOTANTE MARKDOWN SOBRE EL TECLADO (Notesnook & Joplin)
            AnimatedVisibility(visible = !isReadingMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToolbarButton(
                            icon = Icons.Rounded.Title,
                            tint = primaryBlue,
                            onClick = { insertMarkdownToken("# ") }
                        )

                        ToolbarButton(
                            icon = Icons.Rounded.FormatBold,
                            tint = primaryBlue,
                            onClick = { insertMarkdownToken("**", "**") }
                        )

                        ToolbarButton(
                            icon = Icons.Rounded.FormatItalic,
                            tint = primaryBlue,
                            onClick = { insertMarkdownToken("*", "*") }
                        )

                        ToolbarButton(
                            icon = Icons.Rounded.FormatStrikethrough,
                            tint = primaryBlue,
                            onClick = { insertMarkdownToken("~~", "~~") }
                        )

                        ToolbarButton(
                            icon = Icons.AutoMirrored.Rounded.List,
                            tint = primaryBlue,
                            onClick = { insertMarkdownToken("- ") }
                        )

                        ToolbarButton(
                            icon = Icons.Rounded.CheckBox,
                            tint = primaryBlue,
                            onClick = { insertMarkdownToken("- [ ] ") }
                        )

                        ToolbarButton(
                            icon = Icons.Rounded.FormatQuote,
                            tint = primaryBlue,
                            onClick = { insertMarkdownToken("> ") }
                        )

                        ToolbarButton(
                            icon = Icons.Rounded.Code,
                            tint = primaryBlue,
                            onClick = { insertMarkdownToken("`", "`") }
                        )

                        ToolbarButton(
                            icon = Icons.Rounded.HorizontalRule,
                            tint = primaryBlue,
                            onClick = { insertMarkdownToken("\n---\n") }
                        )

                        ToolbarButton(
                            icon = Icons.Rounded.Image,
                            tint = primaryBlue,
                            onClick = { photoPickerLauncher.launch("image/*") }
                        )
                    }
                }
            }
        },
        containerColor = editorBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // SELECTOR DE CARPETA (Joplin / Notesnook) & COLOR DE NOTA (Google Keep)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip selector de carpeta
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.clickable { showFolderPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val folderTint = currentFolder?.let { NoteColor.fromKey(it.color).getAccentColor(isDark) } ?: primaryBlue
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = folderTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentFolder?.name ?: "Sin carpeta",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Paleta de colores interactiva estilo Google Keep
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NoteColor.entries.take(6).forEach { noteColor ->
                        val color = noteColor.getAccentColor(isDark)
                        val isSelected = selectedColor == noteColor

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = noteColor }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TÍTULO DE LA NOTA
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(primaryBlue),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text(
                            text = "Título",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // CONTENIDO: MODO EDICIÓN O MODO LECTURA
            Crossfade(targetState = isReadingMode, label = "editor_mode_crossfade") { reading ->
                if (reading) {
                    MarkdownReader(
                        markdownText = textValue.text,
                        primaryBlue = primaryBlue,
                        onToggleChecklist = { lineIndex ->
                            textValue = TextFieldValue(toggleTaskLine(textValue.text, lineIndex))
                        }
                    )
                } else {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(primaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 350.dp),
                        decorationBox = { innerTextField ->
                            if (textValue.text.isEmpty()) {
                                Text(
                                    text = "Escribe tu nota aquí... Usa formato Markdown, listas de tareas o adjunta fotos.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                    lineHeight = 24.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contador de palabras y caracteres estilo Apple Notes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$wordCount palabras  •  $charCount caracteres",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // DIÁLOGO PARA ASIGNAR CARPETA
    if (showFolderPicker) {
        AlertDialog(
            onDismissRequest = { showFolderPicker = false },
            title = { Text("Mover a carpeta", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Opción Sin Carpeta
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedFolderId = null
                                showFolderPicker = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📁 Sin carpeta (General)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedFolderId == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedFolderId == null) primaryBlue else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    folders.forEach { folder ->
                        val fTint = NoteColor.fromKey(folder.color).getAccentColor(isDark)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFolderId = folder.id
                                    showFolderPicker = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = fTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedFolderId == folder.id) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedFolderId == folder.id) primaryBlue else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderPicker = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // DIÁLOGO DE CONFIRMACIÓN DE BORRADO
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Mover a papelera?", fontWeight = FontWeight.Bold) },
            text = { Text("La nota se enviará a la papelera. Podrás restaurarla cuando desees.") },
            confirmButton = {
                Button(
                    onClick = {
                        // Guardar antes los últimos cambios para que queden en la papelera
                        persistChanges(sync = false)
                        deleted = true
                        onDeleteNote(stableId)
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Mover a papelera", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
