package com.bypass.ide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bypass.ide.R
import com.bypass.ide.model.OpenEditorTab
import com.bypass.ide.ui.theme.CyanElectric
import com.bypass.ide.ui.theme.CyanMuted
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

@Composable
fun CodeEditorView(
    openTabs: List<OpenEditorTab>,
    activeTab: OpenEditorTab?,
    fontSizeSp: Int,
    showLineNumbers: Boolean,
    isApplyingAiEdit: Boolean,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onApplyAiEdit: (String) -> Unit,
    onOpenExplorer: () -> Unit,
    onRunPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var showAiEditBar by remember { mutableStateOf(false) }
    var aiEditPrompt by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        // Open File Tabs Bar
        if (openTabs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                openTabs.forEach { tab ->
                    val selected = tab.relativePath == activeTab?.relativePath
                    Surface(
                        onClick = { onSelectTab(tab.relativePath) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) ObsidianSurfaceHigh else ObsidianBg,
                        border = BorderStroke(
                            1.dp,
                            if (selected) CyanElectric.copy(alpha = 0.6f) else ObsidianBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (tab.isDirty) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(EmeraldMatrix, CircleShape)
                                )
                            }
                            Text(
                                text = tab.fileName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) TextPrimary else TextSecondary
                            )
                            IconButton(
                                onClick = { onCloseTab(tab.relativePath) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close ${tab.fileName}",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (activeTab == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = CyanElectric,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = stringResource(R.string.no_file_open),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.no_file_open_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedButton(
                        onClick = onOpenExplorer,
                        border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = CyanElectric,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open File Explorer", color = CyanSoft)
                    }
                }
            }
            return
        }

        // Toolbar: Undo, Redo, Save, Find, AI Edit, Preview
        Surface(
            color = ObsidianSurfaceElevated,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onUndo,
                        enabled = activeTab.undoStack.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (activeTab.undoStack.isNotEmpty()) TextPrimary else TextMuted
                        )
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = activeTab.redoStack.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (activeTab.redoStack.isNotEmpty()) TextPrimary else TextMuted
                        )
                    }
                    IconButton(
                        onClick = { showFindBar = !showFindBar }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Find & Replace",
                            tint = if (showFindBar) CyanElectric else TextSecondary
                        )
                    }
                    IconButton(
                        onClick = { showAiEditBar = !showAiEditBar },
                        modifier = Modifier.testTag("toggle_ai_edit_bar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Edit",
                            tint = if (showAiEditBar) EmeraldMatrix else CyanElectric
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = onSave,
                        shape = RoundedCornerShape(8.dp),
                        color = if (activeTab.isDirty) EmeraldMatrix else ObsidianSurfaceHigh,
                        modifier = Modifier.testTag("editor_save_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save file",
                                tint = if (activeTab.isDirty) ObsidianBg else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (activeTab.isDirty) "Save*" else "Saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeTab.isDirty) ObsidianBg else TextSecondary
                            )
                        }
                    }

                    if (activeTab.extension in setOf("html", "htm", "css", "js")) {
                        Surface(
                            onClick = {
                                if (activeTab.isDirty) onSave()
                                onRunPreview()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = CyanMuted,
                            border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Run Live Preview",
                                    tint = CyanSoft,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Run",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanSoft
                                )
                            }
                        }
                    }
                }
            }
        }

        // Optional Find & Replace Bar
        if (showFindBar) {
            Surface(
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = findQuery,
                        onValueChange = { findQuery = it },
                        placeholder = { Text("Find", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = replaceQuery,
                        onValueChange = { replaceQuery = it },
                        placeholder = { Text("Replace", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (findQuery.isNotEmpty() && activeTab.content.contains(findQuery)) {
                                onContentChange(activeTab.content.replace(findQuery, replaceQuery))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanElectric,
                            contentColor = ObsidianBg
                        )
                    ) {
                        Text("All")
                    }
                }
            }
        }

        // Optional AI Edit Bar
        if (showAiEditBar) {
            Surface(
                color = ObsidianSurface,
                border = BorderStroke(1.dp, EmeraldMatrix.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = aiEditPrompt,
                        onValueChange = { aiEditPrompt = it },
                        placeholder = {
                            Text("Describe AI edit for ${activeTab.fileName}...")
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldMatrix,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_edit_input")
                    )
                    Button(
                        onClick = {
                            onApplyAiEdit(aiEditPrompt)
                            aiEditPrompt = ""
                        },
                        enabled = !isApplyingAiEdit && aiEditPrompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldMatrix,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier.testTag("ai_edit_apply_button")
                    ) {
                        if (isApplyingAiEdit) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ObsidianBg
                            )
                        } else {
                            Text("Apply")
                        }
                    }
                }
            }
        }

        // Code Canvas with Line Numbers
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ObsidianBg)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 10.dp)
        ) {
            if (showLineNumbers) {
                Column(
                    modifier = Modifier
                        .background(ObsidianSurface.copy(alpha = 0.55f))
                        .padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (line in 1..activeTab.lineCount) {
                        Text(
                            text = line.toString(),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp + 6).sp,
                            color = TextMuted
                        )
                    }
                }
            }

            BasicTextField(
                value = activeTab.content,
                onValueChange = onContentChange,
                textStyle = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp + 6).sp,
                    color = TextPrimary
                ),
                cursorBrush = SolidColor(CyanElectric),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .testTag("code_editor_textfield")
            )
        }

        // Quick Symbol Accessory Bar
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("  ", "{", "}", "(", ")", "<", ">", "/", "=", ";", "\"", "'").forEach { sym ->
                    Surface(
                        onClick = { onContentChange(activeTab.content + sym) },
                        shape = RoundedCornerShape(6.dp),
                        color = ObsidianSurfaceElevated,
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Text(
                            text = if (sym == "  ") "TAB" else sym,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = CyanSoft,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
