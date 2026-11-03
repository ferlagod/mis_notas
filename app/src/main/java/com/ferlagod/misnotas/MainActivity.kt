// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas

import android.content.Context
import android.os.Bundle
import com.ferlagod.misnotas.util.AppLanguage
import com.ferlagod.misnotas.util.LanguageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ferlagod.misnotas.data.local.entity.NoteEntity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ferlagod.misnotas.ui.screen.FoldersScreen
import com.ferlagod.misnotas.ui.screen.LoginScreen
import com.ferlagod.misnotas.ui.screen.NoteEditorScreen
import com.ferlagod.misnotas.ui.screen.NotesScreen
import com.ferlagod.misnotas.ui.theme.MisNotasTheme
import com.ferlagod.misnotas.ui.viewmodel.MainViewModel
import com.ferlagod.misnotas.ui.viewmodel.MainViewModelFactory
import com.ferlagod.misnotas.ui.viewmodel.NotesFilter

object AppDestinations {
    const val LOGIN = "login"
    const val FOLDERS = "folders"
    const val NOTES = "notes"
    const val EDITOR = "editor?noteId={noteId}&folderId={folderId}"

    fun editorRoute(noteId: String? = null, folderId: String? = null): String {
        return "editor?noteId=${noteId ?: ""}&folderId=${folderId ?: ""}"
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(application) }

