// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ferlagod.misnotas.R
import com.ferlagod.misnotas.data.local.entity.FolderEntity
import com.ferlagod.misnotas.data.local.entity.NoteEntity
import com.ferlagod.misnotas.ui.components.ColorPalette
import com.ferlagod.misnotas.ui.components.FolderPickerDialog
import com.ferlagod.misnotas.ui.components.ZoomableImageDialog
import com.ferlagod.misnotas.ui.markdown.LinePrefix
import com.ferlagod.misnotas.ui.markdown.MarkdownReader
import com.ferlagod.misnotas.ui.markdown.TextEdit
import com.ferlagod.misnotas.ui.markdown.continueList
import com.ferlagod.misnotas.ui.markdown.insertAtCursor
import com.ferlagod.misnotas.ui.markdown.markdownPlainText
import com.ferlagod.misnotas.ui.markdown.toggleLinePrefix
import com.ferlagod.misnotas.ui.markdown.toggleTaskLine
import com.ferlagod.misnotas.ui.markdown.toggleWrap
import com.ferlagod.misnotas.ui.theme.AccentOrange
import com.ferlagod.misnotas.ui.theme.AccentRed
import com.ferlagod.misnotas.ui.theme.NoteColor
import com.ferlagod.misnotas.ui.theme.getAccentColor
import com.ferlagod.misnotas.ui.theme.isAppInDarkTheme
import com.ferlagod.misnotas.ui.theme.toEditorBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Historial de deshacer/rehacer del cuerpo de la nota. Agrupa lo escrito seguido en un solo paso
 * (se corta tras una pausa, un espacio o un salto de línea), como los editores de texto habituales.
 */
private class EditHistory {
    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var lastRecord = 0L

    /** Cambia con cada operación para que la interfaz se recomponga. */
    var version by mutableIntStateOf(0)
        private set

    val canUndo get() = version >= 0 && undoStack.isNotEmpty()
    val canRedo get() = version >= 0 && redoStack.isNotEmpty()

    fun record(before: TextFieldValue, after: TextFieldValue, force: Boolean = false) {
        if (before.text == after.text) return
        val now = SystemClock.uptimeMillis()
        val delta = after.text.length - before.text.length
        val boundary = force || undoStack.isEmpty() || now - lastRecord > 1200 || delta !in -1..1 ||
            after.text.getOrNull(after.selection.start - 1)?.isWhitespace() == true
        if (boundary) {
            // Sin la composición del teclado: restaurarla después confundiría al IME.
            undoStack.addLast(before.copy(composition = null))
            if (undoStack.size > MAX) undoStack.removeFirst()
        }
        lastRecord = now
        redoStack.clear()
        version++
    }

    fun undo(current: TextFieldValue): TextFieldValue? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current.copy(composition = null))
        lastRecord = 0
        version++
        return previous
    }

    fun redo(current: TextFieldValue): TextFieldValue? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current.copy(composition = null))
        lastRecord = 0
        version++
        return next
    }

    private companion object {
        const val MAX = 200
    }
}

