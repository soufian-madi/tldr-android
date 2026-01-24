package com.example.tldr_ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.tldr_ai.data.model.InputMode
import com.example.tldr_ai.data.model.SummaryResult
import com.example.tldr_ai.data.model.SummaryUiState
import com.example.tldr_ai.ui.screens.MainScreen
import com.example.tldr_ai.ui.theme.TldraiTheme

/**
 * Main entry point for the TL;DR app.
 *
 * TODO: Phase 2 - Add ViewModel integration
 * - Create MainViewModel
 * - Use viewModel() or hiltViewModel() to inject
 * - Observe UI state from ViewModel
 * - Handle summarization in ViewModel with coroutines
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TldraiTheme {
                // TODO: Phase 2 - Replace with ViewModel state
                // val viewModel: MainViewModel = viewModel()
                // val uiState by viewModel.uiState.collectAsState()

                // Temporary mock state for UI development
                var uiState by remember { mutableStateOf<SummaryUiState>(SummaryUiState.Idle) }

                MainScreen(
                    uiState = uiState,
                    onSummarize = { mode, input ->
                        // TODO: Phase 2 - Call viewModel.summarize(mode, input)
                        // For now, show a mock result to demonstrate UI
                        uiState = SummaryUiState.Success(
                            SummaryResult(
                                summary = when {
                                    // Mock: detect clickbait-like input and return short response
                                    input.contains("won't believe", ignoreCase = true) ||
                                    input.contains("shocking", ignoreCase = true) ->
                                        "The article's main point in one sentence."

                                    else ->
                                        "Here's a summary of the article:\n\n" +
                                        "- Main point one\n" +
                                        "- Main point two\n" +
                                        "- Main point three"
                                },
                                clickbaitScore = if (input.contains("won't believe", ignoreCase = true)) 85 else 25,
                                originalTitle = if (mode == InputMode.URL) "Article from: $input" else null
                            )
                        )
                    }
                )
            }
        }
    }
}
