package com.bypass.ide.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bypass.ide.model.IdeTab
import com.bypass.ide.ui.components.CodeEditorView
import com.bypass.ide.ui.components.DeveloperProfileView
import com.bypass.ide.ui.components.FileExplorerView
import com.bypass.ide.ui.components.HomeAgentView
import com.bypass.ide.ui.components.LivePreviewView
import com.bypass.ide.ui.components.SettingsDialog
import com.bypass.ide.ui.components.TerminalView
import com.bypass.ide.ui.theme.CyanElectric
import com.bypass.ide.ui.theme.CyanMuted
import com.bypass.ide.ui.theme.CyanSoft
import com.bypass.ide.ui.theme.EmeraldMatrix
import com.bypass.ide.ui.theme.ObsidianBg
import com.bypass.ide.ui.theme.ObsidianBorder
import com.bypass.ide.ui.theme.ObsidianSurface
import com.bypass.ide.ui.theme.ObsidianSurfaceElevated
import com.bypass.ide.ui.theme.RoseError
import com.bypass.ide.ui.theme.TextMuted
import com.bypass.ide.ui.theme.TextPrimary
import com.bypass.ide.ui.theme.TextSecondary
import com.bypass.ide.ui.voice.VoiceCallOverlay
import com.bypass.ide.viewmodel.IdeViewModel

