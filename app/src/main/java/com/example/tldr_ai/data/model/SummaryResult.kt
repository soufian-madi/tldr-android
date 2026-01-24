package com.example.tldr_ai.data.model

/**
 * Represents the result of article summarization.
 *
 * @param summary The AI-generated summary text
 * @param clickbaitScore Score from 0-100 indicating how clickbaity the article is
 *                       Higher score = more clickbait = shorter summary
 * @param originalTitle The original article title (if available)
 */
data class SummaryResult(
    val summary: String,
    val clickbaitScore: Int, // 0-100
    val originalTitle: String? = null
)

/**
 * Represents the different input modes for the app.
 */
enum class InputMode {
    URL,
    TEXT
}

/**
 * UI state for the main screen.
 */
sealed class SummaryUiState {
    data object Idle : SummaryUiState()
    data object Loading : SummaryUiState()
    data class Success(val result: SummaryResult) : SummaryUiState()
    data class Error(val message: String) : SummaryUiState()
}
