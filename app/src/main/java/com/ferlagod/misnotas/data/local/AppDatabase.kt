// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ferlagod.misnotas.data.local.converter.Converters
import com.ferlagod.misnotas.data.local.dao.AttachmentDao
import com.ferlagod.misnotas.data.local.dao.FolderDao
import com.ferlagod.misnotas.data.local.dao.NoteDao
import com.ferlagod.misnotas.data.local.dao.RemoteTombstoneDao
import com.ferlagod.misnotas.data.local.entity.AttachmentEntity
import com.ferlagod.misnotas.data.local.entity.FolderEntity
import com.ferlagod.misnotas.data.local.entity.NoteEntity
import com.ferlagod.misnotas.data.local.entity.RemoteTombstoneEntity

/**
 * Base de datos local (Room). Es la fuente de verdad de la app; Nextcloud es una réplica cifrada.
 */
@Database(
    entities = [
        FolderEntity::class,
        NoteEntity::class,
        AttachmentEntity::class,
        RemoteTombstoneEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao
    abstract fun noteDao(): NoteDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun remoteTombstoneDao(): RemoteTombstoneDao

    companion object {
        private const val DATABASE_NAME = "misnotas.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * v1 -> v2:
         * - Elimina la clave foránea notes.folderId -> folders.id (recreando la tabla).
         * - Añade remoteEtag a notas y carpetas.
         * - Crea las tablas de adjuntos y de lápidas remotas.
         * - Las notas en papelera de v1 (PENDING_DELETE) pasan a ser notas en papelera sincronizables.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notes_new` (
                        `id` TEXT NOT NULL,
                        `folderId` TEXT,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `isPinned` INTEGER NOT NULL,
                        `color` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `remoteEtag` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `notes_new` (id, folderId, title, content, isPinned, color, updatedAt, isDeleted, syncStatus, remoteEtag)
                    SELECT id, folderId, title, content, isPinned, color, updatedAt, isDeleted,
                           CASE WHEN syncStatus = 'PENDING_DELETE' THEN 'PENDING_UPLOAD' ELSE syncStatus END,
                           NULL
                    FROM `notes`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `notes`")
                db.execSQL("ALTER TABLE `notes_new` RENAME TO `notes`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_folderId` ON `notes` (`folderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isPinned` ON `notes` (`isPinned`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_syncStatus` ON `notes` (`syncStatus`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isDeleted` ON `notes` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_updatedAt` ON `notes` (`updatedAt`)")

                db.execSQL("ALTER TABLE `folders` ADD COLUMN `remoteEtag` TEXT")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `attachments` (
                        `id` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `size` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `isUploaded` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `remote_tombstones` (`fileName` TEXT NOT NULL, `etag` TEXT, PRIMARY KEY(`fileName`))"
                )
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