@Composable
fun MainIdeScreen(
    viewModel: IdeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSettings by remember { mutableStateOf(false) }
    var showDeveloperProfile by remember { mutableStateOf(false) }
    var showVoiceOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(state.bannerMessage) {
        val msg = state.bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val isCompactWidth = maxWidth < 680.dp

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                IdeTopHeaderBar(
                    activeProject = state.settings.activeProject,
                    shieldConnected = state.networkShield.isConnected,
                    onOpenVoice = { showVoiceOverlay = true },
                    onOpenProfile = { showDeveloperProfile = true },
                    onOpenSettings = { showSettings = true }
                )
            },
            bottomBar = {
                if (isCompactWidth) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        IdeTab.entries.forEach { tab ->
                            val selected = state.activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanElectric,
                                    selectedTextColor = CyanSoft,
                                    indicatorColor = CyanMuted,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
            ) {
                if (!isCompactWidth) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        IdeTab.entries.forEach { tab ->
                            val selected = state.activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CyanElectric,
                                    selectedTextColor = CyanSoft,
                                    indicatorColor = CyanMuted,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (state.activeTab) {
                        IdeTab.HOME -> HomeAgentView(
                            state = state,
                            onSelectSession = viewModel::selectSession,
                            onNewSession = viewModel::createNewSession,
                            onClearChat = viewModel::clearActiveSessionMessages,
                            onPromptChange = viewModel::updatePromptInput,
                            onModeChange = viewModel::setAgentMode,
                            onTierChange = viewModel::setReasoningTier,
                            onToggleWebSearch = viewModel::toggleWebSearch,
                            onSendPrompt = viewModel::sendPrompt,
                            onStopGeneration = viewModel::stopGeneration,
                            onOpenFileInEditor = { path -> viewModel.openFileInEditor(path, true) },
                            onApplyCodeBlock = { code, lang ->
                                val ext = when (lang.lowercase()) {
                                    "html" -> "html"
                                    "css" -> "css"
                                    "javascript", "js" -> "js"
                                    "kotlin", "kt" -> "kt"
                                    "python", "py" -> "py"
                                    else -> "txt"
                                }
                                viewModel.applyCodeBlockToEditor(code, "generated.$ext")
                            },
                            onRunCommandInTerminal = { cmd ->
                                viewModel.selectTab(IdeTab.TERMINAL)
                                viewModel.runTerminalCommand(cmd)
                            },
                            onNavigateToTab = viewModel::selectTab,
                            onOpenSettings = { showSettings = true }
                        )

                        IdeTab.FILES -> FileExplorerView(
                            activeProject = state.settings.activeProject,
                            projects = state.projects,
                            nodes = state.workspaceNodes,
                            onSwitchProject = viewModel::switchProject,
                            onCreateProject = { name -> viewModel.createProject(name) },
                            onCreateFile = viewModel::createNewFile,
                            onCreateFolder = viewModel::createNewFolder,
                            onOpenFile = { path -> viewModel.openFileInEditor(path, true) },
                            onRenameNode = viewModel::renameWorkspaceNode,
                            onDeleteNode = viewModel::deleteWorkspaceNode,
                            onRefresh = { viewModel.refreshProjectFiles() }
                        )

                        IdeTab.EDITOR -> CodeEditorView(
                            openTabs = state.openTabs,
                            activeTab = state.activeEditorTab,
                            fontSizeSp = state.settings.editorFontSizeSp,
                            showLineNumbers = state.settings.showLineNumbers,
                            isApplyingAiEdit = state.isApplyingAiEdit,
                            onSelectTab = viewModel::selectEditorTab,
                            onCloseTab = viewModel::closeEditorTab,
                            onContentChange = viewModel::updateActiveEditorContent,
                            onSave = viewModel::saveActiveEditorFile,
                            onUndo = viewModel::undoEditor,
                            onRedo = viewModel::redoEditor,
                            onApplyAiEdit = viewModel::applyAiEditToActiveFile,
                            onOpenExplorer = { viewModel.selectTab(IdeTab.FILES) },
                            onRunPreview = { viewModel.selectTab(IdeTab.PREVIEW) }
                        )

                        IdeTab.PREVIEW -> LivePreviewView(
                            indexFile = viewModel.getActiveIndexFile(),
                            refreshToken = state.previewRefreshToken,
                            viewport = state.previewViewport,
                            consoleLogs = state.consoleLogs,
                            onViewportChange = viewModel::setPreviewViewport,
                            onReload = viewModel::triggerPreviewReload,
                            onConsoleLog = viewModel::addConsoleLog,
                            onClearConsole = viewModel::clearConsoleLogs,
                            onOpenEditor = { viewModel.selectTab(IdeTab.EDITOR) },
                            onFixWithAi = viewModel::requestAutoFixFromPreview
                        )

                        IdeTab.TERMINAL -> TerminalView(
                            activeProject = state.settings.activeProject,
                            entries = state.terminalEntries,
                            isRunning = state.isRunningCommand,
                            onExecute = viewModel::runTerminalCommand,
                            onClear = viewModel::clearTerminal
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = state.settings,
            networkShield = state.networkShield,
            onUpdateSettings = viewModel::updateSettings,
            onCheckNetwork = { viewModel.refreshNetworkShield(evictSockets = false) },
            onResetSocketPool = { viewModel.refreshNetworkShield(evictSockets = true) },
            onDismiss = { showSettings = false }
        )
    }

    if (showDeveloperProfile) {
        DeveloperProfileView(onDismiss = { showDeveloperProfile = false })
    }

    if (showVoiceOverlay) {
        VoiceCallOverlay(
            transcript = state.mariaTranscript,
            responseText = state.mariaResponse,
            isThinking = state.mariaLoading,
            errorMessage = state.mariaError,
            autoSpeak = state.settings.mariaAutoSpeak,
            onAskMaria = viewModel::submitMariaVoiceQuery,
            onDismiss = { showVoiceOverlay = false }
        )
    }
}

@Composable
private fun IdeTopHeaderBar(
    activeProject: String,
    shieldConnected: Boolean,
    onOpenVoice: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CyanMuted,
                    border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.55f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Bypass IDE Logo",
                            tint = CyanElectric,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Bypass IDE",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = activeProject,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Maria Voice Trigger
                Surface(
                    onClick = onOpenVoice,
                    shape = CircleShape,
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("top_maria_voice_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Maria Voice Assistant",
                            tint = CyanElectric,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Network Shield Indicator -> Settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("top_shield_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Network Shield Status",
                        tint = if (shieldConnected) EmeraldMatrix else RoseError
                    )
                }

                // Developer Profile
                IconButton(
                    onClick = onOpenProfile,
                    modifier = Modifier.testTag("top_profile_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Developer Profile",
                        tint = TextSecondary
                    )
                }

                // IDE Settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("top_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "IDE Settings",
                        tint = TextPrimary
                    )
                }
            }
        }
    }
}

private fun tabIcon(tab: IdeTab, selected: Boolean): ImageVector = when (tab) {
    IdeTab.HOME -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
    IdeTab.FILES -> if (selected) Icons.Filled.Folder else Icons.Outlined.Folder
    IdeTab.EDITOR -> if (selected) Icons.Filled.Code else Icons.Outlined.Code
    IdeTab.PREVIEW -> if (selected) Icons.Filled.PlayArrow else Icons.Outlined.PlayArrow
    IdeTab.TERMINAL -> if (selected) Icons.Filled.Terminal else Icons.Outlined.Terminal
}
