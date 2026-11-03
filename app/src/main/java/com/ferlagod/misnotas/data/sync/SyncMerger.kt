// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.data.sync

import com.ferlagod.misnotas.data.local.entity.FolderEntity
import com.ferlagod.misnotas.data.local.entity.NoteEntity
import com.ferlagod.misnotas.data.local.model.SyncStatus

/** Acción a aplicar en local tras comparar una entidad local con su versión remota. */
sealed interface MergeAction<out T> {
    /** No hay nada que hacer. */
    data object Skip : MergeAction<Nothing>

    /** Borrar la fila local. */
    data object DeleteLocal : MergeAction<Nothing>

    /** Guardar la versión remota como sincronizada. */
    data class SaveRemote<T>(val entity: T) : MergeAction<T>

    /** Conservar los cambios locales pendientes y recordar el ETag remoto actual. */
    data class KeepLocal(val etag: String?) : MergeAction<Nothing>

    /** Conflicto en una nota: se guarda [main] y se crea [copy] como nota nueva. */
    data class Conflict(val main: NoteEntity, val copy: NoteEntity) : MergeAction<NoteEntity>
}

/** Acción cuando una entidad local ya no existe en el servidor. */
enum class MissingRemoteAction { NOTHING, DELETE_LOCAL, MARK_FOR_UPLOAD, FORGET_ETAG }

/**
 * Reglas de fusión de la sincronización. Son funciones puras para poder probarlas.
 *
 * Principio: nunca perder texto. Si una nota cambió a la vez en local y en remoto con
 * contenido distinto, gana la más reciente y la otra se conserva como "(conflicto)".
 */
object SyncMerger {

    const val CONFLICT_SUFFIX = " (conflicto)"

    fun resolveNote(
        local: NoteEntity?,
        remote: RemoteRecord<NoteEntity>,
        etag: String?,
        newId: () -> String
    ): MergeAction<NoteEntity> {
        when (remote) {
            is RemoteRecord.Tombstone -> return when {
                local == null -> MergeAction.Skip
                local.syncStatus == SyncStatus.PENDING_UPLOAD -> MergeAction.KeepLocal(etag)
                else -> MergeAction.DeleteLocal
            }

            is RemoteRecord.Live -> {
                val remoteNote = remote.entity.copy(syncStatus = SyncStatus.SYNCED, remoteEtag = etag)
                if (local == null) return MergeAction.SaveRemote(remoteNote)

                return when (local.syncStatus) {
                    SyncStatus.SYNCED -> MergeAction.SaveRemote(remoteNote)

                    SyncStatus.PENDING_DELETE ->
                        if (remoteNote.updatedAt > local.updatedAt) MergeAction.SaveRemote(remoteNote)
                        else MergeAction.KeepLocal(etag)

                    SyncStatus.PENDING_UPLOAD -> when {
                        sameNoteContent(local, remoteNote) -> MergeAction.KeepLocal(etag)
                        remoteNote.updatedAt > local.updatedAt -> MergeAction.Conflict(
                            main = remoteNote,
                            copy = asConflictCopy(local, newId())
                        )
                        else -> MergeAction.Conflict(
                            main = local.copy(remoteEtag = etag),
                            copy = asConflictCopy(remoteNote, newId())
                        )
                    }
                }
            }
        }
    }

    fun resolveFolder(
        local: FolderEntity?,
        remote: RemoteRecord<FolderEntity>,
        etag: String?
    ): MergeAction<FolderEntity> {
        when (remote) {
            is RemoteRecord.Tombstone -> return when {
                local == null -> MergeAction.Skip
                local.syncStatus == SyncStatus.PENDING_UPLOAD -> MergeAction.KeepLocal(etag)
                else -> MergeAction.DeleteLocal
            }

            is RemoteRecord.Live -> {
                val remoteFolder = remote.entity.copy(syncStatus = SyncStatus.SYNCED, remoteEtag = etag)
                if (local == null || local.syncStatus == SyncStatus.SYNCED) {
                    return MergeAction.SaveRemote(remoteFolder)
                }
                // Carpetas: gana la última modificación (no merece la pena duplicarlas).
                return if (remoteFolder.updatedAt > local.updatedAt) MergeAction.SaveRemote(remoteFolder)
                else MergeAction.KeepLocal(etag)
            }
        }
    }

    fun resolveMissingRemote(syncStatus: SyncStatus, remoteEtag: String?): MissingRemoteAction = when (syncStatus) {
        // Ya estaba en el servidor y ha desaparecido: otro dispositivo (o el usuario) lo borró.
        SyncStatus.SYNCED -> if (remoteEtag != null) MissingRemoteAction.DELETE_LOCAL else MissingRemoteAction.MARK_FOR_UPLOAD
        // Hay cambios locales: se vuelve a crear en el servidor.
        SyncStatus.PENDING_UPLOAD -> if (remoteEtag != null) MissingRemoteAction.FORGET_ETAG else MissingRemoteAction.NOTHING
        SyncStatus.PENDING_DELETE -> MissingRemoteAction.DELETE_LOCAL
    }

    fun sameNoteContent(a: NoteEntity, b: NoteEntity): Boolean =
        a.title == b.title &&
            a.content == b.content &&
            a.folderId == b.folderId &&
            a.color == b.color &&
            a.isPinned == b.isPinned &&
            a.isDeleted == b.isDeleted

    private fun asConflictCopy(note: NoteEntity, id: String) = note.copy(
        id = id,
        title = (note.title.ifBlank { "Sin título" }) + CONFLICT_SUFFIX,
        isPinned = false,
        syncStatus = SyncStatus.PENDING_UPLOAD,
        remoteEtag = null
    )
}
