// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 ferlagod
// Mis Notas — https://github.com/ferlagod/mis_notas

package com.ferlagod.misnotas.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ferlagod.misnotas.data.attachment.AttachmentStore
import com.ferlagod.misnotas.ui.theme.AccentGreen

/**
 * Bloques Markdown reconocidos por el lector. Subconjunto de CommonMark/GFM pensado para notas.
 */
private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Task(val lineIndex: Int, val checked: Boolean, val text: String, val indent: Int) : MdBlock
    data class Bullet(val text: String, val indent: Int) : MdBlock
    data class Numbered(val number: String, val text: String, val indent: Int) : MdBlock
    data class Code(val text: String) : MdBlock
    data class Image(val alt: String, val source: String) : MdBlock
    data object Rule : MdBlock
    data object Blank : MdBlock
}

private val TASK_REGEX = Regex("""^(\s*)[-*+] \[( |x|X)] (.*)$""")
private val BULLET_REGEX = Regex("""^(\s*)[-*+] (.*)$""")
private val NUMBERED_REGEX = Regex("""^(\s*)(\d{1,9})[.)] (.*)$""")
private val HEADING_REGEX = Regex("""^(#{1,6}) (.*)$""")
private val IMAGE_REGEX = Regex("""^\s*!\[(.*?)]\((.*?)\)\s*$""")
private val RULE_REGEX = Regex("""^\s*([-*_])(\s*\1){2,}\s*$""")

private fun parseBlocks(markdown: String): List<MdBlock> {
    val lines = markdown.lines()
    val blocks = mutableListOf<MdBlock>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            line.trimStart().startsWith("```") -> {
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    if (code.isNotEmpty()) code.append('\n')
                    code.append(lines[i])
                    i++
                }
                blocks += MdBlock.Code(code.toString())
            }
            line.isBlank() -> blocks += MdBlock.Blank
            RULE_REGEX.matches(line) -> blocks += MdBlock.Rule
            HEADING_REGEX.matches(line) -> {
                val m = HEADING_REGEX.find(line)!!
                blocks += MdBlock.Heading(m.groupValues[1].length, m.groupValues[2])
            }
            IMAGE_REGEX.matches(line) -> {
                val m = IMAGE_REGEX.find(line)!!
                blocks += MdBlock.Image(m.groupValues[1], m.groupValues[2])
            }
            TASK_REGEX.matches(line) -> {
                val m = TASK_REGEX.find(line)!!
                blocks += MdBlock.Task(i, m.groupValues[2].isNotBlank(), m.groupValues[3], m.groupValues[1].length / 2)
            }
            BULLET_REGEX.matches(line) -> {
                val m = BULLET_REGEX.find(line)!!
                blocks += MdBlock.Bullet(m.groupValues[2], m.groupValues[1].length / 2)
            }
            NUMBERED_REGEX.matches(line) -> {
                val m = NUMBERED_REGEX.find(line)!!
                blocks += MdBlock.Numbered(m.groupValues[2], m.groupValues[3], m.groupValues[1].length / 2)
            }
            line.startsWith(">") -> blocks += MdBlock.Quote(line.removePrefix(">").trimStart())
            line.startsWith("    ") || line.startsWith("\t") -> blocks += MdBlock.Code(line.trim())
            else -> blocks += MdBlock.Paragraph(line)
        }
        i++
    }
    return blocks
}

/** Alterna la casilla de la línea [lineIndex] (`- [ ]` <-> `- [x]`). */
fun toggleTaskLine(markdown: String, lineIndex: Int): String {
    val lines = markdown.lines().toMutableList()
    if (lineIndex !in lines.indices) return markdown
    val m = TASK_REGEX.find(lines[lineIndex]) ?: return markdown
    val checked = m.groupValues[2].isNotBlank()
    val marker = if (checked) "[ ]" else "[x]"
    lines[lineIndex] = lines[lineIndex].replaceFirst(Regex("""\[( |x|X)]"""), Regex.escapeReplacement(marker))
    return lines.joinToString("\n")
}

/** Resuelve el origen de una imagen Markdown a un modelo que entiende Coil. */
@Composable
fun rememberImageModel(source: String): Any? {
    val context = LocalContext.current
    return remember(source) {
        when {
            source.startsWith(AttachmentStore.SCHEME) ->
                AttachmentStore.fileFor(context, source.removePrefix(AttachmentStore.SCHEME))
            source.startsWith("data:image/") || source.startsWith("https://") || source.startsWith("http://") -> source
            else -> null
        }
    }
}

