package com.bypass.ide.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bypass.ide.ai.GeminiAgentEngine
import com.bypass.ide.ai.StreamChunkEvent
import com.bypass.ide.data.NetworkShieldService
import com.bypass.ide.data.TerminalExecutor
import com.bypass.ide.data.WebSearchService
import com.bypass.ide.data.WorkspaceRepository
import com.bypass.ide.model.AgentMode
import com.bypass.ide.model.ChatMessage
import com.bypass.ide.model.ChatSession
import com.bypass.ide.model.ConsoleLogEntry
import com.bypass.ide.model.ExaminedSource
import com.bypass.ide.model.IdeSettings
import com.bypass.ide.model.IdeTab
import com.bypass.ide.model.NetworkShieldStatus
import com.bypass.ide.model.OpenEditorTab
import com.bypass.ide.model.PreviewViewport
import com.bypass.ide.model.ProjectInfo
import com.bypass.ide.model.ReasoningTier
import com.bypass.ide.model.StarterPromptItem
import com.bypass.ide.model.TerminalEntry
import com.bypass.ide.model.WorkspaceNode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "IdeViewModel"

data class IdeUiState(
    val activeTab: IdeTab = IdeTab.HOME,
    val settings: IdeSettings = IdeSettings(),
    val projects: List<ProjectInfo> = emptyList(),
    val workspaceNodes: List<WorkspaceNode> = emptyList(),
    val sessions: List<ChatSession> = emptyList(),
    val activeSessionId: String = "",
    val agentMode: AgentMode = AgentMode.AGENT,
    val promptInput: String = "",
    val isGenerating: Boolean = false,
    val isSearchingWeb: Boolean = false,
    val streamingText: String = "",
    val streamingSources: List<ExaminedSource> = emptyList(),
    val openTabs: List<OpenEditorTab> = emptyList(),
    val activeEditorPath: String? = null,
    val isApplyingAiEdit: Boolean = false,
    val previewRefreshToken: Int = 0,
    val previewViewport: PreviewViewport = PreviewViewport.FULL,
    val consoleLogs: List<ConsoleLogEntry> = emptyList(),
    val terminalEntries: List<TerminalEntry> = emptyList(),
    val isRunningCommand: Boolean = false,
    val networkShield: NetworkShieldStatus = NetworkShieldStatus(),
    val mariaLoading: Boolean = false,
    val mariaTranscript: String = "",
    val mariaResponse: String = "Tap the orb or speak to ask Maria about your project.",
    val mariaError: String? = null,
    val bannerMessage: String? = null
) {
    val activeSession: ChatSession?
        get() = sessions.firstOrNull { it.id == activeSessionId }

    val activeEditorTab: OpenEditorTab?
        get() = openTabs.firstOrNull { it.relativePath == activeEditorPath }

    val starterPrompts: List<StarterPromptItem>
        get() = listOf(
            StarterPromptItem(
                label = "Cyberpunk Game",
                category = "Canvas",
                prompt = "Create a retro Cyberpunk Canvas game with sound effects and neon graphics"
            ),
            StarterPromptItem(
                label = "Portfolio Hub",
                category = "Web UI",
                prompt = "Build a sleek responsive developer portfolio landing page with animated project cards and dark glassmorphism"
            ),
            StarterPromptItem(
                label = "Live Docs @web",
                category = "Search",
                prompt = "@web Explain modern Jetpack Compose state management and Material 3 adaptive navigation best practices"
            ),
            StarterPromptItem(
                label = "Expense Calc",
                category = "Utility",
                prompt = "Build an interactive mobile expense splitter web app in HTML, CSS, and JS with local storage persistence"
            )
        )
}

class IdeViewModel(application: Application) : AndroidViewModel(application) {

    private val workspaceRepo = WorkspaceRepository(application)
    private val webSearchService = WebSearchService()
    private val networkShieldService = NetworkShieldService(application)
    private val terminalExecutor = TerminalExecutor()
    private val geminiEngine = GeminiAgentEngine(workspaceRepo, terminalExecutor)

    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState: StateFlow<IdeUiState> = _uiState.asStateFlow()

    private var activeStreamJob: Job? = null

    init {
        loadInitialWorkspace()
    }

