package com.example.tldr_ai

import com.example.tldr_ai.data.api.ClaudeApiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaudeApiServiceParsingTest {

    private val service = ClaudeApiService()

    // ---- extractDelta -------------------------------------------------

    @Test
    fun extractDelta_openAiReasoningDelta() {
        val payload = """{"type":"response.reasoning_summary_text.delta","delta":"Thinking about it"}"""

        assertEquals("reason" to "Thinking about it", service.extractDelta(payload))
    }

    @Test
    fun extractDelta_openAiAnswerDelta() {
        val payload = """{"type":"response.output_text.delta","delta":"The answer is 42"}"""

        assertEquals("answer" to "The answer is 42", service.extractDelta(payload))
    }

    @Test
    fun extractDelta_anthropicThinkingDelta() {
        val payload =
            """{"type":"content_block_delta","delta":{"type":"thinking_delta","thinking":"Pondering"}}"""

        assertEquals("reason" to "Pondering", service.extractDelta(payload))
    }

    @Test
    fun extractDelta_anthropicTextDelta() {
        val payload =
            """{"type":"content_block_delta","delta":{"type":"text_delta","text":"Final text"}}"""

        assertEquals("answer" to "Final text", service.extractDelta(payload))
    }

    @Test
    fun extractDelta_unknownTopLevelTypeReturnsNull() {
        val payload = """{"type":"message_stop"}"""

        assertNull(service.extractDelta(payload))
    }

    @Test
    fun extractDelta_unknownContentBlockDeltaInnerTypeReturnsNull() {
        val payload =
            """{"type":"content_block_delta","delta":{"type":"signature_delta","signature":"abc"}}"""

        assertNull(service.extractDelta(payload))
    }

    @Test
    fun extractDelta_nonJsonPayloadReturnsNullWithoutThrowing() {
        assertNull(service.extractDelta("not json"))
    }

    @Test
    fun extractDelta_truncatedJsonReturnsNullWithoutThrowing() {
        assertNull(service.extractDelta("{"))
    }

    // ---- parseResponse -------------------------------------------------

    @Test
    fun parseResponse_wellFormedJsonParsesAllFields() {
        val content = """{"clickbait_score": 42, "title": "Sample Title", "summary": "Sample Summary"}"""

        val result = service.parseResponse(content).getOrThrow()

        assertEquals("Sample Summary", result.summary)
        assertEquals(42, result.clickbaitScore)
        assertEquals("Sample Title", result.originalTitle)
    }

    @Test
    fun parseResponse_stripsJsonFencedMarkdownBlock() {
        val content = "```json\n" +
            """{"clickbait_score": 10, "title": "Fence Title", "summary": "Fence Summary"}""" +
            "\n```"

        val result = service.parseResponse(content).getOrThrow()

        assertEquals("Fence Summary", result.summary)
        assertEquals(10, result.clickbaitScore)
        assertEquals("Fence Title", result.originalTitle)
    }

    @Test
    fun parseResponse_stripsBareFencedMarkdownBlock() {
        val content = "```\n" +
            """{"clickbait_score": 20, "title": "Bare Title", "summary": "Bare Summary"}""" +
            "\n```"

        val result = service.parseResponse(content).getOrThrow()

        assertEquals("Bare Summary", result.summary)
        assertEquals(20, result.clickbaitScore)
        assertEquals("Bare Title", result.originalTitle)
    }

    @Test
    fun parseResponse_coercesScoreAboveRangeToMax() {
        val content = """{"clickbait_score": 150, "title": "T", "summary": "S"}"""

        val result = service.parseResponse(content).getOrThrow()

        assertEquals(100, result.clickbaitScore)
    }

    @Test
    fun parseResponse_coercesScoreBelowRangeToMin() {
        val content = """{"clickbait_score": -5, "title": "T", "summary": "S"}"""

        val result = service.parseResponse(content).getOrThrow()

        assertEquals(0, result.clickbaitScore)
    }

    @Test
    fun parseResponse_regexFallbackRecoversSummaryWithUnescapedQuotes() {
        // The unescaped quotes around "quoted" make this invalid JSON, so the strict Gson
        // parse throws and the regex fallback must locate the fields by their markers instead.
        val content = """{"clickbait_score": 30, "title": "Title", "summary": "This is a "quoted" word"}"""

        val result = service.parseResponse(content).getOrThrow()

        assertEquals(30, result.clickbaitScore)
        assertEquals("Title", result.originalTitle)
        assertEquals("This is a \"quoted\" word", result.summary)
    }

    @Test
    fun parseResponse_missingClickbaitScoreDefaultsToFiftyViaFallback() {
        // No clickbait_score field at all, and the unescaped quotes in the summary force the
        // strict Gson parse to throw, so this must go through the regex fallback, where the
        // absent score defaults to 50.
        val content = """{"title": "T", "summary": "A "bad" summary"}"""

        val result = service.parseResponse(content).getOrThrow()

        assertEquals(50, result.clickbaitScore)
        assertEquals("T", result.originalTitle)
        assertEquals("A \"bad\" summary", result.summary)
    }

    @Test
    fun parseResponse_noSummaryFoundFallsBackToRawContent() {
        val content = "The model returned something unexpected without any structure at all."

        val result = service.parseResponse(content).getOrThrow()

        assertEquals(content, result.summary)
        assertEquals(50, result.clickbaitScore)
        assertNull(result.originalTitle)
    }

    @Test
    fun parseResponse_alwaysReturnsSuccess() {
        // There is no path in parseResponse that returns Result.failure: even totally
        // unstructured input falls back to using the raw content as the summary.
        assertTrue(service.parseResponse("¯\\_(ツ)_/¯").isSuccess)
    }
}
