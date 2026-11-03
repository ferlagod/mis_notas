// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.data.attachment

import android.content.Context
import java.io.File

/**
 * Almacén de adjuntos en el almacenamiento privado de la app (`files/attachments/<id>`).
 *
 * En el Markdown de las notas los adjuntos se referencian como `![texto](attachment://<id>)`.
 */
class AttachmentStore(context: Context) {

    private val dir: File = File(context.applicationContext.filesDir, DIR_NAME).apply { mkdirs() }

    fun file(id: String): File = File(dir, sanitize(id))

    fun exists(id: String): Boolean = file(id).let { it.exists() && it.length() > 0 }

    fun read(id: String): ByteArray = file(id).readBytes()

    /** Escritura atómica: primero a un temporal y luego renombrado. */
    fun write(id: String, bytes: ByteArray) {
        val target = file(id)
        val tmp = File(dir, "${target.name}.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }

    fun delete(id: String) {
        file(id).delete()
    }

    companion object {
        const val DIR_NAME = "attachments"
        const val SCHEME = "attachment://"

        private val REF_REGEX = Regex("""attachment://([A-Za-z0-9_-]+)""")

        fun reference(id: String): String = "$SCHEME$id"

        fun referencedIds(markdown: String): Set<String> =
            REF_REGEX.findAll(markdown).map { it.groupValues[1] }.toSet()

        fun firstReferencedId(markdown: String): String? =
            REF_REGEX.find(markdown)?.groupValues?.get(1)

        fun fileFor(context: Context, id: String): File =
            File(File(context.applicationContext.filesDir, DIR_NAME), sanitize(id))

        private fun sanitize(id: String): String = id.replace(Regex("[^A-Za-z0-9_-]"), "_")
    }
}
