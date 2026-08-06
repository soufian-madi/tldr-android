package com.example.tldr_ai.data.api

import com.example.tldr_ai.BuildConfig
import com.example.tldr_ai.data.model.AiModel
import com.example.tldr_ai.data.model.ModelFamily
import com.example.tldr_ai.data.model.StreamEvent
import com.example.tldr_ai.data.model.SummaryResult
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Service for communicating with the Claude API.
 */
class ClaudeApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Streaming responses can stay open far longer than a single read; disable the read
    // timeout so long reasoning streams aren't aborted mid-flight.
    private val streamClient = client.newBuilder()
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Fetches content from a URL and returns preprocessed HTML.
     */
    suspend fun fetchUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile) TL;DR App")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to fetch URL: ${response.code}")
                )
            }

            if (body.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Empty response from URL"))
            }

            Result.success(preprocessHtml(body))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch URL: ${e.message}"))
        }
    }

    /**
     * Preprocesses HTML to reduce token usage by removing non-content elements.
     */
    internal fun preprocessHtml(html: String): String {
        var cleaned = html

        // Remove script tags and content
        cleaned = cleaned.replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")

        // Remove style tags and content
        cleaned = cleaned.replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")

        // Remove navigation elements
        cleaned = cleaned.replace(Regex("<nav[^>]*>[\\s\\S]*?</nav>", RegexOption.IGNORE_CASE), "")

        // Remove header elements (site headers, not article headers)
        cleaned = cleaned.replace(Regex("<header[^>]*>[\\s\\S]*?</header>", RegexOption.IGNORE_CASE), "")

        // Remove footer elements
        cleaned = cleaned.replace(Regex("<footer[^>]*>[\\s\\S]*?</footer>", RegexOption.IGNORE_CASE), "")

        // Remove aside elements (sidebars)
        cleaned = cleaned.replace(Regex("<aside[^>]*>[\\s\\S]*?</aside>", RegexOption.IGNORE_CASE), "")

        // Remove HTML comments
        cleaned = cleaned.replace(Regex("<!--[\\s\\S]*?-->"), "")

        // Remove SVG elements
        cleaned = cleaned.replace(Regex("<svg[^>]*>[\\s\\S]*?</svg>", RegexOption.IGNORE_CASE), "")

        // Remove noscript tags
        cleaned = cleaned.replace(Regex("<noscript[^>]*>[\\s\\S]*?</noscript>", RegexOption.IGNORE_CASE), "")

        // Remove form elements
        cleaned = cleaned.replace(Regex("<form[^>]*>[\\s\\S]*?</form>", RegexOption.IGNORE_CASE), "")

        // Remove iframe elements
        cleaned = cleaned.replace(Regex("<iframe[^>]*>[\\s\\S]*?</iframe>", RegexOption.IGNORE_CASE), "")

        // Strip all remaining tags (attributes like class/style/data-*/href are the bulk of
        // the token cost by this point; only the text between tags carries meaning), keeping
        // a space so words from adjacent block-level elements don't run together.
        cleaned = cleaned.replace(Regex("<[^>]+>"), " ")

        // Decode the handful of HTML entities that actually show up in article text.
        cleaned = cleaned
            .replace(Regex("&#(\\d+);")) { it.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: it.value }
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")

        // Remove excessive whitespace
        cleaned = cleaned.replace(Regex("\\s+"), " ")

        return cleaned.trim()
    }

    /**
     * Streams a summary for the given article text using the selected model.
     *
     * Routes by model family: OpenAI models hit /v1/responses (and stream a reasoning
     * summary), Anthropic models hit /chat/completions (answer only). Reasoning deltas are
     * emitted live; the answer is accumulated and parsed into a [SummaryResult] at the end.
     */
    fun summarizeStream(articleText: String, model: AiModel): Flow<StreamEvent> = flow {
        val apiKey = BuildConfig.CLAUDE_API_KEY
        if (apiKey.isBlank()) {
            emit(StreamEvent.Failure("API key not configured"))
            return@flow
        }

        val prompt = buildPrompt(articleText)
        val (endpoint, jsonBody) = when (model.family) {
            ModelFamily.OPENAI -> "$GATEWAY/v1/responses" to gson.toJson(
                ResponsesRequest(
                    model = model.id,
                    stream = true,
                    reasoning = ReasoningCfg(effort = "medium", summary = "auto"),
                    input = prompt
                )
            )

            ModelFamily.ANTHROPIC -> "$GATEWAY/chat/completions" to gson.toJson(
                ChatRequest(
                    model = model.id,
                    stream = true,
                    maxTokens = 1024,
                    messages = listOf(Message(role = "user", content = prompt))
                )
            )
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-api-key", apiKey)
            .addHeader("x-model", model.id)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            streamClient.newCall(request).execute().use { resp ->
                val body = resp.body
                if (!resp.isSuccessful || body == null) {
                    emit(StreamEvent.Failure("API error ${resp.code}"))
                    return@use
                }

                val source = body.source()
                val answer = StringBuilder()

                while (coroutineContext.isActive) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isEmpty()) continue
                    if (payload == "[DONE]") break

                    val delta = extractDelta(payload) ?: continue
                    when (delta.first) {
                        "reason" -> emit(StreamEvent.Reasoning(delta.second))
                        "answer" -> answer.append(delta.second)
                    }
                }

                emit(
                    parseResponse(answer.toString()).fold(
                        onSuccess = { StreamEvent.Done(it) },
                        onFailure = { StreamEvent.Failure(it.message ?: "Failed to parse response") }
                    )
                )
            }
        } catch (e: Exception) {
            emit(StreamEvent.Failure(e.message ?: "Streaming failed"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Normalizes a single SSE `data:` payload from either backend into a
     * (kind, text) pair where kind is "reason" or "answer". Returns null for housekeeping
     * events (signatures, lifecycle markers, errors). Mirrors the jq filter in chat.sh.
     */
    internal fun extractDelta(payload: String): Pair<String, String>? {
        return try {
            val obj = JsonParser.parseString(payload).asJsonObject
            when (obj.get("type")?.asString) {
                // OpenAI /v1/responses events
                "response.reasoning_summary_text.delta" ->
                    "reason" to (obj.get("delta")?.asString ?: "")
                "response.output_text.delta" ->
                    "answer" to (obj.get("delta")?.asString ?: "")
                // Anthropic native streaming events (/chat/completions)
                "content_block_delta" -> {
                    val d = obj.getAsJsonObject("delta")
                    when (d?.get("type")?.asString) {
                        "thinking_delta" -> "reason" to (d.get("thinking")?.asString ?: "")
                        "text_delta" -> "answer" to (d.get("text")?.asString ?: "")
                        else -> null
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildPrompt(articleText: String): String {
        return """
Analyze this article content and provide:
1. A clickbait score from 0-100 (0 = purely informative, 100 = extreme clickbait)
2. A summary whose length depends on the clickbait score:
   - Score 0-30 (informative): Provide a short paragraph or bullet points
   - Score 31-60 (moderate clickbait): 2-3 sentences
   - Score 61-100 (high clickbait): Just 1 short sentence with the key fact

Clickbait signals to look for:
- Excessive capitalization or punctuation (!!!, ???)
- Sensationalist words ("shocking", "unbelievable", "you won't believe")
- Withholding key information that could be stated simply
- Question headlines with obvious answers
- "This one trick" / "what happened next" patterns
- the title does not reflect the essence of the content

Note: The content is plain text stripped from a webpage and may still contain leftover navigation, ads, or boilerplate text. Extract and focus on the main article content, ignoring that boilerplate.

Summery structure: one important goal is to answer questions that may arise when reading the title. if the title already raises a question it must be answered in the summary, otherwise
try to articulate (to yourself) the most probable question a user WOULD have upon reading this title and answer it in the summery.

Paywall handling: If the article content appears to be behind a paywall (login required, subscription needed, truncated content with "continue reading" prompts), set clickbait_score to 100 and summary to "Paywall". Do not attempt to summarize paywalled content.
Language handling: Respect the original language of the content. if it is in german summerize in german, if it is in english summerize in english

Respond with ONLY this JSON (no markdown, no code blocks, no extra text):
{"clickbait_score": <number>, "title": "<extracted or inferred title>", "summary": "<your summary>"}

Important: In the title field, replace any „ or " characters with backticks (`)

Article content:
$articleText
        """.trimIndent()
    }

    internal fun parseResponse(content: String): Result<SummaryResult> {
        var cleaned = content.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("\n").substringBeforeLast("```").trim()
        }

        val jsonStart = cleaned.indexOf('{')
        val jsonEnd = cleaned.lastIndexOf('}')
        val jsonStr = if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleaned.substring(jsonStart, jsonEnd + 1)
        } else cleaned

        // Try standard JSON parse first
        try {
            val parsed = gson.fromJson(jsonStr, ClaudeSummaryResponse::class.java)
            if (parsed?.summary != null) {
                return Result.success(
                    SummaryResult(
                        summary = parsed.summary,
                        clickbaitScore = parsed.clickbaitScore.coerceIn(0, 100),
                        originalTitle = parsed.title
                    )
                )
            }
        } catch (_: Exception) { /* fall through to regex extraction */ }

        // Regex fallback for malformed JSON (e.g. unescaped quotes inside string values).
        // The model always produces fields in order: clickbait_score → title → summary.
        val score = Regex(""""clickbait_score"\s*:\s*(\d+)""")
            .find(jsonStr)?.groupValues?.get(1)?.toIntOrNull() ?: 50

        // Title: text between "title": " and the closing delimiter before "summary"
        val titleValueStart = Regex(""""title"\s*:\s*"""").find(jsonStr)?.range?.last?.plus(1)
        val summaryKeyPos = Regex("""",\s*"summary"\s*:""").find(jsonStr)?.range?.first
        val title = if (titleValueStart != null && summaryKeyPos != null && summaryKeyPos > titleValueStart) {
            jsonStr.substring(titleValueStart, summaryKeyPos).takeIf { it.isNotBlank() }
        } else null

        // Summary: everything after "summary": " up to the closing "} at the end
        val summaryValueStart = Regex(""""summary"\s*:\s*"""").find(jsonStr)?.range?.last?.plus(1)
        val summary = if (summaryValueStart != null) {
            jsonStr.substring(summaryValueStart)
                .trimEnd()
                .let { if (it.endsWith('}')) it.dropLast(1).trimEnd() else it }
                .let { if (it.endsWith('"')) it.dropLast(1) else it }
                .takeIf { it.isNotBlank() }
        } else null

        return Result.success(
            SummaryResult(
                summary = summary ?: content,
                clickbaitScore = score.coerceIn(0, 100),
                originalTitle = title
            )
        )
    }
}

// Request/Response DTOs

/** OpenAI /v1/responses request body. */
data class ResponsesRequest(
    val model: String,
    val stream: Boolean,
    val reasoning: ReasoningCfg,
    val input: String
)

data class ReasoningCfg(
    val effort: String,
    val summary: String
)

/** Anthropic /chat/completions request body. */
data class ChatRequest(
    val model: String,
    val stream: Boolean,
    @SerializedName("max_tokens") val maxTokens: Int,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class ClaudeSummaryResponse(
    @SerializedName("clickbait_score") val clickbaitScore: Int,
    val title: String?,
    val summary: String
)
