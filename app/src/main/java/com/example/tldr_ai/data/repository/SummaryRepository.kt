package com.example.tldr_ai.data.repository

import com.example.tldr_ai.data.api.ClaudeApiService
import com.example.tldr_ai.data.model.SummaryResult

/**
 * Repository responsible for generating summaries via Claude API.
 */
class SummaryRepository(
    private val apiService: ClaudeApiService = ClaudeApiService()
) {

    /**
     * Summarizes provided article text using Claude API.
     * The API analyzes clickbait level and adjusts summary length accordingly.
     */
    suspend fun summarizeFromText(text: String): Result<SummaryResult> {
        if (text.isBlank()) {
            return Result.failure(IllegalArgumentException("Article text cannot be empty"))
        }
        return apiService.summarize(text)
    }
}
