package com.example.tldr_ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tldr_ai.ui.screens.MainScreen
import com.example.tldr_ai.ui.theme.TldraiTheme
import com.example.tldr_ai.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract shared text if app was opened via share intent
        val sharedUrl = getSharedText(intent)

        setContent {
            TldraiTheme {
                val viewModel: MainViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                MainScreen(
                    uiState = uiState,
                    initialUrl = sharedUrl,
                    onSummarize = { mode, input ->
                        viewModel.summarize(mode, input)
                    }
                )
            }
        }
    }

    private fun getSharedText(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
            val lines = sharedText.trim().lines()
            // Google News shares "Title\nURL" - extract just the URL
            return if (lines.size == 2) lines[1].trim() else sharedText
        }
        return null
    }
}