    private fun loadInitialWorkspace() {
        val settings = workspaceRepo.loadSettings()
        val projects = workspaceRepo.listProjects()
        val validProject = projects.firstOrNull { it.name == settings.activeProject }?.name
            ?: projects.firstOrNull()?.name
            ?: "StarterWebApp"

        val fixedSettings = if (validProject != settings.activeProject) {
            settings.copy(activeProject = validProject).also { workspaceRepo.saveSettings(it) }
        } else settings

        val savedSessions = workspaceRepo.loadSessions()
        val initialSessions = if (savedSessions.isEmpty()) {
            listOf(
                ChatSession(
                    id = UUID.randomUUID().toString(),
                    title = "Workspace Session #1",
                    projectName = validProject
                )
            )
        } else savedSessions

        val nodes = workspaceRepo.listTree(validProject)

        _uiState.update {
            it.copy(
                settings = fixedSettings,
                projects = projects,
                workspaceNodes = nodes,
                sessions = initialSessions,
                activeSessionId = initialSessions.first().id,
                terminalEntries = listOf(
                    TerminalEntry(
                        id = UUID.randomUUID().toString(),
                        command = "status",
                        output = "Bypass IDE Workspace initialized: $validProject\nProject root: ${workspaceRepo.getProjectDir(validProject).absolutePath}",
                        exitCode = 0,
                        workingDir = validProject
                    )
                )
            )
        }

        // Auto-open index.html in Editor if present so Editor tab is immediately useful
        if (nodes.any { it.relativePath == "index.html" }) {
            openFileInEditor("index.html", switchToEditor = false)
        }
        refreshNetworkShield(evictSockets = false)
    }

