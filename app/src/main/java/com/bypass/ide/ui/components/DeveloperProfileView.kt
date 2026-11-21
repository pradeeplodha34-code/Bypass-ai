package com.bypass.ide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.bypass.ide.R
import com.bypass.ide.ui.theme.CyanElectric
import com.bypass.ide.ui.theme.CyanMuted
import com.bypass.ide.ui.theme.CyanSoft
import com.bypass.ide.ui.theme.EmeraldMatrix
import com.bypass.ide.ui.theme.EmeraldSoft
import com.bypass.ide.ui.theme.ObsidianBg
import com.bypass.ide.ui.theme.ObsidianBorder
import com.bypass.ide.ui.theme.ObsidianSurface
import com.bypass.ide.ui.theme.ObsidianSurfaceElevated
import com.bypass.ide.ui.theme.TextPrimary
import com.bypass.ide.ui.theme.TextSecondary

@Composable
fun DeveloperProfileView(
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val telegramHandle = "@divyankcodes"
    val telegramUrl = "https://t.me/divyankcodes"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_dev_avatar),
                    contentDescription = "Developer Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Bypass IDE",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Architecture",
                            tint = CyanElectric,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Lead Architect & Core Systems Developer",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanSoft
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Engine Pipeline: Online & Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldSoft
                        )
                        Text(
                            text = "Powered by Firebase AI Logic (Gemini 3.8 Flash & 3.5 Flash-Lite), App Check attestation, DuckDuckGo/Wikipedia live @web grounding, WebView Console telemetry, and Maria Voice.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                TelegramCreatorCard(
                    handle = telegramHandle,
                    onCopyHandle = { clipboard.setText(AnnotatedString(telegramHandle)) },
                    onOpenTelegram = { runCatching { uriHandler.openUri(telegramUrl) } }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyanElectric)
            }
        }
    )
}

@Composable
fun TelegramCreatorCard(
    handle: String,
    onCopyHandle: () -> Unit,
    onOpenTelegram: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CyanMuted.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Creator Contact • $handle",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = "Android Apps • Websites • Autonomous Mobile Tooling",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyHandle,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_telegram_handle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = CyanSoft,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Handle", color = CyanSoft)
                }

                Button(
                    onClick = onOpenTelegram,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldMatrix,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_telegram_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Telegram")
                }
            }
        }
    }
}
