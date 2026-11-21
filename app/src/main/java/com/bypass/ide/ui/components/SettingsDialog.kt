package com.bypass.ide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bypass.ide.model.IdeSettings
import com.bypass.ide.model.NetworkShieldStatus
import com.bypass.ide.model.ReasoningTier
import com.bypass.ide.ui.theme.CyanElectric
import com.bypass.ide.ui.theme.CyanSoft
import com.bypass.ide.ui.theme.EmeraldMatrix
import com.bypass.ide.ui.theme.JetBrainsMonoFontFamily
import com.bypass.ide.ui.theme.ObsidianBg
import com.bypass.ide.ui.theme.ObsidianBorder
import com.bypass.ide.ui.theme.ObsidianSurface
import com.bypass.ide.ui.theme.ObsidianSurfaceElevated
import com.bypass.ide.ui.theme.RoseError
import com.bypass.ide.ui.theme.TextPrimary
import com.bypass.ide.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    settings: IdeSettings,
    networkShield: NetworkShieldStatus,
    onUpdateSettings: ((IdeSettings) -> IdeSettings) -> Unit,
    onCheckNetwork: () -> Unit,
    onResetSocketPool: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "IDE Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Network & IP Shield Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (networkShield.isConnected) EmeraldMatrix.copy(alpha = 0.5f) else ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Network & IP Shield",
                                tint = if (networkShield.isConnected) EmeraldMatrix else RoseError,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Network & IP Shield",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "Public IP: ${networkShield.publicIp} (${networkShield.ipProvider.ifBlank { "probe" }})",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanSoft
                        )
                        Text(
                            text = networkShield.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCheckNetwork,
                                enabled = !networkShield.isChecking,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_check_network_btn")
                            ) {
                                if (networkShield.isChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.NetworkCheck,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Probe IP")
                            }

                            Button(
                                onClick = onResetSocketPool,
                                enabled = !networkShield.isChecking,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanElectric,
                                    contentColor = ObsidianBg
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_reset_network_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Pool")
                            }
                        }
                    }
                }

                HorizontalDivider(color = ObsidianBorder)

                // Reasoning Tier
                Text(
                    text = "AI Engine Reasoning Tier",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReasoningTier.entries.forEach { tier ->
                        val selected = settings.reasoningTier == tier
                        Surface(
                            onClick = { onUpdateSettings { it.copy(reasoningTier = tier) } },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) ObsidianBg else ObsidianSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) CyanElectric else ObsidianBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${tier.label} — ${tier.modelId}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) CyanSoft else TextPrimary
                                )
                                Text(
                                    text = tier.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = ObsidianBorder)

                // Toggles
                SettingToggleRow(
                    title = "Live @web Grounding",
                    subtitle = "Automatically fetch DuckDuckGo & Wikipedia docs for prompts",
                    checked = settings.webSearchEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(webSearchEnabled = v) } }
                )

                SettingToggleRow(
                    title = "Editor Line Numbers",
                    subtitle = "Show monospace line gutter in Code Editor",
                    checked = settings.showLineNumbers,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showLineNumbers = v) } }
                )

                SettingToggleRow(
                    title = "Maria Voice Auto-Speak",
                    subtitle = "Speak Maria AI replies aloud via Text-to-Speech",
                    checked = settings.mariaAutoSpeak,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(mariaAutoSpeak = v) } }
                )

                Column {
                    Text(
                        text = "Editor Font Size: ${settings.editorFontSizeSp}sp",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = settings.editorFontSizeSp.toFloat(),
                        onValueChange = { value ->
                            onUpdateSettings { it.copy(editorFontSizeSp = value.toInt()) }
                        },
                        valueRange = 10f..20f,
                        steps = 9
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = CyanElectric)
            }
        }
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
