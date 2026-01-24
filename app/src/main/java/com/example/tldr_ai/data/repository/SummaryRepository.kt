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

    /**
     * Fetches article from URL, preprocesses HTML, and summarizes using Claude API.
     */
    suspend fun summarizeFromUrl(url: String): Result<SummaryResult> {
        if (url.isBlank()) {
            return Result.failure(IllegalArgumentException("URL cannot be empty"))
        }

        // Fetch and preprocess the URL content
        val fetchResult = apiService.fetchUrl(url)
        if (fetchResult.isFailure) {
            return Result.failure(fetchResult.exceptionOrNull() ?: Exception("Failed to fetch URL"))
        }

        val htmlContent = fetchResult.getOrThrow()
        return apiService.summarize(htmlContent)
    }
}
