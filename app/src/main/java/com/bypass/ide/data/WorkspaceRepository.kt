package com.bypass.ide.data

import android.content.Context
import android.util.Log
import com.bypass.ide.model.ChatSession
import com.bypass.ide.model.IdeSettings
import com.bypass.ide.model.ProjectInfo
import com.bypass.ide.model.WorkspaceNode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val TAG = "WorkspaceRepository"
private const val PREFS_NAME = "bypass_ide_settings"
private const val KEY_SETTINGS = "ide_settings_json"
private const val KEY_SESSIONS = "chat_sessions_json"

class WorkspaceRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val projectsRootDir: File = File(context.filesDir, "BypassProjects").apply {
        if (!exists()) mkdirs()
    }

    init {
        seedStarterProjectIfEmpty()
    }

    fun loadSettings(): IdeSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return IdeSettings()
        return runCatching { json.decodeFromString<IdeSettings>(raw) }
            .getOrElse {
                Log.w(TAG, "Failed to decode settings, using defaults", it)
                IdeSettings()
            }
    }

    fun saveSettings(settings: IdeSettings) {
        prefs.edit().putString(KEY_SETTINGS, json.encodeToString(settings)).apply()
    }

    fun loadSessions(): List<ChatSession> {
        val raw = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<ChatSession>>(raw) }
            .getOrElse {
                Log.w(TAG, "Failed to decode chat sessions", it)
                emptyList()
            }
    }

    fun saveSessions(sessions: List<ChatSession>) {
        val trimmed = sessions.take(25)
        prefs.edit().putString(KEY_SESSIONS, json.encodeToString(trimmed)).apply()
    }

    fun getProjectDir(projectName: String): File {
        val sanitized = sanitizeName(projectName).ifBlank { "StarterWebApp" }
        return File(projectsRootDir, sanitized).apply {
            if (!exists()) mkdirs()
        }
    }

    fun listProjects(): List<ProjectInfo> {
        val dirs = projectsRootDir.listFiles { file -> file.isDirectory }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
        return dirs.map { dir ->
            val files = dir.walkTopDown().filter { it.isFile }.toList()
            ProjectInfo(
                name = dir.name,
                path = dir.absolutePath,
                fileCount = files.size,
                hasIndexHtml = File(dir, "index.html").exists()
            )
        }
    }

    fun createProject(rawName: String, templateType: String = "Web"): ProjectInfo {
        val safeName = sanitizeName(rawName).ifBlank { "Project_${System.currentTimeMillis() % 10000}" }
        val dir = File(projectsRootDir, safeName).apply { mkdirs() }
        if (dir.listFiles().isNullOrEmpty()) {
            when (templateType) {
                "Web" -> {
                    File(dir, "index.html").writeText(
                        """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                          <meta charset="UTF-8" />
                          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                          <title>$safeName</title>
                          <link rel="stylesheet" href="styles.css" />
                        </head>
                        <body>
                          <main class="card">
                            <span class="badge">Bypass IDE</span>
                            <h1>$safeName</h1>
                            <p>Ready for autonomous edits and live preview.</p>
                            <button id="pulseBtn">Run Test Pulse</button>
                            <p id="status"></p>
                          </main>
                          <script src="app.js"></script>
                        </body>
                        </html>
                        """.trimIndent()
                    )
                    File(dir, "styles.css").writeText(
                        """
                        body {
                          margin: 0;
                          min-height: 100vh;
                          display: grid;
                          place-items: center;
                          background: #080c14;
                          color: #f1f5f9;
                          font-family: system-ui, -apple-system, sans-serif;
                        }
                        .card {
                          padding: 28px;
                          border-radius: 18px;
                          background: #0f172a;
                          border: 1px solid #1e293b;
                          text-align: center;
                          max-width: 340px;
                        }
                        .badge {
                          display: inline-block;
                          padding: 4px 10px;
                          border-radius: 999px;
                          font-size: 12px;
                          background: rgba(34, 211, 238, 0.15);
                          color: #22d3ee;
                        }
                        button {
                          margin-top: 12px;
                          padding: 10px 18px;
                          border-radius: 10px;
                          border: none;
                          background: #22d3ee;
                          color: #080c14;
                          font-weight: 700;
                          cursor: pointer;
                        }
                        """.trimIndent()
                    )
                    File(dir, "app.js").writeText(
                        """
                        const btn = document.getElementById('pulseBtn');
                        const status = document.getElementById('status');
                        let count = 0;
                        btn?.addEventListener('click', () => {
                          count += 1;
                          status.textContent = `Pulse check #${'$'}{count} OK`;
                          console.log('Pulse check', count);
                        });
                        """.trimIndent()
                    )
                }
                else -> {
                    File(dir, "README.md").writeText("# $safeName\n\nCreated in Bypass IDE.\n")
                }
            }
        }
        val files = dir.walkTopDown().filter { it.isFile }.toList()
        return ProjectInfo(
            name = dir.name,
            path = dir.absolutePath,
            fileCount = files.size,
            hasIndexHtml = File(dir, "index.html").exists()
        )
    }

    fun listTree(projectName: String): List<WorkspaceNode> {
        val root = getProjectDir(projectName)
        val result = mutableListOf<WorkspaceNode>()
        fun walk(dir: File, depth: Int) {
            val children = dir.listFiles()?.sortedWith(
                compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
            ) ?: return
            for (child in children) {
                val rel = child.relativeTo(root).path.replace(File.separatorChar, '/')
                result.add(
                    WorkspaceNode(
                        name = child.name,
                        relativePath = rel,
                        absolutePath = child.absolutePath,
                        isDirectory = child.isDirectory,
                        sizeBytes = if (child.isFile) child.length() else 0L,
                        depth = depth
                    )
                )
                if (child.isDirectory) {
                    walk(child, depth + 1)
                }
            }
        }
        walk(root, 0)
        return result
    }

    fun buildDirectorySummary(projectName: String): String {
        val nodes = listTree(projectName)
        if (nodes.isEmpty()) return "(empty project)"
        return nodes.take(80).joinToString("\n") { node ->
            val indent = "  ".repeat(node.depth)
            val suffix = if (node.isDirectory) "/" else " (${node.sizeBytes}B)"
            "$indent${node.relativePath}$suffix"
        }
    }

    fun readFile(projectName: String, relativePath: String): Result<String> = runCatching {
        val target = resolveSafe(projectName, relativePath)
        require(target.exists() && target.isFile) { "File not found: $relativePath" }
        target.readText()
    }

    fun writeFile(projectName: String, relativePath: String, content: String): Result<File> = runCatching {
        val target = resolveSafe(projectName, relativePath)
        target.parentFile?.mkdirs()
        target.writeText(content)
        target
    }

    fun appendFile(projectName: String, relativePath: String, content: String): Result<File> = runCatching {
        val target = resolveSafe(projectName, relativePath)
        target.parentFile?.mkdirs()
        target.appendText(content)
        target
    }

    fun editFileSearchReplace(
        projectName: String,
        relativePath: String,
        search: String,
        replace: String
    ): Result<File> = runCatching {
        val target = resolveSafe(projectName, relativePath)
        require(target.exists() && target.isFile) { "Target file does not exist: $relativePath" }
        val original = target.readText()
        require(original.contains(search)) { "Search snippet not found in $relativePath" }
        val updated = original.replaceFirst(search, replace)
        target.writeText(updated)
        target
    }

    fun createDirectory(projectName: String, relativePath: String): Result<File> = runCatching {
        val target = resolveSafe(projectName, relativePath)
        target.mkdirs()
        target
    }

    fun renameNode(projectName: String, oldRelativePath: String, newName: String): Result<File> = runCatching {
        val oldFile = resolveSafe(projectName, oldRelativePath)
        require(oldFile.exists()) { "Path not found: $oldRelativePath" }
        val cleanNewName = newName.trim().replace("/", "").replace("\\", "")
        require(cleanNewName.isNotBlank()) { "New name cannot be blank" }
        val newFile = File(oldFile.parentFile, cleanNewName)
        val rootCanonical = getProjectDir(projectName).canonicalPath
        require(newFile.canonicalPath.startsWith(rootCanonical)) { "Invalid destination path" }
        check(oldFile.renameTo(newFile)) { "Failed to rename ${oldFile.name}" }
        newFile
    }

    fun deleteNode(projectName: String, relativePath: String): Result<Unit> = runCatching {
        val target = resolveSafe(projectName, relativePath)
        if (target.exists()) {
            if (target.isDirectory) {
                target.deleteRecursively()
            } else {
                target.delete()
            }
        }
    }

    fun getIndexHtmlFile(projectName: String): File? {
        val root = getProjectDir(projectName)
        val primary = File(root, "index.html")
        if (primary.exists() && primary.isFile) return primary
        return root.walkTopDown()
            .firstOrNull { it.isFile && it.extension.equals("html", ignoreCase = true) }
    }

    private fun resolveSafe(projectName: String, relativePath: String): File {
        val root = getProjectDir(projectName)
        val cleaned = relativePath.trim().trimStart('/').replace('\\', '/')
        require(cleaned.isNotBlank()) { "Path cannot be empty" }
        val target = File(root, cleaned)
        val rootCanonical = root.canonicalPath
        val targetCanonical = target.canonicalPath
        require(
            targetCanonical == rootCanonical ||
                targetCanonical.startsWith("$rootCanonical${File.separator}")
        ) {
            "Unsafe path outside project workspace: $relativePath"
        }
        return target
    }

    private fun sanitizeName(raw: String): String {
        return raw.trim().replace(Regex("[^A-Za-z0-9_.-]"), "_").take(48)
    }

    private fun seedStarterProjectIfEmpty() {
        val existing = projectsRootDir.listFiles { it.isDirectory }
        if (!existing.isNullOrEmpty()) return
        val dir = File(projectsRootDir, "StarterWebApp").apply { mkdirs() }
        File(dir, "index.html").writeText(
            """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>Bypass IDE — Live Workspace</title>
              <link rel="stylesheet" href="styles.css" />
            </head>
            <body>
              <div class="shell">
                <header class="top">
                  <span class="pill">BYPASS IDE</span>
                  <span id="clock" class="mono">00:00:00</span>
                </header>
                <h1>Interactive Mobile Sandbox</h1>
                <p class="sub">Edit HTML, CSS, and JS in the Code Editor or ask Bypass AI in Agent mode to build any web app live.</p>
                <div class="metrics">
                  <div class="metric">
                    <span class="label">Engine</span>
                    <span class="val cyan">Gemini AI</span>
                  </div>
                  <div class="metric">
                    <span class="label">Runtime</span>
                    <span class="val emerald">WebView</span>
                  </div>
                </div>
                <button id="actionBtn" class="btn">Trigger Telemetry Event</button>
                <div id="logBox" class="log">Ready. Tap button to emit console telemetry.</div>
              </div>
              <script src="app.js"></script>
            </body>
            </html>
            """.trimIndent()
        )
        File(dir, "styles.css").writeText(
            """
            * { box-sizing: border-box; }
            body {
              margin: 0;
              min-height: 100vh;
              background: radial-gradient(circle at top right, #0f2942, #080c14 60%);
              color: #f1f5f9;
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
              display: flex;
              align-items: center;
              justify-content: center;
              padding: 20px;
            }
            .shell {
              width: 100%;
              max-width: 400px;
              background: rgba(15, 23, 42, 0.85);
              border: 1px solid #23324d;
              border-radius: 20px;
              padding: 24px;
              box-shadow: 0 20px 50px rgba(0,0,0,0.45);
            }
            .top {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 16px;
            }
            .pill {
              font-size: 11px;
              font-weight: 700;
              letter-spacing: 0.08em;
              padding: 4px 10px;
              border-radius: 999px;
              background: rgba(34, 211, 238, 0.14);
              color: #22d3ee;
              border: 1px solid rgba(34, 211, 238, 0.35);
            }
            .mono { font-family: monospace; font-size: 12px; color: #94a3b8; }
            h1 { margin: 0 0 8px; font-size: 22px; }
            .sub { margin: 0 0 18px; font-size: 13px; color: #94a3b8; line-height: 1.5; }
            .metrics { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 18px; }
            .metric {
              background: #0b1120;
              border: 1px solid #1e293b;
              border-radius: 12px;
              padding: 12px;
            }
            .label { display: block; font-size: 11px; color: #64748b; margin-bottom: 4px; }
            .val { font-size: 14px; font-weight: 700; }
            .cyan { color: #22d3ee; }
            .emerald { color: #10b981; }
            .btn {
              width: 100%;
              padding: 13px;
              border-radius: 12px;
              border: none;
              background: linear-gradient(135deg, #22d3ee, #10b981);
              color: #080c14;
              font-weight: 700;
              font-size: 14px;
              cursor: pointer;
            }
            .log {
              margin-top: 14px;
              padding: 10px 12px;
              border-radius: 10px;
              background: #05080f;
              border: 1px solid #1e293b;
              font-family: monospace;
              font-size: 12px;
              color: #6ee7b7;
            }
            """.trimIndent()
        )
        File(dir, "app.js").writeText(
            """
            const clockEl = document.getElementById('clock');
            const logBox = document.getElementById('logBox');
            const btn = document.getElementById('actionBtn');
            let clicks = 0;

            function tick() {
              if (clockEl) clockEl.textContent = new Date().toLocaleTimeString();
            }
            tick();
            setInterval(tick, 1000);

            btn?.addEventListener('click', () => {
              clicks += 1;
              const msg = `Telemetry event #${'$'}{clicks} dispatched at ${'$'}{new Date().toLocaleTimeString()}`;
              if (logBox) logBox.textContent = msg;
              console.log(msg);
            });
            """.trimIndent()
        )
    }
}
