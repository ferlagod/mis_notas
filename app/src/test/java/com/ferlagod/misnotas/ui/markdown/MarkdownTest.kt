// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.ui.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTest {

    @Test
    fun togglesTaskLines() {
        val md = "Lista\n- [ ] uno\n  * [x] dos"
        assertEquals("Lista\n- [x] uno\n  * [x] dos", toggleTaskLine(md, 1))
        assertEquals("Lista\n- [ ] uno\n  * [ ] dos", toggleTaskLine(md, 2))
        assertEquals(md, toggleTaskLine(md, 0))
        assertEquals(md, toggleTaskLine(md, 99))
    }

    @Test
    fun previewStripsSyntax() {
        val md = "# Título\n![img](attachment://a)\n- [x] hecho\n- [ ] pendiente\n**negrita** y [enlace](https://x.y)"
        assertEquals("Título\n☑ hecho\n☐ pendiente\nnegrita y enlace", markdownPreview(md))
    }
}
