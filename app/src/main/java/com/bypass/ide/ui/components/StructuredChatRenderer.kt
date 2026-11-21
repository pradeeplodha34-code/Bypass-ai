package com.bypass.ide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypass.ide.model.ExaminedSource
import com.bypass.ide.ui.theme.CyanElectric
import com.bypass.ide.ui.theme.CyanSoft
import com.bypass.ide.ui.theme.EmeraldMatrix
import com.bypass.ide.ui.theme.JetBrainsMonoFontFamily
import com.bypass.ide.ui.theme.ObsidianBg
import com.bypass.ide.ui.theme.ObsidianBorder
import com.bypass.ide.ui.theme.ObsidianSurface
import com.bypass.ide.ui.theme.ObsidianSurfaceElevated
import com.bypass.ide.ui.theme.ObsidianSurfaceHigh
import com.bypass.ide.ui.theme.TextMuted
import com.bypass.ide.ui.theme.TextPrimary
import com.bypass.ide.ui.theme.TextSecondary

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class NumberedStep(val number: String, val text: String) : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data object Divider : MarkdownBlock
}

@Composable
fun StructuredChatMessage(
    markdown: String,
    sources: List<ExaminedSource>,
    onApplyCode: (code: String, language: String) -> Unit,
    onRunCommand: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    var showSourcesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (sources.isNotEmpty()) {
            Surface(
                onClick = { showSourcesDialog = true },
                shape = RoundedCornerShape(999.dp),
                color = ObsidianSurfaceHigh,
                border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.35f)),
                modifier = Modifier.testTag("examined_sources_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Examined web sources",
                        tint = CyanElectric,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Examined Sources (${sources.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanSoft
                    )
                }
            }
        }

        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> HeadingItem(block.level, block.text)
                is MarkdownBlock.Paragraph -> ParagraphItem(block.text)
                is MarkdownBlock.CodeBlock -> {
                    val isShell = block.language.lowercase() in setOf("bash", "sh", "shell", "zsh")
                    if (isShell && onRunCommand != null && block.code.lines().size <= 3) {
                        TerminalCommandItem(
                            command = block.code,
                            onRun = { onRunCommand(block.code) }
                        )
                    } else {
                        CodeBlockItem(
                            language = block.language,
                            code = block.code,
                            onApply = { onApplyCode(block.code, block.language) }
                        )
                    }
                }
                is MarkdownBlock.Bullet -> BulletPointItem(block.text)
                is MarkdownBlock.NumberedStep -> NumberedStepItem(block.number, block.text)
                is MarkdownBlock.Blockquote -> BlockquoteItem(block.text)
                is MarkdownBlock.Table -> TableItem(block.headers, block.rows)
                MarkdownBlock.Divider -> DividerItem()
            }
        }
    }

    if (showSourcesDialog) {
        SourcesDetailDialog(
            sources = sources,
            onDismiss = { showSourcesDialog = false }
        )
    }
}

@Composable
fun HeadingItem(level: Int, text: String) {
    val style = when (level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.titleMedium
    }
    Text(
        text = buildInlineMarkdown(text),
        style = style,
        color = CyanSoft,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun ParagraphItem(text: String) {
    ClickableMarkdownText(text = text)
}

@Composable
fun ClickableMarkdownText(text: String) {
    val uriHandler = LocalUriHandler.current
    val annotated = remember(text) { buildInlineMarkdown(text) }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = TextPrimary,
        modifier = Modifier.clickable(enabled = annotated.getStringAnnotations("URL", 0, annotated.length).isNotEmpty()) {
            annotated.getStringAnnotations("URL", 0, annotated.length).firstOrNull()?.let { ann ->
                runCatching { uriHandler.openUri(ann.item) }
            }
        }
    )
}

@Composable
fun BulletPointItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(CyanElectric)
        )
        Box(modifier = Modifier.weight(1f)) {
            ClickableMarkdownText(text)
        }
    }
}

@Composable
fun NumberedStepItem(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.labelMedium,
            color = EmeraldMatrix,
            modifier = Modifier.padding(top = 2.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            ClickableMarkdownText(text)
        }
    }
}

@Composable
fun BlockquoteItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianSurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(CyanElectric, RoundedCornerShape(2.dp))
        )
        Text(
            text = buildInlineMarkdown(text),
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = TextSecondary
        )
    }
}

@Composable
fun CodeBlockItem(
    language: String,
    code: String,
    onApply: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianBg,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurfaceHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = CyanElectric,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = language.ifBlank { "code" }.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanSoft
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = {
                            clipboard.setText(AnnotatedString(code))
                            copied = true
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = ObsidianSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy code",
                                tint = if (copied) EmeraldMatrix else TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (copied) "Copied" else "Copy",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (copied) EmeraldMatrix else TextSecondary
                            )
                        }
                    }
                    Surface(
                        onClick = onApply,
                        shape = RoundedCornerShape(6.dp),
                        color = CyanElectric.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "Apply",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanElectric,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun TerminalCommandItem(
    command: String,
    onRun: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ObsidianBg,
        border = BorderStroke(1.dp, EmeraldMatrix.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = EmeraldMatrix,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = command,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    color = EmeraldMatrix,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                onClick = onRun,
                shape = RoundedCornerShape(8.dp),
                color = EmeraldMatrix.copy(alpha = 0.16f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run command",
                        tint = EmeraldMatrix,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Run",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldMatrix
                    )
                }
            }
        }
    }
}

