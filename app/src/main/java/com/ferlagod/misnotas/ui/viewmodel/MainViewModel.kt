// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.ferlagod.misnotas.data.attachment.AttachmentStore
import com.ferlagod.misnotas.data.local.AppDatabase
import com.ferlagod.misnotas.data.local.entity.AttachmentEntity
import com.ferlagod.misnotas.data.local.entity.FolderEntity
import com.ferlagod.misnotas.data.local.entity.NoteEntity
import com.ferlagod.misnotas.data.local.model.SyncStatus
import com.ferlagod.misnotas.data.local.pref.NextcloudCredentials
import com.ferlagod.misnotas.data.sync.SyncRepository
import com.ferlagod.misnotas.data.sync.SyncState
import com.ferlagod.misnotas.data.sync.SyncWorker
import com.ferlagod.misnotas.ui.theme.NoteColor
import com.ferlagod.misnotas.util.ImageCompressor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Filtro de navegación para la vista de notas.
 */
sealed interface NotesFilter {
    data object All : NotesFilter
    data object Pinned : NotesFilter
    data object Trash : NotesFilter
    data class Folder(val folderId: String, val folderName: String) : NotesFilter
}

/**
 * ViewModel principal: sesión/cifrado, sincronización, carpetas y notas.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val folderDao = database.folderDao()
    private val noteDao = database.noteDao()
    private val attachmentDao = database.attachmentDao()
    private val attachmentStore = AttachmentStore(application)

    private val syncRepository = SyncRepository.getInstance(application)
    private val authPreferences = syncRepository.authPreferences

    val syncState: StateFlow<SyncState> = syncRepository.syncState

    /** Serializa las escrituras de notas (p. ej. autoguardado seguido de "mover a papelera"). */
    private val noteWriteMutex = Mutex()

    private val _isConfigured = MutableStateFlow(authPreferences.hasCredentials())
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    private val _isUnlocked = MutableStateFlow(
        if (authPreferences.hasCredentials()) authPreferences.isKeyUnlocked() else true
    )
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _currentFilter = MutableStateFlow<NotesFilter>(NotesFilter.All)
    val currentFilter: StateFlow<NotesFilter> = _currentFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGridLayout = MutableStateFlow(true)
    val isGridLayout: StateFlow<Boolean> = _isGridLayout.asStateFlow()

    val allActiveNotes = noteDao.getAllNotesFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val trashNotes = noteDao.getTrashNotesFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allFolders = folderDao.getAllFoldersFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val foldersMap: StateFlow<Map<String, FolderEntity>> = allFolders
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val displayedNotes: StateFlow<List<NoteEntity>> = combine(
        allActiveNotes,
        trashNotes,
        _currentFilter,
        _searchQuery
    ) { activeNotes, trashList, filter, query ->
        val filtered = when (filter) {
            is NotesFilter.All -> activeNotes
            is NotesFilter.Pinned -> activeNotes.filter { it.isPinned }
            is NotesFilter.Trash -> trashList
            is NotesFilter.Folder -> activeNotes.filter { it.folderId == filter.folderId }
        }
        if (query.isBlank()) {
            filtered
        } else {
            val q = query.trim()
            filtered.filter {
                it.title.contains(q, ignoreCase = true) || it.content.contains(q, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalActiveNotesCount = allActiveNotes.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pinnedNotesCount = allActiveNotes.map { notes -> notes.count { it.isPinned } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trashNotesCount = trashNotes.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch { syncRepository.migrateLegacyImages() }
        if (_isConfigured.value && _isUnlocked.value) {
            SyncWorker.schedule(application)
            triggerSync()
        }
    }

    // ==========================================
    // Cuenta y cifrado
    // ==========================================

    fun setupAndUnlock(
        serverUrl: String,
        username: String,
        appPassword: String,
        masterPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (serverUrl.isBlank() || username.isBlank() || appPassword.isBlank() || masterPassword.isBlank()) {
            onError("Todos los campos son obligatorios")
            return
        }
        val url = serverUrl.trim()
        if (!url.startsWith("https://")) {
            onError("La URL del servidor debe empezar por https:// (no se permiten conexiones sin cifrar)")
            return
        }
        viewModelScope.launch {
            val credentials = NextcloudCredentials(url, username.trim(), appPassword.trim())
            syncRepository.connectAccount(credentials, masterPassword)
                .onSuccess {
                    _isConfigured.value = true
                    _isUnlocked.value = true
                    SyncWorker.schedule(getApplication())
                    triggerSync()
                    onSuccess()
                }
                .onFailure { onError(it.message ?: "No se pudo conectar") }
        }
    }

    fun unlockWithPassword(
        masterPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val key = syncRepository.vault.unlockLocal(masterPassword)
                authPreferences.saveDerivedKey(key)
                _isUnlocked.value = true
                SyncWorker.schedule(getApplication())
                triggerSync()
                onSuccess()
            } catch (e: Exception) {
                onError(SyncRepository.friendlyMessage(e))
            }
        }
    }

    fun lock() {
        if (!_isConfigured.value) return
        authPreferences.clearDerivedKey()
        _isUnlocked.value = false
    }

    fun disconnectNextcloud() {
        SyncWorker.cancel(getApplication())
        authPreferences.clearCredentials()
        authPreferences.clearDerivedKey()
        syncRepository.resetSyncState()
        _isConfigured.value = false
        _isUnlocked.value = true
    }

    fun logout() {
        SyncWorker.cancel(getApplication())
        authPreferences.clearAll()
        syncRepository.resetSyncState()
        _isConfigured.value = false
        _isUnlocked.value = true
    }

    suspend fun getNoteById(id: String): NoteEntity? = noteDao.getNoteById(id)

    fun getCredentials(): NextcloudCredentials? = authPreferences.getCredentials()

    fun getLastSync(): Long? = authPreferences.getLastSync()

    fun configureNextcloud(
        serverUrl: String,
        username: String,
        appPassword: String,
        masterPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = setupAndUnlock(serverUrl, username, appPassword, masterPassword, onSuccess, onError)

    // ==========================================
    // Sincronización
    // ==========================================

    fun triggerSync() {
        if (!_isConfigured.value || !_isUnlocked.value) return
        viewModelScope.launch { syncRepository.synchronize() }
    }

    // ==========================================
    // Navegación y búsqueda
    // ==========================================

    fun setFilter(filter: NotesFilter) {
        _currentFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleLayoutMode() {
        _isGridLayout.value = !_isGridLayout.value
    }

    // ==========================================
    // Carpetas
    // ==========================================

    fun createFolder(name: String, color: NoteColor, parentId: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            folderDao.upsertFolder(
                FolderEntity(
                    id = UUID.randomUUID().toString(),
                    parentId = parentId,
                    name = name.trim(),
                    color = color.key,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_UPLOAD
                )
            )
            triggerSync()
        }
    }

    fun updateFolder(id: String, name: String, color: NoteColor, parentId: String? = null) {
        if (name.isBlank()) return
        // Evita ciclos: una carpeta no puede colgar de sí misma.
        val safeParent = parentId?.takeIf { it != id }
        viewModelScope.launch {
            folderDao.updateFolderDetails(id, name.trim(), color.key, safeParent)
            triggerSync()
        }
    }

    /**
     * Elimina una carpeta: sus notas pasan a "Todas las notas" y sus subcarpetas suben un nivel.
     */
    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            database.withTransaction {
                val folder = folderDao.getFolderById(folderId) ?: return@withTransaction
                noteDao.detachFromFolder(folderId)
                folderDao.reparentChildren(folderId, folder.parentId)
                if (_isConfigured.value) folderDao.markAsDeleted(folderId)
                else folderDao.deletePermanently(folderId)
            }
            triggerSync()
        }
    }

    // ==========================================
    // Notas
    // ==========================================

    /**
     * Guarda una nota. No hace nada si no hay cambios reales (así abrir y cerrar una nota no
     * altera su fecha ni provoca subidas). Conserva el estado remoto y de papelera existentes.
     *
     * @param sync si es false solo guarda en local (autoguardado mientras se escribe).
     */
    fun saveNote(
        id: String,
        title: String,
        content: String,
        folderId: String? = null,
        color: NoteColor = NoteColor.DEFAULT,
        isPinned: Boolean = false,
        sync: Boolean = true
    ) {
        viewModelScope.launch {
            noteWriteMutex.withLock { saveNoteLocked(id, title, content, folderId, color, isPinned, sync) }
        }
    }

    private suspend fun saveNoteLocked(
        id: String,
        title: String,
        content: String,
        folderId: String?,
        color: NoteColor,
        isPinned: Boolean,
        sync: Boolean
    ) {
        run {
            val existing = noteDao.getNoteById(id)
            if (existing == null && title.isBlank() && content.isBlank()) return

            val cleanTitle = title.trim()
            val cleanContent = content.trimEnd()
            if (existing != null &&
                existing.title == cleanTitle &&
                existing.content == cleanContent &&
                existing.folderId == folderId &&
                existing.color == color.key &&
                existing.isPinned == isPinned
            ) {
                if (sync) triggerSync()
                return
            }

            val entity = (existing ?: NoteEntity(id = id, title = "", content = "")).copy(
                folderId = folderId,
                title = cleanTitle,
                content = cleanContent,
                isPinned = isPinned,
                color = color.key,
                updatedAt = System.currentTimeMillis(),
                syncStatus = if (existing?.syncStatus == SyncStatus.PENDING_DELETE) SyncStatus.PENDING_DELETE else SyncStatus.PENDING_UPLOAD
            )
            noteDao.upsertNote(entity)
            if (sync) triggerSync()
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            noteWriteMutex.withLock {
                noteDao.setPinned(note.id, !note.isPinned)
            }
            triggerSync()
        }
    }

    /** Mueve la nota a la papelera. */
    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            noteWriteMutex.withLock {
                noteDao.moveToTrash(noteId)
            }
            triggerSync()
        }
    }

    fun restoreNote(noteId: String) {
        viewModelScope.launch {
            noteWriteMutex.withLock {
                noteDao.restoreFromTrash(noteId)
            }
            triggerSync()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteWriteMutex.withLock {
                if (_isConfigured.value) noteDao.markTrashForPurge() else noteDao.deleteTrashPermanently()
            }
            triggerSync()
        }
    }

    fun deleteNotePermanently(noteId: String) {
        viewModelScope.launch {
            noteWriteMutex.withLock {
                if (_isConfigured.value) noteDao.markForPurge(noteId) else noteDao.deletePermanently(noteId)
            }
            triggerSync()
        }
    }

    /**
     * Comprime la imagen, la guarda como adjunto y devuelve la referencia Markdown
     * (`attachment://<id>`) para insertarla en la nota.
     */
    suspend fun addImageAttachment(uri: Uri): String {
        val bytes = ImageCompressor.compressImage(getApplication(), uri)
        val id = UUID.randomUUID().toString()
        attachmentStore.write(id, bytes)
        attachmentDao.upsert(
            AttachmentEntity(id = id, mimeType = ImageCompressor.MIME_TYPE, size = bytes.size.toLong())
        )
        return AttachmentStore.reference(id)
    }
}

class MainViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
