package com.bypass.ide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

data class CommandExecutionResult(
    val output: String,
    val exitCode: Int
)

class TerminalExecutor {

    suspend fun execute(command: String, workingDir: File): CommandExecutionResult =
        withContext(Dispatchers.IO) {
            val trimmed = command.trim()
            if (trimmed.isBlank()) {
                return@withContext CommandExecutionResult("", 0)
            }
            if (!workingDir.exists()) {
                workingDir.mkdirs()
            }

            runCatching {
                val process = ProcessBuilder("sh", "-c", trimmed)
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .start()

                val finished = process.waitFor(12, TimeUnit.SECONDS)
                if (!finished) {
                    process.destroyForcibly()
                    return@withContext CommandExecutionResult(
                        "Command timed out after 12s: $trimmed",
                        124
                    )
                }

                val text = process.inputStream.bufferedReader().use { it.readText() }
                    .replace(Regex("\u001B\\[[;\\d]*m"), "")
                    .trimEnd()
                    .take(12_000)

                CommandExecutionResult(
                    output = text.ifBlank { "(exit ${process.exitValue()})" },
                    exitCode = process.exitValue()
                )
            }.getOrElse { err ->
                CommandExecutionResult(
                    output = "Command execution error: ${err.localizedMessage ?: "Unable to launch shell"}",
                    exitCode = 1
                )
            }
        }
}