@Composable
fun TableItem(headers: List<String>, rows: List<List<String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .background(ObsidianSurfaceHigh)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanSoft,
                    modifier = Modifier.width(130.dp)
                )
            }
        }
        rows.forEach { row ->
            HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.5f))
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.width(130.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DividerItem() {
    HorizontalDivider(
        color = ObsidianBorder.copy(alpha = 0.7f),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun SourcesDetailDialog(
    sources: List<ExaminedSource>,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "Examined Sources (${sources.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sources) { src ->
                    Surface(
                        onClick = { runCatching { uriHandler.openUri(src.url) } },
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianSurfaceElevated,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = src.domain,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanElectric
                                )
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open source",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = src.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (src.snippet.isNotBlank()) {
                                Text(
                                    text = src.snippet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyanElectric)
            }
        }
    )
}

private fun buildInlineMarkdown(raw: String): AnnotatedString = buildAnnotatedString {
    val tokenRegex = Regex(
        """(\[([^\]]+)\]\((https?://[^\s)]+)\))|(\*\*([^*]+)\*\*)|(`([^`]+)`)|(https?://[^\s)\],]+)"""
    )
    var lastIdx = 0
    tokenRegex.findAll(raw).forEach { match ->
        if (match.range.first > lastIdx) {
            append(raw.substring(lastIdx, match.range.first))
        }
        when {
            match.groupValues[2].isNotEmpty() -> {
                val label = match.groupValues[2]
                val url = match.groupValues[3]
                pushStringAnnotation("URL", url)
                withStyle(
                    SpanStyle(
                        color = CyanElectric,
                        textDecoration = TextDecoration.Underline
                    )
                ) { append(label) }
                pop()
            }
            match.groupValues[5].isNotEmpty() -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                    append(match.groupValues[5])
                }
            }
            match.groupValues[7].isNotEmpty() -> {
                withStyle(
                    SpanStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        background = ObsidianSurfaceHigh,
                        color = CyanSoft,
                        fontSize = 12.sp
                    )
                ) { append(" ${match.groupValues[7]} ") }
            }
            match.groupValues[8].isNotEmpty() -> {
                val url = match.groupValues[8]
                pushStringAnnotation("URL", url)
                withStyle(
                    SpanStyle(
                        color = CyanElectric,
                        textDecoration = TextDecoration.Underline
                    )
                ) { append(url) }
                pop()
            }
        }
        lastIdx = match.range.last + 1
    }
    if (lastIdx < raw.length) {
        append(raw.substring(lastIdx))
    }
}

private fun parseMarkdownBlocks(input: String): List<MarkdownBlock> {
    val lines = input.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (trimmed.isEmpty()) {
            i++
            continue
        }

        if (trimmed.startsWith("```")) {
            val lang = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++
            blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            continue
        }

        if (trimmed == "---" || trimmed == "***") {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        val headingMatch = Regex("""^(#{1,3})\s+(.+)$""").matchEntire(trimmed)
        if (headingMatch != null) {
            blocks.add(MarkdownBlock.Heading(headingMatch.groupValues[1].length, headingMatch.groupValues[2]))
            i++
            continue
        }

        if (trimmed.startsWith("> ")) {
            blocks.add(MarkdownBlock.Blockquote(trimmed.removePrefix("> ").trim()))
            i++
            continue
        }

        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            blocks.add(MarkdownBlock.Bullet(trimmed.drop(2).trim()))
            i++
            continue
        }

        val numberedMatch = Regex("""^(\d+)\.\s+(.+)$""").matchEntire(trimmed)
        if (numberedMatch != null) {
            blocks.add(MarkdownBlock.NumberedStep(numberedMatch.groupValues[1], numberedMatch.groupValues[2]))
            i++
            continue
        }

        if (trimmed.startsWith("|") && i + 1 < lines.size && lines[i + 1].trim().contains("---")) {
            val headers = trimmed.trim('|').split("|").map { it.trim() }
            i += 2
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trim().startsWith("|")) {
                rows.add(lines[i].trim().trim('|').split("|").map { it.trim() })
                i++
            }
            blocks.add(MarkdownBlock.Table(headers, rows))
            continue
        }

        val paraLines = mutableListOf(line)
        i++
        while (i < lines.size) {
            val nextTrim = lines[i].trim()
            if (nextTrim.isEmpty() ||
                nextTrim.startsWith("```") ||
                nextTrim.startsWith("#") ||
                nextTrim.startsWith("- ") ||
                nextTrim.startsWith("* ") ||
                nextTrim.startsWith("> ") ||
                nextTrim == "---"
            ) break
            paraLines.add(lines[i])
            i++
        }
        blocks.add(MarkdownBlock.Paragraph(paraLines.joinToString("\n")))
    }
    return blocks
}
