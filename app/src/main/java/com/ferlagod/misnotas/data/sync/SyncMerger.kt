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

    /**
     * Mover la nota local a la papelera (recuperable) y recordar el ETag remoto. Se usa con
     * borrados que no se pueden verificar, para que nunca se pierda texto sin que el usuario lo vea.
     */
    data class TrashLocal(val etag: String?) : MergeAction<Nothing>

    /** Guardar la versión remota como sincronizada. */
    data class SaveRemote<T>(val entity: T) : MergeAction<T>

    /** Conservar los cambios locales pendientes y recordar el ETag remoto actual. */
    data class KeepLocal(val etag: String?) : MergeAction<Nothing>

    /** Conflicto en una nota: se guarda [main] y se crea [copy] como nota nueva. */
    data class Conflict(val main: NoteEntity, val copy: NoteEntity) : MergeAction<NoteEntity>
}

/** Acción cuando una entidad local ya no existe en el servidor. */
enum class MissingRemoteAction {
    NOTHING,
    DELETE_LOCAL,
    MARK_FOR_UPLOAD,
    FORGET_ETAG,
    /** Nota: pasa a la papelera local y se vuelve a subir (como nota en papelera). */
    TRASH_AND_REUPLOAD
}

/**
 * Reglas de fusión de la sincronización. Son funciones puras para poder probarlas.
 *
 * Principio: nunca perder texto. Si una nota cambió a la vez en local y en remoto con
 * contenido distinto, gana la más reciente y la otra se conserva como "(conflicto)".
 */
object SyncMerger {

    const val CONFLICT_SUFFIX = " (conflict)"
    const val UNTITLED = "Untitled"

    fun resolveNote(
        local: NoteEntity?,
        remote: RemoteRecord<NoteEntity>,
        etag: String?,
        newId: () -> String,
        conflictSuffix: String = CONFLICT_SUFFIX,
        untitled: String = UNTITLED
    ): MergeAction<NoteEntity> {
        fun copyOf(note: NoteEntity) = asConflictCopy(note, newId(), conflictSuffix, untitled)
        when (remote) {
            is RemoteRecord.Tombstone -> return when {
                local == null -> MergeAction.Skip
                // Cambios locales sin subir: se conservan y resucitan la nota en el servidor.
                local.syncStatus == SyncStatus.PENDING_UPLOAD -> MergeAction.KeepLocal(etag)
                local.syncStatus == SyncStatus.PENDING_DELETE -> MergeAction.DeleteLocal
                // Borrado definitivo hecho por otro dispositivo con la misma clave.
                remote.verified -> MergeAction.DeleteLocal
                // Lápida sin prueba (versión antigua o servidor): a la papelera, recuperable.
                else -> MergeAction.TrashLocal(etag)
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
                            copy = copyOf(local)
                        )
                        else -> MergeAction.Conflict(
                            main = local.copy(remoteEtag = etag),
                            copy = copyOf(remoteNote)
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

    /**
     * Como [resolveMissingRemote], pero para notas: si una nota sincronizada desaparece del
     * servidor sin lápida (alguien borró el fichero a mano, o el servidor perdió datos) no se
     * borra, sino que va a la papelera y se vuelve a subir.
     */
    fun resolveMissingRemoteNote(syncStatus: SyncStatus, remoteEtag: String?): MissingRemoteAction =
        when (val action = resolveMissingRemote(syncStatus, remoteEtag)) {
            MissingRemoteAction.DELETE_LOCAL ->
                if (syncStatus == SyncStatus.PENDING_DELETE) action else MissingRemoteAction.TRASH_AND_REUPLOAD
            else -> action
        }

    fun sameNoteContent(a: NoteEntity, b: NoteEntity): Boolean =
        a.title == b.title &&
            a.content == b.content &&
            a.folderId == b.folderId &&
            a.color == b.color &&
            a.isPinned == b.isPinned &&
            a.isDeleted == b.isDeleted

    private fun asConflictCopy(note: NoteEntity, id: String, suffix: String, untitled: String) =
        conflictCopy(note, id, suffix, untitled)

    /** Copia de [note] como nota nueva "(conflicto)", pendiente de subir. */
    fun conflictCopy(note: NoteEntity, id: String, suffix: String = CONFLICT_SUFFIX, untitled: String = UNTITLED) = note.copy(
        id = id,
        title = note.title.ifBlank { untitled } + suffix,
        isPinned = false,
        syncStatus = SyncStatus.PENDING_UPLOAD,
        remoteEtag = null
    )
}
