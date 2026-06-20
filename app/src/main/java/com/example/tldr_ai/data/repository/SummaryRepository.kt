package com.example.tldr_ai.data.repository

import com.example.tldr_ai.data.api.ClaudeApiService
import com.example.tldr_ai.data.model.AiModel
import com.example.tldr_ai.data.model.StreamEvent
import kotlinx.coroutines.flow.Flow

/**
 * Repository responsible for generating summaries via the AI gateway.
 */
class SummaryRepository(
    private val apiService: ClaudeApiService = ClaudeApiService()
) {

    /**
     * Fetches a URL and returns its preprocessed HTML content. Exposed so callers can show
     * a distinct "fetching" phase before streaming begins.
     */
    suspend fun fetch(url: String): Result<String> {
        if (url.isBlank()) {
            return Result.failure(IllegalArgumentException("URL cannot be empty"))
        }
        return apiService.fetchUrl(url)
    }

    /**
     * Streams a summary for the given article text using [model].
     */
    fun summarizeStream(text: String, model: AiModel): Flow<StreamEvent> =
        apiService.summarizeStream(text, model)
}
