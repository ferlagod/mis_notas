// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ferlagod.misnotas.data.local.entity.FolderEntity
import com.ferlagod.misnotas.ui.components.EmptyState
import java.util.UUID
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ferlagod.misnotas.data.local.entity.NoteEntity
import com.ferlagod.misnotas.data.local.pref.ThemeMode
import com.ferlagod.misnotas.ui.screen.AppLockScreen
import com.ferlagod.misnotas.ui.screen.FoldersScreen
import com.ferlagod.misnotas.ui.screen.LoginScreen
import com.ferlagod.misnotas.ui.screen.NoteEditorScreen
import com.ferlagod.misnotas.ui.screen.NotesActions
import com.ferlagod.misnotas.ui.screen.NotesScreen
import com.ferlagod.misnotas.ui.screen.SettingsScreen
import com.ferlagod.misnotas.ui.theme.MisNotasTheme
import com.ferlagod.misnotas.ui.viewmodel.MainViewModel
import com.ferlagod.misnotas.ui.viewmodel.MainViewModelFactory
import com.ferlagod.misnotas.ui.viewmodel.NotesFilter
import com.ferlagod.misnotas.ui.viewmodel.PendingNavigation
import com.ferlagod.misnotas.ui.viewmodel.UiEvent
import com.ferlagod.misnotas.util.AppLanguage
import com.ferlagod.misnotas.util.AppLock
import com.ferlagod.misnotas.util.LanguageManager
import kotlinx.coroutines.launch

object AppDestinations {
    const val LOGIN = "login"
    const val FOLDERS = "folders"
    const val NOTES = "notes"
    const val SETTINGS = "settings"
    const val EDITOR = "editor?noteId={noteId}&folderId={folderId}&pin={pin}"

    fun editorRoute(noteId: String? = null, folderId: String? = null, pin: Boolean = false): String =
        "editor?noteId=${noteId.orEmpty()}&folderId=${folderId.orEmpty()}&pin=$pin"
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(application) }

    /** La app está tapada por la pantalla de bloqueo (huella/PIN). */
    private var appLocked by mutableStateOf(false)
    private var authInProgress = false
    private var backgroundSince = 0L

    private val confirmCredential = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onAuthResult(result.resultCode == Activity.RESULT_OK)
    }

    override fun attachBaseContext(newBase: Context) {
        // Android 8-12: aplica el idioma elegido en Ajustes (en 13+ lo hace el sistema).
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appLocked = savedInstanceState?.getBoolean(STATE_LOCKED)
            ?: (viewModel.settings.value.appLock && AppLock.isAvailable(this))
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(settings.appLock) { applyRecentsPrivacy(settings.appLock) }

            MisNotasTheme(darkTheme = darkTheme, amoled = settings.amoled) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigationHost(
                            viewModel = viewModel,
                            appLocked = appLocked,
                            currentLanguage = LanguageManager.current(this@MainActivity),
                            onLanguageSelected = { LanguageManager.set(this@MainActivity, it) },
                            isAppLockAvailable = { AppLock.isAvailable(this@MainActivity) },
                            onAppLockChange = { enabled ->
                                // Activarlo exige autenticarse una vez, para comprobar que funciona.
                                if (enabled) requestUnlock { ok -> if (ok) viewModel.setAppLock(true) }
                                else viewModel.setAppLock(false)
                            }
                        )
                        AnimatedVisibility(visible = appLocked, enter = fadeIn(), exit = fadeOut()) {
                            AppLockScreen(onUnlock = { requestUnlock() })
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        val lockEnabled = viewModel.settings.value.appLock && AppLock.isAvailable(this)
        if (lockEnabled && backgroundSince > 0 && SystemClock.elapsedRealtime() - backgroundSince > AppLock.GRACE_PERIOD_MS) {
            appLocked = true
        }
        backgroundSince = 0
        if (appLocked && lockEnabled) requestUnlock() else appLocked = false
        // Sincroniza al volver a la app para traer cambios de otros dispositivos.
        viewModel.triggerSync()
    }

    override fun onStop() {
        super.onStop()
        if (!authInProgress) backgroundSince = SystemClock.elapsedRealtime()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_LOCKED, appLocked)
    }

    private var pendingAuthCallback: ((Boolean) -> Unit)? = null

    private fun requestUnlock(callback: ((Boolean) -> Unit)? = null) {
        if (authInProgress) return
        authInProgress = true
        pendingAuthCallback = callback
        val intent = AppLock.authenticate(
            activity = this,
            title = getString(R.string.app_lock_prompt_title),
            subtitle = getString(R.string.app_lock_prompt_subtitle),
            onResult = ::onAuthResult
        )
        if (intent != null) confirmCredential.launch(intent)
    }

    private fun onAuthResult(success: Boolean) {
        authInProgress = false
        if (success) appLocked = false
        pendingAuthCallback?.invoke(success)
        pendingAuthCallback = null
    }

    /** Oculta el contenido en la vista de apps recientes cuando el bloqueo está activado. */
    private fun applyRecentsPrivacy(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(!enabled)
    }

    /** Accesos directos y contenido compartido desde otras apps. */
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            ACTION_NEW_NOTE -> viewModel.requestNavigation(PendingNavigation.NewNote)
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: intent.getStringExtra(Intent.EXTRA_TITLE)
                val image = if (intent.type?.startsWith("image/") == true) {
                    IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                } else null
                viewModel.createNoteFromShare(subject, text, listOfNotNull(image))
                intent.action = null
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val images = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
                viewModel.createNoteFromShare(intent.getStringExtra(Intent.EXTRA_SUBJECT), intent.getStringExtra(Intent.EXTRA_TEXT), images)
                intent.action = null
            }
        }
    }

    companion object {
        const val ACTION_NEW_NOTE = "com.ferlagod.misnotas.action.NEW_NOTE"
        private const val STATE_LOCKED = "app_locked"
    }
}

