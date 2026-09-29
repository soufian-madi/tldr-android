package com.example.tldr_ai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.data.model.AiModel
import com.example.tldr_ai.data.model.InputMode
import com.example.tldr_ai.data.model.Models
import com.example.tldr_ai.data.model.SummaryUiState
import com.example.tldr_ai.data.model.detectInputMode
import com.example.tldr_ai.ui.components.ClearAction
import com.example.tldr_ai.ui.components.Composer
import com.example.tldr_ai.ui.components.ConceptLegend
import com.example.tldr_ai.ui.components.ErrorView
import com.example.tldr_ai.ui.components.ModelSheet
import com.example.tldr_ai.ui.components.QuietChip
import com.example.tldr_ai.ui.components.SummaryView
import com.example.tldr_ai.ui.components.ThinkingView
import com.example.tldr_ai.ui.theme.WordmarkStyle
import com.example.tldr_ai.ui.theme.palette

@Composable
fun MainScreen(
    uiState: SummaryUiState = SummaryUiState.Idle,
    onSummarize: (InputMode, String) -> Unit = { _, _ -> },
    selectedModel: AiModel = Models.DEFAULT,
    models: List<AiModel> = Models.FALLBACK,
    onModelSelected: (AiModel) -> Unit = {},
    onOpenHistory: () -> Unit = {}
) {
    val context = LocalContext.current

    // Saveable so a rotation or theme change doesn't throw away a pasted article.
    var input by rememberSaveable { mutableStateOf("") }
    var modeOverride by rememberSaveable { mutableStateOf<InputMode?>(null) }
    var showModelSheet by remember { mutableStateOf(false) }

    val isLoading = uiState is SummaryUiState.Loading || uiState is SummaryUiState.Reasoning
    val mode = modeOverride ?: detectInputMode(input)
    val canSubmit = input.isNotBlank() && !isLoading

    fun submit() {
        if (canSubmit) onSummarize(mode, input.trim())
    }

    if (showModelSheet) {
        ModelSheet(
            models = models,
            selected = selectedModel,
            onSelect = {
                onModelSelected(it)
                showModelSheet = false
            },
            onDismiss = { showModelSheet = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Header(onOpenHistory = onOpenHistory)

        // The answer occupies everything between the mark and the composer, and only this
        // region scrolls — the input never slides out from under you mid-stream.
        AnimatedContent(
            targetState = uiState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = {
                (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 14 })
                    .togetherWith(fadeOut(tween(140)))
            },
            contentKey = { state ->
                when (state) {
                    is SummaryUiState.Idle -> "idle"
                    is SummaryUiState.Loading, is SummaryUiState.Reasoning -> "thinking"
                    is SummaryUiState.Success -> "success"
                    is SummaryUiState.Error -> "error"
                }
            },
            label = "result"
        ) { state ->
            // Content is anchored to the bottom so it rises out of the composer: a one-line
            // verdict sits right above the input instead of stranded at the top of a void,
            // and a long summary grows upward and then scrolls.
            BoxWithConstraints {
                val viewport = maxHeight
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = viewport)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                        when (state) {
                        // Idle explains the app rather than pretending to be a result.
                        is SummaryUiState.Idle -> ConceptLegend()

                        is SummaryUiState.Loading -> ThinkingView(
                            label = if (mode == InputMode.URL) "Reading the article" else "Reading",
                            reasoning = ""
                        )

                        is SummaryUiState.Reasoning -> ThinkingView(
                            label = "Thinking",
                            reasoning = state.text
                        )

                        is SummaryUiState.Success -> SummaryView(
                            result = state.result,
                            onCopy = { copyToClipboard(context, state.result.summary) },
                            onShare = { shareText(context, state.result.summary) }
                        )

                        is SummaryUiState.Error -> ErrorView(
                            message = state.message,
                            onRetry = ::submit
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                // The detected mode is shown only once there is something to classify, and it
                // is tappable in case the guess is wrong: information, not a question.
                if (input.isNotBlank()) {
                    QuietChip(
                        label = if (mode == InputMode.URL) "Link" else "Text",
                        leadingIcon = if (mode == InputMode.URL) {
                            Icons.Rounded.Link
                        } else {
                            Icons.AutoMirrored.Rounded.Notes
                        },
                        enabled = !isLoading,
                        onClick = {
                            modeOverride =
                                if (mode == InputMode.URL) InputMode.TEXT else InputMode.URL
                        }
                    )
                }
                QuietChip(
                    label = selectedModel.displayName,
                    trailingIcon = Icons.Rounded.UnfoldMore,
                    enabled = !isLoading,
                    onClick = { showModelSheet = true }
                )
                if (input.isNotBlank() && !isLoading) {
                    Spacer(Modifier.weight(1f))
                    ClearAction(onClear = {
                        input = ""
                        modeOverride = null
                    })
                }
            }

            Composer(
                value = input,
                onValueChange = {
                    input = it
                    if (it.isBlank()) modeOverride = null
                },
                onSubmit = ::submit,
                canSubmit = canSubmit,
                isLoading = isLoading
            )
        }
    }
}

@Composable
private fun Header(onOpenHistory: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = wordmark(), style = WordmarkStyle)
        IconButton(onClick = onOpenHistory) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = "History",
                tint = palette.muted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** The semicolon carries the whole joke, so it gets to be the quiet part of the mark. */
@Composable
fun wordmark() = buildAnnotatedString {
    withStyle(SpanStyle(color = palette.onSurface)) { append("TL") }
    withStyle(SpanStyle(color = palette.muted)) { append(";") }
    withStyle(SpanStyle(color = palette.onSurface)) { append("DR") }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("TL;DR Summary", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share TL;DR"))
}