private fun TextFieldValue.apply(edit: TextEdit) =
    TextFieldValue(edit.text, TextRange(edit.selectionStart, edit.selectionEnd))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    folderId: String?,
    initialPinned: Boolean,
    existingNote: NoteEntity?,
    noteLoaded: Boolean,
    folders: List<FolderEntity>,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSaveNote: (id: String, title: String, content: String, folderId: String?, color: NoteColor, isPinned: Boolean, sync: Boolean, closing: Boolean) -> Unit,
    onDeleteNote: (String) -> Unit,
    onDuplicateNote: suspend (String) -> String?,
    onOpenNote: (String) -> Unit,
    onAddImage: suspend (Uri) -> String,
    /** Nota recién creada (con id ya asignado o sin él): el cursor va directo al texto. */
    isNewNote: Boolean = noteId == null
) {
    val isDark = isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val imageAlt = stringResource(R.string.image_alt)
    val imageAddFailed = stringResource(R.string.image_add_failed)
    val copiedMessage = stringResource(R.string.copied_to_clipboard)

    // Id estable: una nota nueva conserva el mismo id en todos los guardados.
    val stableId = rememberSaveable { noteId ?: UUID.randomUUID().toString() }

    var title by rememberSaveable { mutableStateOf("") }
    var textValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var selectedColor by rememberSaveable { mutableStateOf(NoteColor.DEFAULT) }
    var isPinned by rememberSaveable { mutableStateOf(initialPinned) }
    var selectedFolderId by rememberSaveable { mutableStateOf(folderId) }
    var updatedAt by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }

    // true cuando el contenido inicial ya se ha cargado (para no guardar una nota vacía encima).
    var loaded by rememberSaveable { mutableStateOf(noteId == null) }
    var deleted by remember { mutableStateOf(false) }
    // Tras el primer guardado de una nota nueva ya se puede borrar, duplicar, etc.
    var dirty by remember { mutableStateOf(false) }

    var isReadingMode by rememberSaveable { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var viewerImage by remember { mutableStateOf<Any?>(null) }

    val history = remember { EditHistory() }
    val bodyFocus = remember { FocusRequester() }

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

    // Nota nueva: el cursor va directo al cuerpo para escribir sin un toque extra.
    LaunchedEffect(Unit) {
        if (isNewNote) {
            delay(150)
            runCatching { bodyFocus.requestFocus() }
        }
    }

    fun persistChanges(sync: Boolean, closing: Boolean = false) {
        if (!loaded || deleted) return
        onSaveNote(stableId, title, textValue.text, selectedFolderId, selectedColor, isPinned, sync, closing)
    }

    fun close() {
        persistChanges(sync = true, closing = true)
        onBack()
    }

    // Autoguardado local mientras se escribe
    LaunchedEffect(title, textValue.text, selectedColor, isPinned, selectedFolderId, loaded) {
        if (!loaded) return@LaunchedEffect
        delay(1000)
        persistChanges(sync = false)
        if (title.isNotBlank() || textValue.text.isNotBlank()) {
            if (dirty || existingNote == null) updatedAt = System.currentTimeMillis()
            dirty = true
        }
    }

    // Guardar al salir con el gesto/botón "atrás" del sistema y al pasar a segundo plano
    BackHandler { close() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { persistChanges(sync = true) }
    // Al cerrar el editor sin pasar por "atrás" (p. ej. cambiar de nota en el panel lateral
    // de un plegable) se guarda también lo escrito en el último segundo.
    DisposableEffect(Unit) { onDispose { persistChanges(sync = true) } }

    fun updateBody(newValue: TextFieldValue, forceHistory: Boolean = false) {
        history.record(textValue, newValue, forceHistory)
        textValue = newValue
    }

    fun applyEdit(edit: TextEdit) = updateBody(textValue.apply(edit), forceHistory = true)

    fun onBodyChange(newValue: TextFieldValue) {
        val continued = if (newValue.selection.collapsed) {
            continueList(textValue.text, newValue.text, newValue.selection.start)
        } else null
        updateBody(if (continued != null) newValue.apply(continued) else newValue)
    }

    fun linePrefix(prefix: LinePrefix) {
        val sel = textValue.selection
        applyEdit(toggleLinePrefix(textValue.text, sel.min, sel.max, prefix))
    }

    fun wrap(prefix: String, suffix: String = prefix) {
        val sel = textValue.selection
        applyEdit(toggleWrap(textValue.text, sel.min, sel.max, prefix, suffix))
    }

    fun insert(snippet: String) {
        val sel = textValue.selection
        applyEdit(insertAtCursor(textValue.text, sel.min, sel.max, snippet))
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val references = mutableListOf<String>()
            for (uri in uris) {
                try {
                    references += onAddImage(uri)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    Toast.makeText(context, imageAddFailed, Toast.LENGTH_SHORT).show()
                }
            }
            if (references.isNotEmpty()) {
                val before = textValue.text.substring(0, textValue.selection.min)
                val lead = if (before.isEmpty() || before.endsWith("\n")) "" else "\n"
                insert(lead + references.joinToString("\n") { "![$imageAlt]($it)" } + "\n")
            }
        }
    }

    val editorBackground = selectedColor.toEditorBackground(isDark)
    val wordCount = remember(textValue.text) {
        textValue.text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val charCount = textValue.text.length
    val readingMinutes = (wordCount + 199) / 200
    val currentFolder = remember(selectedFolderId, folders) { folders.firstOrNull { it.id == selectedFolderId } }
    val canManage = existingNote != null || dirty

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val formattedDate = remember(updatedAt) {
                        val locale = Locale.getDefault()
                        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "dMMMjmm"), locale).format(Date(updatedAt))
                    }
                    Text(
                        text = stringResource(R.string.edited_at, formattedDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { close() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isReadingMode = !isReadingMode }) {
                        Icon(
                            imageVector = if (isReadingMode) Icons.Rounded.Edit else Icons.Rounded.Visibility,
                            contentDescription = if (isReadingMode) stringResource(R.string.cd_edit_mode) else stringResource(R.string.cd_read_mode),
                            tint = if (isReadingMode) primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = stringResource(if (isPinned) R.string.unpin else R.string.cd_pin_note),
                            tint = if (isPinned) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.cd_more))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.note_color)) },
                                leadingIcon = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showColorPicker = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.move_to_folder)) },
                                leadingIcon = { Icon(Icons.Rounded.Folder, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showFolderPicker = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share)) },
                                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                                enabled = title.isNotBlank() || textValue.text.isNotBlank(),
                                onClick = {
                                    showMenu = false
                                    shareNote(context, title, textValue.text)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.copy_text)) },
                                leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                                enabled = title.isNotBlank() || textValue.text.isNotBlank(),
                                onClick = {
                                    showMenu = false
                                    copyNote(context, title, textValue.text)
                                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                                }
                            )
                            if (canManage) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.duplicate)) },
                                    leadingIcon = { Icon(Icons.Rounded.FileCopy, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        persistChanges(sync = false)
                                        coroutineScope.launch {
                                            onDuplicateNote(stableId)?.let(onOpenNote)
                                        }
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.move_to_trash), color = AccentRed) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = AccentRed) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = editorBackground)
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = !isReadingMode) {
                FormattingToolbar(
                    canUndo = history.canUndo,
                    canRedo = history.canRedo,
                    onUndo = { history.undo(textValue)?.let { textValue = it } },
                    onRedo = { history.redo(textValue)?.let { textValue = it } },
                    onHeading = {
                        // Alterna: texto → Título 1 → Título 2 → texto
                        val line = currentLine(textValue)
                        when {
                            line.trimStart().startsWith("## ") -> linePrefix(LinePrefix.HEADING2)
                            line.trimStart().startsWith("# ") -> linePrefix(LinePrefix.HEADING2)
                            else -> linePrefix(LinePrefix.HEADING1)
                        }
                    },
                    onBold = { wrap("**") },
                    onItalic = { wrap("*") },
                    onStrike = { wrap("~~") },
                    onBullet = { linePrefix(LinePrefix.BULLET) },
                    onNumbered = { linePrefix(LinePrefix.NUMBERED) },
                    onTask = { linePrefix(LinePrefix.TASK) },
                    onQuote = { linePrefix(LinePrefix.QUOTE) },
                    onCode = {
                        if (textValue.selection.collapsed || !textValue.text.substring(textValue.selection.min, textValue.selection.max).contains('\n')) wrap("`")
                        else wrap("```\n", "\n```")
                    },
                    onLink = { wrap("[", "](https://)") },
                    onRule = { insert(if (textValue.text.substring(0, textValue.selection.min).let { it.isEmpty() || it.endsWith("\n") }) "---\n" else "\n---\n") },
                    onImage = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = editorBackground
    ) { paddingValues ->
        // En pantallas anchas (plegables abiertos, tablets) el texto no pasa de un ancho cómodo de lectura.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = READABLE_MAX_WIDTH)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetaChip(
                    icon = Icons.Rounded.Folder,
                    tint = currentFolder?.let { f ->
                        NoteColor.fromKey(f.color).takeIf { it != NoteColor.DEFAULT }?.getAccentColor(isDark)
                    } ?: primary,
                    label = currentFolder?.name ?: stringResource(R.string.no_folder),
                    onClick = { showFolderPicker = true },
                    modifier = Modifier.weight(1f, fill = false)
                )
                MetaChip(
                    icon = Icons.Rounded.Palette,
                    tint = if (selectedColor == NoteColor.DEFAULT) MaterialTheme.colorScheme.onSurfaceVariant else selectedColor.getAccentColor(isDark),
                    label = stringResource(R.string.note_color),
                    onClick = { showColorPicker = true }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            BasicTextField(
                value = title,
                onValueChange = { title = it.replace("\n", " ") },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                readOnly = isReadingMode,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text(
                            text = stringResource(R.string.title_hint),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Crossfade(targetState = isReadingMode, label = "editor_mode_crossfade") { reading ->
                if (reading) {
                    if (textValue.text.isBlank()) {
                        Text(
                            text = stringResource(R.string.empty_note),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp)
                                .clickable { isReadingMode = false }
                        )
                    } else {
                        MarkdownReader(
                            markdownText = textValue.text,
                            primaryBlue = primary,
                            onToggleChecklist = { lineIndex ->
                                updateBody(TextFieldValue(toggleTaskLine(textValue.text, lineIndex)), forceHistory = true)
                            },
                            onImageClick = { viewerImage = it }
                        )
                    }
                } else {
                    BasicTextField(
                        value = textValue,
                        onValueChange = ::onBodyChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(primary),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 350.dp)
                            .focusRequester(bodyFocus),
                        decorationBox = { innerTextField ->
                            if (textValue.text.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.note_hint),
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

            Text(
                text = stringResource(
                    R.string.word_count_separator,
                    pluralStringResource(R.plurals.word_count, wordCount, wordCount),
                    pluralStringResource(R.plurals.char_count, charCount, charCount)
                ) + if (wordCount >= 200) "  •  " + pluralStringResource(R.plurals.reading_minutes, readingMinutes, readingMinutes) else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            title = stringResource(R.string.move_to_folder),
            folders = folders,
            selectedId = selectedFolderId,
            onDismiss = { showFolderPicker = false },
            onSelect = {
                selectedFolderId = it
                showFolderPicker = false
            }
        )
    }

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text(stringResource(R.string.note_color), fontWeight = FontWeight.Bold) },
            text = {
                ColorPalette(
                    selected = selectedColor,
                    onSelect = {
                        selectedColor = it
                        showColorPicker = false
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDestructiveDialog(
            title = stringResource(R.string.move_to_trash_title),
            message = stringResource(R.string.move_to_trash_message),
            confirmLabel = stringResource(R.string.move_to_trash),
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                // Guardar antes los últimos cambios para que queden en la papelera
                persistChanges(sync = false)
                deleted = true
                onDeleteNote(stableId)
                showDeleteConfirm = false
                onBack()
            }
        )
    }

    viewerImage?.let { model ->
        ZoomableImageDialog(model = model, onDismiss = { viewerImage = null })
    }
}

/** Ancho máximo del texto de la nota, para que las líneas no sean incómodas de leer. */
val READABLE_MAX_WIDTH = 760.dp

private fun currentLine(value: TextFieldValue): String {
    val text = value.text
    val pos = value.selection.min.coerceIn(0, text.length)
    val start = text.lastIndexOf('\n', pos - 1) + 1
    val end = text.indexOf('\n', pos).let { if (it == -1) text.length else it }
    return text.substring(start, end)
}

private fun noteAsPlainText(title: String, content: String): String =
    listOf(title.trim(), markdownPlainText(content).trim()).filter { it.isNotEmpty() }.joinToString("\n\n")

private fun shareNote(context: Context, title: String, content: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title.trim())
        putExtra(Intent.EXTRA_TEXT, noteAsPlainText(title, content))
    }
    context.startActivity(Intent.createChooser(send, null))
}

private fun copyNote(context: Context, title: String, content: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(title.ifBlank { "Mis Notas" }, noteAsPlainText(title, content)))
}

@Composable
private fun MetaChip(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FormattingToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onHeading: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onStrike: () -> Unit,
    onBullet: () -> Unit,
    onNumbered: () -> Unit,
    onTask: () -> Unit,
    onQuote: () -> Unit,
    onCode: () -> Unit,
    onLink: () -> Unit,
    onRule: () -> Unit,
    onImage: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
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
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarButton(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.undo), primary, onUndo, enabled = canUndo)
            ToolbarButton(Icons.AutoMirrored.Rounded.Redo, stringResource(R.string.redo), primary, onRedo, enabled = canRedo)
            VerticalDivider(modifier = Modifier
                .height(24.dp)
                .padding(horizontal = 4.dp))
            ToolbarButton(Icons.Rounded.CheckBox, stringResource(R.string.fmt_task), primary, onTask)
            ToolbarButton(Icons.AutoMirrored.Rounded.FormatListBulleted, stringResource(R.string.fmt_bullet), primary, onBullet)
            ToolbarButton(Icons.Rounded.FormatListNumbered, stringResource(R.string.fmt_numbered), primary, onNumbered)
            ToolbarButton(Icons.Rounded.Title, stringResource(R.string.fmt_heading), primary, onHeading)
            ToolbarButton(Icons.Rounded.FormatBold, stringResource(R.string.fmt_bold), primary, onBold)
            ToolbarButton(Icons.Rounded.FormatItalic, stringResource(R.string.fmt_italic), primary, onItalic)
            ToolbarButton(Icons.Rounded.FormatStrikethrough, stringResource(R.string.fmt_strike), primary, onStrike)
            ToolbarButton(Icons.Rounded.Image, stringResource(R.string.fmt_image), primary, onImage)
            ToolbarButton(Icons.Rounded.Link, stringResource(R.string.fmt_link), primary, onLink)
            ToolbarButton(Icons.Rounded.FormatQuote, stringResource(R.string.fmt_quote), primary, onQuote)
            ToolbarButton(Icons.Rounded.Code, stringResource(R.string.fmt_code), primary, onCode)
            ToolbarButton(Icons.Rounded.HorizontalRule, stringResource(R.string.fmt_rule), primary, onRule)
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp)
        )
    }
}