@Composable
private fun AppNavigationHost(
    viewModel: MainViewModel,
    appLocked: Boolean,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    isAppLockAvailable: () -> Boolean,
    onAppLockChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val isConfigured by viewModel.isConfigured.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val folders by viewModel.allFolders.collectAsStateWithLifecycle()
    val notes by viewModel.displayedNotes.collectAsStateWithLifecycle()
    val allActiveNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalActiveNotesCount.collectAsStateWithLifecycle()
    val pinnedCount by viewModel.pinnedNotesCount.collectAsStateWithLifecycle()
    val trashNotesCount by viewModel.trashNotesCount.collectAsStateWithLifecycle()
    val foldersMap by viewModel.foldersMap.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val pendingNavigation by viewModel.pendingNavigation.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    // Avisos del ViewModel: "Movida a la papelera · Deshacer", resultados de exportar…
    // Cada aviso sustituye al anterior en lugar de esperar a que termine.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            snackbarHostState.currentSnackbarData?.dismiss()
            scope.launch {
                when (event) {
                    is UiEvent.NotesTrashed -> {
                        val result = snackbarHostState.showSnackbar(
                            message = resources.getQuantityString(R.plurals.notes_moved_to_trash, event.ids.size, event.ids.size),
                            actionLabel = resources.getString(R.string.undo),
                            withDismissAction = true,
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.restoreNotes(event.ids)
                    }
                    is UiEvent.Message -> snackbarHostState.showSnackbar(event.text, withDismissAction = true)
                }
            }
        }
    }

    // Control de navegación automático según estado de bloqueo de la bóveda
    LaunchedEffect(isUnlocked, isConfigured) {
        if (isConfigured && !isUnlocked) {
            navController.navigate(AppDestinations.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Accesos directos y notas creadas desde "Compartir" (cuando la app está desbloqueada)
    LaunchedEffect(pendingNavigation, isUnlocked, appLocked) {
        val target = pendingNavigation ?: return@LaunchedEffect
        if (appLocked || (isConfigured && !isUnlocked)) return@LaunchedEffect
        viewModel.consumeNavigation()
        when (target) {
            PendingNavigation.NewNote -> navController.navigate(AppDestinations.editorRoute())
            is PendingNavigation.OpenNote -> navController.navigate(AppDestinations.editorRoute(noteId = target.noteId))
        }
    }

    fun NavHostController.goHome() = navigate(AppDestinations.FOLDERS) {
        popUpTo(AppDestinations.LOGIN) { inclusive = true }
    }

    NavHost(
        navController = navController,
        startDestination = if (isConfigured && !isUnlocked) AppDestinations.LOGIN else AppDestinations.FOLDERS
    ) {
        composable(AppDestinations.LOGIN) {
            LoginScreen(
                isConfigured = isConfigured,
                onSetupAndUnlock = { serverUrl, user, appPass, masterPass, onError ->
                    viewModel.setupAndUnlock(serverUrl, user, appPass, masterPass, onSuccess = { navController.goHome() }, onError = onError)
                },
                onUnlock = { masterPass, onError ->
                    viewModel.unlockWithPassword(masterPass, onSuccess = { navController.goHome() }, onError = onError)
                },
                onResetConfig = {
                    viewModel.logout()
                    navController.goHome()
                },
                onSkip = { navController.goHome() }
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
                snackbarHostState = snackbarHostState,
                onSelectFilter = { filter ->
                    viewModel.setFilter(filter)
                    navController.navigate(AppDestinations.NOTES)
                },
                onSearch = { query ->
                    viewModel.setFilter(NotesFilter.All)
                    viewModel.setSearchQuery(query)
                    navController.navigate(AppDestinations.NOTES)
                },
                onNewNote = { navController.navigate(AppDestinations.editorRoute()) },
                onCreateFolder = viewModel::createFolder,
                onUpdateFolder = viewModel::updateFolder,
                onDeleteFolder = viewModel::deleteFolder,
                onTriggerSync = viewModel::triggerSync,
                onLockApp = viewModel::lock,
                onOpenSettings = { navController.navigate(AppDestinations.SETTINGS) }
            )
        }

        composable(AppDestinations.SETTINGS) {
            SettingsScreen(
                isConfigured = isConfigured,
                currentCredentials = remember(isConfigured) { viewModel.getCredentials() },
                syncState = syncState,
                lastSync = remember(isConfigured, syncState) { viewModel.getLastSync() },
                settings = settings,
                busy = busy,
                snackbarHostState = snackbarHostState,
                isAppLockAvailable = isAppLockAvailable(),
                onBack = { navController.popBackStack() },
                onSaveNextcloud = viewModel::setupAndUnlock,
                onDisconnectNextcloud = viewModel::disconnectNextcloud,
                onTriggerSync = viewModel::triggerSync,
                onThemeChange = viewModel::setThemeMode,
                onAmoledChange = viewModel::setAmoled,
                onGridLayoutChange = { if (it != settings.gridLayout) viewModel.toggleLayoutMode() },
                onSortOrderChange = viewModel::setSortOrder,
                onAutoPurgeChange = viewModel::setAutoPurgeTrash,
                onAppLockChange = onAppLockChange,
                onExport = viewModel::exportBackup,
                onImport = viewModel::importBackup,
                currentLanguage = currentLanguage,
                onLanguageSelected = onLanguageSelected
            )
        }

        composable(AppDestinations.NOTES) {
            val filter = currentFilter
            val newNoteFolder = (filter as? NotesFilter.Folder)?.folderId
            val newNotePinned = filter is NotesFilter.Pinned

            // Nota abierta en el panel derecho (solo en pantallas anchas: móviles plegables
            // desplegados, tablets o en horizontal). Una nota nueva recibe su id al abrirse,
            // así sigue siendo la misma nota si se pliega el móvil mientras se escribe.
            var paneNoteId by rememberSaveable { mutableStateOf<String?>(null) }
            var paneIsNew by rememberSaveable { mutableStateOf(false) }

            fun openInPane(noteId: String?) {
                paneIsNew = noteId == null
                paneNoteId = noteId ?: UUID.randomUUID().toString()
            }

            fun closePane() {
                paneNoteId = null
                paneIsNew = false
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val twoPane = maxWidth >= TWO_PANE_MIN_WIDTH

                // Al plegar el móvil con una nota abierta, la nota sigue abierta a pantalla completa.
                LaunchedEffect(twoPane) {
                    val noteId = paneNoteId
                    if (!twoPane && noteId != null) {
                        val isNew = paneIsNew
                        closePane()
                        // Si la nota nueva aún no se había guardado, conserva carpeta y fijado.
                        navController.navigate(
                            AppDestinations.editorRoute(
                                noteId = noteId,
                                folderId = newNoteFolder.takeIf { isNew },
                                pin = isNew && newNotePinned
                            )
                        )
                    }
                }

                val notesList = @Composable { modifier: Modifier ->
                    Box(modifier = modifier) {
                        NotesScreen(
                            notes = notes,
                            folders = folders,
                            foldersMap = foldersMap,
                            currentFilter = filter,
                            searchQuery = searchQuery,
                            isGridLayout = settings.gridLayout,
                            sortOrder = settings.sortOrder,
                            autoPurgeTrash = settings.autoPurgeTrash,
                            snackbarHostState = snackbarHostState,
                            activeNoteId = if (twoPane) paneNoteId else null,
                            actions = NotesActions(
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onToggleLayout = viewModel::toggleLayoutMode,
                                onSortOrderChange = viewModel::setSortOrder,
                                onBack = { navController.popBackStack() },
                                onOpenNote = { noteId ->
                                    if (twoPane) {
                                        openInPane(noteId)
                                    } else {
                                        navController.navigate(
                                            AppDestinations.editorRoute(
                                                noteId = noteId,
                                                folderId = newNoteFolder,
                                                pin = noteId == null && newNotePinned
                                            )
                                        )
                                    }
                                },
                                onTogglePin = viewModel::togglePin,
                                onTrash = viewModel::moveNotesToTrash,
                                onRestore = viewModel::restoreNotes,
                                onDeletePermanently = viewModel::deleteNotesPermanently,
                                onEmptyTrash = viewModel::emptyTrash,
                                onSetPinned = viewModel::setNotesPinned,
                                onSetColor = viewModel::setNotesColor,
                                onMove = viewModel::moveNotes
                            )
                        )
                    }
                }

                if (!twoPane) {
                    notesList(Modifier.fillMaxSize())
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        notesList(
                            Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                        )
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Box(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                        ) {
                            val currentPaneNote = paneNoteId
                            if (currentPaneNote == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyState(
                                        icon = Icons.Rounded.EditNote,
                                        title = stringResource(R.string.pane_no_note_title),
                                        message = stringResource(R.string.pane_no_note_message)
                                    )
                                }
                            } else {
                                // Snackbars propios: los avisos generales ya los muestra la lista.
                                val paneSnackbar = remember { SnackbarHostState() }
                                key(currentPaneNote) {
                                    EditorHost(
                                        viewModel = viewModel,
                                        noteId = currentPaneNote,
                                        isNewNote = paneIsNew,
                                        folderId = newNoteFolder.takeIf { paneIsNew },
                                        initialPinned = paneIsNew && newNotePinned,
                                        folders = folders,
                                        snackbarHostState = paneSnackbar,
                                        onClose = ::closePane,
                                        onOpenNote = { id -> openInPane(id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        composable(
            route = AppDestinations.EDITOR,
            arguments = listOf(
                navArgument("noteId") { type = NavType.StringType; defaultValue = "" },
                navArgument("folderId") { type = NavType.StringType; defaultValue = "" },
                navArgument("pin") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val noteIdArg = backStackEntry.arguments?.getString("noteId")?.takeIf { it.isNotBlank() }
            EditorHost(
                viewModel = viewModel,
                noteId = noteIdArg,
                folderId = backStackEntry.arguments?.getString("folderId")?.takeIf { it.isNotBlank() },
                initialPinned = backStackEntry.arguments?.getBoolean("pin") ?: false,
                folders = folders,
                snackbarHostState = snackbarHostState,
                onClose = { navController.popBackStack() },
                onOpenNote = { id ->
                    navController.navigate(AppDestinations.editorRoute(noteId = id)) {
                        popUpTo(AppDestinations.EDITOR) { inclusive = true }
                    }
                }
            )
        }
    }
}

/** Ancho a partir del cual la lista de notas y el editor se muestran lado a lado. */
private val TWO_PANE_MIN_WIDTH = 600.dp

/**
 * Editor con la carga de la nota desde la base de datos. Se usa a pantalla completa
 * y en el panel derecho de las pantallas anchas.
 */
@Composable
private fun EditorHost(
    viewModel: MainViewModel,
    noteId: String?,
    folderId: String?,
    initialPinned: Boolean,
    folders: List<FolderEntity>,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onOpenNote: (String) -> Unit,
    isNewNote: Boolean = noteId == null
) {
    // Se lee una sola vez de la base de datos (incluye notas en papelera).
    var existingNote by remember(noteId) { mutableStateOf<NoteEntity?>(null) }
    var noteLoaded by remember(noteId) { mutableStateOf(noteId == null) }
    LaunchedEffect(noteId) {
        if (noteId != null) {
            existingNote = viewModel.getNoteById(noteId)
            noteLoaded = true
        }
    }
    // Versión en vivo: si llega un cambio de otro dispositivo y no se ha tocado la nota, se muestra.
    val liveNoteFlow = remember(noteId) { noteId?.let(viewModel::observeNote) ?: kotlinx.coroutines.flow.flowOf(null) }
    val liveNote by liveNoteFlow.collectAsState(initial = null)

    NoteEditorScreen(
        noteId = noteId,
        isNewNote = isNewNote,
        folderId = folderId,
        initialPinned = initialPinned,
        existingNote = existingNote,
        liveNote = liveNote,
        noteLoaded = noteLoaded,
        onBaseLoaded = viewModel::registerEditorBase,
        folders = folders,
        snackbarHostState = snackbarHostState,
        onBack = onClose,
        onSaveNote = { id, title, content, folder, color, isPinned, sync, closing ->
            viewModel.saveNote(id, title, content, folder, color, isPinned, sync, closing)
        },
        onDeleteNote = viewModel::deleteNote,
        onDuplicateNote = viewModel::duplicateNote,
        onOpenNote = onOpenNote,
        onAddImage = viewModel::addImageAttachment
    )
}
