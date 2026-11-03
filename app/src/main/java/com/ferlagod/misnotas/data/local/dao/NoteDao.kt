// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ferlagod.misnotas.data.local.entity.NoteEntity
import com.ferlagod.misnotas.data.local.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 AND syncStatus != 'PENDING_DELETE' ORDER BY updatedAt DESC")
    fun getTrashNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteByIdFlow(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    /** Todas las filas, incluidas papelera y borrados pendientes (uso interno de sincronización). */
    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE syncStatus = :syncStatus")
    suspend fun getNotesBySyncStatus(syncStatus: SyncStatus): List<NoteEntity>

    @Query("SELECT content FROM notes WHERE syncStatus != 'PENDING_DELETE'")
    suspend fun getAllLiveContents(): List<String>

    @Query("SELECT * FROM notes WHERE content LIKE '{%' AND content LIKE '%\"images\"%'")
    suspend fun getNotesWithLegacyImages(): List<NoteEntity>

    @Upsert
    suspend fun upsertNote(note: NoteEntity)

    @Upsert
    suspend fun upsertNotes(notes: List<NoteEntity>)

    @Query("UPDATE notes SET isPinned = :isPinned, updatedAt = :updatedAt, syncStatus = 'PENDING_UPLOAD' WHERE id = :id")
    suspend fun setPinned(id: String, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    /** Mueve la nota a la papelera (se sincroniza como nota en papelera). */
    @Query("UPDATE notes SET isDeleted = 1, syncStatus = 'PENDING_UPLOAD', updatedAt = :updatedAt WHERE id = :id")
    suspend fun moveToTrash(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, syncStatus = 'PENDING_UPLOAD', updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreFromTrash(id: String, updatedAt: Long = System.currentTimeMillis())

    /** Marca la nota para borrado definitivo; la sincronización subirá una lápida y eliminará la fila. */
    @Query("UPDATE notes SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markForPurge(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET syncStatus = 'PENDING_DELETE' WHERE isDeleted = 1")
    suspend fun markTrashForPurge()

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun deleteTrashPermanently()

    /** Saca las notas de una carpeta eliminada y las deja en "Todas las notas". */
    @Query("UPDATE notes SET folderId = NULL, updatedAt = :updatedAt, syncStatus = 'PENDING_UPLOAD' WHERE folderId = :folderId AND syncStatus != 'PENDING_DELETE'")
    suspend fun detachFromFolder(folderId: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * Marca como sincronizada la versión subida, pero solo si no ha cambiado mientras se subía.
     * Devuelve el número de filas actualizadas (0 si el usuario editó entretanto).
     */
    @Query("UPDATE notes SET syncStatus = 'SYNCED', remoteEtag = :etag WHERE id = :id AND updatedAt = :updatedAt AND syncStatus = 'PENDING_UPLOAD'")
    suspend fun markSynced(id: String, updatedAt: Long, etag: String?): Int

    @Query("UPDATE notes SET remoteEtag = :etag WHERE id = :id")
    suspend fun setRemoteEtag(id: String, etag: String?)

    @Query("UPDATE notes SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deletePermanently(id: String)

    /**
     * Olvida los ETags (al conectar una cuenta). En la siguiente sincronización se compara todo:
     * lo que no tenga cambios locales acepta la versión del servidor (o su borrado) y lo que no
     * exista en el servidor se sube.
     */
    @Query("UPDATE notes SET remoteEtag = NULL")
    suspend fun resetSyncMetadata()
}
