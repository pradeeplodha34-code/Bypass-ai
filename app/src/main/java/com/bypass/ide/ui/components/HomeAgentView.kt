package com.bypass.ide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bypass.ide.R
import com.bypass.ide.model.AgentActionReport
import com.bypass.ide.model.AgentMode
import com.bypass.ide.model.ChatMessage
import com.bypass.ide.model.IdeTab
import com.bypass.ide.viewmodel.IdeUiState
import com.bypass.ide.ui.theme.CyanElectric
import com.bypass.ide.ui.theme.CyanMuted
import com.bypass.ide.ui.theme.CyanSoft
import com.bypass.ide.ui.theme.EmeraldMatrix
import com.bypass.ide.ui.theme.EmeraldMuted
import com.bypass.ide.ui.theme.EmeraldSoft
import com.bypass.ide.ui.theme.ObsidianBg
import com.bypass.ide.ui.theme.ObsidianBorder
import com.bypass.ide.ui.theme.ObsidianSurface
import com.bypass.ide.ui.theme.ObsidianSurfaceElevated
import com.bypass.ide.ui.theme.ObsidianSurfaceHigh
import com.bypass.ide.ui.theme.RoseError
import com.bypass.ide.ui.theme.TextMuted
import com.bypass.ide.ui.theme.TextPrimary
import com.bypass.ide.ui.theme.TextSecondary