    override fun attachBaseContext(newBase: Context) {
        // Android 8-12: aplica el idioma elegido en Ajustes (en 13+ lo hace el sistema).
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MisNotasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigationHost(
                        viewModel = viewModel,
                        currentLanguage = LanguageManager.current(this),
                        onLanguageSelected = { LanguageManager.set(this, it) }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Sincroniza al volver a la app para traer cambios de otros dispositivos.
        viewModel.triggerSync()
    }
}

@Composable
private fun AppNavigationHost(
    viewModel: MainViewModel,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    val navController = rememberNavController()

    val isConfigured by viewModel.isConfigured.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()

    val currentFilter by viewModel.currentFilter.collectAsState()
    val folders by viewModel.allFolders.collectAsState()
    val notes by viewModel.displayedNotes.collectAsState()
    val allActiveNotes by viewModel.allActiveNotes.collectAsState()
    val totalCount by viewModel.totalActiveNotesCount.collectAsState()
    val pinnedCount by viewModel.pinnedNotesCount.collectAsState()
    val trashNotesCount by viewModel.trashNotesCount.collectAsState()
    val foldersMap by viewModel.foldersMap.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGridLayout by viewModel.isGridLayout.collectAsState()

    // Control de navegación automático según estado de bloqueo
    LaunchedEffect(isUnlocked, isConfigured) {
        if (isConfigured && !isUnlocked) {
            navController.navigate(AppDestinations.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isConfigured && !isUnlocked) AppDestinations.LOGIN else AppDestinations.FOLDERS
    ) {
        composable(AppDestinations.LOGIN) {
            LoginScreen(
                isConfigured = isConfigured,
                onSetupAndUnlock = { serverUrl, user, appPass, masterPass, onError ->
                    viewModel.setupAndUnlock(
                        serverUrl,
                        user,
                        appPass,
                        masterPass,
                        onSuccess = {
                            navController.navigate(AppDestinations.FOLDERS) {
                                popUpTo(AppDestinations.LOGIN) { inclusive = true }
                            }
                        },
                        onError = onError
                    )
                },
                onUnlock = { masterPass, onError ->
                    viewModel.unlockWithPassword(
                        masterPass,
                        onSuccess = {
                            navController.navigate(AppDestinations.FOLDERS) {
                                popUpTo(AppDestinations.LOGIN) { inclusive = true }
                            }
                        },
                        onError = onError
                    )
                },
                onResetConfig = {
                    viewModel.logout()
                    navController.navigate(AppDestinations.FOLDERS) {
                        popUpTo(AppDestinations.LOGIN) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(AppDestinations.FOLDERS) {
                        popUpTo(AppDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(AppDestinations.FOLDERS) {
            FoldersScreen(
                isConfigured = isConfigured,
                folders = folders,
                notes = allActiveNotes,
                totalNotesCount = totalCount,
                pinnedNotesCount = pinnedCount,
                trashNotesCount = trashNotesCount,
                syncState = syncState,
                onSelectFilter = { filter ->
                    viewModel.setFilter(filter)
                    navController.navigate(AppDestinations.NOTES)
                },
                onCreateFolder = { name, color, parentId ->
                    viewModel.createFolder(name, color, parentId)
                },
                onUpdateFolder = { id, name, color, parentId ->
                    viewModel.updateFolder(id, name, color, parentId)
                },
                onDeleteFolder = { folderId ->
                    viewModel.deleteFolder(folderId)
                },
                onTriggerSync = {
                    viewModel.triggerSync()
                },
                onLockApp = {
                    viewModel.lock()
                },
                onConfigureNextcloud = { serverUrl, user, appPass, masterPass, onSuccess, onError ->
                    viewModel.configureNextcloud(
                        serverUrl,
                        user,
                        appPass,
                        masterPass,
                        onSuccess,
                        onError
                    )
                },
                onDisconnectNextcloud = {
                    viewModel.disconnectNextcloud()
                },
                getCredentials = {
                    viewModel.getCredentials()
                },
                getLastSync = {
                    viewModel.getLastSync()
                },
                currentLanguage = currentLanguage,
                onLanguageSelected = onLanguageSelected
            )
        }

        composable(AppDestinations.NOTES) {
            val activeFolderId = (currentFilter as? NotesFilter.Folder)?.folderId

            NotesScreen(
                notes = notes,
                foldersMap = foldersMap,
                currentFilter = currentFilter,
                searchQuery = searchQuery,
                isGridLayout = isGridLayout,
                onSearchQueryChange = { query ->
                    viewModel.setSearchQuery(query)
                },
                onToggleLayout = {
                    viewModel.toggleLayoutMode()
                },
                onBackToFolders = {
                    viewModel.setSearchQuery("")
                    navController.popBackStack()
                },
                onOpenNote = { noteId ->
                    navController.navigate(AppDestinations.editorRoute(noteId = noteId, folderId = activeFolderId))
                },
                onTogglePin = { note ->
                    viewModel.togglePin(note)
                },
                onDeleteNote = { noteId ->
                    viewModel.deleteNote(noteId)
                },
                onRestoreNote = { noteId ->
                    viewModel.restoreNote(noteId)
                },
                onDeletePermanently = { noteId ->
                    viewModel.deleteNotePermanently(noteId)
                },
                onEmptyTrash = {
                    viewModel.emptyTrash()
                }
            )
        }

        composable(
            route = AppDestinations.EDITOR,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("folderId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val noteIdArg = backStackEntry.arguments?.getString("noteId")?.takeIf { it.isNotBlank() }
            val folderIdArg = backStackEntry.arguments?.getString("folderId")?.takeIf { it.isNotBlank() }

            // Se lee una sola vez de la base de datos (incluye notas en papelera).
            var existingNote by remember(noteIdArg) { mutableStateOf<NoteEntity?>(null) }
            var noteLoaded by remember(noteIdArg) { mutableStateOf(noteIdArg == null) }
            LaunchedEffect(noteIdArg) {
                if (noteIdArg != null) {
                    existingNote = viewModel.getNoteById(noteIdArg)
                    noteLoaded = true
                }
            }

            NoteEditorScreen(
                noteId = noteIdArg,
                folderId = folderIdArg,
                existingNote = existingNote,
                noteLoaded = noteLoaded,
                folders = folders,
                onBack = {
                    navController.popBackStack()
                },
                onSaveNote = { id, title, content, folderId, color, isPinned, sync ->
                    viewModel.saveNote(
                        id = id,
                        title = title,
                        content = content,
                        folderId = folderId,
                        color = color,
                        isPinned = isPinned,
                        sync = sync
                    )
                },
                onDeleteNote = { id ->
                    viewModel.deleteNote(id)
                },
                onAddImage = { uri ->
                    viewModel.addImageAttachment(uri)
                }
            )
        }
    }
}