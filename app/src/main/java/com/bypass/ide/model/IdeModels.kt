package com.bypass.ide.model

import kotlinx.serialization.Serializable

enum class IdeTab(val label: String) {
    HOME("Agent"),
    FILES("Files"),
    EDITOR("Editor"),
    PREVIEW("Preview"),
    TERMINAL("Terminal")
}

enum class AgentMode(val label: String, val subtitle: String) {
    CHAT("Chat", "Consultation, Planning, Explanations"),
    AGENT("Agent", "Autonomous Tasks, File Creation, Terminal Ops")
}

enum class ReasoningTier(
    val label: String,
    val modelId: String,
    val description: String
) {
    LIGHT(
        label = "Light",
        modelId = "gemini-3.5-flash-lite",
        description = "Fast responses & quick single-file edits (~2k thinking)"
    ),
    BALANCED(
        label = "Balanced",
        modelId = "gemini-3.8-flash",
        description = "Recommended default for full-stack mobile & web builds"
    ),
    DEEP(
        label = "Deep",
        modelId = "gemini-3.8-flash",
        description = "Deep architectural reasoning for multi-file synthesis"
    )
}

enum class PreviewViewport(val label: String, val maxWidthDp: Int?) {
    FULL("Full", null),
    MOBILE("Mobile 375px", 375),
    TABLET("Tablet 768px", 768)
}

@Serializable
data class ExaminedSource(
    val title: String,
    val url: String,
    val domain: String,
    val snippet: String = ""
)

@Serializable
data class AgentActionReport(
    val createdFiles: List<String> = emptyList(),
    val editedFiles: List<String> = emptyList(),
    val deletedFiles: List<String> = emptyList(),
    val directoriesCreated: List<String> = emptyList(),
    val commandsExecuted: List<String> = emptyList(),
    val commandOutputs: List<String> = emptyList(),
    val hasPreviewableChanges: Boolean = false
)

@Serializable
data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val mode: AgentMode = AgentMode.CHAT,
    val sources: List<ExaminedSource> = emptyList(),
    val actionReport: AgentActionReport? = null,
    val isError: Boolean = false
)

@Serializable
data class ChatSession(
    val id: String,
    val title: String,
    val projectName: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList()
)

data class ProjectInfo(
    val name: String,
    val path: String,
    val fileCount: Int,
    val hasIndexHtml: Boolean
)

data class WorkspaceNode(
    val name: String,
    val relativePath: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val depth: Int,
    val extension: String = name.substringAfterLast('.', "").lowercase()
)

data class OpenEditorTab(
    val relativePath: String,
    val fileName: String,
    val content: String,
    val savedContent: String,
    val undoStack: List<String> = emptyList(),
    val redoStack: List<String> = emptyList()
) {
    val isDirty: Boolean get() = content != savedContent
    val extension: String get() = fileName.substringAfterLast('.', "").lowercase()
    val lineCount: Int get() = if (content.isEmpty()) 1 else content.count { it == '\n' } + 1
}

data class TerminalEntry(
    val id: String,
    val command: String,
    val output: String,
    val exitCode: Int,
    val workingDir: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class ConsoleLogEntry(
    val id: String,
    val level: String,
    val message: String,
    val source: String,
    val lineNumber: Int
)

data class NetworkShieldStatus(
    val publicIp: String = "Not checked",
    val ipProvider: String = "",
    val isConnected: Boolean = false,
    val transportLabel: String = "Unknown",
    val latencyMs: Long? = null,
    val isChecking: Boolean = false,
    val statusMessage: String = "Tap Check Network to verify connection & public IP",
    val lastCheckedAt: Long? = null
)

@Serializable
data class IdeSettings(
    val activeProject: String = "StarterWebApp",
    val webSearchEnabled: Boolean = false,
    val reasoningTier: ReasoningTier = ReasoningTier.BALANCED,
    val editorFontSizeSp: Int = 13,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val mariaAutoSpeak: Boolean = true,
    val customGeminiApiKey: String = "",
    val customGeminiModel: String = "gemini-2.5-flash"
)

data class StarterPromptItem(
    val label: String,
    val category: String,
    val prompt: String
)
