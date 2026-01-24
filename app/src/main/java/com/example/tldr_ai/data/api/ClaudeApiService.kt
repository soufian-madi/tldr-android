package com.example.tldr_ai.data.api

import com.example.tldr_ai.BuildConfig
import com.example.tldr_ai.data.model.SummaryResult
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Service for communicating with the Claude API.
 */
class ClaudeApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-sonnet-4-20250514"
        private const val API_VERSION = "2023-06-01"
    }

    /**
     * Sends article text to Claude for summarization and clickbait analysis.
     */
    suspend fun summarize(articleText: String): Result<SummaryResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.CLAUDE_API_KEY
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("API key not configured"))
            }

            val prompt = buildPrompt(articleText)
            val requestBody = ClaudeRequest(
                model = MODEL,
                maxTokens = 1024,
                messages = listOf(
                    Message(role = "user", content = prompt)
                )
            )

            val jsonBody = gson.toJson(requestBody)
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", API_VERSION)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("API error ${response.code}: ${responseBody ?: "Unknown error"}")
                )
            }

            if (responseBody == null) {
                return@withContext Result.failure(Exception("Empty response from API"))
            }

            val claudeResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)
            val content = claudeResponse.content.firstOrNull()?.text
                ?: return@withContext Result.failure(Exception("No content in response"))

            parseResponse(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(articleText: String): String {
        return """
Analyze this article and provide:
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

Respond in this exact JSON format:
{"clickbait_score": <number>, "title": "<extracted or inferred title>", "summary": "<your summary>"}

Article text:
$articleText
        """.trimIndent()
    }

    private fun parseResponse(content: String): Result<SummaryResult> {
        return try {
            // Extract JSON from response (Claude might add extra text)
            val jsonMatch = Regex("""\{[^{}]*"clickbait_score"[^{}]*\}""").find(content)
            val jsonStr = jsonMatch?.value ?: content

            val parsed = gson.fromJson(jsonStr, ClaudeSummaryResponse::class.java)
            Result.success(
                SummaryResult(
                    summary = parsed.summary,
                    clickbaitScore = parsed.clickbaitScore.coerceIn(0, 100),
                    originalTitle = parsed.title
                )
            )
        } catch (e: Exception) {
            // Fallback: treat entire response as summary with default score
            Result.success(
                SummaryResult(
                    summary = content,
                    clickbaitScore = 50,
                    originalTitle = null
                )
            )
        }
    }
}

// Request/Response DTOs

data class ClaudeRequest(
    val model: String,
    @SerializedName("max_tokens") val maxTokens: Int,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class ClaudeResponse(
    val content: List<ContentBlock>
)

data class ContentBlock(
    val type: String,
    val text: String
)

data class ClaudeSummaryResponse(
    @SerializedName("clickbait_score") val clickbaitScore: Int,
    val title: String?,
    val summary: String
)
