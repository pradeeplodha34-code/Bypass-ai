package com.bypass.ide.ui.components

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bypass.ide.model.ConsoleLogEntry
import com.bypass.ide.model.PreviewViewport
import com.bypass.ide.ui.theme.AmberSignal
import com.bypass.ide.ui.theme.CyanElectric
import com.bypass.ide.ui.theme.CyanSoft
import com.bypass.ide.ui.theme.EmeraldMatrix
import com.bypass.ide.ui.theme.JetBrainsMonoFontFamily
import com.bypass.ide.ui.theme.ObsidianBg
import com.bypass.ide.ui.theme.ObsidianBorder
import com.bypass.ide.ui.theme.ObsidianSurface
import com.bypass.ide.ui.theme.ObsidianSurfaceElevated
import com.bypass.ide.ui.theme.ObsidianSurfaceHigh
import com.bypass.ide.ui.theme.RoseError
import com.bypass.ide.ui.theme.TextMuted
import com.bypass.ide.ui.theme.TextPrimary
import com.bypass.ide.ui.theme.TextSecondary
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LivePreviewView(
    indexFile: File?,
    refreshToken: Int,
    viewport: PreviewViewport,
    consoleLogs: List<ConsoleLogEntry>,
    onViewportChange: (PreviewViewport) -> Unit,
    onReload: () -> Unit,
    onConsoleLog: (level: String, message: String, source: String, line: Int) -> Unit,
    onClearConsole: () -> Unit,
    onOpenEditor: () -> Unit,
    onFixWithAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConsole by remember { mutableStateOf(false) }
    val errorCount = consoleLogs.count { it.level.equals("ERROR", ignoreCase = true) }

    Column(modifier = modifier.fillMaxSize()) {
        // Preview Controls Bar
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PreviewViewport.entries.forEach { vp ->
                        FilterChip(
                            selected = vp == viewport,
                            onClick = { onViewportChange(vp) },
                            label = {
                                Text(
                                    text = vp.label.substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ObsidianSurfaceHigh,
                                selectedLabelColor = CyanSoft
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = { showConsole = !showConsole },
                        shape = RoundedCornerShape(8.dp),
                        color = if (errorCount > 0) RoseError.copy(alpha = 0.16f) else ObsidianSurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (errorCount > 0) RoseError.copy(alpha = 0.5f) else ObsidianBorder
                        ),
                        modifier = Modifier.testTag("preview_console_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Toggle Console Logs",
                                tint = if (errorCount > 0) RoseError else CyanElectric,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Logs (${consoleLogs.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (errorCount > 0) RoseError else TextPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onReload,
                        modifier = Modifier.testTag("preview_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Preview",
                            tint = CyanElectric
                        )
                    }
                }
            }
        }

        // WebView Stage
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ObsidianBg),
            contentAlignment = Alignment.TopCenter
        ) {
            val targetModifier = if (viewport.maxWidthDp != null && maxWidth > viewport.maxWidthDp.dp) {
                Modifier
                    .width(viewport.maxWidthDp.dp)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp)
            } else {
                Modifier.fillMaxSize()
            }

            if (indexFile == null || !indexFile.exists()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = false
                            setBackgroundColor(0xFF080C14.toInt())
                            loadDataWithBaseURL(
                                null,
                                fallbackPreviewHtml(),
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    modifier = targetModifier
                )
            } else {
                val targetUrl = remember(indexFile.absolutePath, refreshToken) {
                    "file://${indexFile.absolutePath}?v=$refreshToken"
                }
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(0xFF080C14.toInt())
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                @Suppress("DEPRECATION")
                                allowFileAccessFromFileURLs = true
                                cacheMode = WebSettings.LOAD_NO_CACHE
                            }
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                                    if (msg != null) {
                                        onConsoleLog(
                                            msg.messageLevel().name,
                                            msg.message(),
                                            msg.sourceId().orEmpty(),
                                            msg.lineNumber()
                                        )
                                    }
                                    return true
                                }
                            }
                            loadUrl(targetUrl)
                        }
                    },
                    update = { webView ->
                        if (webView.tag != targetUrl) {
                            webView.tag = targetUrl
                            webView.loadUrl(targetUrl)
                        }
                    },
                    modifier = targetModifier
                )
            }
        }

        // Collapsible Console Logs & Auto-Fix Bar
        if (showConsole) {
            Surface(
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianSurfaceElevated)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Console Logs (${consoleLogs.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (errorCount > 0) {
                                Button(
                                    onClick = onFixWithAi,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldMatrix,
                                        contentColor = ObsidianBg
                                    ),
                                    modifier = Modifier.testTag("preview_fix_with_ai_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fix with AI")
                                }
                            }
                            IconButton(onClick = onClearConsole, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear logs",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = onOpenEditor, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Open Editor",
                                    tint = CyanElectric,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (consoleLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No console messages recorded",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(consoleLogs, key = { it.id }) { log ->
                                val tint = when (log.level.uppercase()) {
                                    "ERROR" -> RoseError
                                    "WARNING" -> AmberSignal
                                    else -> EmeraldMatrix
                                }
                                Text(
                                    text = "[${log.level}] ${log.message} (${log.source}:${log.lineNumber})",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun fallbackPreviewHtml(): String = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Preview Ready - Bypass IDE</title>
      <style>
        body {
          background: #080c14;
          color: #f1f5f9;
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          min-height: 100vh;
          margin: 0;
          padding: 24px;
          text-align: center;
        }
        .box {
          border: 1px solid #23324d;
          background: #0d1422;
          border-radius: 16px;
          padding: 24px;
          max-width: 320px;
        }
        h2 { color: #22d3ee; margin: 0 0 8px; font-size: 18px; }
        p { color: #94a3b8; font-size: 13px; line-height: 1.5; margin: 0; }
      </style>
    </head>
    <body>
      <div class="box">
        <h2>No index.html Found</h2>
        <p>Prompt Bypass AI in Agent mode to build a web app, or create an <code>index.html</code> file in the Files tab.</p>
      </div>
    </body>
    </html>
""".trimIndent()