@Composable
fun HomeAgentView(
    state: IdeUiState,
    onSelectSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onClearChat: () -> Unit,
    onPromptChange: (String) -> Unit,
    onModeChange: (AgentMode) -> Unit,
    onTierChange: (com.bypass.ide.model.ReasoningTier) -> Unit,
    onToggleWebSearch: () -> Unit,
    onSendPrompt: (String?) -> Unit,
    onStopGeneration: () -> Unit,
    onOpenFileInEditor: (String) -> Unit,
    onApplyCodeBlock: (code: String, lang: String) -> Unit,
    onRunCommandInTerminal: (String) -> Unit,
    onNavigateToTab: (IdeTab) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeMessages = state.activeSession?.messages.orEmpty()
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(activeMessages.size, state.streamingText.length) {
        val total = activeMessages.size + if (state.isGenerating) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Session bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = onNewSession,
                    shape = RoundedCornerShape(999.dp),
                    color = CyanMuted,
                    border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("new_chat_session_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat Session",
                            tint = CyanSoft,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "New Chat",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanSoft
                        )
                    }
                }

                state.sessions.forEach { session ->
                    val selected = session.id == state.activeSessionId
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSession(session.id) },
                        label = {
                            Text(
                                text = session.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 130.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ObsidianSurfaceHigh,
                            selectedLabelColor = TextPrimary,
                            containerColor = ObsidianBg,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            if (activeMessages.isNotEmpty()) {
                IconButton(
                    onClick = {
                        val transcript = activeMessages.joinToString("\n\n") {
                            "${if (it.isUser) "You" else "Bypass AI"}:\n${it.text}"
                        }
                        clipboard.setText(AnnotatedString(transcript))
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Chat",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear Chat",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Chat Feed or Empty Hero State
        Box(modifier = Modifier.weight(1f)) {
            if (activeMessages.isEmpty() && !state.isGenerating) {
                EmptyAgentLanding(
                    starterPrompts = state.starterPrompts,
                    activeProject = state.settings.activeProject,
                    onPickPrompt = { item ->
                        onModeChange(AgentMode.AGENT)
                        onSendPrompt(item.prompt)
                    },
                    onOpenPreview = { onNavigateToTab(IdeTab.PREVIEW) }
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(activeMessages, key = { it.id }) { msg ->
                        ChatMessageCard(
                            message = msg,
                            onOpenFile = onOpenFileInEditor,
                            onOpenPreview = { onNavigateToTab(IdeTab.PREVIEW) },
                            onApplyCode = onApplyCodeBlock,
                            onRunCommand = onRunCommandInTerminal,
                            onOpenSettings = onOpenSettings
                        )
                    }

                    if (state.isGenerating) {
                        item(key = "streaming_bubble") {
                            StreamingAssistantCard(
                                isSearchingWeb = state.isSearchingWeb,
                                streamingText = state.streamingText,
                                sources = state.streamingSources,
                                onApplyCode = onApplyCodeBlock
                            )
                        }
                    }
                }
            }
        }

        AgentPromptBar(
            prompt = state.promptInput,
            agentMode = state.agentMode,
            reasoningTier = state.settings.reasoningTier,
            webSearchEnabled = state.settings.webSearchEnabled,
            isGenerating = state.isGenerating,
            onPromptChange = onPromptChange,
            onModeChange = onModeChange,
            onTierChange = onTierChange,
            onToggleWebSearch = onToggleWebSearch,
            onSend = { onSendPrompt(null) },
            onStop = onStopGeneration
        )
    }
}

@Composable
private fun EmptyAgentLanding(
    starterPrompts: List<com.bypass.ide.model.StarterPromptItem>,
    activeProject: String,
    onPickPrompt: (com.bypass.ide.model.StarterPromptItem) -> Unit,
    onOpenPreview: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_bypass_hero),
                            contentDescription = "Bypass IDE Hero Workspace Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, ObsidianSurface)
                                    )
                                )
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = ObsidianBg.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyanElectric,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Workspace: $activeProject",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyanSoft
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.empty_chat_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.empty_chat_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onOpenPreview,
                            border = BorderStroke(1.dp, EmeraldMatrix.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("empty_open_preview_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = EmeraldMatrix,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Launch Starter Live Preview", color = EmeraldSoft)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "QUICK AUTONOMOUS BUILDS",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
        }

        items(starterPrompts) { item ->
            Surface(
                onClick = { onPickPrompt(item) },
                shape = RoundedCornerShape(14.dp),
                color = ObsidianSurfaceElevated,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("starter_prompt_${item.label.lowercase().replace(" ", "_")}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CyanMuted,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = CyanElectric,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSoft
                            )
                        }
                        Text(
                            text = item.prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageCard(
    message: ChatMessage,
    onOpenFile: (String) -> Unit,
    onOpenPreview: () -> Unit,
    onApplyCode: (String, String) -> Unit,
    onRunCommand: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    if (message.isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                color = CyanMuted,
                border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.4f)),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (message.mode == AgentMode.AGENT) "AGENT TASK" else "CHAT",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanSoft
                    )
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (message.isError) RoseError.copy(alpha = 0.12f) else ObsidianSurface,
            border = BorderStroke(
                1.dp,
                if (message.isError) RoseError.copy(alpha = 0.5f) else ObsidianBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (message.isError) Icons.Default.ErrorOutline else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (message.isError) RoseError else EmeraldMatrix,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (message.isError) "Engine Error" else "Bypass AI",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (message.isError) RoseError else EmeraldSoft
                    )
                }

                if (message.isError) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    OutlinedButton(
                        onClick = onOpenSettings,
                        border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.55f)),
                        modifier = Modifier.testTag("error_open_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = CyanElectric,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Open IDE Settings", color = CyanSoft)
                    }
                } else {
                    StructuredChatMessage(
                        markdown = message.text,
                        sources = message.sources,
                        onApplyCode = onApplyCode,
                        onRunCommand = onRunCommand
                    )
                }

                message.actionReport?.let { report ->
                    AgentActionReportCard(
                        report = report,
                        onOpenFile = onOpenFile,
                        onOpenPreview = onOpenPreview
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingAssistantCard(
    isSearchingWeb: Boolean,
    streamingText: String,
    sources: List<com.bypass.ide.model.ExaminedSource>,
    onApplyCode: (String, String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = CyanElectric
                )
                Text(
                    text = if (isSearchingWeb) "Fetching live @web sources..." else "Synthesizing response...",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanSoft
                )
            }
            if (streamingText.isNotBlank()) {
                StructuredChatMessage(
                    markdown = streamingText,
                    sources = sources,
                    onApplyCode = onApplyCode
                )
            }
        }
    }
}

@Composable
private fun AgentActionReportCard(
    report: AgentActionReport,
    onOpenFile: (String) -> Unit,
    onOpenPreview: () -> Unit
) {
    val touchedFiles = (report.createdFiles + report.editedFiles).distinct()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = EmeraldMuted.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, EmeraldMatrix.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Workspace Changes Applied",
                style = MaterialTheme.typography.titleSmall,
                color = EmeraldSoft
            )
            if (touchedFiles.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    touchedFiles.forEach { file ->
                        Surface(
                            onClick = { onOpenFile(file) },
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianBg,
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Open $file",
                                    tint = CyanElectric,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = file,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
            if (report.hasPreviewableChanges) {
                Button(
                    onClick = onOpenPreview,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldMatrix,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier.testTag("agent_open_live_preview_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Open Live Preview")
                }
            }
        }
    }
}
