package com.bypass.ide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bypass.ide.model.AgentMode
import com.bypass.ide.model.ReasoningTier
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
fun AgentPromptBar(
    prompt: String,
    agentMode: AgentMode,
    reasoningTier: ReasoningTier,
    webSearchEnabled: Boolean,
    isGenerating: Boolean,
    onPromptChange: (String) -> Unit,
    onModeChange: (AgentMode) -> Unit,
    onTierChange: (ReasoningTier) -> Unit,
    onToggleWebSearch: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTierMenu by remember { mutableStateOf(false) }

    Surface(
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Mode switch: Chat vs Agent
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AgentMode.entries.forEach { mode ->
                        val selected = mode == agentMode
                        val activeBg = if (mode == AgentMode.AGENT) EmeraldMuted else CyanMuted
                        val activeFg = if (mode == AgentMode.AGENT) EmeraldSoft else CyanSoft
                        Surface(
                            onClick = { onModeChange(mode) },
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) activeBg else ObsidianSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) activeFg.copy(alpha = 0.55f) else ObsidianBorder
                            ),
                            modifier = Modifier.testTag("mode_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (mode == AgentMode.AGENT) {
                                        Icons.Default.Bolt
                                    } else {
                                        Icons.Default.ChatBubbleOutline
                                    },
                                    contentDescription = mode.label,
                                    tint = if (selected) activeFg else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) activeFg else TextSecondary
                                )
                            }
                        }
                    }
                }

                // @web grounding & Reasoning tier pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onToggleWebSearch,
                        shape = RoundedCornerShape(999.dp),
                        color = if (webSearchEnabled) CyanMuted else ObsidianSurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (webSearchEnabled) CyanElectric.copy(alpha = 0.55f) else ObsidianBorder
                        ),
                        modifier = Modifier.testTag("toggle_web_search")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Toggle @web live search",
                                tint = if (webSearchEnabled) CyanElectric else TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "@web",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (webSearchEnabled) CyanSoft else TextMuted
                            )
                        }
                    }

                    Box {
                        Surface(
                            onClick = { showTierMenu = true },
                            shape = RoundedCornerShape(999.dp),
                            color = ObsidianSurfaceElevated,
                            border = BorderStroke(1.dp, ObsidianBorder),
                            modifier = Modifier.testTag("reasoning_tier_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Select reasoning tier",
                                    tint = CyanElectric,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = reasoningTier.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showTierMenu,
                            onDismissRequest = { showTierMenu = false }
                        ) {
                            ReasoningTier.entries.forEach { tier ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "${tier.label} (${tier.modelId})",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            Text(
                                                text = tier.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    },
                                    onClick = {
                                        onTierChange(tier)
                                        showTierMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    placeholder = {
                        Text(
                            text = if (agentMode == AgentMode.AGENT) {
                                "Autonomous tasks, file builds, or terminal ops..."
                            } else {
                                "Chat, explore architectures, or ask questions..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (!isGenerating) onSend() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg,
                        focusedBorderColor = if (agentMode == AgentMode.AGENT) EmeraldMatrix else CyanElectric,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("agent_prompt_input")
                )

                Surface(
                    shape = CircleShape,
                    color = when {
                        isGenerating -> RoseError.copy(alpha = 0.2f)
                        prompt.isNotBlank() && agentMode == AgentMode.AGENT -> EmeraldMatrix
                        prompt.isNotBlank() -> CyanElectric
                        else -> ObsidianSurfaceHigh
                    },
                    border = if (isGenerating) BorderStroke(1.dp, RoseError) else null,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = { if (isGenerating) onStop() else onSend() },
                        enabled = isGenerating || prompt.isNotBlank(),
                        modifier = Modifier.testTag("agent_send_button")
                    ) {
                        Icon(
                            imageVector = if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                            contentDescription = if (isGenerating) "Stop generation" else "Send prompt",
                            tint = when {
                                isGenerating -> RoseError
                                prompt.isNotBlank() -> ObsidianBg
                                else -> TextMuted
                            }
                        )
                    }
                }
            }
        }
    }
}
