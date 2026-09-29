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

private val URL_SHAPED = Regex(
    "^(https?://)?([\\w-]+\\.)+[a-z]{2,}(:\\d+)?([/?#]\\S*)?$",
    RegexOption.IGNORE_CASE
)

/**
 * Guesses whether the user handed us a link or an article body, so they never have to pick a
 * mode. Anything containing whitespace is prose; a single URL-shaped token is a link.
 */
fun detectInputMode(raw: String): InputMode {
    val trimmed = raw.trim()
    if (trimmed.isEmpty() || trimmed.any(Char::isWhitespace)) return InputMode.TEXT
    return if (URL_SHAPED.matches(trimmed)) InputMode.URL else InputMode.TEXT
}

/** Adds the scheme a pasted `example.com/article` is missing, so OkHttp can parse it. */
fun normalizeUrl(raw: String): String {
    val trimmed = raw.trim()
    return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

/**
 * UI state for the main screen.
 */
sealed class SummaryUiState {
    data object Idle : SummaryUiState()
    data object Loading : SummaryUiState()                    // fetching / waiting for first token
    data class Reasoning(val text: String) : SummaryUiState() // faint live reasoning (gpt-* only)
    data class Success(val result: SummaryResult) : SummaryUiState()
    data class Error(val message: String) : SummaryUiState()
}

/**
 * Events emitted while streaming a summary response from the gateway.
 *
 * Reasoning deltas are only produced by OpenAI models; the answer (the JSON payload) is
 * accumulated internally and surfaced as the parsed [SummaryResult] in [Done].
 */
sealed class StreamEvent {
    data class Reasoning(val delta: String) : StreamEvent()
    data class Done(val result: SummaryResult) : StreamEvent()
    data class Failure(val message: String) : StreamEvent()
}
