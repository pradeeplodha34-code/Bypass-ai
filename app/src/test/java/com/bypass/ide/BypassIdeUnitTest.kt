package com.bypass.ide

import com.bypass.ide.ai.GeminiAgentEngine
import com.bypass.ide.model.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BypassIdeUnitTest {

    @Test
    fun sanitizeHistoryTurns_dropsErrorsLeadingModelAndTrailingUser() {
        val raw = listOf(
            ChatMessage(id = "1", isUser = false, text = "Old assistant greeting"),
            ChatMessage(id = "2", isUser = true, text = "Hello"),
            ChatMessage(id = "3", isUser = false, text = "Failed error", isError = true),
            ChatMessage(id = "4", isUser = true, text = "Are you there?"),
            ChatMessage(id = "5", isUser = false, text = "Yes, ready to help!"),
            ChatMessage(id = "6", isUser = true, text = "Trailing unsent user msg")
        )

        val sanitized = GeminiAgentEngine.sanitizeHistoryTurns(raw)

        assertEquals(2, sanitized.size)
        assertEquals("user" to "Hello\n\nAre you there?", sanitized[0])
        assertEquals("model" to "Yes, ready to help!", sanitized[1])
    }

    @Test
    fun sanitizeHistoryTurns_returnsEmptyWhenOnlyUserOrErrorMessages() {
        val raw = listOf(
            ChatMessage(id = "1", isUser = false, text = "Boom", isError = true),
            ChatMessage(id = "2", isUser = true, text = "Single user turn")
        )

        val sanitized = GeminiAgentEngine.sanitizeHistoryTurns(raw)
        assertEquals(0, sanitized.size)
    }

    @Test
    fun extractSseText_parsesValidGeminiSseLine() {
        val sseLine = """data: {"candidates":[{"content":{"parts":[{"text":"Hello "},{"text":"World!"}]}}]}"""
        assertEquals("Hello World!", GeminiAgentEngine.extractSseText(sseLine))
    }

    @Test
    fun extractSseText_ignoresDoneAndMalformedLines() {
        assertNull(GeminiAgentEngine.extractSseText("data: [DONE]"))
        assertNull(GeminiAgentEngine.extractSseText(": keep-alive"))
        assertNull(GeminiAgentEngine.extractSseText("data: {invalid json"))
    }

    @Test
    fun extractApiErrorMessage_extractsJsonErrorMessage() {
        val errJson = """{"error":{"code":400,"message":"API key not valid."}}"""
        assertEquals(
            "Gemini API error (400): API key not valid.",
            GeminiAgentEngine.extractApiErrorMessage(errJson, 400)
        )
    }
}
