package com.bypass.ide.ai

import android.util.Log
import com.bypass.ide.data.TerminalExecutor
import com.bypass.ide.data.WorkspaceRepository
import com.bypass.ide.model.AgentActionReport
import com.bypass.ide.model.AgentMode
import com.bypass.ide.model.ChatMessage
import com.bypass.ide.model.ExaminedSource
import com.bypass.ide.model.ReasoningTier
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private const val TAG = "GeminiAgentEngine"
private const val AI_TIMEOUT_MS = 60_000L
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

sealed interface StreamChunkEvent {
    data class Token(val accumulatedText: String) : StreamChunkEvent
    data class Completed(
        val finalText: String,
        val actionReport: AgentActionReport?
    ) : StreamChunkEvent
}

class GeminiAgentEngine(
    private val workspaceRepo: WorkspaceRepository,
    private val terminalExecutor: TerminalExecutor
) {

    private val restClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val defaultFlashModel by lazy {
        Firebase.ai.generativeModel(
            modelName = ReasoningTier.BALANCED.modelId,
            generationConfig = generationConfig {
                temperature = 0.45f
            }
        )
    }

    private val liteFlashModel by lazy {
        Firebase.ai.generativeModel(
            modelName = ReasoningTier.LIGHT.modelId,
            generationConfig = generationConfig {
                temperature = 0.35f
            }
        )
    }

    private fun pickModel(tier: ReasoningTier) = when (tier) {
        ReasoningTier.LIGHT -> liteFlashModel
        ReasoningTier.BALANCED, ReasoningTier.DEEP -> defaultFlashModel
    }

    fun streamAssistantResponse(
        userPrompt: String,
        history: List<ChatMessage>,
        mode: AgentMode,
        tier: ReasoningTier,
        projectName: String,
        webSources: List<ExaminedSource>,
        customGeminiApiKey: String = "",
        customGeminiModel: String = "gemini-2.5-flash"
    ): Flow<StreamChunkEvent> = flow {
        val projectTree = workspaceRepo.buildDirectorySummary(projectName)
        val systemPreamble = buildSystemPreamble(mode, tier, projectName, projectTree, webSources)
        val sanitizedTurns = sanitizeHistoryTurns(history.takeLast(10))

        val fullPrompt = buildString {
            append(systemPreamble)
            append("\n\nUser request:\n")
            append(userPrompt)
        }

        val accumulated = StringBuilder()
        val apiKey = customGeminiApiKey.trim()

        try {
            withTimeout(AI_TIMEOUT_MS) {
                if (apiKey.isNotBlank()) {
                    streamViaCustomRestKey(
                        apiKey = apiKey,
                        modelId = customGeminiModel.trim().ifBlank { "gemini-2.5-flash" },
                        turns = sanitizedTurns,
                        fullPrompt = fullPrompt,
                        onToken = { piece ->
                            accumulated.append(piece)
                            emit(StreamChunkEvent.Token(accumulated.toString()))
                        }
                    )
                } else {
                    val firebaseHistory = sanitizedTurns.map { (role, text) ->
                        content(role = role) { text(text) }
                    }
                    val primaryModel = pickModel(tier)
                    try {
                        val chat = primaryModel.startChat(history = firebaseHistory)
                        chat.sendMessageStream(fullPrompt).collect { chunk ->
                            val piece = chunk.text.orEmpty()
                            if (piece.isNotEmpty()) {
                                accumulated.append(piece)
                                emit(StreamChunkEvent.Token(accumulated.toString()))
                            }
                        }
                    } catch (primaryErr: Exception) {
                        if (primaryErr is CancellationException) throw primaryErr
                        if (accumulated.isNotEmpty() || tier == ReasoningTier.LIGHT) {
                            throw primaryErr
                        }
                        Log.w(TAG, "Primary Firebase model failed, falling back to lite model", primaryErr)
                        val fallbackChat = liteFlashModel.startChat(history = firebaseHistory)
                        fallbackChat.sendMessageStream(fullPrompt).collect { chunk ->
                            val piece = chunk.text.orEmpty()
                            if (piece.isNotEmpty()) {
                                accumulated.append(piece)
                                emit(StreamChunkEvent.Token(accumulated.toString()))
                            }
                        }
                    }
                }
            }

            val rawResponse = accumulated.toString().trim()
            if (rawResponse.isBlank()) {
                throw IllegalStateException("Gemini returned an empty response. Please try again.")
            }

            val actionReport = if (mode == AgentMode.AGENT) {
                executeToolActions(projectName, rawResponse)
            } else {
                null
            }

            val displayResponse = stripInternalToolBodiesForDisplay(rawResponse, actionReport)
            emit(StreamChunkEvent.Completed(displayResponse, actionReport))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini stream timed out after ${AI_TIMEOUT_MS}ms", e)
            throw IllegalStateException("Request timed out after 60 seconds. Check your connection and try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation failed", e)
            val msg = e.localizedMessage.orEmpty()
            val isAppCheckOrAuth = msg.contains("403", ignoreCase = true) ||
                msg.contains("app check", ignoreCase = true) ||
                msg.contains("attestation", ignoreCase = true) ||
                msg.contains("API key", ignoreCase = true)
            if (apiKey.isBlank() && isAppCheckOrAuth) {
                throw IllegalStateException(
                    "$msg\n\nTip: If running a standalone debug APK outside AI Studio, add your own Gemini API key in IDE Settings or use the signed release APK."
                )
            }
            throw e
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun streamViaCustomRestKey(
        apiKey: String,
        modelId: String,
        turns: List<Pair<String, String>>,
        fullPrompt: String,
        onToken: suspend (String) -> Unit
    ) {
        val contentsArray = JSONArray()
        turns.forEach { (role, text) ->
            contentsArray.put(
                JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", text)))
                }
            )
        }
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", fullPrompt)))
            }
        )

        val payload = JSONObject().apply {
            put("contents", contentsArray)
            put(
                "generationConfig",
                JSONObject().put("temperature", 0.45)
            )
        }

        val encodedModel = URLEncoder.encode(modelId, "UTF-8")
        val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$encodedModel:streamGenerateContent?alt=sse&key=$encodedKey"

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "text/event-stream")
            .build()

        restClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty()
                throw IllegalStateException(extractApiErrorMessage(errBody, response.code))
            }
            val source = response.body?.source()
                ?: throw IllegalStateException("Empty HTTP response body from Gemini API.")

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                val piece = extractSseText(line)
                if (!piece.isNullOrEmpty()) {
                    onToken(piece)
                }
            }
        }
    }

    suspend fun generateInlineCodeEdit(
        fileName: String,
        currentContent: String,
        instruction: String,
        customGeminiApiKey: String = "",
        customGeminiModel: String = "gemini-2.5-flash"
    ): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are an expert software engineer inside Bypass IDE's Code Editor.
            Modify the file '$fileName' according to the user's instruction.
            Return ONLY the complete updated file content without markdown code fences or commentary.

            Instruction: $instruction

            Current content of $fileName:
            $currentContent
        """.trimIndent()

        try {
            val apiKey = customGeminiApiKey.trim()
            val raw = withTimeout(AI_TIMEOUT_MS) {
                if (apiKey.isNotBlank()) {
                    generateUnaryCustomRest(
                        apiKey = apiKey,
                        modelId = customGeminiModel.trim().ifBlank { "gemini-2.5-flash" },
                        prompt = prompt
                    )
                } else {
                    defaultFlashModel.generateContent(prompt).text?.trim().orEmpty()
                }
            }
            if (raw.isBlank()) {
                throw IllegalStateException("Model returned empty code edit.")
            }
            stripSingleOuterCodeFence(raw)
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Inline code edit timed out", e)
            throw IllegalStateException("AI Edit timed out after 60 seconds.")
        }
    }

    suspend fun askMariaVoice(
        spokenQuery: String,
        projectName: String,
        customGeminiApiKey: String = "",
        customGeminiModel: String = "gemini-2.5-flash"
    ): String = withContext(Dispatchers.IO) {
        try {
            val tree = workspaceRepo.buildDirectorySummary(projectName)
            val prompt = """
                You are Maria, the concise, friendly voice coding assistant inside Bypass IDE.
                Answer in 2 to 4 clear spoken sentences with no markdown formatting, no asterisks, and no code fences.
                Active project: $projectName
                Project files:
                $tree

                User said: $spokenQuery
            """.trimIndent()

            val apiKey = customGeminiApiKey.trim()
            val reply = withTimeout(AI_TIMEOUT_MS) {
                if (apiKey.isNotBlank()) {
                    generateUnaryCustomRest(
                        apiKey = apiKey,
                        modelId = customGeminiModel.trim().ifBlank { "gemini-2.5-flash" },
                        prompt = prompt
                    )
                } else {
                    liteFlashModel.generateContent(prompt).text?.trim().orEmpty()
                }
            }
            reply.takeIf { it.isNotBlank() }
                ?: "I did not catch an answer from the engine. Please try asking again."
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Maria voice query timed out", e)
            "The response took too long. Please check your network and try again."
        }
    }

    private fun generateUnaryCustomRest(
        apiKey: String,
        modelId: String,
        prompt: String
    ): String {
        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                    }
                )
            )
            put("generationConfig", JSONObject().put("temperature", 0.35))
        }
        val encodedModel = URLEncoder.encode(modelId, "UTF-8")
        val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$encodedModel:generateContent?key=$encodedKey"
        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        restClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(extractApiErrorMessage(body, response.code))
            }
            return extractSseText("data: $body")?.trim().orEmpty()
        }
    }

    companion object {
        internal fun sanitizeHistoryTurns(history: List<ChatMessage>): List<Pair<String, String>> {
            val valid = history.filter { !it.isError && it.text.isNotBlank() }
            if (valid.isEmpty()) return emptyList()

            val normalized = mutableListOf<Pair<String, String>>()
            for (msg in valid) {
                val role = if (msg.isUser) "user" else "model"
                val contentText = msg.text.take(3000).trim()
                if (normalized.isEmpty()) {
                    // Gemini chat history must start with a user turn
                    if (role == "user") {
                        normalized.add(role to contentText)
                    }
                } else {
                    val last = normalized.last()
                    if (last.first == role) {
                        normalized[normalized.lastIndex] = role to "${last.second}\n\n$contentText"
                    } else {
                        normalized.add(role to contentText)
                    }
                }
            }

            // History passed to startChat must end with a model turn so the new user prompt follows cleanly
            if (normalized.lastOrNull()?.first == "user") {
                normalized.removeAt(normalized.lastIndex)
            }
            return normalized
        }

        internal fun extractSseText(line: String): String? {
            val trimmed = line.trim()
            if (!trimmed.startsWith("data:")) return null
            val jsonPart = trimmed.removePrefix("data:").trim()
            if (jsonPart.isEmpty() || jsonPart == "[DONE]") return null

            return runCatching {
                val root = JSONObject(jsonPart)
                val candidates = root.optJSONArray("candidates") ?: return null
                if (candidates.length() == 0) return null
                val parts = candidates.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?: return null
                val sb = StringBuilder()
                for (i in 0 until parts.length()) {
                    val partText = parts.optJSONObject(i)?.optString("text", "").orEmpty()
                    sb.append(partText)
                }
                sb.toString().takeIf { it.isNotEmpty() }
            }.getOrNull()
        }

        internal fun extractApiErrorMessage(rawBody: String, httpCode: Int): String {
            val parsed = runCatching {
                JSONObject(rawBody).optJSONObject("error")?.optString("message").orEmpty()
            }.getOrDefault("")
            return if (parsed.isNotBlank()) {
                "Gemini API error ($httpCode): $parsed"
            } else {
                "Gemini API request failed with HTTP $httpCode."
            }
        }
    }

    private suspend fun executeToolActions(
        projectName: String,
        responseText: String
    ): AgentActionReport? {
        val created = mutableListOf<String>()
        val edited = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        val dirs = mutableListOf<String>()
        val commands = mutableListOf<String>()
        val outputs = mutableListOf<String>()

        // 1. <mkdir path="..." />
        val mkdirRegex = Regex("""<(?:mkdir|create_directory)\s+path=["']([^"']+)["']\s*/?>""")
        mkdirRegex.findAll(responseText).forEach { m ->
            val path = m.groupValues[1].trim()
            workspaceRepo.createDirectory(projectName, path).onSuccess {
                dirs.add(path)
            }
        }

        // 2. <file path="...">content</file>
        val fileRegex = Regex(
            """<file\s+(?:path|filename)=["']([^"']+)["']\s*>([\s\S]*?)</file>"""
        )
        fileRegex.findAll(responseText).forEach { m ->
            val path = m.groupValues[1].trim()
            val body = m.groupValues[2].trim('\n', '\r')
            val existedBefore = workspaceRepo.readFile(projectName, path).isSuccess
            workspaceRepo.writeFile(projectName, path, body).onSuccess {
                if (existedBefore) edited.add(path) else created.add(path)
            }
        }

        // 3. <append_file path="...">content</append_file>
        val appendRegex = Regex(
            """<(?:append_file|append)\s+(?:path|filename)=["']([^"']+)["']\s*>([\s\S]*?)</(?:append_file|append)>"""
        )
        appendRegex.findAll(responseText).forEach { m ->
            val path = m.groupValues[1].trim()
            val body = m.groupValues[2]
            workspaceRepo.appendFile(projectName, path, body).onSuccess {
                edited.add(path)
            }
        }

        // 4. <edit_file path="..."><search>...</search><replace>...</replace></edit_file>
        val editRegex = Regex(
            """<edit_file\s+(?:path|filename)=["']([^"']+)["']\s*>\s*<search>([\s\S]*?)</search>\s*<replace>([\s\S]*?)</replace>\s*</edit_file>"""
        )
        editRegex.findAll(responseText).forEach { m ->
            val path = m.groupValues[1].trim()
            val search = m.groupValues[2].trim('\n', '\r')
            val replace = m.groupValues[3].trim('\n', '\r')
            workspaceRepo.editFileSearchReplace(projectName, path, search, replace).onSuccess {
                edited.add(path)
            }
        }

        // 5. <delete_file path="..." />
        val deleteRegex = Regex("""<(?:delete_file|remove_file)\s+path=["']([^"']+)["']\s*/?>""")
        deleteRegex.findAll(responseText).forEach { m ->
            val path = m.groupValues[1].trim()
            workspaceRepo.deleteNode(projectName, path).onSuccess {
                deleted.add(path)
            }
        }

        // 6. <command>...</command>
        val cmdRegex = Regex("""<(?:command|bash|shell|terminal)\s*>([\s\S]*?)</(?:command|bash|shell|terminal)>""")
        val projectDir = workspaceRepo.getProjectDir(projectName)
        cmdRegex.findAll(responseText).forEach { m ->
            val cmd = m.groupValues[1].trim()
            if (cmd.isNotBlank()) {
                commands.add(cmd)
                val res = terminalExecutor.execute(cmd, projectDir)
                outputs.add(res.output)
            }
        }

        if (created.isEmpty() && edited.isEmpty() && deleted.isEmpty() && dirs.isEmpty() && commands.isEmpty()) {
            return null
        }

        val allTouched = (created + edited).distinct()
        val previewable = allTouched.any {
            it.endsWith(".html", ignoreCase = true) ||
                it.endsWith(".css", ignoreCase = true) ||
                it.endsWith(".js", ignoreCase = true)
        } || workspaceRepo.getIndexHtmlFile(projectName) != null

        return AgentActionReport(
            createdFiles = created.distinct(),
            editedFiles = edited.distinct(),
            deletedFiles = deleted.distinct(),
            directoriesCreated = dirs.distinct(),
            commandsExecuted = commands,
            commandOutputs = outputs,
            hasPreviewableChanges = previewable
        )
    }

    private fun stripInternalToolBodiesForDisplay(
        raw: String,
        report: AgentActionReport?
    ): String {
        if (report == null) return raw
        val cleaned = raw
            .replace(Regex("""<file\s+(?:path|filename)=["']([^"']+)["']\s*>[\s\S]*?</file>""")) { m ->
                "\n> **File written:** `${m.groupValues[1]}`\n"
            }
            .replace(Regex("""<edit_file\s+(?:path|filename)=["']([^"']+)["']\s*>[\s\S]*?</edit_file>""")) { m ->
                "\n> **File edited:** `${m.groupValues[1]}`\n"
            }
            .replace(Regex("""<(?:append_file|append)\s+(?:path|filename)=["']([^"']+)["']\s*>[\s\S]*?</(?:append_file|append)>""")) { m ->
                "\n> **File appended:** `${m.groupValues[1]}`\n"
            }
            .replace(Regex("""<(?:mkdir|create_directory)\s+path=["']([^"']+)["']\s*/?>""")) { m ->
                "\n> **Directory created:** `${m.groupValues[1]}`\n"
            }
            .replace(Regex("""<(?:delete_file|remove_file)\s+path=["']([^"']+)["']\s*/?>""")) { m ->
                "\n> **File deleted:** `${m.groupValues[1]}`\n"
            }
            .replace(Regex("""<(?:command|bash|shell|terminal)\s*>([\s\S]*?)</(?:command|bash|shell|terminal)>""")) { m ->
                "\n```bash\n${m.groupValues[1].trim()}\n```\n"
            }
            .trim()
        return cleaned.ifBlank { "Workspace updates applied successfully." }
    }

    private fun stripSingleOuterCodeFence(raw: String): String {
        val lines = raw.lines()
        return if (lines.size >= 2 && lines.first().startsWith("```") && lines.last().startsWith("```")) {
            lines.drop(1).dropLast(1).joinToString("\n")
        } else {
            raw
        }
    }

    private fun buildSystemPreamble(
        mode: AgentMode,
        tier: ReasoningTier,
        projectName: String,
        projectTree: String,
        webSources: List<ExaminedSource>
    ): String = buildString {
        appendLine("You are Bypass AI, an expert mobile software architect inside Bypass IDE.")
        appendLine("Active project: $projectName")
        appendLine("Reasoning tier: ${tier.label}")
        appendLine("Current workspace file tree:")
        appendLine(projectTree)
        if (webSources.isNotEmpty()) {
            appendLine("\nLive @web search results:")
            webSources.forEachIndexed { index, src ->
                appendLine("[${index + 1}] ${src.title} (${src.url}): ${src.snippet}")
            }
        }
        if (mode == AgentMode.AGENT) {
            appendLine(
                """
                
                AGENT MODE TOOL PROTOCOL:
                You can autonomously create/edit project files and run terminal commands in the active workspace using XML tags:
                - Create or overwrite file: <file path="relative/path/filename.ext">...full file content...</file>
                - Partial edit: <edit_file path="relative/path"><search>exact lines</search><replace>new lines</replace></edit_file>
                - Create folder: <mkdir path="relative/dir"/>
                - Delete file: <delete_file path="relative/path"/>
                - Run shell command: <command>ls -la</command>
                When asked to build a website, app, or game, always ensure index.html is the primary entry point linked cleanly to CSS and JS files (or self-contained) so Live Preview renders immediately with zero broken imports.
                Provide a brief explanation alongside the XML file blocks.
                """.trimIndent()
            )
        } else {
            appendLine(
                """
                
                CHAT MODE:
                Answer clearly using structured Markdown (headings, bullet lists, tables, and fenced code blocks). Do not emit XML <file> tool tags in Chat mode.
                """.trimIndent()
            )
        }
    }
}