    fun selectTab(tab: IdeTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == IdeTab.PREVIEW) {
            _uiState.update { it.copy(previewRefreshToken = it.previewRefreshToken + 1) }
        }
    }

    fun setAgentMode(mode: AgentMode) {
        _uiState.update { it.copy(agentMode = mode) }
    }

    fun updatePromptInput(text: String) {
        _uiState.update { it.copy(promptInput = text) }
    }

    fun updateSettings(transform: (IdeSettings) -> IdeSettings) {
        val updated = transform(_uiState.value.settings)
        workspaceRepo.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun setReasoningTier(tier: ReasoningTier) {
        updateSettings { it.copy(reasoningTier = tier) }
    }

    fun toggleWebSearch() {
        updateSettings { it.copy(webSearchEnabled = !it.webSearchEnabled) }
    }

    fun clearBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }

    // --- Chat Session Management ---

    fun createNewSession() {
        val project = _uiState.value.settings.activeProject
        val nextNum = _uiState.value.sessions.size + 1
        val newSession = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "Chat #$nextNum",
            projectName = project
        )
        val updated = listOf(newSession) + _uiState.value.sessions
        workspaceRepo.saveSessions(updated)
        _uiState.update {
            it.copy(
                sessions = updated,
                activeSessionId = newSession.id,
                bannerMessage = "New chat session started"
            )
        }
    }

    fun selectSession(sessionId: String) {
        _uiState.update { it.copy(activeSessionId = sessionId) }
    }

    fun deleteSession(sessionId: String) {
        val current = _uiState.value.sessions
        val remaining = current.filterNot { it.id == sessionId }
        val finalSessions = if (remaining.isEmpty()) {
            listOf(
                ChatSession(
                    id = UUID.randomUUID().toString(),
                    title = "Chat #1",
                    projectName = _uiState.value.settings.activeProject
                )
            )
        } else remaining
        workspaceRepo.saveSessions(finalSessions)
        val nextActive = if (_uiState.value.activeSessionId == sessionId) {
            finalSessions.first().id
        } else {
            _uiState.value.activeSessionId
        }
        _uiState.update { it.copy(sessions = finalSessions, activeSessionId = nextActive) }
    }

    fun clearActiveSessionMessages() {
        val activeId = _uiState.value.activeSessionId
        val updated = _uiState.value.sessions.map {
            if (it.id == activeId) it.copy(messages = emptyList()) else it
        }
        workspaceRepo.saveSessions(updated)
        _uiState.update { it.copy(sessions = updated, bannerMessage = "Chat cleared") }
    }

    fun stopGeneration() {
        activeStreamJob?.cancel()
        activeStreamJob = null
        _uiState.update {
            it.copy(
                isGenerating = false,
                isSearchingWeb = false,
                streamingText = ""
            )
        }
    }

    fun sendPrompt(overridePrompt: String? = null) {
        val raw = (overridePrompt ?: _uiState.value.promptInput).trim()
        if (raw.isBlank() || _uiState.value.isGenerating) return

        val stateSnapshot = _uiState.value
        val activeProject = stateSnapshot.settings.activeProject
        val mode = stateSnapshot.agentMode
        val shouldSearchWeb = stateSnapshot.settings.webSearchEnabled ||
            raw.contains("@web", ignoreCase = true)

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = raw,
            mode = mode
        )
        appendMessageToActiveSession(userMsg, renameTitleFrom = raw)

        if (overridePrompt == null) {
            _uiState.update { it.copy(promptInput = "") }
        }

        activeStreamJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    isSearchingWeb = shouldSearchWeb,
                    streamingText = "",
                    streamingSources = emptyList()
                )
            }

            try {
                val sources = if (shouldSearchWeb) {
                    webSearchService.searchLiveWeb(raw)
                } else emptyList()

                _uiState.update {
                    it.copy(
                        isSearchingWeb = false,
                        streamingSources = sources
                    )
                }

                val history = _uiState.value.activeSession?.messages
                    ?.dropLast(1)
                    ?.filterNot { it.isError }
                    ?: emptyList()

                geminiEngine.streamAssistantResponse(
                    userPrompt = raw,
                    history = history,
                    mode = mode,
                    tier = stateSnapshot.settings.reasoningTier,
                    projectName = activeProject,
                    webSources = sources
                ).collect { event ->
                    when (event) {
                        is StreamChunkEvent.Token -> {
                            _uiState.update { it.copy(streamingText = event.accumulatedText) }
                        }
                        is StreamChunkEvent.Completed -> {
                            val assistantMsg = ChatMessage(
                                id = UUID.randomUUID().toString(),
                                isUser = false,
                                text = event.finalText,
                                mode = mode,
                                sources = sources,
                                actionReport = event.actionReport
                            )
                            appendMessageToActiveSession(assistantMsg)
                            refreshProjectFiles(activeProject)

                            // Refresh any open editor tabs modified by Agent mode
                            event.actionReport?.let { report ->
                                val touched = (report.createdFiles + report.editedFiles).distinct()
                                reloadOpenTabsIfModified(activeProject, touched)
                                if (report.commandsExecuted.isNotEmpty()) {
                                    val newLogs = report.commandsExecuted.zip(report.commandOutputs)
                                        .map { (cmd, out) ->
                                            TerminalEntry(
                                                id = UUID.randomUUID().toString(),
                                                command = cmd,
                                                output = out,
                                                exitCode = 0,
                                                workingDir = activeProject
                                            )
                                        }
                                    _uiState.update { st ->
                                        st.copy(terminalEntries = st.terminalEntries + newLogs)
                                    }
                                }
                                if (report.hasPreviewableChanges) {
                                    _uiState.update { st ->
                                        st.copy(previewRefreshToken = st.previewRefreshToken + 1)
                                    }
                                }
                            }

                            _uiState.update {
                                it.copy(
                                    isGenerating = false,
                                    streamingText = "",
                                    streamingSources = emptyList()
                                )
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Assistant stream failed", e)
                val errMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = e.localizedMessage ?: "Failed to receive response from Gemini engine.",
                    mode = mode,
                    isError = true
                )
                appendMessageToActiveSession(errMsg)
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        isSearchingWeb = false,
                        streamingText = "",
                        streamingSources = emptyList()
                    )
                }
            }
        }
    }

    private fun appendMessageToActiveSession(
        message: ChatMessage,
        renameTitleFrom: String? = null
    ) {
        val activeId = _uiState.value.activeSessionId
        val updated = _uiState.value.sessions.map { session ->
            if (session.id != activeId) session
            else {
                val nextTitle = if (
                    renameTitleFrom != null &&
                    session.messages.isEmpty() &&
                    (session.title.startsWith("Chat") || session.title.startsWith("Workspace"))
                ) {
                    renameTitleFrom.replace("@web", "", ignoreCase = true).trim().take(28)
                        .ifBlank { session.title }
                } else session.title
                session.copy(
                    title = nextTitle,
                    messages = session.messages + message
                )
            }
        }
        workspaceRepo.saveSessions(updated)
        _uiState.update { it.copy(sessions = updated) }
    }

    // --- Project & File Explorer Operations ---

    fun switchProject(projectName: String) {
        updateSettings { it.copy(activeProject = projectName) }
        val nodes = workspaceRepo.listTree(projectName)
        _uiState.update {
            it.copy(
                workspaceNodes = nodes,
                openTabs = emptyList(),
                activeEditorPath = null,
                consoleLogs = emptyList(),
                previewRefreshToken = it.previewRefreshToken + 1,
                bannerMessage = "Switched active project to $projectName"
            )
        }
        if (nodes.any { it.relativePath == "index.html" }) {
            openFileInEditor("index.html", switchToEditor = false)
        }
    }

    fun createProject(name: String, template: String = "Web") {
        val info = workspaceRepo.createProject(name, template)
        refreshProjectFiles(info.name)
        switchProject(info.name)
    }

    fun refreshProjectFiles(projectName: String = _uiState.value.settings.activeProject) {
        _uiState.update {
            it.copy(
                projects = workspaceRepo.listProjects(),
                workspaceNodes = workspaceRepo.listTree(projectName)
            )
        }
    }

    fun createNewFile(relativePath: String) {
        val project = _uiState.value.settings.activeProject
        workspaceRepo.writeFile(project, relativePath, "").onSuccess {
            refreshProjectFiles(project)
            openFileInEditor(relativePath, switchToEditor = true)
        }.onFailure { err ->
            _uiState.update { it.copy(bannerMessage = "Error creating file: ${err.message}") }
        }
    }

    fun createNewFolder(relativePath: String) {
        val project = _uiState.value.settings.activeProject
        workspaceRepo.createDirectory(project, relativePath).onSuccess {
            refreshProjectFiles(project)
        }.onFailure { err ->
            _uiState.update { it.copy(bannerMessage = "Error creating directory: ${err.message}") }
        }
    }

    fun renameWorkspaceNode(oldRelativePath: String, newName: String) {
        val project = _uiState.value.settings.activeProject
        workspaceRepo.renameNode(project, oldRelativePath, newName).onSuccess {
            refreshProjectFiles(project)
            _uiState.update { it.copy(bannerMessage = "Renamed to $newName") }
        }.onFailure { err ->
            _uiState.update { it.copy(bannerMessage = "Error renaming: ${err.message}") }
        }
    }

    fun deleteWorkspaceNode(relativePath: String) {
        val project = _uiState.value.settings.activeProject
        workspaceRepo.deleteNode(project, relativePath).onSuccess {
            val remainingTabs = _uiState.value.openTabs.filterNot {
                it.relativePath == relativePath || it.relativePath.startsWith("$relativePath/")
            }
            val nextActive = if (_uiState.value.activeEditorPath == relativePath) {
                remainingTabs.firstOrNull()?.relativePath
            } else _uiState.value.activeEditorPath

            _uiState.update {
                it.copy(
                    openTabs = remainingTabs,
                    activeEditorPath = nextActive,
                    previewRefreshToken = it.previewRefreshToken + 1
                )
            }
            refreshProjectFiles(project)
        }.onFailure { err ->
            _uiState.update { it.copy(bannerMessage = "Error deleting: ${err.message}") }
        }
    }

    // --- Code Editor Operations ---

    fun openFileInEditor(relativePath: String, switchToEditor: Boolean = true) {
        val existing = _uiState.value.openTabs.firstOrNull { it.relativePath == relativePath }
        if (existing != null) {
            _uiState.update {
                it.copy(
                    activeEditorPath = relativePath,
                    activeTab = if (switchToEditor) IdeTab.EDITOR else it.activeTab
                )
            }
            return
        }

        val project = _uiState.value.settings.activeProject
        workspaceRepo.readFile(project, relativePath).onSuccess { text ->
            val tab = OpenEditorTab(
                relativePath = relativePath,
                fileName = relativePath.substringAfterLast('/'),
                content = text,
                savedContent = text
            )
            _uiState.update {
                it.copy(
                    openTabs = it.openTabs + tab,
                    activeEditorPath = relativePath,
                    activeTab = if (switchToEditor) IdeTab.EDITOR else it.activeTab
                )
            }
        }.onFailure { err ->
            _uiState.update { it.copy(bannerMessage = err.message ?: "Unable to open file") }
        }
    }

    fun selectEditorTab(relativePath: String) {
        _uiState.update { it.copy(activeEditorPath = relativePath) }
    }

    fun closeEditorTab(relativePath: String) {
        val remaining = _uiState.value.openTabs.filterNot { it.relativePath == relativePath }
        val nextActive = if (_uiState.value.activeEditorPath == relativePath) {
            remaining.lastOrNull()?.relativePath
        } else _uiState.value.activeEditorPath
        _uiState.update {
            it.copy(openTabs = remaining, activeEditorPath = nextActive)
        }
    }

    fun updateActiveEditorContent(newContent: String) {
        val activePath = _uiState.value.activeEditorPath ?: return
        _uiState.update { state ->
            state.copy(
                openTabs = state.openTabs.map { tab ->
                    if (tab.relativePath != activePath || tab.content == newContent) tab
                    else {
                        val nextUndo = (tab.undoStack + tab.content).takeLast(25)
                        tab.copy(
                            content = newContent,
                            undoStack = nextUndo,
                            redoStack = emptyList()
                        )
                    }
                }
            )
        }
    }

    fun undoEditor() {
        val activePath = _uiState.value.activeEditorPath ?: return
        _uiState.update { state ->
            state.copy(
                openTabs = state.openTabs.map { tab ->
                    if (tab.relativePath != activePath || tab.undoStack.isEmpty()) tab
                    else {
                        val previous = tab.undoStack.last()
                        tab.copy(
                            content = previous,
                            undoStack = tab.undoStack.dropLast(1),
                            redoStack = (tab.redoStack + tab.content).takeLast(25)
                        )
                    }
                }
            )
        }
    }

    fun redoEditor() {
        val activePath = _uiState.value.activeEditorPath ?: return
        _uiState.update { state ->
            state.copy(
                openTabs = state.openTabs.map { tab ->
                    if (tab.relativePath != activePath || tab.redoStack.isEmpty()) tab
                    else {
                        val next = tab.redoStack.last()
                        tab.copy(
                            content = next,
                            undoStack = (tab.undoStack + tab.content).takeLast(25),
                            redoStack = tab.redoStack.dropLast(1)
                        )
                    }
                }
            )
        }
    }

    fun saveActiveEditorFile() {
        val tab = _uiState.value.activeEditorTab ?: return
        val project = _uiState.value.settings.activeProject
        workspaceRepo.writeFile(project, tab.relativePath, tab.content).onSuccess {
            _uiState.update { state ->
                state.copy(
                    openTabs = state.openTabs.map {
                        if (it.relativePath == tab.relativePath) it.copy(savedContent = tab.content)
                        else it
                    },
                    previewRefreshToken = state.previewRefreshToken + 1,
                    bannerMessage = "Saved ${tab.fileName}"
                )
            }
            refreshProjectFiles(project)
        }.onFailure { err ->
            _uiState.update { it.copy(bannerMessage = "Save failed: ${err.message}") }
        }
    }

    fun applyAiEditToActiveFile(instruction: String) {
        val tab = _uiState.value.activeEditorTab ?: return
        if (instruction.isBlank() || _uiState.value.isApplyingAiEdit) return

        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingAiEdit = true) }
            try {
                val updatedCode = geminiEngine.generateInlineCodeEdit(
                    fileName = tab.fileName,
                    currentContent = tab.content,
                    instruction = instruction
                )
                updateActiveEditorContent(updatedCode)
                saveActiveEditorFile()
                _uiState.update {
                    it.copy(
                        isApplyingAiEdit = false,
                        bannerMessage = "AI Edit applied to ${tab.fileName}"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI inline edit failed", e)
                _uiState.update {
                    it.copy(
                        isApplyingAiEdit = false,
                        bannerMessage = e.localizedMessage ?: "AI Edit failed"
                    )
                }
            }
        }
    }

    fun applyCodeBlockToEditor(code: String, suggestedFilename: String = "snippet.txt") {
        val activeTab = _uiState.value.activeEditorTab
        if (activeTab != null) {
            updateActiveEditorContent(code)
            saveActiveEditorFile()
            _uiState.update { it.copy(activeTab = IdeTab.EDITOR) }
        } else {
            val project = _uiState.value.settings.activeProject
            workspaceRepo.writeFile(project, suggestedFilename, code).onSuccess {
                refreshProjectFiles(project)
                openFileInEditor(suggestedFilename, switchToEditor = true)
            }
        }
    }

    private fun reloadOpenTabsIfModified(projectName: String, modifiedPaths: List<String>) {
        if (modifiedPaths.isEmpty()) return
        _uiState.update { state ->
            val updatedTabs = state.openTabs.map { tab ->
                if (tab.relativePath in modifiedPaths) {
                    val fresh = workspaceRepo.readFile(projectName, tab.relativePath).getOrNull()
                    if (fresh != null) {
                        tab.copy(content = fresh, savedContent = fresh)
                    } else tab
                } else tab
            }
            state.copy(openTabs = updatedTabs)
        }
    }

    // --- Live Preview Operations ---

    fun getActiveIndexFile() = workspaceRepo.getIndexHtmlFile(_uiState.value.settings.activeProject)

    fun setPreviewViewport(viewport: PreviewViewport) {
        _uiState.update { it.copy(previewViewport = viewport) }
    }

    fun triggerPreviewReload() {
        _uiState.update {
            it.copy(
                consoleLogs = emptyList(),
                previewRefreshToken = it.previewRefreshToken + 1
            )
        }
    }

    fun addConsoleLog(level: String, message: String, source: String, line: Int) {
        val entry = ConsoleLogEntry(
            id = UUID.randomUUID().toString(),
            level = level,
            message = message,
            source = source.substringAfterLast('/'),
            lineNumber = line
        )
        _uiState.update {
            it.copy(consoleLogs = (it.consoleLogs + entry).takeLast(100))
        }
    }

    fun clearConsoleLogs() {
        _uiState.update { it.copy(consoleLogs = emptyList()) }
    }

    fun requestAutoFixFromPreview() {
        val errors = _uiState.value.consoleLogs
            .filter { it.level.equals("ERROR", ignoreCase = true) }
            .joinToString("\n") { "${it.source}:${it.lineNumber} - ${it.message}" }
        val prompt = buildString {
            append("Preview Diagnostic & Auto-Fix Request: Please inspect the project structure and fix any HTML, CSS, or JavaScript issues.")
            if (errors.isNotBlank()) {
                append("\nObserved WebView console errors:\n")
                append(errors)
            }
        }
        _uiState.update {
            it.copy(
                activeTab = IdeTab.HOME,
                agentMode = AgentMode.AGENT
            )
        }
        sendPrompt(prompt)
    }

    // --- Terminal Operations ---

    fun runTerminalCommand(rawCommand: String) {
        val cmd = rawCommand.trim()
        if (cmd.isBlank() || _uiState.value.isRunningCommand) return

        if (cmd.equals("clear", ignoreCase = true)) {
            clearTerminal()
            return
        }

        val activeProject = _uiState.value.settings.activeProject
        val projectDir = workspaceRepo.getProjectDir(activeProject)

        viewModelScope.launch {
            _uiState.update { it.copy(isRunningCommand = true) }
            val result = terminalExecutor.execute(cmd, projectDir)
            val entry = TerminalEntry(
                id = UUID.randomUUID().toString(),
                command = cmd,
                output = result.output,
                exitCode = result.exitCode,
                workingDir = activeProject
            )
            _uiState.update {
                it.copy(
                    terminalEntries = (it.terminalEntries + entry).takeLast(80),
                    isRunningCommand = false
                )
            }
            refreshProjectFiles(activeProject)
        }
    }

    fun clearTerminal() {
        _uiState.update {
            it.copy(
                terminalEntries = listOf(
                    TerminalEntry(
                        id = UUID.randomUUID().toString(),
                        command = "clear",
                        output = "Bypass IDE Terminal Cleared",
                        exitCode = 0,
                        workingDir = it.settings.activeProject
                    )
                )
            )
        }
    }

    // --- Network & IP Shield ---

    fun refreshNetworkShield(evictSockets: Boolean = false) {
        if (_uiState.value.networkShield.isChecking) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    networkShield = it.networkShield.copy(
                        isChecking = true,
                        statusMessage = if (evictSockets) "Cycling socket pool & verifying IP..." else "Probing network pipeline..."
                    )
                )
            }
            val updated = networkShieldService.checkShieldStatus(evictSockets = evictSockets)
            _uiState.update { it.copy(networkShield = updated) }
        }
    }

    // --- Maria Voice Assistant ---

    fun submitMariaVoiceQuery(spokenText: String, onResponseReady: (String) -> Unit) {
        val clean = spokenText.trim()
        if (clean.isBlank()) return
        val activeProject = _uiState.value.settings.activeProject

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    mariaLoading = true,
                    mariaTranscript = clean,
                    mariaError = null
                )
            }
            try {
                val reply = geminiEngine.askMariaVoice(clean, activeProject)
                _uiState.update {
                    it.copy(
                        mariaLoading = false,
                        mariaResponse = reply
                    )
                }
                onResponseReady(reply)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Maria voice query failed", e)
                _uiState.update {
                    it.copy(
                        mariaLoading = false,
                        mariaError = e.localizedMessage ?: "Maria could not reach Gemini engine."
                    )
                }
            }
        }
    }
}