/**
 * Renderizador de Markdown nativo para el modo lectura.
 */
@Composable
fun MarkdownReader(
    markdownText: String,
    primaryBlue: Color,
    onToggleChecklist: (lineIndex: Int) -> Unit
) {
    val blocks = remember(markdownText) { parseBlocks(markdownText) }
    val linkColor = primaryBlue

    Column(modifier = Modifier.fillMaxWidth()) {
        for (block in blocks) {
            when (block) {
                is MdBlock.Heading -> Text(
                    text = inline(block.text, linkColor),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                        2 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = if (block.level == 1) 14.dp else 10.dp, bottom = 4.dp)
                )

                MdBlock.Rule -> HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                is MdBlock.Quote -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(primaryBlue, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = inline(block.text, linkColor),
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is MdBlock.Task -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleChecklist(block.lineIndex) }
                        .padding(start = (block.indent * 20).dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = block.checked,
                        onCheckedChange = { onToggleChecklist(block.lineIndex) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentGreen,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = inline(block.text, linkColor),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (block.checked) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (block.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }

                is MdBlock.Bullet -> ListRow(marker = "•", indent = block.indent, markerColor = primaryBlue) {
                    Text(inline(block.text, linkColor), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }

                is MdBlock.Numbered -> ListRow(marker = "${block.number}.", indent = block.indent, markerColor = primaryBlue) {
                    Text(inline(block.text, linkColor), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }

                is MdBlock.Code -> Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = block.text,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp)
                    )
                }

                is MdBlock.Image -> {
                    val model = rememberImageModel(block.source)
                    if (model != null) {
                        AsyncImage(
                            model = model,
                            contentDescription = block.alt.ifBlank { "Imagen" },
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 480.dp)
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                is MdBlock.Paragraph -> Text(
                    text = inline(block.text, linkColor),
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 3.dp)
                )

                MdBlock.Blank -> Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ListRow(marker: String, indent: Int, markerColor: Color, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 20).dp, top = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = marker,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = markerColor,
            modifier = Modifier.padding(end = 8.dp)
        )
        content()
    }
}

private val INLINE_REGEX = Regex("""(\*\*[^*]+\*\*|__[^_]+__|~~[^~]+~~|`[^`]+`|\[[^\]]+]\([^)\s]+\)|\*[^*\s][^*]*\*|_[^_\s][^_]*_)""")

/**
 * Formato en línea: **negrita**, *cursiva*, ~~tachado~~, `código` y [enlaces](https://…).
 */
fun inline(text: String, linkColor: Color = Color.Unspecified): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in INLINE_REGEX.findAll(text)) {
        if (match.range.first > cursor) append(text.substring(cursor, match.range.first))
        val token = match.value
        when {
            token.startsWith("**") || token.startsWith("__") ->
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(inline(token.substring(2, token.length - 2), linkColor)) }
            token.startsWith("~~") ->
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(token.removeSurrounding("~~")) }
            token.startsWith("`") ->
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.Gray.copy(alpha = 0.15f))) {
                    append(token.removeSurrounding("`"))
                }
            token.startsWith("[") -> {
                val label = token.substring(1, token.indexOf("]("))
                val url = token.substring(token.indexOf("](") + 2, token.length - 1)
                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("mailto:")) {
                    withLink(
                        LinkAnnotation.Url(
                            url,
                            TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                        )
                    ) { append(label) }
                } else {
                    append(token)
                }
            }
            else ->
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(token.substring(1, token.length - 1)) }
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}

/** Texto plano aproximado de una nota, para vistas previas en tarjetas. */
fun markdownPreview(markdown: String, maxLines: Int = 8): String =
    markdown.lineSequence()
        .filterNot { it.isBlank() || IMAGE_REGEX.matches(it) || it.trimStart().startsWith("```") }
        .map { line ->
            line.replace(Regex("""^#{1,6} """), "")
                .replace(Regex("""^(\s*)[-*+] \[(x|X)] """), "$1☑ ")
                .replace(Regex("""^(\s*)[-*+] \[ ] """), "$1☐ ")
                .replace(Regex("""^(\s*)[-*+] """), "$1• ")
                .replace(Regex("""^> ?"""), "")
                .replace(Regex("""\*\*|__|~~|`"""), "")
                .replace(Regex("""\[([^\]]+)]\([^)]*\)"""), "$1")
        }
        .take(maxLines)
        .joinToString("\n")
