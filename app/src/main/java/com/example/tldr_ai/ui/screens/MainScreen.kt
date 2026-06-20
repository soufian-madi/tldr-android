package com.example.tldr_ai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.data.model.AiModel
import com.example.tldr_ai.data.model.InputMode
import com.example.tldr_ai.data.model.Models
import com.example.tldr_ai.data.model.SummaryResult
import com.example.tldr_ai.data.model.SummaryUiState
import com.example.tldr_ai.ui.components.InputSection
import com.example.tldr_ai.ui.components.SummaryCard
import com.example.tldr_ai.ui.theme.TldraiTheme

@Composable
fun MainScreen(
    uiState: SummaryUiState = SummaryUiState.Idle,
    onSummarize: (InputMode, String) -> Unit = { _, _ -> },
    selectedModel: AiModel = Models.DEFAULT,
    models: List<AiModel> = Models.ALL,
    onModelSelected: (AiModel) -> Unit = {},
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current

    var inputMode by remember { mutableStateOf(InputMode.URL) }
    var urlInput by remember { mutableStateOf("") }
    var textInput by remember { mutableStateOf("") }
    var modelMenuExpanded by remember { mutableStateOf(false) }

    val isLoading = uiState is SummaryUiState.Loading || uiState is SummaryUiState.Reasoning
    val currentInput = if (inputMode == InputMode.URL) urlInput else textInput
    val canSubmit = currentInput.isNotBlank() && !isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding, bottom = bottomPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        InputSection(
            inputMode = inputMode,
            onInputModeChange = { inputMode = it },
            urlValue = urlInput,
            onUrlChange = { urlInput = it },
            textValue = textInput,
            onTextChange = { textInput = it },
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Model picker
        Box {
            OutlinedButton(
                onClick = { modelMenuExpanded = true },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedModel.displayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select model"
                )
            }
            DropdownMenu(
                expanded = modelMenuExpanded,
                onDismissRequest = { modelMenuExpanded = false }
            ) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.displayName) },
                        onClick = {
                            onModelSelected(model)
                            modelMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSummarize(inputMode, currentInput) },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit
        ) {
            Text(
                text = if (isLoading) "Summarizing..." else "Get TL;DR",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (uiState) {
            is SummaryUiState.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Paste a URL or article text to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is SummaryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is SummaryUiState.Reasoning -> {
                // Faint live reasoning while the model thinks; replaced by the card on success.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = uiState.text,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            is SummaryUiState.Success -> {
                SummaryCard(
                    result = uiState.result,
                    onCopyClick = {
                        copyToClipboard(context, uiState.result.summary)
                    },
                    onShareClick = {
                        shareText(context, uiState.result.summary)
                    }
                )
            }

            is SummaryUiState.Error -> {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("TL;DR Summary", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share TL;DR"))
}

@Preview(showBackground = true)
@Composable
private fun MainScreenIdlePreview() {
    TldraiTheme {
        MainScreen(uiState = SummaryUiState.Idle)
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenLoadingPreview() {
    TldraiTheme {
        MainScreen(uiState = SummaryUiState.Loading)
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenReasoningPreview() {
    TldraiTheme {
        MainScreen(uiState = SummaryUiState.Reasoning("Analyzing the headline for clickbait signals..."))
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenSuccessPreview() {
    TldraiTheme {
        MainScreen(
            uiState = SummaryUiState.Success(
                SummaryResult(
                    summary = "Elon Musk bought Twitter.",
                    clickbaitScore = 92,
                    originalTitle = "You Won't BELIEVE Who Just Bought Twitter!"
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenErrorPreview() {
    TldraiTheme {
        MainScreen(uiState = SummaryUiState.Error("Failed to fetch article. Please check the URL."))
    }
}
